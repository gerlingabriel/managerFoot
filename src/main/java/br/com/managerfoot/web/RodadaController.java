package br.com.managerfoot.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

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
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.LeilaoRepositorio;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Etapa;
import br.com.managerfoot.persistencia.DecisaoPendenteException;
import br.com.managerfoot.persistencia.PlantelRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio;
import br.com.managerfoot.persistencia.TrocaRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.EstadoRodada;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.PlacarAoVivo;
import br.com.managerfoot.temporada.Velocidade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jogos da rodada ao vivo (SPEC §10.2 e §11.2; modelos 1a, 1d e 1e; docs/decisions/0010). A tela
 * consulta o quadro a cada segundo; clicar num clube pausa e abre a janela do jogo; quando a rodada
 * termina, volta ao plantel com o placar do usuário.
 */
@Controller
class RodadaController {

	private final RodadaAoVivoRepositorio aoVivo;
	private final PlantelRepositorio plantel;
	private final LeilaoRepositorio leiloes;
	private final TrocaRepositorio trocas;
	private final Clock relogio;

	RodadaController(RodadaAoVivoRepositorio aoVivo, PlantelRepositorio plantel, LeilaoRepositorio leiloes,
			TrocaRepositorio trocas, Clock relogio) {
		this.aoVivo = aoVivo;
		this.plantel = plantel;
		this.leiloes = leiloes;
		this.trocas = trocas;
		this.relogio = relogio;
	}

