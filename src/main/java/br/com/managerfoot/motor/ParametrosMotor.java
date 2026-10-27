package br.com.managerfoot.motor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import br.com.managerfoot.dominio.Formacao;

/**
 * Parâmetros de balanceamento do motor. Valores padrão são provisórios e serão calibrados
 * pelas metas da SPEC §7.6 (Etapa 1d).
 *
 * @param minutos duração da partida
 * @param chancePorMinuto probabilidade de haver uma jogada de finalização em cada minuto (sem pênalti)
 * @param fatorMando multiplicador do domínio do mandante (Elifoot 98 usava 7000/6000 ≈ 1,17; recalibrado)
 * @param conversaoBase probabilidade de gol quando ataque × defesa e finalizador × goleiro se equivalem
 * @param sensibilidadeForca expoente aplicado às forças nas disputas; maior = força pesa mais
 * @param expoenteMeias quanto a quantidade de meias pesa no domínio (1 = soma simples)
 * @param expoenteAtacantes quanto a quantidade de atacantes pesa no ataque (1 = soma simples)
 * @param expoenteDefensores quanto a quantidade de defensores pesa na defesa (1 = soma simples).
 *        Os três expoentes equilibram as formações entre si (SPEC §7.2); ver Partida#forcaSetor
 * @param fatorPorGolMarcado multiplica a conversão do time a cada gol que ele já marcou (anti-goleada)
 * @param conversaoMinima piso da probabilidade de gol, para que o mais fraco sempre tenha chance
 * @param conversaoMaxima teto da probabilidade de gol, para que nenhuma chance seja garantida
 * @param proporcaoDefesa entre finalizações sem gol, fração defendida pelo goleiro
 * @param proporcaoTrave entre finalizações sem gol, fração na trave; o restante vai para fora
 * @param pesosFinalizador pesos por formação (SPEC §7.2)
 * @param disciplina cartões e expulsões
 * @param penalti pênaltis e goleiro improvisado
 */
public record ParametrosMotor(int minutos, double chancePorMinuto, double fatorMando, double conversaoBase,
		double sensibilidadeForca, double expoenteMeias, double expoenteAtacantes, double expoenteDefensores,
		double fatorPorGolMarcado, double conversaoMinima, double conversaoMaxima,
		double proporcaoDefesa, double proporcaoTrave, Map<Formacao, PesosFinalizador> pesosFinalizador,
		ParametrosDisciplina disciplina, ParametrosPenalti penalti) {

	public ParametrosMotor {
		Objects.requireNonNull(disciplina, "parâmetros de disciplina são obrigatórios");
		Objects.requireNonNull(penalti, "parâmetros de pênalti são obrigatórios");
		Objects.requireNonNull(pesosFinalizador, "pesos do finalizador são obrigatórios");
		pesosFinalizador = Map.copyOf(pesosFinalizador);
		for (Formacao formacao : Formacao.values()) {
			if (!pesosFinalizador.containsKey(formacao)) {
				throw new IllegalArgumentException("faltam pesos do finalizador para a formação " + formacao.rotulo());
			}
		}
		exigirProbabilidade("chancePorMinuto", chancePorMinuto);
		exigirProbabilidade("conversaoBase", conversaoBase);
		exigirProbabilidade("conversaoMinima", conversaoMinima);
		exigirProbabilidade("conversaoMaxima", conversaoMaxima);
		exigirProbabilidade("proporcaoDefesa", proporcaoDefesa);
		exigirProbabilidade("proporcaoTrave", proporcaoTrave);
		if (minutos <= 0) {
			throw new IllegalArgumentException("minutos deve ser positivo, mas foi " + minutos);
		}
		if (fatorMando <= 0 || sensibilidadeForca <= 0 || expoenteMeias <= 0 || expoenteAtacantes <= 0
				|| expoenteDefensores <= 0 || fatorPorGolMarcado <= 0) {
			throw new IllegalArgumentException(
					"fatorMando, sensibilidadeForca, expoentes dos setores e fatorPorGolMarcado devem ser positivos");
		}
		if (conversaoMinima > conversaoMaxima) {
			throw new IllegalArgumentException("conversaoMinima não pode ser maior que conversaoMaxima");
		}
		if (proporcaoDefesa + proporcaoTrave > 1) {
			throw new IllegalArgumentException("proporcaoDefesa + proporcaoTrave não pode passar de 1");
		}
	}

	public static ParametrosMotor padrao() {
		Map<Formacao, PesosFinalizador> pesos = new EnumMap<>(Formacao.class);
		pesos.put(Formacao.F_4_4_2, new PesosFinalizador(64, 31, 4.99, 0.01));
		pesos.put(Formacao.F_4_3_3, new PesosFinalizador(75, 21, 3.99, 0.01));
		pesos.put(Formacao.F_3_5_2, new PesosFinalizador(58, 38, 3.99, 0.01));
		pesos.put(Formacao.F_3_4_3, new PesosFinalizador(74, 23, 2.99, 0.01));
		pesos.put(Formacao.F_4_2_4, new PesosFinalizador(82, 14, 3.99, 0.01));
		pesos.put(Formacao.F_5_4_1, new PesosFinalizador(45, 47, 7.99, 0.01));
		pesos.put(Formacao.F_4_5_1, new PesosFinalizador(43, 51, 5.99, 0.01));
		pesos.put(Formacao.F_3_6_1, new PesosFinalizador(40, 55, 4.99, 0.01));
		// calibrado na Etapa 1d: docs/simulacoes/calibracao-motor.md
		return new ParametrosMotor(90, 0.15, 1.45, 0.23, 0.95, 0.5, 0.55, 1.25, 0.85, 0.10, 0.35, 0.5, 0.1, pesos,
				ParametrosDisciplina.padrao(), ParametrosPenalti.padrao());
	}

	private static void exigirProbabilidade(String nome, double valor) {
		if (valor < 0 || valor > 1) {
			throw new IllegalArgumentException(nome + " deve estar entre 0 e 1, mas foi " + valor);
		}
	}
}
