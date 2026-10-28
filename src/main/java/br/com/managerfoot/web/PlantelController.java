package br.com.managerfoot.web;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.persistencia.ClubeNaFilaException;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import br.com.managerfoot.persistencia.FimDeTemporadaRepositorio;
import br.com.managerfoot.persistencia.FimDeTemporadaRepositorio.NovaTemporada;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.PlantelRepositorio;
import br.com.managerfoot.persistencia.PlantelRepositorio.LinhaJogador;
import br.com.managerfoot.persistencia.PlantelRepositorio.VisaoPlantel;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Plantel e tática (SPEC §10.3; modelo aprovado, rev. 16, 31 e 34). Cada mudança grava e volta à
 * tela (com HTMX, só a área do plantel é trocada). "Confirmar e jogar rodada" fica em
 * {@link RodadaController}.
 */
@Controller
class PlantelController {

	private static final SecureRandom SEMENTES = new SecureRandom();

	private final PlantelRepositorio plantel;
	private final JogosDoTreinadorRepositorio jogos;
	private final RodadaAoVivoRepositorio aoVivo;
	private final ConsultasRepositorio consultas;
	private final FimDeTemporadaRepositorio fimDeTemporada;
	private final JogoRepositorio jogo;

	PlantelController(PlantelRepositorio plantel, JogosDoTreinadorRepositorio jogos, RodadaAoVivoRepositorio aoVivo,
			ConsultasRepositorio consultas, FimDeTemporadaRepositorio fimDeTemporada, JogoRepositorio jogo) {
		this.plantel = plantel;
		this.jogos = jogos;
		this.aoVivo = aoVivo;
		this.consultas = consultas;
		this.fimDeTemporada = fimDeTemporada;
		this.jogo = jogo;
	}