	/**
	 * "Confirmar e jogar rodada" (SPEC §10.3): antes, os leilões da máquina (SPEC §9.4, rev. 41;
	 * modelo 7b); sem leilão pendente, começa a rodada ao vivo. Com {@code etapa}, começa a etapa da
	 * copa (SPEC §8.2, rev. 70) ou, com {@code continental}, a da continental (§8.3.6, rev. 79), sem
	 * leilões. Repetir não começa outra.
	 */
	@PostMapping("/jogos/{jogoId}/rodada")
	String iniciar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("rodada") int rodada,
			@RequestParam(name = "etapa", required = false) Integer etapa,
			@RequestParam(name = "continental", defaultValue = "false") boolean continental, RedirectAttributes redirect) {
		PlantelRepositorio.VisaoPlantel visao = plantel.visao(jogoId, usuario.id()).orElseThrow(NoSuchElementException::new);
		// clique repetido numa rodada já jogada: a escalação pode ter mudado depois (suspensão), mas o aviso é este
		if (rodada < visao.rodada()) {
			redirect.addFlashAttribute("erro", "A rodada " + rodada + " já foi jogada. Confira o plantel.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		if (etapa == null && visao.indiceCopa() != null) {
			// página antiga: a etapa da copa vem antes da rodada da liga (SPEC §8.2, rev. 70)
			redirect.addFlashAttribute("erro", "Antes da rodada " + rodada + ", jogue a " + (visao.continental() ? "continental." : "copa."));
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		if (!visao.problemas().isEmpty()) {
			redirect.addFlashAttribute("erro", "Complete a escalação antes de jogar a rodada.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		if (trocas.algumConvitePendente(jogoId)) {
			// o convite (modelo 11a) vem antes dos leilões e da rodada
			redirect.addFlashAttribute("erro", "Responda ao convite para assumir outro clube antes de jogar a rodada.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		try {
			if (etapa != null) {
				if (continental) {
					aoVivo.iniciarContinental(jogoId, usuario.id(), etapa, relogio.instant());
				} else {
					aoVivo.iniciarCopa(jogoId, usuario.id(), etapa, relogio.instant());
				}
				return "redirect:/jogos/" + jogoId + "/rodada";
			}
			if (leiloes.preparar(jogoId, usuario.id(), rodada) != Etapa.RODADA) {
				return "redirect:/jogos/" + jogoId + "/leiloes";
			}
			aoVivo.iniciar(jogoId, usuario.id(), rodada, relogio.instant());
		}
		catch (DecisaoPendenteException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		catch (IllegalStateException e) {
			redirect.addFlashAttribute("erro", (etapa != null ? (continental ? "Esta etapa da continental" : "Esta etapa da copa") : "A rodada " + rodada)
					+ " já foi jogada. Confira o plantel.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		return "redirect:/jogos/" + jogoId + "/rodada";
	}

	/** O quadro; com HTMX (a cada segundo) a página troca só o quadro. */
	@GetMapping("/jogos/{jogoId}/rodada")
	String quadro(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "janela", required = false) Long janela,
			@RequestParam(name = "sai", required = false) Long sai,
			@RequestParam(name = "entra", required = false) Long entra,
			@RequestHeader(name = "HX-Request", required = false) String htmx, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		Instant agora = relogio.instant();
		Optional<EstadoRodada> estado = aoVivo.estado(jogoId, usuario.id(), agora);
		if (estado.isEmpty() || estado.get().terminada()) {
			String plantel = "/jogos/" + jogoId + "/plantel";
			estado.map(EstadoRodada::jogoDoUsuario).ifPresentOrElse(
					jogo -> guardarResultado(request, response, plantel, resultado(estado.get(), jogo)),
					() -> estado.ifPresent(e -> guardarResultado(request, response, plantel, e.copa() != null
							? e.copa() + ": seu clube não jogou." : "Rodada " + e.rodada() + ": seu clube folgou.")));
			if (htmx != null) {
				response.setHeader("HX-Redirect", plantel);
				return null;
			}
			return "redirect:" + plantel;
		}
		model.addAttribute("usuario", usuario);
		model.addAttribute("e", estado.get());
		model.addAttribute("velocidades", Velocidade.values());
		if (janela == null) {
			// lesão no clube do usuário: a rodada parou e a janela do jogo abre sozinha (SPEC §7.5, rev. 69)
			janela = estado.get().janelaLesao();
		}
		if (janela != null && estado.get().comUsuario()) {
			aoVivo.janela(jogoId, usuario.id(), janela, sai, entra, agora).ifPresent(j -> {
				model.addAttribute("janela", j);
				model.addAttribute("sai", sai);
				model.addAttribute("entra", entra);
			});
		}
		return "rodada";
	}

	/** Clicar num clube da divisão do usuário pausa a partida e abre a janela do jogo (SPEC rev. 30 e 33). */
	@PostMapping("/jogos/{jogoId}/rodada/janela")
	String abrirJanela(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("mandante") long mandante) {
		aoVivo.pausar(jogoId, usuario.id(), relogio.instant());
		return "redirect:/jogos/" + jogoId + "/rodada?janela=" + mandante;
	}

	/** "Continuar" ou fechar a janela: a partida volta a andar. */
	@PostMapping("/jogos/{jogoId}/rodada/continuar")
	String continuar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId) {
		aoVivo.continuar(jogoId, usuario.id(), relogio.instant());
		return "redirect:/jogos/" + jogoId + "/rodada";
	}

	@PostMapping("/jogos/{jogoId}/rodada/substituir")
	String substituir(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("mandante") long mandante, @RequestParam(name = "sai", required = false) Long sai,
			@RequestParam(name = "entra", required = false) Long entra, RedirectAttributes redirect) {
		if (sai == null || entra == null) {
			redirect.addFlashAttribute("erro", "Marque quem sai e quem entra.");
		} else {
			try {
				aoVivo.substituir(jogoId, usuario.id(), sai, entra, relogio.instant());
			}
			catch (IllegalArgumentException e) {
				redirect.addFlashAttribute("erro", e.getMessage());
			}
		}
		return "redirect:/jogos/" + jogoId + "/rodada?janela=" + mandante;
	}

	@PostMapping("/jogos/{jogoId}/rodada/som")
	String som(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("ligado") boolean ligado,
			@RequestParam(name = "janela", required = false) Long janela) {
		aoVivo.definirSom(jogoId, usuario.id(), ligado);
		return "redirect:/jogos/" + jogoId + "/rodada" + (janela == null ? "" : "?janela=" + janela);
	}

	@PostMapping("/jogos/{jogoId}/rodada/velocidade")
	String velocidade(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("velocidade") Velocidade velocidade) {
		aoVivo.mudarVelocidade(jogoId, usuario.id(), velocidade, relogio.instant());
		return "redirect:/jogos/" + jogoId + "/rodada";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

	static String resultado(EstadoRodada estado, PlacarAoVivo jogo) {
		String placar = jogo.golsCasa() + " x " + jogo.golsFora();
		String penaltis = jogo.penaltisCasa() == null ? "" : " (pên. " + jogo.penaltisCasa() + " x " + jogo.penaltisFora() + ")";
		return (estado.copa() != null ? estado.copa() : "Rodada " + estado.rodada()) + ": " + jogo.mandante().nome() + " "
				+ placar + " " + jogo.visitante().nome() + penaltis;
	}

	/** Mensagem para a próxima tela, também quando o redirecionamento é feito pelo HTMX. */
	private static void guardarResultado(HttpServletRequest request, HttpServletResponse response, String destino,
			String texto) {
		FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
		flash.put("resultado", texto);
		flash.setTargetRequestPath(destino);
		RequestContextUtils.getFlashMapManager(request).saveOutputFlashMap(flash, request, response);
	}
}
