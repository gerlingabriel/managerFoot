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
import br.com.managerfoot.persistencia.CaixaRepositorio;
import br.com.managerfoot.persistencia.ClubeNaFilaException;
import br.com.managerfoot.persistencia.CompraRepositorio;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abas Classificação, Artilheiros da temporada e Artilheiros de todos os tempos (SPEC §10.3, rev.
 * 16; modelos aprovados). Com {@code ficha=<id>}, a aba abre a ficha do jogador por cima; na
 * classificação, {@code calendario=<clube>} abre o calendário do clube (rev. 17 e 36). A aba
 * Finanças mostra só o clube do usuário (modelo 6a, rev. 39). A janela do clube tem também a aba
 * Plantel, com os jogadores à venda ({@code aba=plantel}, modelo 7a, rev. 51). A aba Histórico tem os
 * campeões e o ranking de treinadores (modelo 8a, rev. 54).
 */
@Controller
class AbasController {

	private final ConsultasRepositorio consultas;
	private final CaixaRepositorio caixas;
	private final CompraRepositorio compras;

	AbasController(ConsultasRepositorio consultas, CaixaRepositorio caixas, CompraRepositorio compras) {
		this.consultas = consultas;
		this.caixas = caixas;
		this.compras = compras;
	}

	@GetMapping("/jogos/{jogoId}/classificacao")
	String classificacao(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "divisao", required = false) Integer divisao,
			@RequestParam(name = "calendario", required = false) Long calendario,
			@RequestParam(name = "aba", required = false) String aba, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("c", consultas.classificacao(jogoId, usuario.id(), divisao));
		if (calendario != null) {
			consultas.calendario(jogoId, usuario.id(), calendario).ifPresent(cal -> {
				model.addAttribute("calendario", cal);
				// a janela do clube tem as abas Calendário e Plantel (modelo 7a, rev. 51)
				if ("plantel".equals(aba)) {
					model.addAttribute("vitrine", compras.vitrine(jogoId, usuario.id(), calendario));
				}
			});
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

	@GetMapping("/jogos/{jogoId}/financas")
	String financas(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("f", caixas.financas(jogoId, usuario.id()));
		return "financas";
	}

	/** Histórico de campeões e ranking de treinadores (SPEC §8.1.1, modelo 8a, rev. 54). */
	@GetMapping("/jogos/{jogoId}/historico")
	String historico(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("h", consultas.historico(jogoId, usuario.id()));
		return "historico";
	}

	/** Clube na fila de espera: não joga a temporada e não tem extrato (SPEC §4.1). */
	@ExceptionHandler(ClubeNaFilaException.class)
	String naFila(Model model) {
		model.addAttribute("mensagem", "Seu clube caiu para a fila de espera e não joga esta temporada, "
				+ "por isso não tem finanças da temporada.");
		return "aviso";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}
}
