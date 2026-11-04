package br.com.managerfoot.web;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.persistencia.TurnoRepositorio;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Faixa "Aguardando" do jogo em rede ({@link FaixaDoTurno}): consulta a cada 1 s, o que também marca o
 * treinador como conectado; quando o jogo segue, a tela vai para o plantel, que leva aonde o jogo está.
 */
@Controller
class TurnoController {

	private final TurnoRepositorio turnos;
	private final FaixaDoTurno faixa;

	TurnoController(TurnoRepositorio turnos, FaixaDoTurno faixa) {
		this.turnos = turnos;
		this.faixa = faixa;
	}

	/** A faixa, a cada 1 s. Se o jogo seguiu, a tela inteira vai para o plantel, que leva aonde o jogo está. */
	@GetMapping("/jogos/{jogoId}/turno")
	String faixa(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam String momento,
			@RequestParam String tela, @RequestParam(defaultValue = "") String rotulo, Model model,
			HttpServletResponse response) {
		Optional<Turno> turno = faixa.preencher(model, jogoId, usuario, tela, rotulo);
		if (turno.isEmpty() || !turno.get().momento().equals(momento)) {
			response.setHeader("HX-Redirect", "/jogos/" + jogoId + "/plantel");
			return null;
		}
		return "turno :: painel";
	}

	/** "Tirar meu OK e mexer" ou "Mudar meus lances" (SPEC §3.3 item 5, rev. 92). */
	@PostMapping("/jogos/{jogoId}/ok/tirar")
	String tirar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId) {
		turnos.tirarOk(jogoId, usuario.id());
		return "redirect:/jogos/" + jogoId + "/plantel";
	}

}
