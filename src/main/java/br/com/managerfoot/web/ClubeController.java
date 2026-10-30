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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.CompraRepositorio;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Janela do clube (modelo 14e, rev. 84): abre por cima de qualquer tela, pelo nome do clube. Clube da
 * liga tem as abas Calendário e Plantel ({@code aba=plantel}, modelos 3b e 7a); clube de fora do
 * campeonato só o Plantel. Sem HTMX, a mesma janela vem numa página própria.
 */
@Controller
class ClubeController {

	private final ConsultasRepositorio consultas;
	private final CompraRepositorio compras;

	ClubeController(ConsultasRepositorio consultas, CompraRepositorio compras) {
		this.consultas = consultas;
		this.compras = compras;
	}

	@GetMapping("/jogos/{jogoId}/clube/{clubeId}")
	String clube(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @PathVariable long clubeId,
			@RequestParam(name = "aba", required = false) String aba,
			@RequestHeader(name = "HX-Request", required = false) String htmx, Model model) {
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("clubeId", clubeId);
		model.addAttribute("sobreposta", htmx != null);
		var calendario = consultas.calendario(jogoId, usuario.id(), clubeId);
		calendario.ifPresentOrElse(k -> model.addAttribute("k", k), () -> model.addAttribute("fora",
				consultas.clubeDeFora(jogoId, usuario.id(), clubeId).orElseThrow(NoSuchElementException::new)));
		if (calendario.isEmpty() || "plantel".equals(aba)) {
			model.addAttribute("vitrine", compras.vitrine(jogoId, usuario.id(), clubeId));
		}
		return htmx != null ? "clube-janela :: janela" : "clube";
	}

	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}
}
