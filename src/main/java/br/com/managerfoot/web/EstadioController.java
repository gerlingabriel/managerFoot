package br.com.managerfoot.web;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.AmpliacaoRecusadaException;
import br.com.managerfoot.persistencia.EstadioRepositorio;

/**
 * Ampliação do estádio pela janela das Finanças (SPEC §9.2, modelo 6c, rev. 55). O servidor confere
 * obra, limite, rodada e caixa; a tela só mostra o resultado.
 */
@Controller
class EstadioController {

	private final EstadioRepositorio estadios;

	EstadioController(EstadioRepositorio estadios) {
		this.estadios = estadios;
	}

	@PostMapping("/jogos/{jogoId}/estadio/ampliar")
	String ampliar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		try {
			estadios.ampliar(jogoId, usuario.id());
			redirect.addFlashAttribute("resultado", "Obra iniciada. Os lugares novos valem depois da obra.");
		}
		catch (AmpliacaoRecusadaException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		return "redirect:/jogos/" + jogoId + "/financas?estadio=true";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	void naoEncontrado() {
	}
}
