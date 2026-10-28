package br.com.managerfoot.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.JogoGravado;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.NovoJogo;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;

/** Início, novo jogo por sorteio (o caminho da escolha está em {@link EscolhaClubeController}) e abertura de um jogo gravado (SPEC §3.2 e §10.5, rev. 28 e 29). */
@Controller
class JogosController {

	private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

	private final JogosDoTreinadorRepositorio jogos;
	private final MoldeRepositorio moldes;
	private final Campeonatos campeonatos;
	private final int candidatosClubeInicial;

	JogosController(JogosDoTreinadorRepositorio jogos, MoldeRepositorio moldes, Campeonatos campeonatos,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		this.jogos = jogos;
		this.moldes = moldes;
		this.campeonatos = campeonatos;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	@GetMapping("/")
	String inicio(@AuthenticationPrincipal Usuario usuario, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogos", jogos.listar(usuario.id()).stream().map(JogosController::linha).toList());
		return "inicio";
	}

	@GetMapping("/jogos/novo")
	String novo(@AuthenticationPrincipal Usuario usuario, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("campeonatos", campeonatos.disponiveis().stream().map(this::cartao).toList());
		model.addAttribute("candidatos", candidatosClubeInicial);
		return "novo-jogo";
	}

	@PostMapping("/jogos")
	String criar(@AuthenticationPrincipal Usuario usuario, @RequestParam("paisId") long paisId, Model model,
			RedirectAttributes redirect) {
		if (campeonatos.buscar(paisId).isEmpty()) {
			model.addAttribute("erro", "Escolha um dos campeonatos da lista.");
			return novo(usuario, model);
		}
		NovoJogo novo = jogos.criarComSorteio(paisId, usuario.id());
		redirect.addFlashAttribute("sorteadoEntre",
				novo.candidatos().stream().map(Clube::nome).collect(Collectors.joining(", ")));
		return "redirect:/jogos/" + novo.jogoId();
	}

	@GetMapping("/jogos/{jogoId}")
	String abrir(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, Model model) {
		JogoGravado jogo = jogos.abrir(jogoId, usuario.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogo", linha(jogo));
		return "jogo";
	}

	private Cartao cartao(CampeonatoDisponivel c) {
		return new Cartao(c.paisId(), c.codigoPais(), c.nomePais(), c.nomeLiga(), detalhe(moldes.carregarFormato(c.paisId())));
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

	private static Linha linha(JogoGravado j) {
		String divisao = j.divisao() == null ? "Fila de espera" : j.divisao();
		return new Linha(j.jogoId(), j.clube(), divisao,
				j.pais() + " · " + divisao + " · temporada " + j.ano() + ", rodada " + j.rodada(),
				"Jogado em " + QUANDO.format(j.ultimoAcesso().atZone(ZoneId.systemDefault())));
	}

	/** Campeonato como cartão da faixa com rolagem lateral. */
	record Cartao(long paisId, String codigo, String pais, String liga, String detalhe) {
	}

	/** Jogo como aparece na lista do início e no topo do jogo. */
	record Linha(long jogoId, Clube clube, String divisao, String onde, String quando) {
	}
}
