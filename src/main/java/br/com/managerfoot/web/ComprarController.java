package br.com.managerfoot.web;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.FiltroMercado;
import br.com.managerfoot.economia.FiltroMercado.Asterisco;
import br.com.managerfoot.economia.FiltroMercado.Ordem;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.persistencia.CompraRepositorio;
import br.com.managerfoot.persistencia.CompraRepositorio.ListaDeCompra;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Aba "Comprar" (modelo 20b): os jogadores à venda da liga com filtros. Os filtros ficam guardados na
 * sessão, por jogo, enquanto o treinador está no jogo. A compra é a mesma da janela do clube
 * ({@link MercadoController}), que volta para cá.
 */
@Controller
class ComprarController {

	private final CompraRepositorio compras;

	ComprarController(CompraRepositorio compras) {
		this.compras = compras;
	}

	/**
	 * @param filtrar presente quando o formulário dos filtros foi enviado; sem ele, valem os filtros guardados
	 * @param precoDe inteiro na moeda da liga (pontos de milhar aceitos)
	 */
	@GetMapping("/jogos/{jogoId}/comprar")
	String comprar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId,
			@RequestParam(required = false) String filtrar, @RequestParam(name = "pos", required = false) List<Posicao> posicoes,
			@RequestParam(required = false) String forcaDe, @RequestParam(required = false) String forcaAte,
			@RequestParam(required = false) String precoDe, @RequestParam(required = false) String precoAte,
			@RequestParam(defaultValue = "TODOS") Asterisco asterisco, @RequestParam(defaultValue = "FORCA") Ordem ordem,
			@RequestParam(required = false) String cabe, HttpSession sessao, Model model) {
		String chave = "filtroComprar-" + jogoId;
		FiltroNaTela naTela;
		if (filtrar == null) {
			naTela = sessao.getAttribute(chave) instanceof FiltroNaTela guardado ? guardado : FiltroNaTela.PADRAO;
		} else {
			naTela = new FiltroNaTela(posicoes == null || posicoes.isEmpty() ? Set.of() : EnumSet.copyOf(posicoes),
					inteiro(forcaDe), inteiro(forcaAte), inteiro(precoDe), inteiro(precoAte), asterisco, ordem, cabe != null);
			sessao.setAttribute(chave, naTela);
		}
		// os limites de preço vão em centavos para o filtro
		ListaDeCompra lista = compras.lista(jogoId, usuario.id(), naTela.filtro(compras.moeda(jogoId)));
		model.addAttribute("usuario", usuario);
		model.addAttribute("jogoId", jogoId);
		model.addAttribute("l", lista);
		model.addAttribute("f", naTela);
		model.addAttribute("posicoes", Posicao.values());
		model.addAttribute("ordens", Ordem.values());
		model.addAttribute("asteriscos", Asterisco.values());
		return "comprar";
	}

	/** Jogo de outro usuário ou inexistente: para quem pede, ele não existe. */
	@ExceptionHandler(NoSuchElementException.class)
	void naoEncontrado(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

	/** Número digitado, com ou sem pontos de milhar; o que não é número fica sem limite. */
	static Long inteiro(String texto) {
		if (texto == null) {
			return null;
		}
		String limpo = texto.replaceAll("[\\s.]", "");
		if (!limpo.matches("\\d{1,12}")) {
			return null;
		}
		return Long.parseLong(limpo);
	}

	/**
	 * Os filtros como o treinador digitou, guardados na sessão.
	 *
	 * @param precoDe inteiro na moeda da liga
	 */
	record FiltroNaTela(Set<Posicao> posicoes, Long forcaDe, Long forcaAte, Long precoDe, Long precoAte, Asterisco asterisco,
			Ordem ordem, boolean soQuemCabe) implements java.io.Serializable {

		static final FiltroNaTela PADRAO = new FiltroNaTela(Set.of(), null, null, null, null, Asterisco.TODOS, Ordem.FORCA, true);

		public boolean marcada(Posicao posicao) {
			return posicoes.contains(posicao);
		}

		FiltroMercado filtro(Moeda moeda) {
			return new FiltroMercado(posicoes, forca(forcaDe), forca(forcaAte), precoDe == null ? null : moeda.centavos(precoDe),
					precoAte == null ? null : moeda.centavos(precoAte), asterisco, ordem, soQuemCabe);
		}

		private static Integer forca(Long valor) {
			return valor == null ? null : (int) Math.min(valor, Integer.MAX_VALUE);
		}
	}
}
