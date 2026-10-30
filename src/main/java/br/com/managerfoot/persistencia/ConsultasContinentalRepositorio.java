package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.Premiacao;
import br.com.managerfoot.persistencia.ConsultasRepositorio.ConfrontoNaCopa;
import br.com.managerfoot.persistencia.ConsultasRepositorio.FaseDaChave;
import br.com.managerfoot.persistencia.ConsultasRepositorio.JogoDaChave;
import br.com.managerfoot.persistencia.ConsultasRepositorio.SituacaoDaFase;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.GrupoContinental;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.ParametrosContinental;
import br.com.managerfoot.temporada.Temporada;

/** Consulta da aba Continental (SPEC §8.3.6, modelo 13b, rev. 79). Só leitura. */
@Repository
public class ConsultasContinentalRepositorio {

	/** Valor de {@code fase} na aba para a fase de grupos. */
	private static final String GRUPOS = "GRUPOS";

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final ContinentalRepositorio continentais;
	private final Premiacao premiacao = Premiacao.padrao();

	public ConsultasContinentalRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			ContinentalRepositorio continentais) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.continentais = continentais;
	}

	/**
	 * A aba Continental da temporada atual; vazio se ela não tem continental.
	 *
	 * @param fase {@code GRUPOS} ou o nome de uma {@link FaseCopa}; sem ela, ou inválida, a fase atual
	 * @param grupo índice do grupo cujos jogos aparecem; sem ele, o do clube ou o primeiro
	 * @throws NoSuchElementException se o usuário não é treinador do jogo
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoContinental> visao(long jogoId, long usuarioId, String fase, Integer grupo) {
		long clubeId = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		Temporada temporada = temporadas.carregarAtual(jogoId);
		long temporadaId = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo AND numero = :numero")
			.param("jogo", jogoId).param("numero", temporada.numero()).query(Long.class).single();
		Optional<Continental> carregada = continentais.carregar(jogoId, temporadaId);
		if (carregada.isEmpty()) {
			return Optional.empty();
		}
		Continental continental = carregada.get();
		List<Continental.Etapa> quadro = continentais.quadro(temporadaId);
		Map<Long, String> paises = continentais.paisesDeFora(jogoId);
		Optional<FaseCopa> mostrada = faseMostrada(continental, fase);
		long semente = temporada.semente();

		List<GrupoNaTela> grupos = continental.grupos().stream().map(g -> grupoNaTela(g, semente, clubeId, paises)).toList();
		Optional<GrupoContinental> doClube = continental.grupos().stream()
			.filter(g -> g.clubes().stream().anyMatch(c -> c.id() == clubeId)).findFirst();
		int indiceDoGrupo = grupo != null && grupo >= 0 && grupo < continental.grupos().size() ? grupo
				: doClube.map(GrupoContinental::indice).orElse(0);
		GrupoContinental escolhido = continental.grupos().get(indiceDoGrupo);

		List<Copa.Etapa> quadroDoMataMata = quadro.stream().filter(e -> !e.grupos()).map(Continental.Etapa::mataMata).toList();
		List<ConfrontoNaCopa> confrontos = mostrada.isEmpty() ? List.of()
				: continental.mataMata().map(m -> m.confrontosDa(mostrada.get()).stream()
					.map(c -> ConsultasRepositorio.confrontoNaCopa(c, quadroDoMataMata, clubeId))
					.toList()).orElse(List.of());
		int teto = jogos.carregarLiga(jogoId).divisoes().getFirst().limiteForca();
		String pais = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId).query(String.class).single();
		int numeroDePaises = (int) Stream.concat(paises.values().stream().filter(p -> continental.grupos().stream()
			.flatMap(g -> g.clubes().stream()).anyMatch(c -> p.equals(paises.get(c.id())))), Stream.of(pais)).distinct().count();
		return Optional.of(new VisaoContinental(continentais.nome(jogoId), jogos.ano(jogoId, temporada.numero()),
				continental.grupos().stream().mapToInt(g -> g.clubes().size()).sum(), numeroDePaises,
				premiacao.premioDaCopa(1, teto), premiacao.premioDaCopa(2, teto), Moeda.doPais(pais),
				situacao(continental, clubeId, semente), fases(continental, quadro, mostrada), mostrada.isEmpty(), grupos,
				escolhido.nome(), jogosDoGrupo(escolhido, quadro, clubeId, paises), mostrada.map(FaseCopa::nome).orElse(null),
				mostrada.map(FaseCopa::jogoUnico).orElse(false), confrontos, chave(continental, mostrada, clubeId),
				continental.campeao().map(Clube::nome).orElse(null)));
	}

	/** A fase pedida, ou a atual; vazio é a fase de grupos. A fase que ainda não existe mostra a chave. */
	private static Optional<FaseCopa> faseMostrada(Continental continental, String fase) {
		if (GRUPOS.equals(fase)) {
			return Optional.empty();
		}
		for (FaseCopa f : FaseCopa.values()) {
			if (f.name().equals(fase) && f.ordinal() >= FaseCopa.OITAVAS.ordinal()) {
				return Optional.of(f);
			}
		}
		return continental.mataMata().map(Copa::faseAtual);
	}

	private static GrupoNaTela grupoNaTela(GrupoContinental g, long semente, long clubeId, Map<Long, String> paises) {
		List<LinhaClassificacao> tabela = g.classificacao(semente);
		List<LinhaDoGrupo> linhas = new ArrayList<>();
		for (int i = 0; i < tabela.size(); i++) {
			LinhaClassificacao l = tabela.get(i);
			linhas.add(new LinhaDoGrupo(l.clube().nome(), paises.get(l.clube().id()), l.jogos(), l.pontos(), l.saldo(),
					i < ParametrosContinental.CLASSIFICADOS_POR_GRUPO, l.clube().id() == clubeId));
		}
		return new GrupoNaTela(g.indice(), g.nome(), linhas, linhas.stream().anyMatch(LinhaDoGrupo::voce));
	}

	private static List<JogoDoGrupo> jogosDoGrupo(GrupoContinental g, List<Continental.Etapa> quadro, long clubeId,
			Map<Long, String> paises) {
		return g.partidas().stream().map(p -> new JogoDoGrupo(
				p.jogo() + "º · depois da " + quadro.get(p.jogo() - 1).depoisDaRodada(), p.mandante().nome(),
				paises.get(p.mandante().id()), p.placar().map(l -> l.golsMandante() + " x " + l.golsVisitante()).orElse("—"),
				p.visitante().nome(), paises.get(p.visitante().id()), p.mandante().id() == clubeId || p.visitante().id() == clubeId))
			.toList();
	}

	/** "Grupo C · 2º lugar", "Eliminado na fase de grupos", "Eliminado: Quartas de final", "Campeão". */
	private static String situacao(Continental continental, long clubeId, long semente) {
		Optional<GrupoContinental> grupo = continental.grupos().stream()
			.filter(g -> g.clubes().stream().anyMatch(c -> c.id() == clubeId)).findFirst();
		if (grupo.isEmpty()) {
			return "Fora da continental nesta temporada";
		}
		Clube clube = grupo.get().clubes().stream().filter(c -> c.id() == clubeId).findFirst().orElseThrow();
		if (continental.mataMata().isEmpty()) {
			List<LinhaClassificacao> tabela = grupo.get().classificacao(semente);
			for (int i = 0; i < tabela.size(); i++) {
				if (tabela.get(i).clube().equals(clube)) {
					return grupo.get().nome() + " · " + (i + 1) + "º lugar";
				}
			}
		}
		Copa mataMata = continental.mataMata().orElseThrow();
		if (continental.campeao().map(clube::equals).orElse(false)) {
			return "Campeão";
		}
		Optional<ConfrontoCopa> eliminacao = mataMata.confrontos().stream()
			.filter(c -> c.perdedor().map(clube::equals).orElse(false)).findFirst();
		if (eliminacao.isPresent()) {
			return eliminacao.get().fase().jogoUnico() ? "Vice-campeão" : "Eliminado: " + eliminacao.get().fase().nome();
		}
		boolean noMataMata = mataMata.confrontos().stream().anyMatch(c -> c.envolve(clube));
		return noMataMata ? "Na disputa · " + mataMata.faseAtual().nome() : "Eliminado na fase de grupos";
	}

	private static List<FaseNaContinental> fases(Continental continental, List<Continental.Etapa> quadro,
			Optional<FaseCopa> mostrada) {
		List<FaseNaContinental> fases = new ArrayList<>();
		List<Continental.Etapa> dosGrupos = quadro.stream().filter(Continental.Etapa::grupos).toList();
		long feitos = dosGrupos.stream()
			.filter(e -> continental.grupos().stream().allMatch(g -> g.partidasDoJogo(e.jogoDoGrupo()).stream()
				.allMatch(p -> p.placar().isPresent())))
			.count();
		boolean gruposFeitos = feitos == dosGrupos.size();
		String quando = gruposFeitos ? "feita · " + feitos + " jogos"
				: feitos + " de " + dosGrupos.size() + " jogos · próximo depois da " + dosGrupos.get((int) feitos).depoisDaRodada();
		fases.add(new FaseNaContinental(GRUPOS, "Grupos", quando, gruposFeitos ? SituacaoDaFase.TERMINADA : SituacaoDaFase.JOGANDO,
				mostrada.isEmpty()));
		Optional<Copa> mataMata = continental.mataMata();
		for (FaseCopa f = FaseCopa.OITAVAS; f != null; f = f.seguinte().orElse(null)) {
			FaseCopa estaFase = f;
			List<Continental.Etapa> etapas = quadro.stream()
				.filter(e -> !e.grupos() && e.mataMata().fase() == estaFase).toList();
			List<ConfrontoCopa> daFase = mataMata.map(m -> m.confrontosDa(estaFase)).orElse(List.of());
			boolean feita = !daFase.isEmpty() && daFase.stream().allMatch(ConfrontoCopa::decidido);
			boolean idaFeita = !daFase.isEmpty() && daFase.stream().allMatch(c -> c.ida().isPresent());
			String texto = f.jogoUnico() ? (feita ? "feita · jogo único" : "jogo único depois da final da copa")
					: feita ? "feita · rodadas " + etapas.get(0).depoisDaRodada() + " e " + etapas.get(1).depoisDaRodada()
					: idaFeita ? "ida feita · volta depois da " + etapas.get(1).depoisDaRodada()
					: "depois das rodadas " + etapas.get(0).depoisDaRodada() + " e " + etapas.get(1).depoisDaRodada();
			SituacaoDaFase situacao = feita ? SituacaoDaFase.TERMINADA
					: mataMata.map(m -> m.faseAtual() == estaFase).orElse(false) ? SituacaoDaFase.JOGANDO
					: SituacaoDaFase.AGUARDANDO;
			fases.add(new FaseNaContinental(f.name(), f.nome(), texto, situacao, mostrada.filter(estaFase::equals).isPresent()));
		}
		return fases;
	}

	/** A chave depois da fase mostrada; nos grupos, os códigos até a final (modelo 13b). */
	private static List<FaseDaChave> chave(Continental continental, Optional<FaseCopa> mostrada, long clubeId) {
		if (continental.mataMata().isPresent() && mostrada.isPresent()) {
			return ConsultasRepositorio.chave(continental.mataMata().get(), mostrada.get(), clubeId);
		}
		List<FaseDaChave> chave = new ArrayList<>();
		chave.add(new FaseDaChave(FaseCopa.OITAVAS.nome(),
				List.of(new JogoDaChave(continental.mataMata().isPresent() ? "Sorteio já feito: veja a fase" : "Sorteio depois do 6º jogo dos grupos", false, true))));
		int confrontos = 8;
		String prefixo = "Venc. ";
		for (FaseCopa f = FaseCopa.QUARTAS; f != null; f = f.seguinte().orElse(null)) {
			List<JogoDaChave> jogos = new ArrayList<>();
			for (int k = 0; k < confrontos / 2; k++) {
				jogos.add(new JogoDaChave(prefixo + (2 * k + 1) + " × " + prefixo + (2 * k + 2), false, true));
			}
			chave.add(new FaseDaChave(f.jogoUnico() ? f.nome() + " · campo neutro" : f.nome(), jogos));
			prefixo = "Venc. " + f.nome().substring(0, 1);
			confrontos /= 2;
		}
		return chave;
	}

	/**
	 * @param clubes clubes nos grupos
	 * @param paises países que têm clube nos grupos
	 * @param seuClube situação do clube do usuário ("Grupo C · 2º lugar")
	 * @param nosGrupos a aba mostra a fase de grupos
	 * @param grupoDosJogos nome do grupo de {@code jogos}
	 * @param fase nome da fase de mata-mata mostrada; nulo nos grupos
	 * @param campeao nome do campeão; nulo enquanto a final não foi jogada
	 */
	public record VisaoContinental(String nome, int ano, int clubes, int paises, long premioCampeao, long premioVice,
			Moeda moeda, String seuClube, List<FaseNaContinental> fases, boolean nosGrupos, List<GrupoNaTela> grupos,
			String grupoDosJogos, List<JogoDoGrupo> jogos, String fase, boolean jogoUnico, List<ConfrontoNaCopa> confrontos,
			List<FaseDaChave> chave, String campeao) {
	}

	/** @param parametro valor de {@code fase} no link da fase */
	public record FaseNaContinental(String parametro, String nome, String quando, SituacaoDaFase situacao, boolean mostrada) {
	}

	/** @param seu é o grupo do clube do usuário */
	public record GrupoNaTela(int indice, String nome, List<LinhaDoGrupo> linhas, boolean seu) {
	}

	/**
	 * @param pais sigla do clube de fora; nulo no clube do país jogado
	 * @param passa está entre os que passam agora
	 */
	public record LinhaDoGrupo(String clube, String pais, int jogos, int pontos, int saldo, boolean passa, boolean voce) {

		public String saldoTexto() {
			return saldo > 0 ? "+" + saldo : String.valueOf(saldo).replace('-', '−');
		}
	}

	/** @param jogo "1º · depois da 2" */
	public record JogoDoGrupo(String jogo, String mandante, String paisMandante, String placar, String visitante,
			String paisVisitante, boolean doUsuario) {
	}
}
