package br.com.managerfoot.web;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.persistencia.BancoEditavel;
import br.com.managerfoot.persistencia.BancoEditavelRepositorio;
import br.com.managerfoot.persistencia.BancoRecusadoException;
import jakarta.servlet.http.HttpSession;

/**
 * Banco editável no novo jogo (SPEC §3.5, modelos 17a a 17c): baixar o banco padrão, enviar o próprio arquivo e voltar
 * ao banco padrão. O arquivo aceito fica só na sessão, até criar o jogo.
 */
@Controller
class BancoEditavelController {

	/** Atributo da sessão com o arquivo aceito. */
	static final String SESSAO = "managerfoot.bancoEnviado";

	private final BancoEditavelRepositorio bancos;

	BancoEditavelController(BancoEditavelRepositorio bancos) {
		this.bancos = bancos;
	}

	@GetMapping("/jogos/novo/banco")
	ResponseEntity<byte[]> baixar() {
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
				.filename("managerfoot-banco-padrao.json", StandardCharsets.UTF_8).build().toString())
			.body(bancos.baixarBancoPadrao());
	}

	@PostMapping("/jogos/novo/banco")
	String enviar(@RequestParam("arquivo") MultipartFile arquivo, HttpSession sessao, RedirectAttributes redirect) {
		if (arquivo.isEmpty()) {
			redirect.addFlashAttribute("problemas", java.util.List.of("Escolha o arquivo do banco."));
			return "redirect:/jogos/novo";
		}
		try {
			byte[] conteudo = arquivo.getBytes();
			BancoEditavel banco = bancos.ler(conteudo);
			sessao.setAttribute(SESSAO, new BancoEnviado(nome(arquivo), conteudo));
			int campeonatos = banco.comLiga().size();
			redirect.addFlashAttribute("resultado", "Arquivo aceito: " + campeonatos + (campeonatos == 1 ? " campeonato." : " campeonatos."));
		}
		catch (BancoRecusadoException e) {
			redirect.addFlashAttribute("problemas", e.problemas());
		}
		catch (IOException e) {
			redirect.addFlashAttribute("problemas", java.util.List.of("Não foi possível ler o arquivo. Tente enviar de novo."));
		}
		return "redirect:/jogos/novo";
	}

	@PostMapping("/jogos/novo/banco/padrao")
	String usarPadrao(HttpSession sessao) {
		sessao.removeAttribute(SESSAO);
		return "redirect:/jogos/novo";
	}

	private static String nome(MultipartFile arquivo) {
		String nome = arquivo.getOriginalFilename();
		if (nome == null || nome.isBlank()) {
			return "arquivo enviado";
		}
		nome = nome.replaceAll(".*[/\\\\]", "");
		return nome.length() > 60 ? nome.substring(0, 60) : nome;
	}

	/** O arquivo aceito, guardado na sessão até criar o jogo. */
	record BancoEnviado(String nome, byte[] conteudo) implements Serializable {
	}
}
