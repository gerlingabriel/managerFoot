package br.com.managerfoot.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.SaveRecusadoException;
import br.com.managerfoot.persistencia.SaveRepositorio;
import br.com.managerfoot.persistencia.SaveRepositorio.ArquivoDeSave;

/** Baixar o jogo em arquivo, enviar o arquivo de volta e apagar o jogo do servidor (SPEC §3.5). */
@Controller
class SaveController {

	private final SaveRepositorio saves;

	SaveController(SaveRepositorio saves) {
		this.saves = saves;
	}

	@GetMapping("/jogos/{jogoId}/arquivo")
	Object baixar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		try {
			ArquivoDeSave arquivo = saves.exportar(jogoId, usuario.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
					.filename(nomeDoArquivo(arquivo), StandardCharsets.UTF_8).build().toString())
				.body(arquivo.conteudo());
		}
		catch (SaveRecusadoException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			return "redirect:/";
		}
	}

	@PostMapping("/jogos/arquivo")
	String enviar(@AuthenticationPrincipal Usuario usuario, @RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes redirect) {
		if (arquivo.isEmpty()) {
			redirect.addFlashAttribute("erro", "Escolha o arquivo do jogo salvo.");
			return "redirect:/";
		}
		try {
			saves.importar(arquivo.getInputStream(), usuario.id());
			redirect.addFlashAttribute("resultado", "Jogo carregado. Ele está na lista como um jogo novo.");
		}
		catch (SaveRecusadoException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		catch (IOException e) {
			redirect.addFlashAttribute("erro", "Não foi possível ler o arquivo. Tente enviar de novo.");
		}
		return "redirect:/";
	}

	@PostMapping("/jogos/{jogoId}/apagar")
	String apagar(@AuthenticationPrincipal Usuario usuario, @PathVariable long jogoId, RedirectAttributes redirect) {
		if (!saves.apagar(jogoId, usuario.id())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		redirect.addFlashAttribute("resultado", "Jogo apagado do servidor.");
		return "redirect:/";
	}

	/** "managerfoot-sao-jorge-2027-rodada-5.json" */
	static String nomeDoArquivo(ArquivoDeSave arquivo) {
		String clube = Normalizer.normalize(arquivo.clube(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		return "managerfoot-" + clube + "-" + arquivo.ano() + "-rodada-" + arquivo.rodada() + ".json";
	}
}