	@GetMapping("/jogos/{jogoId}/plantel")
	String plantel(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "ficha", required = false) Long ficha, Model model) {
		jogos.abrir(jogoId, usuario.id()).orElseThrow(NoSuchElementException::new);
		if (aoVivo.emAndamento(jogoId)) {
			// rodada ao vivo: o plantel fica fechado até ela terminar (docs/decisions/0010)
			return "redirect:/jogos/" + jogoId + "/rodada";
		}
		VisaoPlantel v = plantel.visao(jogoId, usuario.id()).orElseThrow(NoSuchElementException::new);
		List<LinhaJogador> titulares = grupo(v, Lugar.TITULAR);
		model.addAttribute("usuario", usuario);
		model.addAttribute("v", v);
		model.addAttribute("formacoes", Formacao.values());
		List<LinhaJogador> banco = grupo(v, Lugar.BANCO);
		List<LinhaJogador> fora = grupo(v, Lugar.FORA);
		Formacao f = v.escalacao().formacao();
		model.addAttribute("grupos", List.of(
				new Grupo("Titulares", titulares, titulares.size() + " de " + Formacao.TITULARES + " · " + f.rotulo()),
				new Grupo("Banco", banco, banco.size() + " de " + Escalacao.MAXIMO_RESERVAS + " reservas"),
				new Grupo("Fora do banco", fora, fora.size() + (fora.size() == 1 ? " jogador" : " jogadores"))));
		model.addAttribute("valida", "Escalação válida: 1 G · " + f.quantidade(Posicao.DEFENSOR) + " D · "
				+ f.quantidade(Posicao.MEIO_CAMPISTA) + " M · " + f.quantidade(Posicao.ATACANTE) + " A");
		model.addAttribute("batedores", v.jogadores().stream().filter(l -> l.lugar() != Lugar.FORA).toList());
		model.addAttribute("forcaMedia", titulares.isEmpty() ? "–" : String.format(Locale.forLanguageTag("pt-BR"), "%.1f",
				titulares.stream().mapToInt(l -> l.jogador().forca()).average().orElse(0)));
		model.addAttribute("estrangeirosEmCampo", titulares.stream().filter(l -> l.estrangeiro() != null).count());
		model.addAttribute("asteriscosEmCampo", titulares.stream().filter(l -> l.jogador().asterisco()).count());
		model.addAttribute("limiteAsteriscos", ParametrosMotor.padrao().asterisco().limiteSemPenalidade());
		model.addAttribute("substituicoes", RegrasPartida.padrao().substituicoes());
		if (ficha != null) {
			consultas.ficha(jogoId, usuario.id(), ficha).ifPresent(dados -> model.addAttribute("ficha", dados));
		}
		return "plantel";
	}

	@PostMapping("/jogos/{jogoId}/plantel/jogador/{jogadorId}")
	String mover(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @PathVariable long jogadorId,
			@RequestParam("lugar") Lugar lugar, RedirectAttributes redirect) {
		tentar(redirect, () -> plantel.mover(jogoId, usuario.id(), jogadorId, lugar));
		return voltar(jogoId);
	}

	@PostMapping("/jogos/{jogoId}/plantel/formacao")
	String formacao(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("formacao") Formacao formacao) {
		plantel.definirFormacao(jogoId, usuario.id(), formacao);
		return voltar(jogoId);
	}

	@PostMapping("/jogos/{jogoId}/plantel/batedor")
	String batedor(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "batedor", required = false) Long batedor, RedirectAttributes redirect) {
		tentar(redirect, () -> plantel.definirBatedor(jogoId, usuario.id(), batedor));
		return voltar(jogoId);
	}

	/**
	 * Fim de temporada na tela: o usuário só confirma (SPEC §15, 3c.4). O número da temporada vem da
	 * tela, e um segundo clique é recusado pelo servidor.
	 */
	@PostMapping("/jogos/{jogoId}/temporada")
	String encerrarTemporada(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("temporada") int temporada, RedirectAttributes redirect) {
		long clube = jogo.clubeDoTreinador(jogoId, usuario.id()).orElseThrow(NoSuchElementException::new);
		try {
			NovaTemporada nova = fimDeTemporada.avancarTemporada(jogoId, temporada, SEMENTES.nextLong());
			Clube novoClube = nova.demitidos().get(usuario.id());
			String destino = novoClube != null
					? " Seu clube caiu para a fila de espera: você foi demitido e assumiu o " + novoClube.nome() + "."
					: nova.transicao().promovidos().stream().anyMatch(c -> c.id() == clube)
					? " Seu clube subiu de divisão!"
					: nova.transicao().rebaixados().stream().anyMatch(c -> c.id() == clube) ? " Seu clube foi rebaixado." : "";
			redirect.addFlashAttribute("resultado", "Começou a temporada " + nova.temporada().numero() + "." + destino);
		}
		catch (IllegalStateException e) {
			redirect.addFlashAttribute("erro", "A temporada " + temporada + " já foi encerrada.");
		}
		return voltar(jogoId);
	}

	/** Clube na fila de espera: a regra do treinador nesse caso está em aberto (SPEC §4.1). */
	@ExceptionHandler(ClubeNaFilaException.class)
	String naFila(Model model) {
		model.addAttribute("mensagem", "Seu clube caiu para a fila de espera e não joga esta temporada. "
				+ "O que acontece com o treinador nesse caso ainda não foi decidido.");
		return "aviso";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

	/** Titulares, banco ou fora, por posição e da maior força para a menor, como no modelo. */
	private static List<LinhaJogador> grupo(VisaoPlantel v, Lugar lugar) {
		return v.jogadores().stream().filter(l -> l.lugar() == lugar)
			.sorted(Comparator.comparing((LinhaJogador l) -> l.jogador().posicao())
				.thenComparing(Comparator.comparingInt((LinhaJogador l) -> l.jogador().forca()).reversed()))
			.toList();
	}

	/** Titulares, banco ou fora do banco, com a nota do cabeçalho. */
	record Grupo(String titulo, List<LinhaJogador> jogadores, String nota) {
	}

	private static void tentar(RedirectAttributes redirect, Runnable acao) {
		try {
			acao.run();
		}
		catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
	}

	private static String voltar(long jogoId) {
		return "redirect:/jogos/" + jogoId + "/plantel";
	}
}
