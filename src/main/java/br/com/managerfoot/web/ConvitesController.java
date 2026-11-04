package br.com.managerfoot.web;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;

/**
 * "Gerar link de cadastro" no Início do administrador (SPEC §3.4; modelo 20c), no lugar do comando no
 * terminal. O link só aparece uma vez, na volta ao Início: o servidor guarda só o hash.
 */
@Controller
class ConvitesController {

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ContasRepositorio contas;
	private final Clock relogio;
	private final String urlBase;

	ConvitesController(ContasRepositorio contas, Clock relogio, @Value("${managerfoot.url-base}") String urlBase) {
		this.contas = contas;
		this.relogio = relogio;
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
	}

	@PostMapping("/convites/cadastro")
	String gerar(@AuthenticationPrincipal Usuario usuario, RedirectAttributes redirect) {
		String token = contas.gerarLinkCadastro(usuario.id());
		redirect.addFlashAttribute("linkCadastro", urlBase + "/cadastro?convite=" + token);
		redirect.addFlashAttribute("linkCadastroVale",
				DATA.format(relogio.instant().plus(contas.validadeLink()).atZone(ZoneId.systemDefault())));
		return "redirect:/";
	}

	/** Endereço que só abre no próprio computador: o link não serve para outra pessoa. */
	static boolean soNesteComputador(String urlBase) {
		return urlBase.matches("(?i)https?://(localhost|127\\.0\\.0\\.1|\\[::1\\])(:\\d+)?/?");
	}

	/** Pedido de quem não é administrador: a tela não oferece o botão. */
	@ExceptionHandler(AcessoNegadoException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ResponseBody
	String negado() {
		return "Só o administrador gera link de cadastro.";
	}
}
