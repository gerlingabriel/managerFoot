package br.com.managerfoot.web;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.persistencia.BancoEditavelRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.DecisaoPendenteException;
import br.com.managerfoot.persistencia.BancoRecusadoException;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.JogoGravado;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.NovoJogo;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.Rede;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio;
import br.com.managerfoot.persistencia.TurnoRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;
import br.com.managerfoot.web.BancoEditavelController.BancoEnviado;
import jakarta.servlet.http.HttpSession;

/** Início, novo jogo por sorteio (o caminho da escolha está em {@link EscolhaClubeController}) e abertura de um jogo gravado (SPEC §3.2 e §10.5, rev. 28 e 29). */
@Controller
class JogosController {

	private static final SecureRandom SEMENTES = new SecureRandom();
	private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

	private final JogosDoTreinadorRepositorio jogos;
	private final MoldeRepositorio moldes;
	private final Campeonatos campeonatos;
	private final BancoEditavelRepositorio bancos;
	private final Clock relogio;
	private final int candidatosClubeInicial;
	private final ContasRepositorio contas;
	private final boolean urlSoNesteComputador;
	private final TurnoRepositorio turnos;
	private final RodadaAoVivoRepositorio aoVivo;

	JogosController(JogosDoTreinadorRepositorio jogos, MoldeRepositorio moldes, Campeonatos campeonatos, BancoEditavelRepositorio bancos,
			Clock relogio, @Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial,
			ContasRepositorio contas, @Value("${managerfoot.url-base}") String urlBase, TurnoRepositorio turnos,
			RodadaAoVivoRepositorio aoVivo) {
		this.turnos = turnos;
		this.aoVivo = aoVivo;
		this.contas = contas;
		this.urlSoNesteComputador = ConvitesController.soNesteComputador(urlBase);
		this.relogio = relogio;
		this.jogos = jogos;
		this.moldes = moldes;
		this.campeonatos = campeonatos;
		this.bancos = bancos;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	@GetMapping("/")
	String inicio(@AuthenticationPrincipal Usuario usuario, @RequestParam(name = "apagar", required = false) Long apagar,
			@RequestParam(name = "sair", required = false) Long sair, Model model) {
		model.addAttribute("usuario", usuario);
		List<Linha> linhas = jogos.listar(usuario.id()).stream().map(j -> linha(j, usuario.id())).toList();
		model.addAttribute("jogos", linhas);
		model.addAttribute("salasSemClube", jogos.salasSemClube(usuario.id()));
		if (usuario.papel() == Papel.ADMINISTRADOR) {
			// "Convidar pessoas" (modelo 20c)
			model.addAttribute("convites", contas.convites(usuario.id()));
			model.addAttribute("urlSoNesteComputador", urlSoNesteComputador);
			model.addAttribute("diasLink", contas.validadeLink().toDays());
		}
		// janela de confirmação do modelo 16b; id de jogo que não é do usuário não abre nada
		model.addAttribute("apagando", linhas.stream().filter(l -> l.podeApagar() && Long.valueOf(l.jogoId()).equals(apagar)).findFirst().orElse(null));
		// confirmação de "Sair da gravação" (SPEC §11.4, rev. 129), só do convidado no jogo em rede começado
		model.addAttribute("saindo", linhas.stream().filter(l -> l.podeSair() && Long.valueOf(l.jogoId()).equals(sair)).findFirst().orElse(null));
		return "inicio";
	}

	/** O convidado sai de vez do jogo em rede começado; o clube passa à máquina (SPEC §11.4, rev. 129). */
	@PostMapping("/jogos/{jogoId}/rede/sair")
	String sairDaGravacao(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		String clube = jogos.listar(usuario.id()).stream().filter(j -> j.jogoId() == jogoId).findFirst()
			.map(j -> j.clube().nome()).orElse("");
		try {
			turnos.sairDaGravacao(jogoId, usuario.id(), SEMENTES.nextLong(), relogio.instant());
			redirect.addFlashAttribute("resultado", "Você saiu do jogo do " + clube + ". O clube agora é da máquina.");
		}
		catch (IllegalArgumentException | DecisaoPendenteException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		return "redirect:/";
	}

	@GetMapping("/jogos/novo")
	String novo(@AuthenticationPrincipal Usuario usuario, HttpSession sessao, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("candidatos", candidatosClubeInicial);
		Optional<BancoEnviado> enviado = bancoEnviado(sessao);
		if (enviado.isPresent()) {
			try {
				model.addAttribute("campeonatos", bancos.ler(enviado.get().conteudo()).comLiga().stream()
					.map(p -> new Cartao(p.codigo(), p.codigo(), p.nome(), p.liga().orElseThrow().nome(), detalhe(p.liga().orElseThrow())))
					.toList());
				model.addAttribute("bancoEnviado", enviado.get().nome());
				return "novo-jogo";
			}
			catch (BancoRecusadoException e) {
				// o arquivo só entra na sessão depois de aceito; recusado agora, volta ao banco padrão
				sessao.removeAttribute(BancoEditavelController.SESSAO);
			}
		}
		model.addAttribute("campeonatos", campeonatos.disponiveis().stream().map(this::cartao).toList());
		return "novo-jogo";
	}

	@PostMapping("/jogos")
	String criar(@AuthenticationPrincipal Usuario usuario, @RequestParam(name = "paisId", required = false) Long paisId,
			@RequestParam(name = "paisCodigo", required = false) String paisCodigo,
			@RequestParam(name = "emRede", defaultValue = "false") boolean emRede, HttpSession sessao, Model model,
			RedirectAttributes redirect) {
		// só o administrador cria jogo em rede (SPEC §3.1); o repositório confere de novo
		boolean rede = emRede && usuario.papel() == Papel.ADMINISTRADOR;
		Optional<BancoEnviado> enviado = bancoEnviado(sessao);
		NovoJogo novo;
		if (enviado.isPresent()) {
			try {
				novo = bancos.criarJogo(enviado.get().conteudo(), paisCodigo == null ? "" : paisCodigo, usuario.id(),
						rede ? relogio.instant() : null);
			}
			catch (BancoRecusadoException | NoSuchElementException e) {
				model.addAttribute("erro", "Escolha um dos campeonatos da lista.");
				return novo(usuario, sessao, model);
			}
			sessao.removeAttribute(BancoEditavelController.SESSAO);
		} else {
			if (paisId == null || campeonatos.buscar(paisId).isEmpty()) {
				model.addAttribute("erro", "Escolha um dos campeonatos da lista.");
				return novo(usuario, sessao, model);
			}
			novo = rede ? jogos.criarEmRedeComSorteio(paisId, usuario.id(), relogio.instant())
					: jogos.criarComSorteio(paisId, usuario.id());
		}
		if (rede) {
			return "redirect:/jogos/" + novo.jogoId() + "/sala";
		}
		redirect.addFlashAttribute("sorteadoEntre",
				novo.candidatos().stream().map(Clube::nome).collect(Collectors.joining(", ")));
		return "redirect:/jogos/" + novo.jogoId();
	}

	@GetMapping("/jogos/{jogoId}")
	String abrir(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, Model model) {
		JogoGravado jogo = jogos.abrir(jogoId, usuario.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogo", linha(jogo, usuario.id()));
		return "jogo";
	}

	private static Optional<BancoEnviado> bancoEnviado(HttpSession sessao) {
		return sessao.getAttribute(BancoEditavelController.SESSAO) instanceof BancoEnviado b ? Optional.of(b) : Optional.empty();
	}

	private Cartao cartao(CampeonatoDisponivel c) {
		return new Cartao(String.valueOf(c.paisId()), c.codigoPais(), c.nomePais(), c.nomeLiga(), detalhe(moldes.carregarFormato(c.paisId())));
	}

	/** "3 divisões · 20, 20 e 20 clubes · 4 sobem e 4 descem" (a última parte só se for igual em todas). */
	static String detalhe(FormatoLiga formato) {
		List<FormatoDivisao> divisoes = formato.divisoes();
		List<String> quantidades = divisoes.stream().map(d -> String.valueOf(d.quantidadeClubes())).toList();
		String clubes = quantidades.size() == 1 ? quantidades.getFirst()
				: String.join(", ", quantidades.subList(0, quantidades.size() - 1)) + " e " + quantidades.getLast();
		String texto = divisoes.size() + (divisoes.size() == 1 ? " divisão · " : " divisões · ") + clubes + " clubes";
		List<Integer> trocas = divisoes.subList(0, Math.max(0, divisoes.size() - 1)).stream()
			.map(FormatoDivisao::rebaixados).distinct().toList();
		if (trocas.size() == 1 && trocas.getFirst() > 0) {
			texto += " · " + trocas.getFirst() + " sobem e " + trocas.getFirst() + " descem";
		}
		return texto;
	}

	private Linha linha(JogoGravado j, long usuarioId) {
		String divisao = j.divisao() == null ? "Fila de espera" : j.divisao();
		String onde = j.pais() + " · " + divisao + " · temporada " + j.ano() + ", rodada " + j.rodada();
		String quando = "Jogado em " + QUANDO.format(j.ultimoAcesso().atZone(ZoneId.systemDefault()));
		if (j.rede().isEmpty()) {
			return new Linha(j.jogoId(), j.clube(), divisao, onde, quando, false, false, true, false);
		}
		Rede r = j.rede().get();
		// modelo 18a: o selo, de quem é o jogo e o que ele espera
		if (r.naSala()) {
			return new Linha(j.jogoId(), j.clube(), divisao, "Sala de " + r.administrador() + " · " + j.pais()
					+ " · aguardando o início da temporada", r.treinadores() + (r.treinadores() == 1 ? " treinador" : " treinadores")
					+ " na sala", true, true, r.souAdministrador(), false);
		}
		return new Linha(j.jogoId(), j.clube(), divisao, "Jogo de " + r.administrador() + " · " + onde,
				esperando(j.jogoId(), usuarioId).orElse(quando), true, false, r.souAdministrador(), !r.souAdministrador());
	}

	/** O que o jogo em rede começado está esperando (modelo 18a): o OK de quem vê, o dos outros ou a rodada. */
	private Optional<String> esperando(long jogoId, long usuarioId) {
		Optional<Turno> turno = turnos.turno(jogoId);
		if (turno.isPresent()) {
			return Optional.of(turno.get().deuOk(usuarioId) ? "Você deu OK · aguardando " + turno.get().faltam()
					: "Aguardando o seu OK");
		}
		return aoVivo.emAndamento(jogoId) ? Optional.of("Rodada ao vivo") : Optional.empty();
	}

	/**
	 * Campeonato como cartão da faixa com rolagem lateral.
	 *
	 * @param valor o id do país no banco padrão, ou o código do país num arquivo enviado
	 */
	record Cartao(String valor, String codigo, String pais, String liga, String detalhe) {
	}

	/**
	 * Jogo como aparece na lista do início e no topo do jogo.
	 *
	 * @param naSala jogo em rede que ainda não começou: o botão abre a sala
	 * @param podeApagar no jogo em rede, só o administrador apaga (SPEC §3.3, rev. 92)
	 * @param podeSair o convidado sai de vez do jogo em rede começado (SPEC §11.4, rev. 129)
	 */
	record Linha(long jogoId, Clube clube, String divisao, String onde, String quando, boolean emRede, boolean naSala,
			boolean podeApagar, boolean podeSair) {
	}
}
