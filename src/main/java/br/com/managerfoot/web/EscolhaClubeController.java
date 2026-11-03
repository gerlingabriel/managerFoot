package br.com.managerfoot.web;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.persistencia.BancoEditavel;
import br.com.managerfoot.persistencia.BancoEditavel.Pais;
import br.com.managerfoot.persistencia.BancoEditavelRepositorio;
import br.com.managerfoot.persistencia.BancoRecusadoException;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.web.BancoEditavelController.BancoEnviado;
import jakarta.servlet.http.HttpSession;

/**
 * Quadro de escolha de clube do administrador (SPEC §3.1 e §10.5, rev. 28 e 29). A semente da
 * distribuição fica na sessão, não no formulário: o quadro mostrado é o jogo que será criado, e
 * assumir o clube consome a semente, para um segundo clique não criar outro jogo.
 * Só o administrador chega aqui (SegurancaConfig).
 */
@Controller
class EscolhaClubeController {

	static final String SESSAO = "escolha-clube";
	private static final SecureRandom SEMENTES = new SecureRandom();

	private final Campeonatos campeonatos;
	private final JogoRepositorio jogo;
	private final JogosDoTreinadorRepositorio jogos;
	private final MoldeRepositorio moldes;
	private final BancoEditavelRepositorio bancos;
	private final int candidatosClubeInicial;

