package br.com.managerfoot.web;

import java.io.IOException;
import java.time.Clock;
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
import br.com.managerfoot.persistencia.PlantelRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.EstadoRodada;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.PlacarAoVivo;
import br.com.managerfoot.temporada.Velocidade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jogos da rodada ao vivo (SPEC §10.2 e §11.2; modelo 1a; docs/decisions/0010). A tela consulta o
 * quadro a cada segundo; quando a rodada termina, volta ao plantel com o placar do usuário.
 */
@Controller
class RodadaController {

	private final RodadaAoVivoRepositorio aoVivo;
	private final PlantelRepositorio plantel;
	private final Clock relogio;

	RodadaController(RodadaAoVivoRepositorio aoVivo, PlantelRepositorio plantel, Clock relogio) {
		this.aoVivo = aoVivo;
		this.plantel = plantel;
		this.relogio = relogio;
	}

	/** "Confirmar e jogar rodada" (SPEC §10.3): começa a rodada ao vivo. Repetir não começa outra. */
	@PostMapping("/jogos/{jogoId}/rodada")
	String iniciar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("rodada") int rodada,
			RedirectAttributes redirect) {
		if (!plantel.visao(jogoId, usuario.id()).orElseThrow(NoSuchElementException::new).problemas().isEmpty()) {
			redirect.addFlashAttribute("erro", "Complete a escalação antes de jogar a rodada.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		try {
			aoVivo.iniciar(jogoId, usuario.id(), rodada, relogio.instant());
		}
		catch (IllegalStateException e) {
			redirect.addFlashAttribute("erro", "A rodada " + rodada + " já foi jogada. Confira o plantel.");
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		return "redirect:/jogos/" + jogoId + "/rodada";
	}

	/** O quadro; com HTMX (a cada segundo) a página troca só o quadro. */
	@GetMapping("/jogos/{jogoId}/rodada")
	String quadro(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestHeader(name = "HX-Request", required = false) String htmx, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		Optional<EstadoRodada> estado = aoVivo.estado(jogoId, usuario.id(), relogio.instant());
		if (estado.isEmpty() || estado.get().terminada()) {
			String plantel = "/jogos/" + jogoId + "/plantel";
			estado.map(EstadoRodada::jogoDoUsuario).ifPresentOrElse(
					jogo -> guardarResultado(request, response, plantel, resultado(estado.get().rodada(), jogo)),
					() -> estado.ifPresent(e -> guardarResultado(request, response, plantel,
							"Rodada " + e.rodada() + ": seu clube folgou.")));
			if (htmx != null) {
				response.setHeader("HX-Redirect", plantel);
				return null;
			}
			return "redirect:" + plantel;
		}
		model.addAttribute("usuario", usuario);
		model.addAttribute("e", estado.get());
		model.addAttribute("velocidades", Velocidade.values());
		return "rodada";
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

	static String resultado(int rodada, PlacarAoVivo jogo) {
		return "Rodada " + rodada + ": " + jogo.mandante().nome() + " " + jogo.golsCasa() + " x " + jogo.golsFora() + " "
				+ jogo.visitante().nome();
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
