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
import br.com.managerfoot.persistencia.ConsultasContinentalRepositorio;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import br.com.managerfoot.persistencia.EstadioRepositorio;
import br.com.managerfoot.temporada.FaseCopa;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abas Classificação, Artilheiros da temporada e Artilheiros de todos os tempos (SPEC §10.3, rev.
 * 16; modelos aprovados). Com {@code ficha=<id>}, a aba abre a ficha do jogador por cima. A aba
 * Finanças mostra só o clube do usuário (modelo 6a, rev. 39). A aba Histórico tem os
 * campeões e o ranking de treinadores (modelo 8a, rev. 54). A aba Copa tem as fases, os confrontos e a
 * chave da copa da temporada (modelo 12a, rev. 71).
 */
@Controller
class AbasController {

	private final ConsultasRepositorio consultas;
	private final CaixaRepositorio caixas;
	private final EstadioRepositorio estadios;
	private final ConsultasContinentalRepositorio continentais;

	AbasController(ConsultasRepositorio consultas, CaixaRepositorio caixas,
			EstadioRepositorio estadios, ConsultasContinentalRepositorio continentais) {
		this.consultas = consultas;
		this.caixas = caixas;
		this.estadios = estadios;
		this.continentais = continentais;
	}

	@GetMapping("/jogos/{jogoId}/classificacao")
	String classificacao(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "divisao", required = false) Integer divisao,
			Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("c", consultas.classificacao(jogoId, usuario.id(), divisao));
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

	/** Com {@code estadio}, abre a janela do estádio por cima (modelo 6c, rev. 55). */
	@GetMapping("/jogos/{jogoId}/financas")
	String financas(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "estadio", defaultValue = "false") boolean estadio, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("f", caixas.financas(jogoId, usuario.id()));
		if (estadio) {
			model.addAttribute("estadio", estadios.janela(jogoId, usuario.id()));
		}
		return "financas";
	}

	/** Aba Continental (SPEC §8.3.6, modelo 13b, rev. 79): fases, grupos ou confrontos da fase escolhida e chave. */
	@GetMapping("/jogos/{jogoId}/continental")
	String continental(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "fase", required = false) String fase,
			@RequestParam(name = "grupo", required = false) Integer grupo, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("continental", continentais.visao(jogoId, usuario.id(), fase, grupo).orElse(null));
		return "continental";
	}

	/** Aba Copa (SPEC §8.2, modelo 12a, rev. 71): fases, confrontos da fase escolhida e chave. */
	@GetMapping("/jogos/{jogoId}/copa")
	String copa(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(name = "fase", required = false) FaseCopa fase, Model model) {
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("copa", consultas.copa(jogoId, usuario.id(), fase).orElse(null));
		return "copa";
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
