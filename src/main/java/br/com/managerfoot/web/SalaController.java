package br.com.managerfoot.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.ModoRede;
import br.com.managerfoot.multiplayer.Sala;
import br.com.managerfoot.multiplayer.Sala.Situacao;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.SalaRecusadaException;
import br.com.managerfoot.persistencia.SalaRepositorio;
import br.com.managerfoot.persistencia.SalaRepositorio.Convite;
import br.com.managerfoot.persistencia.SalaRepositorio.VisaoSala;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sala do jogo em rede e convite por link (SPEC §3.3, rev. 90 e 92; modelos 18c e 18d). A sala consulta o
 * servidor a cada poucos segundos: a lista se atualiza e a consulta conta como "conectado".
 */
@Controller
class SalaController {

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final SalaRepositorio salas;
	private final JogoRepositorio jogos;
	private final Clock relogio;
	private final Duration semConexaoApos;
	private final String urlBase;

	SalaController(SalaRepositorio salas, JogoRepositorio jogos, Clock relogio,
			@Value("${managerfoot.rede.sem-conexao-apos}") Duration semConexaoApos, @Value("${managerfoot.url-base}") String urlBase) {
		this.salas = salas;
		this.jogos = jogos;
		this.relogio = relogio;
		this.semConexaoApos = semConexaoApos;
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
	}

	/** A sala; com HTMX (a cada 3 s) a página troca só a lista e o que falta para começar. */
	@GetMapping("/jogos/{jogoId}/sala")
	String sala(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestHeader(name = "HX-Request", required = false) String htmx, Model model, HttpServletResponse response) {
		Instant agora = relogio.instant();
		salas.marcarVisto(jogoId, usuario.id(), agora);
		Optional<VisaoSala> visao = salas.visao(jogoId, usuario.id());
		if (visao.isEmpty() || visao.get().comecou()) {
			// tirado da sala, ou a temporada começou: a sala não existe mais para ele
			String destino = visao.isEmpty() ? "/" : "/jogos/" + jogoId + "/plantel";
			if (visao.isEmpty() && htmx == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			if (htmx != null) {
				response.setHeader("HX-Redirect", destino);
				return null;
			}
			return "redirect:" + destino;
		}
		VisaoSala v = visao.get();
		Sala sala = v.sala();
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("v", v);
		model.addAttribute("linhas", sala.participantes().stream().map(p -> linha(p, usuario.id(), agora)).toList());
		model.addAttribute("modo", (sala.modo() == ModoRede.ESCOLHA ? "Modo escolha · " : "Modo sorteio · ") + v.divisoes()
				+ (v.divisoes() == 1 ? " divisão" : " divisões"));
		long conectados = sala.conectados(agora, semConexaoApos);
		int total = sala.participantes().size();
		model.addAttribute("contagem", total + (total == 1 ? " treinador · " : " treinadores · ") + conectados
				+ (conectados == 1 ? " conectado" : " conectados"));
		model.addAttribute("podeComecar", sala.podeComecar(agora, semConexaoApos));
		model.addAttribute("falta", sala.falta(agora, semConexaoApos));
		sala.participantes().stream().filter(p -> p.usuarioId() == usuario.id()).findFirst()
			.ifPresent(eu -> model.addAttribute("eu", linha(eu, usuario.id(), agora)));
		v.convite().ifPresent(c -> {
			model.addAttribute("link", urlBase + "/convite/" + c.token());
			model.addAttribute("valeAte", DATA.format(c.valeAte().atZone(ZoneId.systemDefault())));
		});
		return htmx != null ? "sala :: corpo" : "sala";
	}

	@PostMapping("/jogos/{jogoId}/sala/convite")
	String novoConvite(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		salas.novoConvite(jogoId, usuario.id(), relogio.instant());
		redirect.addFlashAttribute("resultado", "Link novo gerado. O anterior não vale mais.");
		return "redirect:/jogos/" + jogoId + "/sala";
	}

	@PostMapping("/jogos/{jogoId}/sala/tirar")
	String tirar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("usuario") long tirado) {
		salas.tirar(jogoId, usuario.id(), tirado);
		return "redirect:/jogos/" + jogoId + "/sala";
	}

