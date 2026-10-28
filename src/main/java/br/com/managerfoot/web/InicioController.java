package br.com.managerfoot.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.managerfoot.contas.Usuario;

/** Início depois de entrar. Por enquanto só identifica o usuário; as opções entram na 3f.2. */
@Controller
class InicioController {

	@GetMapping("/")
	String inicio(@AuthenticationPrincipal Usuario usuario, Model model) {
		model.addAttribute("usuario", usuario);
		return "inicio";
	}
}
