package br.com.managerfoot.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.managerfoot.contas.EmailEmUsoException;
import br.com.managerfoot.contas.LinkInvalidoException;
import br.com.managerfoot.contas.RegrasConta;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Telas de entrar e de cadastro pelo link (SPEC §3.4 e §10.5, rev. 28). */
@Controller
class EntradaController {

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ContasRepositorio contas;
	private final SecurityContextRepository contexto;

	EntradaController(ContasRepositorio contas, SecurityContextRepository contexto) {
		this.contas = contas;
		this.contexto = contexto;
	}

	@GetMapping("/entrar")
	String entrar() {
		return "entrar";
	}

	@GetMapping("/cadastro")
	String cadastro(@RequestParam(name = "convite", required = false) String convite, Model model) {
		return telaCadastro(convite, model);
	}

	@PostMapping("/cadastro")
	String cadastrar(@RequestParam("convite") String convite, @RequestParam("nome") String nome,
			@RequestParam("email") String email, @RequestParam("senha") String senha,
			@RequestParam("senhaRepetida") String senhaRepetida, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		Map<String, String> erros = new LinkedHashMap<>(RegrasConta.problemasPorCampo(email, nome, senha));
		if (!erros.containsKey("senha") && !senha.equals(senhaRepetida)) {
			erros.put("senhaRepetida", "As senhas não conferem.");
		}
		if (erros.isEmpty()) {
			try {
				entrarNaSessao(contas.cadastrar(convite, email, nome, senha), request, response);
				return "redirect:/";
			}
			catch (LinkInvalidoException e) {
				return telaCadastro(convite, model);
			}
			catch (EmailEmUsoException e) {
				erros.put("email", e.getMessage());
			}
		}
		model.addAttribute("nome", nome);
		model.addAttribute("email", email);
		model.addAttribute("erros", erros);
		return telaCadastro(convite, model);
	}

	private String telaCadastro(String convite, Model model) {
		Optional<Instant> validade = contas.validadeDoLink(convite);
		model.addAttribute("convite", convite);
		model.addAttribute("conviteValido", validade.isPresent());
		validade.ifPresent(v -> model.addAttribute("validoAte", DATA.format(v.atZone(ZoneId.systemDefault()))));
		return "cadastro";
	}

	/** Cadastro já entra: troca o id da sessão (contra fixação de sessão) e grava o usuário nela. */
	private void entrarNaSessao(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		Authentication autenticacao = AutenticacaoContas.autenticado(usuario);
		SecurityContext sc = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
		sc.setAuthentication(autenticacao);
		SecurityContextHolder.getContextHolderStrategy().setContext(sc);
		contexto.saveContext(sc, request, response);
	}
}