	@PostMapping("/jogos/{jogoId}/sala/comecar")
	String comecar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		salas.comecar(jogoId, usuario.id(), relogio.instant(), semConexaoApos);
		redirect.addFlashAttribute("resultado", "A temporada começou.");
		return "redirect:/jogos/" + jogoId + "/plantel";
	}

	/** Quadro de escolha de quem entrou sem clube (modo escolha), com os clubes de usuário bloqueados. */
	@GetMapping("/jogos/{jogoId}/sala/escolher")
	String quadro(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "clube", required = false) Long clubeId, Model model) {
		VisaoSala v = salas.visao(jogoId, usuario.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (v.comecou() || v.sala().modo() != ModoRede.ESCOLHA || jogos.clubeDoTreinador(jogoId, usuario.id()).isPresent()) {
			return "redirect:/jogos/" + jogoId + "/sala";
		}
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Set<Long> ocupados = salas.ocupados(jogoId);
		model.addAttribute("usuario", usuario);
		model.addAttribute("rotulo", "Sala de " + v.sala().administrador() + " · " + v.pais() + " · " + liga.nome());
		model.addAttribute("quadroUrl", "/jogos/" + jogoId + "/sala/escolher");
		model.addAttribute("voltar", "/jogos/" + jogoId + "/sala");
		model.addAttribute("explicacao", "Clubes do jogo como estão nas divisões. Clube com treinador fica bloqueado. Clique num clube para ver o plantel.");
		model.addAttribute("colunas", EscolhaClubeController.colunas(liga, Set.of(), ocupados));
		EscolhaClubeController.escolhivel(liga, clubeId).filter(c -> !ocupados.contains(c.id()))
			.ifPresent(c -> model.addAttribute("escolhido", EscolhaClubeController.escolhido(liga, c, jogos.carregarPlantel(c.id()))));
		return "quadro-escolha";
	}

	@PostMapping("/jogos/{jogoId}/sala/escolher/assumir")
	String escolher(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("clube") long clubeId,
			RedirectAttributes redirect) {
		try {
			salas.escolher(jogoId, usuario.id(), clubeId);
		}
		catch (SalaRecusadaException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			return "redirect:/jogos/" + jogoId + "/sala/escolher";
		}
		return "redirect:/jogos/" + jogoId + "/sala";
	}

	/**
	 * O link do convite. Quem ainda não entrou na conta vê o formulário de entrar e volta para cá depois
	 * (modelo 18d, caso 2).
	 */
	@GetMapping("/convite/{token}")
	String convite(@AuthenticationPrincipal Usuario usuario, @PathVariable String token, Model model) {
		Optional<Convite> convite = salas.convite(token, usuario == null ? 0 : usuario.id(), relogio.instant());
		if (convite.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("usuario", usuario);
		model.addAttribute("token", token);
		switch (convite.get()) {
			case Convite.JaNaSala j -> {
				return "redirect:/jogos/" + j.jogoId() + "/sala";
			}
			case Convite.Valido c -> {
				model.addAttribute("administrador", c.administrador());
				model.addAttribute("valido", c);
			}
			case Convite.Invalido i -> {
				model.addAttribute("administrador", i.administrador());
				model.addAttribute("motivo", i.motivo() == Convite.Motivo.VENCEU
						? "O convite venceu (valia até " + DATA.format(i.valeAte().atZone(ZoneId.systemDefault())) + ")."
						: i.motivo() == Convite.Motivo.TROCADO ? i.administrador() + " gerou um link novo. Peça o link atual."
						: i.motivo().texto());
			}
		}
		return "convite";
	}

	@PostMapping("/convite/{token}/entrar")
	String entrar(@AuthenticationPrincipal Usuario usuario, @PathVariable String token) {
		long jogoId = salas.entrar(token, usuario.id(), relogio.instant());
		boolean semClube = jogos.clubeDoTreinador(jogoId, usuario.id()).isEmpty();
		return "redirect:/jogos/" + jogoId + (semClube ? "/sala/escolher" : "/sala");
	}

	/** Pedido que a sala recusou (temporada começou, link trocado, falta alguém): volta com a mensagem. */
	@ExceptionHandler(SalaRecusadaException.class)
	String recusado(SalaRecusadaException e, RedirectAttributes redirect, HttpServletRequest request) {
		redirect.addFlashAttribute("erro", e.getMessage());
		String caminho = request.getRequestURI().substring(request.getContextPath().length());
		if (caminho.startsWith("/jogos/")) {
			return "redirect:" + caminho.replaceAll("^(/jogos/\\d+/sala).*$", "$1");
		}
		return "redirect:/";
	}

	/** Pedido de quem não é o administrador do jogo: as telas não oferecem, então não há mensagem. */
	@ExceptionHandler(AcessoNegadoException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ResponseBody
	String negado() {
		return "Só o administrador do jogo pode fazer isso.";
	}

	private LinhaSala linha(Sala.Participante p, long eu, Instant agora) {
		Situacao situacao = p.situacao(agora, semConexaoApos);
		String estado = switch (situacao) {
			case PRONTO -> "Pronto";
			case ESCOLHENDO -> "Escolhendo clube";
			case SEM_CONEXAO -> "Sem conexão há " + tempo(Duration.between(p.vistoEm(), agora));
		};
		String nota = p.usuarioId() == eu ? (p.administrador() ? "(você · administrador)" : "(você)")
				: p.administrador() ? "(administrador)" : "";
		String clube = p.clube().map(c -> c.clube().nome() + " · " + c.divisao() + " · força " + c.clube().forca())
			.orElse("ainda sem clube");
		return new LinhaSala(p.usuarioId(), p.nome(), nota, p.clube().map(Sala.ClubeNaSala::clube).orElse(null), clube, estado,
				switch (situacao) {
					case PRONTO -> "estado-pronto";
					case ESCOLHENDO -> "estado-mexendo";
					case SEM_CONEXAO -> "estado-fora";
				}, situacao == Situacao.SEM_CONEXAO ? "Sem conexão" : situacao == Situacao.ESCOLHENDO ? "Escolhendo" : "Pronto",
				!p.administrador());
	}

	/** "40 s", "2 min", "3 h". */
	static String tempo(Duration d) {
		long s = Math.max(0, d.toSeconds());
		if (s < 60) {
			return s + " s";
		}
		if (s < 3600) {
			return (s / 60) + " min";
		}
		return (s / 3600) + " h";
	}

	/**
	 * Treinador na lista da sala.
	 *
	 * @param clube nulo enquanto escolhe
	 * @param estadoCurto o estado na lista pequena do participante (modelo 18d, caso 4)
	 * @param podeSerTirado o administrador não sai da própria sala
	 */
	record LinhaSala(long usuarioId, String nome, String nota, Clube clube, String detalhe, String estado, String classe,
			String estadoCurto, boolean podeSerTirado) {
	}
}
