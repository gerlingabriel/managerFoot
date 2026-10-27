package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/** Molde jogável para testes de integração: liga com planteis completos e um jogo criado dele. */
final class LigaDeTeste {

	static final String SENHA = "senha-forte-1";
	private static final AtomicInteger SEQUENCIA = new AtomicInteger();

	private LigaDeTeste() {
	}

	/**
	 * Cria no molde uma liga com Série A de 16, Série B de 15 (número ímpar: há folga) e fila de 2,
	 * todos com o elenco padrão de 23, e devolve o id do jogo criado dela, com dono participante.
	 */
	static long criarJogo(MoldeRepositorio moldes, JogoRepositorio jogos, ContasRepositorio contas) {
		long pais = moldes.criarPais(moldes.criarMolde("Padrão"), "BRA", "Brasil");
		ConfiguracaoLiga liga = moldes.criarLiga(pais, new ConfiguracaoLiga("Nacional",
				List.of(new Divisao("Série A", 1, 50, 0, 2, clubes("A", 16, 35)),
						new Divisao("Série B", 2, 30, 2, 2, clubes("B", 15, 22))),
				clubes("Fila", 2, 15)));
		SplittableRandom aleatorio = new SplittableRandom(11);
		liga.divisoes().forEach(d -> d.clubes().forEach(c -> moldes.adicionarJogadores(c.id(), plantel(c, aleatorio))));
		liga.filaDeEspera().forEach(c -> moldes.adicionarJogadores(c.id(), plantel(c, aleatorio)));
		return jogos.criarJogoComClubeSorteado(pais, participante(contas).id(), 1);
	}

	static Usuario administrador(ContasRepositorio contas) {
		return contas.criarAdministrador(email("admin"), "Admin", SENHA);
	}

	/** Participante cadastrado por um link de um administrador novo. */
	static Usuario participante(ContasRepositorio contas) {
		String token = contas.gerarLinkCadastro(administrador(contas).id());
		return contas.cadastrar(token, email("participante"), "Participante", SENHA);
	}

	private static String email(String prefixo) {
		return prefixo + "-liga-" + SEQUENCIA.incrementAndGet() + "-" + System.nanoTime() + "@exemplo.com";
	}

	private static List<Clube> clubes(String prefixo, int quantidade, int forca) {
		return IntStream.rangeClosed(1, quantidade).mapToObj(i -> new Clube(0, prefixo + " " + i, forca)).toList();
	}

	/** Elenco padrão: 3 goleiros, 7 defensores, 7 meias e 6 atacantes, perto da força do clube. */
	private static List<Jogador> plantel(Clube clube, SplittableRandom aleatorio) {
		int[] quantidades = { 3, 7, 7, 6 };
		Temperamento[] temperamentos = Temperamento.values();
		List<Jogador> plantel = new ArrayList<>();
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < quantidades[posicao.ordinal()]; i++) {
				plantel.add(new Jogador(0, clube.nome() + " " + posicao.name() + " " + i, posicao,
						Math.clamp(clube.forca() + aleatorio.nextInt(-5, 6), 1, 50),
						temperamentos[aleatorio.nextInt(temperamentos.length)], aleatorio.nextInt(10) == 0, "BRA"));
			}
		}
		return plantel;
	}
}