	EscolhaClubeController(Campeonatos campeonatos, JogoRepositorio jogo, JogosDoTreinadorRepositorio jogos,
			MoldeRepositorio moldes, BancoEditavelRepositorio bancos,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		this.bancos = bancos;
		this.campeonatos = campeonatos;
		this.jogo = jogo;
		this.jogos = jogos;
		this.moldes = moldes;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	/** Sorteia a distribuição do campeonato e abre o quadro. */
	@PostMapping("/jogos/escolher")
	String abrirQuadro(@RequestParam(name = "paisId", required = false) Long paisId,
			@RequestParam(name = "paisCodigo", required = false) String paisCodigo, HttpSession sessao) {
		Optional<BancoEnviado> enviado = bancoEnviado(sessao);
		if (enviado.isPresent()) {
			// com o arquivo enviado, o campeonato vem do arquivo (SPEC §3.5, rev. 90)
			if (paisCodigo == null || paisDoArquivo(enviado.get(), paisCodigo).isEmpty()) {
				return "redirect:/jogos/novo";
			}
			sessao.setAttribute(SESSAO, new Quadro(-1, paisCodigo, SEMENTES.nextLong()));
			return "redirect:/jogos/escolher";
		}
		if (paisId == null || campeonatos.buscar(paisId).isEmpty()) {
			return "redirect:/jogos/novo";
		}
		sessao.setAttribute(SESSAO, new Quadro(paisId, null, SEMENTES.nextLong()));
		return "redirect:/jogos/escolher";
	}

	@GetMapping("/jogos/escolher")
	String quadro(@AuthenticationPrincipal Usuario usuario, @RequestParam(name = "clube", required = false) Long clubeId,
			HttpSession sessao, Model model) {
		Optional<Quadro> quadro = quadro(sessao);
		if (quadro.isPresent() && quadro.get().paisCodigo() != null) {
			return quadroDoArquivo(usuario, quadro.get(), clubeId, sessao, model);
		}
		Optional<CampeonatoDisponivel> campeonato = quadro.flatMap(q -> campeonatos.buscar(q.paisId()));
		if (campeonato.isEmpty()) {
			return "redirect:/jogos/novo";
		}
		ConfiguracaoLiga liga = jogo.distribuicao(quadro.get().paisId(), quadro.get().semente());
		Set<Long> candidatos = ClubeInicial.candidatos(liga.divisoes().getLast(), candidatosClubeInicial).stream()
			.map(Clube::id)
			.collect(Collectors.toSet());
		model.addAttribute("usuario", usuario);
		model.addAttribute("campeonato", campeonato.get());
		model.addAttribute("colunas", colunas(liga, candidatos));
		escolhivel(liga, clubeId).ifPresent(c -> model.addAttribute("escolhido", escolhido(liga, c)));
		return "quadro-escolha";
	}

	@PostMapping("/jogos/escolher/assumir")
	String assumir(@AuthenticationPrincipal Usuario usuario, @RequestParam("clube") long clubeId, HttpSession sessao) {
		Optional<Quadro> quadro = quadro(sessao);
		if (quadro.isEmpty()) {
			// semente já consumida (segundo clique) ou sessão nova: o jogo, se houve, está na lista
			return "redirect:/";
		}
		sessao.removeAttribute(SESSAO);
		long jogoId;
		if (quadro.get().paisCodigo() != null) {
			Optional<BancoEnviado> enviado = bancoEnviado(sessao);
			if (enviado.isEmpty()) {
				return "redirect:/jogos/novo";
			}
			jogoId = bancos.criarJogoComEscolha(enviado.get().conteudo(), quadro.get().paisCodigo(), usuario.id(),
					quadro.get().semente(), Math.toIntExact(clubeId));
			sessao.removeAttribute(BancoEditavelController.SESSAO);
		} else {
			jogoId = jogos.criarComEscolha(quadro.get().paisId(), usuario.id(), quadro.get().semente(), clubeId);
		}
		return "redirect:/jogos/" + jogoId;
	}

	/** O quadro de um campeonato do arquivo enviado: os clubes são numerados na ordem do arquivo. */
	private String quadroDoArquivo(Usuario usuario, Quadro quadro, Long clubeId, HttpSession sessao, Model model) {
		Optional<Pais> pais = bancoEnviado(sessao).flatMap(b -> paisDoArquivo(b, quadro.paisCodigo()));
		if (pais.isEmpty()) {
			return "redirect:/jogos/novo";
		}
		List<Clube> clubes = BancoEditavel.clubesNumerados(pais.get());
		ConfiguracaoLiga liga = pais.get().liga().orElseThrow().distribuir(clubes, new SplittableRandom(quadro.semente()));
		Set<Long> candidatos = ClubeInicial.candidatos(liga.divisoes().getLast(), candidatosClubeInicial).stream()
			.map(Clube::id)
			.collect(Collectors.toSet());
		model.addAttribute("usuario", usuario);
		model.addAttribute("campeonato", new CampeonatoDisponivel(-1, pais.get().codigo(), pais.get().nome(),
				pais.get().liga().orElseThrow().nome()));
		model.addAttribute("colunas", colunas(liga, candidatos));
		escolhivel(liga, clubeId).ifPresent(c -> model.addAttribute("escolhido",
				escolhido(liga, c, pais.get().clubes().get(Math.toIntExact(c.id()) - 1).jogadores())));
		return "quadro-escolha";
	}

	private Optional<Pais> paisDoArquivo(BancoEnviado enviado, String codigo) {
		try {
			return bancos.ler(enviado.conteudo()).pais(codigo).filter(p -> p.liga().isPresent());
		}
		catch (BancoRecusadoException e) {
			return Optional.empty();
		}
	}

	private static Optional<BancoEnviado> bancoEnviado(HttpSession sessao) {
		return sessao.getAttribute(BancoEditavelController.SESSAO) instanceof BancoEnviado b ? Optional.of(b) : Optional.empty();
	}

	private static Optional<Quadro> quadro(HttpSession sessao) {
		return sessao.getAttribute(SESSAO) instanceof Quadro q ? Optional.of(q) : Optional.empty();
	}

	/** O clube, se ele está numa divisão do quadro: a fila não pode ser escolhida. */
	private static Optional<Clube> escolhivel(ConfiguracaoLiga liga, Long clubeId) {
		if (clubeId == null) {
			return Optional.empty();
		}
		return liga.divisoes().stream().flatMap(d -> d.clubes().stream()).filter(c -> c.id() == clubeId).findFirst();
	}

	private Escolhido escolhido(ConfiguracaoLiga liga, Clube clube) {
		return escolhido(liga, clube, moldes.carregarPlantel(clube.id()));
	}

	private static Escolhido escolhido(ConfiguracaoLiga liga, Clube clube, List<Jogador> jogadores) {
		String divisao = liga.divisoes().stream().filter(d -> d.clubes().contains(clube)).findFirst()
			.map(Divisao::nome).orElseThrow();
		List<Jogador> plantel = jogadores.stream()
			.sorted(Comparator.comparing(Jogador::posicao).thenComparing(Comparator.comparingInt(Jogador::forca).reversed()))
			.toList();
		return new Escolhido(clube, divisao + " · força " + clube.forca() + " · " + plantel.size() + " jogadores", plantel);
	}

	/** Divisões pela prioridade e a fila no fim; clubes do mais forte ao mais fraco. */
	private static List<Coluna> colunas(ConfiguracaoLiga liga, Set<Long> candidatos) {
		Comparator<Clube> pelaForca = Comparator.comparingInt(Clube::forca).reversed().thenComparing(Clube::nome);
		List<Coluna> colunas = new ArrayList<>();
		for (int i = 0; i < liga.divisoes().size(); i++) {
			Divisao d = liga.divisoes().get(i);
			boolean ultima = i == liga.divisoes().size() - 1;
			colunas.add(new Coluna(d.nome(), d.clubes().size() + " clubes" + (ultima ? " · mais fracos ▾" : ""), false,
					d.clubes().stream().sorted(pelaForca).toList(), ultima ? candidatos : Set.of()));
		}
		if (!liga.filaDeEspera().isEmpty()) {
			colunas.add(new Coluna("Fila de espera", "não podem ser escolhidos", true,
					liga.filaDeEspera().stream().sorted(pelaForca).toList(), Set.of()));
		}
		return colunas;
	}

	/**
	 * Campeonato e semente do quadro aberto na sessão.
	 *
	 * @param paisCodigo o país do arquivo enviado; nulo no banco padrão, que usa {@code paisId}
	 */
	record Quadro(long paisId, String paisCodigo, long semente) implements Serializable {
	}

	/** Divisão (ou a fila) no quadro; {@code candidatos} são os do sorteio, destacados de leve. */
	record Coluna(String titulo, String nota, boolean fila, List<Clube> clubes, Set<Long> candidatos) {
	}

	record Escolhido(Clube clube, String resumo, List<Jogador> plantel) {
	}
}
