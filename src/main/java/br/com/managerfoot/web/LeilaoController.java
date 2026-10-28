package br.com.managerfoot.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import br.com.managerfoot.persistencia.LeilaoRepositorio;
import br.com.managerfoot.persistencia.LeilaoRepositorio.VisaoLeiloes;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Leilões da máquina antes da rodada (SPEC §9.4, rev. 41 e 42; modelo 7b): os lances do treinador
 * e o resultado; e a venda de um jogador do treinador por leilão, pela ficha (rev. 53). "Ir para a rodada" volta a {@link RodadaController}, que começa a rodada.
 */
@Controller
class LeilaoController {

	private static final String PREFIXO_LANCE = "lance-";

	private final LeilaoRepositorio leiloes;

	LeilaoController(LeilaoRepositorio leiloes) {
		this.leiloes = leiloes;
	}

	@GetMapping("/jogos/{jogoId}/leiloes")
	String leiloes(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, Model model) {
		VisaoLeiloes v = leiloes.visao(jogoId, usuario.id()).orElse(null);
		if (v == null) {
			return "redirect:/jogos/" + jogoId + "/plantel";
		}
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("v", v);
		model.addAttribute("venda", false);
		return "leiloes";
	}

	/** Resultado de um jogador do treinador posto em leilão pela ficha (mesma tela do modelo 7b). */
	@GetMapping("/jogos/{jogoId}/leiloes/{leilaoId}")
	String venda(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @PathVariable long leilaoId, Model model) {
		VisaoLeiloes v = leiloes.venda(jogoId, usuario.id(), leilaoId).orElseThrow(NoSuchElementException::new);
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("v", v);
		model.addAttribute("venda", true);
		return "leiloes";
	}

	/**
	 * "Vender por leilão" na ficha do jogador (SPEC §9.4, rev. 53): o lance mínimo é inteiro na moeda
	 * da liga. O leilão é resolvido na hora; recusado, volta à ficha com o motivo.
	 */
	@PostMapping("/jogos/{jogoId}/mercado/vender")
	String vender(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam long jogador,
			@RequestParam String minimo, RedirectAttributes redirect) {
		String ficha = "redirect:/jogos/" + jogoId + "/plantel?ficha=" + jogador;
		long valor;
		try {
			valor = Long.parseLong(minimo.replaceAll("[\\s.]", ""));
		}
		catch (NumberFormatException e) {
			redirect.addFlashAttribute("erro", "Digite o lance mínimo só com números.");
			return ficha;
		}
		try {
			return "redirect:/jogos/" + jogoId + "/leiloes/" + leiloes.venderPorLeilao(jogoId, usuario.id(), jogador, valor);
		}
		catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			return ficha;
		}
	}

	/**
	 * "Confirmar lances" ou "Não quero nenhum": campos {@code lance-<id do leilão>} com o valor inteiro
	 * na moeda da liga; campo vazio é sem lance.
	 */
	@PostMapping("/jogos/{jogoId}/leiloes")
	String darLances(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam("rodada") int rodada,
			@RequestParam(name = "nenhum", required = false) String nenhum, @RequestParam Map<String, String> campos,
			RedirectAttributes redirect) {
		Map<Long, Long> lances = new HashMap<>();
		if (nenhum == null) {
			for (var campo : campos.entrySet()) {
				String valor = campo.getValue().replaceAll("[\\s.]", "");
				if (!campo.getKey().startsWith(PREFIXO_LANCE) || valor.isEmpty()) {
					continue;
				}
				try {
					lances.put(Long.parseLong(campo.getKey().substring(PREFIXO_LANCE.length())), Long.parseLong(valor));
				}
				catch (NumberFormatException e) {
					redirect.addFlashAttribute("erro", "Digite os lances só com números.");
					return voltar(jogoId);
				}
			}
		}
		try {
			leiloes.darLances(jogoId, usuario.id(), rodada, lances);
		}
		catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		catch (IllegalStateException e) {
			// já resolvidos (clique duplo): a tela mostra o resultado
		}
		return voltar(jogoId);
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

	private static String voltar(long jogoId) {
		return "redirect:/jogos/" + jogoId + "/leiloes";
	}
}
