package br.com.managerfoot.web;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.persistencia.DecisaoPendenteException;
import br.com.managerfoot.persistencia.TurnoRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Faixa "Aguardando" do jogo em rede ({@link FaixaDoTurno}): consulta a cada 1 s, o que também marca o
 * treinador como conectado; quando o jogo segue, a tela vai para o plantel, que leva aonde o jogo está.
 */
@Controller
class TurnoController {

	private static final SecureRandom SEMENTES = new SecureRandom();

	private final TurnoRepositorio turnos;
	private final FaixaDoTurno faixa;
	private final Clock relogio;

	TurnoController(TurnoRepositorio turnos, FaixaDoTurno faixa, Clock relogio) {
		this.turnos = turnos;
		this.faixa = faixa;
		this.relogio = relogio;
	}

	/** A faixa, a cada 1 s. Se o jogo seguiu, a tela inteira vai para o plantel, que leva aonde o jogo está. */
	@GetMapping("/jogos/{jogoId}/turno")
	String faixa(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam String momento,
			@RequestParam String tela, @RequestParam(defaultValue = "") String rotulo, Model model,
			HttpServletRequest request, HttpServletResponse response) {
		Optional<Turno> turno = faixa.preencher(model, jogoId, usuario, tela, rotulo);
		if (turnos.emRede(jogoId) && !turnos.participa(jogoId, usuario.id())) {
			// o administrador tirou este treinador do jogo (SPEC §11.4, rev. 104): volta ao início com o aviso
			FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
			flash.put("erro", "O administrador tirou você deste jogo em rede. O clube agora é da máquina.");
			flash.setTargetRequestPath("/");
			RequestContextUtils.getFlashMapManager(request).saveOutputFlashMap(flash, request, response);
			response.setHeader("HX-Redirect", "/");
			return null;
		}
		if (turno.isEmpty() || !turno.get().momento().equals(momento)) {
			response.setHeader("HX-Redirect", "/jogos/" + jogoId + "/plantel");
			return null;
		}
		return "turno :: painel";
	}

	/**
	 * A confirmação de "Tirar do jogo" (SPEC §11.4, rev. 104): uma tela à parte, porque a faixa se refaz a
	 * cada segundo e fecharia a janela. Só quem criou o jogo a vê.
	 */
	@GetMapping("/jogos/{jogoId}/rede/tirar")
	String confirmarTirar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("usuario") long quem, Model model) {
		Optional<Turno.Treinador> treinador = turnos.turno(jogoId)
			.filter(t -> turnos.administrador(jogoId).filter(d -> d == usuario.id()).isPresent())
			.flatMap(t -> t.treinadores().stream().filter(x -> x.usuarioId() == quem && quem != usuario.id()).findFirst());
		if (treinador.isEmpty()) {
			// não é o administrador, o treinador já saiu ou a rodada está ao vivo: o plantel leva aonde o jogo está
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("treinador", treinador.get());
		return "tirar-do-jogo";
	}

	/** Tira o treinador do jogo; se os que ficaram já tinham dado OK, o jogo segue (SPEC §11.4, rev. 104). */
	@PostMapping("/jogos/{jogoId}/rede/tirar")
	String tirarDoJogo(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam("usuario") long quem, RedirectAttributes redirect) {
		String nome = turnos.turno(jogoId).flatMap(t -> t.treinadores().stream().filter(x -> x.usuarioId() == quem).findFirst())
			.map(Turno.Treinador::nome).orElse("O treinador");
		try {
			turnos.tirarDoJogo(jogoId, usuario.id(), quem, SEMENTES.nextLong(), relogio.instant());
			redirect.addFlashAttribute("resultado", nome + " saiu do jogo. O clube dele agora é da máquina.");
		}
		catch (IllegalArgumentException | DecisaoPendenteException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		catch (AcessoNegadoException e) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return "redirect:/jogos/" + jogoId + "/plantel";
	}

	/** "Tirar meu OK e mexer" ou "Mudar meus lances" (SPEC §3.3 item 5, rev. 92). */
	@PostMapping("/jogos/{jogoId}/ok/tirar")
	String tirar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId) {
		turnos.tirarOk(jogoId, usuario.id());
		return "redirect:/jogos/" + jogoId + "/plantel";
	}

}
