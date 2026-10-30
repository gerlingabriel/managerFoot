package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.motor.CondicoesPartida;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.Partida;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Uma rodada preparada para jogar ao vivo (docs/decisions/0010): escalações e sementes de todas as
 * partidas fixadas antes do primeiro minuto. Qualquer partida pode ser refeita até um minuto, com as
 * substituições feitas pelos treinadores, e dá sempre o mesmo jogo. Domínio puro: quem chama grava
 * as substituições e o resultado. É criada por {@link ProcessamentoRodada#preparar}.
 */
public final class RodadaAoVivo {

	/**
	 * Substituição feita por um treinador entre os minutos.
	 *
	 * @param mandanteId identifica a partida na rodada (cada clube joga uma vez por rodada)
	 * @param minuto minuto já jogado quando a troca foi feita (0 = antes do início)
	 */
	public record Substituicao(long mandanteId, int minuto, Lado lado, long saiId, long entraId) {
	}

	/** Uma partida da rodada com tudo o que a reproduz. */
	public record PartidaDaRodada(int prioridade, Jogo jogo, long semente, Escalacao casa, Escalacao fora,
			Set<Lado> ladosDoUsuario, CondicoesPartida condicoes) {

		public Clube clube(Lado lado) {
			return lado == Lado.CASA ? jogo.mandante() : jogo.visitante();
		}
	}

	private final MotorPartida motor;
	private final RegrasPartida regras;
	private final ParametrosSuspensao suspensao;
	private final ParametrosMoral parametrosMoral;
	private final List<PartidaDaRodada> partidas;
	private final Map<Long, List<Jogador>> planteis;
	private final Disciplina disciplina;
	private final long sementeDisciplina;
	private final List<Substituicao> substituicoes;

	RodadaAoVivo(MotorPartida motor, RegrasPartida regras, ParametrosSuspensao suspensao, ParametrosMoral parametrosMoral,
			List<PartidaDaRodada> partidas, Map<Long, List<Jogador>> planteis, Disciplina disciplina, long sementeDisciplina,
			List<Substituicao> substituicoes) {
		this.motor = motor;
		this.regras = regras;
		this.suspensao = suspensao;
		this.parametrosMoral = parametrosMoral;
		this.partidas = List.copyOf(partidas);
		this.planteis = planteis;
		this.disciplina = disciplina;
		this.sementeDisciplina = sementeDisciplina;
		this.substituicoes = List.copyOf(substituicoes);
	}

	/** Partidas na ordem de jogo: da divisão de maior número de prioridade até a 1ª. */
	public List<PartidaDaRodada> partidas() {
		return partidas;
	}

	/** A partida refeita até o minuto (ou até o fim, se o minuto passa dele), com as substituições. */
	public Partida noMinuto(PartidaDaRodada partida, int minuto) {
		Partida p = motor.iniciar(regras, partida.casa(), partida.fora(), partida.ladosDoUsuario(), partida.condicoes(),
				partida.semente());
		aplicarSubstituicoes(p, partida, 0);
		while (!p.terminada() && p.minuto() < minuto) {
			p.avancarMinuto();
			aplicarSubstituicoes(p, partida, p.minuto());
		}
		return p;
	}

	/**
	 * Confere uma substituição do treinador no minuto e a devolve para ser gravada (SPEC §7.4.1).
	 *
	 * @throws IllegalArgumentException se a troca não é permitida, com o motivo para a tela
	 */
	public Substituicao substituir(PartidaDaRodada partida, int minuto, Lado lado, long saiId, long entraId) {
		Partida p = trocar(partida, minuto, lado, saiId, entraId);
		return new Substituicao(partida.jogo().mandante().id(), p.minuto(), lado, saiId, entraId);
	}

	private Partida trocar(PartidaDaRodada partida, int minuto, Lado lado, long saiId, long entraId) {
		if (!partida.ladosDoUsuario().contains(lado)) {
			throw new IllegalArgumentException("Só o treinador do clube faz substituições.");
		}
		Partida p = noMinuto(partida, minuto);
		if (p.terminada()) {
			throw new IllegalArgumentException("A partida já terminou.");
		}
		Jogador sai = p.emCampo(lado).stream().filter(j -> j.id() == saiId).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Quem sai precisa estar em campo."));
		Jogador entra = p.reservasDisponiveis(lado).stream().filter(j -> j.id() == entraId).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Quem entra precisa estar no banco e ainda não ter jogado."));
		p.substituir(lado, sai, entra);
		return p;
	}

	/** Uma rodada igual a esta com mais uma substituição. */
	public RodadaAoVivo com(Substituicao substituicao) {
		List<Substituicao> todas = new ArrayList<>(substituicoes);
		todas.add(Objects.requireNonNull(substituicao));
		return new RodadaAoVivo(motor, regras, suspensao, parametrosMoral, partidas, planteis, disciplina, sementeDisciplina,
				todas);
	}

	/** Joga todas as partidas até o fim e calcula disciplina e moral, como a rodada rápida. */
	public ProcessamentoRodada.Resultado finalizar(Map<Long, Moral> moral) {
		SplittableRandom aleatorio = new SplittableRandom(sementeDisciplina);
		Map<Long, Moral> novaMoral = new HashMap<>(moral);
		Disciplina depois = disciplina;
		List<ProcessamentoRodada.PartidaJogada> jogadas = new ArrayList<>();
		for (PartidaDaRodada partida : partidas) {
			ResultadoPartida resultado = noMinuto(partida, Integer.MAX_VALUE).resultado();
			jogadas.add(new ProcessamentoRodada.PartidaJogada(partida.prioridade(), partida.jogo(), partida.semente(),
					resultado));
			List<Long> jogadores = new ArrayList<>();
			planteis.getOrDefault(partida.jogo().mandante().id(), List.of()).forEach(j -> jogadores.add(j.id()));
			planteis.getOrDefault(partida.jogo().visitante().id(), List.of()).forEach(j -> jogadores.add(j.id()));
			depois = depois.aposPartida(jogadores, resultado, suspensao, aleatorio,
					Map.of(Lado.CASA, partida.jogo().mandante().id(), Lado.FORA, partida.jogo().visitante().id()));
			double forcaCasa = Clube.forcaEmCampo(planteis.getOrDefault(partida.jogo().mandante().id(), List.of()));
			double forcaFora = Clube.forcaEmCampo(planteis.getOrDefault(partida.jogo().visitante().id(), List.of()));
			atualizarMoral(novaMoral, partida.jogo().mandante(), true, resultado.golsCasa(), resultado.golsFora(), forcaCasa,
					forcaFora);
			atualizarMoral(novaMoral, partida.jogo().visitante(), false, resultado.golsFora(), resultado.golsCasa(),
					forcaFora, forcaCasa);
		}
		return new ProcessamentoRodada.Resultado(jogadas, depois, novaMoral);
	}

	private void aplicarSubstituicoes(Partida p, PartidaDaRodada partida, int minuto) {
		for (Substituicao s : substituicoes) {
			if (s.mandanteId() == partida.jogo().mandante().id() && s.minuto() == minuto && !p.terminada()) {
				Jogador sai = p.emCampo(s.lado()).stream().filter(j -> j.id() == s.saiId()).findFirst().orElseThrow();
				Jogador entra = p.reservasDisponiveis(s.lado()).stream().filter(j -> j.id() == s.entraId()).findFirst()
					.orElseThrow();
				p.substituir(s.lado(), sai, entra);
			}
		}
	}

	/** A força é a em campo dos planteis (média dos 11 mais fortes), para a surpresa da moral (rev. 61). */
	private void atualizarMoral(Map<Long, Moral> moral, Clube clube, boolean emCasa, int golsPro, int golsContra,
			double forcaPropria, double forcaAdversario) {
		Moral atual = moral.getOrDefault(clube.id(), Moral.inicial(parametrosMoral));
		moral.put(clube.id(), atual.aposJogo(emCasa, golsPro, golsContra, forcaPropria, forcaAdversario, parametrosMoral));
	}

	/** Eventos da partida até o minuto, para a tela. */
	public List<EventoPartida> eventosAte(PartidaDaRodada partida, int minuto) {
		return noMinuto(partida, minuto).resultado().eventos();
	}
}
