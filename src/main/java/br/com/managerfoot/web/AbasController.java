package br.com.managerfoot.web;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abas Classificação, Artilheiros da temporada e Artilheiros de todos os tempos (SPEC §10.3, rev.
 * 16; modelos aprovados). Com {@code ficha=<id>}, a aba abre a ficha do jogador por cima; na
 * classificação, {@code calendario=<clube>} abre o calendário do clube (rev. 17 e 36).
 */
@Controller
class AbasController {

	private final ConsultasRepositorio consultas;

	AbasController(ConsultasRepositorio consultas) {
		this.consultas = consultas;
	}

	@GetMapping("/jogos/{jogoId}/classificacao")
	String classificacao(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "divisao", required = false) Integer divisao,
			@RequestParam(name = "calendario", required = false) Long calendario, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("c", consultas.classificacao(jogoId, usuario.id(), divisao));
		if (calendario != null) {
			consultas.calendario(jogoId, usuario.id(), calendario).ifPresent(cal -> model.addAttribute("calendario", cal));
		}
		return "classificacao";
	}

	@GetMapping("/jogos/{jogoId}/artilheiros")
	String artilheiros(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "divisao", required = false) Integer divisao,
			@RequestParam(name = "ficha", required = false) Long ficha, Model model) {
		model.addAttribute("a", consultas.artilheirosDaTemporada(jogoId, usuario.id(), divisao));
		return artilheiros(usuario, jogoId, ficha, "temporada", model);
	}

	@GetMapping("/jogos/{jogoId}/artilheiros/todos-os-tempos")
	String artilheirosDeTodosOsTempos(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "ficha", required = false) Long ficha, Model model) {
		model.addAttribute("a", consultas.artilheirosDeTodosOsTempos(jogoId, usuario.id()));
		return artilheiros(usuario, jogoId, ficha, "todos", model);
	}

	private String artilheiros(Usuario usuario, long jogoId, Long ficha, String aba, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("aba", aba);
		if (ficha != null) {
			consultas.ficha(jogoId, usuario.id(), ficha).ifPresent(f -> model.addAttribute("ficha", f));
		}
		return "artilheiros";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}
}
