package br.com.managerfoot.web;

import java.util.NoSuchElementException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.CompraRecusadaException;
import br.com.managerfoot.persistencia.CompraRepositorio;

/**
 * Compra direta pela janela do clube na classificação (SPEC §9.4, modelo 7a, rev. 51). O servidor
 * confere anúncio, caixa, plantel e rodada; a tela só mostra o resultado.
 */
@Controller
class MercadoController {

	private final CompraRepositorio compras;

	MercadoController(CompraRepositorio compras) {
		this.compras = compras;
	}

	@PostMapping("/jogos/{jogoId}/mercado/comprar")
	String comprar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, @RequestParam long jogador,
			@RequestParam long clube, @RequestParam(required = false) Integer divisao, RedirectAttributes redirect) {
		try {
			compras.comprar(jogoId, usuario.id(), jogador);
			redirect.addFlashAttribute("resultado", "Jogador comprado. Ele está no seu plantel, fora do banco.");
		}
		catch (CompraRecusadaException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		catch (NoSuchElementException e) {
			redirect.addFlashAttribute("erro", "Este jogador não está mais à venda.");
		}
		return "redirect:/jogos/" + jogoId + "/classificacao?calendario=" + clube + "&aba=plantel"
				+ (divisao == null ? "" : "&divisao=" + divisao);
	}
}
