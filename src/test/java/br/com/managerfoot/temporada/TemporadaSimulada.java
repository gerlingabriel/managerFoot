package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Joga uma temporada inteira de uma divisão com o motor (ferramenta de teste/simulação).
 * Simplificação até existirem elencos (Etapa 3): cada clube joga em 4-4-2 com os 11 titulares
 * na força do clube.
 */
final class TemporadaSimulada {

	final Classificacao classificacao;
	final Map<Clube, Moral> moral;
	final int gols;
	final int jogos;

	private TemporadaSimulada(Classificacao classificacao, Map<Clube, Moral> moral, int gols, int jogos) {
		this.classificacao = classificacao;
		this.moral = moral;
		this.gols = gols;
		this.jogos = jogos;
	}

	static TemporadaSimulada jogar(Divisao divisao, long semente) {
		return jogar(divisao, semente, ParametrosMoral.padrao());
	}

	static TemporadaSimulada jogar(Divisao divisao, long semente, ParametrosMoral parametrosMoral) {
		MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
		SplittableRandom aleatorio = new SplittableRandom(semente);
		Map<Clube, Escalacao> escalacoes = new HashMap<>();
		Map<Clube, Moral> moral = new HashMap<>();
		for (Clube clube : divisao.clubes()) {
			escalacoes.put(clube, escalacao(clube));
			moral.put(clube, Moral.inicial(parametrosMoral));
		}

		List<ResultadoJogo> resultados = new ArrayList<>();
		int gols = 0;
		for (Rodada rodada : Calendario.sorteado(divisao.clubes(), aleatorio).rodadas()) {
			for (Jogo jogo : rodada.jogos()) {
				ResultadoPartida r = motor.simular(escalacoes.get(jogo.mandante()), escalacoes.get(jogo.visitante()),
						aleatorio.nextLong());
				resultados.add(new ResultadoJogo(jogo, r.golsCasa(), r.golsFora()));
				moral.put(jogo.mandante(),
						moral.get(jogo.mandante()).aposJogo(true, r.golsCasa(), r.golsFora(), parametrosMoral));
				moral.put(jogo.visitante(),
						moral.get(jogo.visitante()).aposJogo(false, r.golsFora(), r.golsCasa(), parametrosMoral));
				gols += r.golsCasa() + r.golsFora();
			}
		}
		return new TemporadaSimulada(Classificacao.calcular(divisao.clubes(), resultados, semente), moral, gols,
				resultados.size());
	}

	private static Escalacao escalacao(Clube clube) {
		List<Jogador> titulares = new ArrayList<>();
		long id = clube.id() * 100;
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < Formacao.F_4_4_2.quantidade(posicao); i++) {
				titulares.add(new Jogador(id, clube.nome() + "-" + id, posicao, clube.forca(), Temperamento.NORMAL));
				id++;
			}
		}
		return new Escalacao(Formacao.F_4_4_2, titulares);
	}
}
