package br.com.managerfoot.motor;

import java.util.EnumMap;
import java.util.List;
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
 * @param sorte efeito da sorte e do azar da temporada
 * @param asterisco efeitos do asterisco
 * @param lesao lesões (SPEC §7.5)
 * @param aberturaPorAtacantes jogo aberto pela quantidade de atacantes em campo (índice 0 a 4): multiplica a conversão
 *        do próprio time e a do adversário contra ele, então mais atacantes fazem e sofrem mais gols (SPEC §7.2, rev. 107)
 * @param ajustesFormacao bônus de meio, ataque e defesa de cada formação, para equilibrá-las (SPEC §7.2, rev. 113)
 */
public record ParametrosMotor(int minutos, double chancePorMinuto, double fatorMando, double conversaoBase,
		double sensibilidadeForca, double expoenteMeias, double expoenteAtacantes, double expoenteDefensores,
		double fatorPorGolMarcado, double conversaoMinima, double conversaoMaxima,
		double proporcaoDefesa, double proporcaoTrave, Map<Formacao, PesosFinalizador> pesosFinalizador,
		ParametrosDisciplina disciplina, ParametrosPenalti penalti, ParametrosSorte sorte,
		ParametrosAsterisco asterisco, ParametrosLesao lesao, List<Double> aberturaPorAtacantes,
		Map<Formacao, AjusteFormacao> ajustesFormacao) {

	public ParametrosMotor {
		aberturaPorAtacantes = List.copyOf(aberturaPorAtacantes);
		if (aberturaPorAtacantes.size() != 5 || aberturaPorAtacantes.stream().anyMatch(a -> a <= 0)) {
			throw new IllegalArgumentException("aberturaPorAtacantes precisa de 5 valores positivos (0 a 4 atacantes)");
		}
		Objects.requireNonNull(lesao, "parâmetros de lesão são obrigatórios");
		Objects.requireNonNull(disciplina, "parâmetros de disciplina são obrigatórios");
		Objects.requireNonNull(penalti, "parâmetros de pênalti são obrigatórios");
		Objects.requireNonNull(sorte, "parâmetros de sorte são obrigatórios");
		Objects.requireNonNull(asterisco, "parâmetros do asterisco são obrigatórios");
		Objects.requireNonNull(pesosFinalizador, "pesos do finalizador são obrigatórios");
		pesosFinalizador = Map.copyOf(pesosFinalizador);
		Objects.requireNonNull(ajustesFormacao, "ajustes das formações são obrigatórios");
		ajustesFormacao = Map.copyOf(ajustesFormacao);
		for (Formacao formacao : Formacao.values()) {
			if (!pesosFinalizador.containsKey(formacao)) {
				throw new IllegalArgumentException("faltam pesos do finalizador para a formação " + formacao.rotulo());
			}
			if (!ajustesFormacao.containsKey(formacao)) {
				throw new IllegalArgumentException("falta o ajuste da formação " + formacao.rotulo());
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
		// pesos do finalizador (SPEC §7.2, rev. 106, pelo usuário); o 0,01% do goleiro sai da defesa
		pesos.put(Formacao.F_4_4_2, new PesosFinalizador(58, 36, 5.99, 0.01));
		pesos.put(Formacao.F_4_3_3, new PesosFinalizador(70, 25, 4.99, 0.01));
		pesos.put(Formacao.F_3_5_2, new PesosFinalizador(55, 41, 3.99, 0.01));
		pesos.put(Formacao.F_3_4_3, new PesosFinalizador(68, 28, 3.99, 0.01));
		pesos.put(Formacao.F_4_2_4, new PesosFinalizador(78, 16, 5.99, 0.01));
		pesos.put(Formacao.F_5_4_1, new PesosFinalizador(42, 46, 11.99, 0.01));
		pesos.put(Formacao.F_4_5_1, new PesosFinalizador(42, 50, 7.99, 0.01));
		pesos.put(Formacao.F_3_6_1, new PesosFinalizador(38, 58, 3.99, 0.01));
		// calibrado na Etapa 1d: docs/simulacoes/calibracao-motor.md
		return new ParametrosMotor(90, 0.15, 1.45, 0.23, 0.95, 0.5, 0.55, 1.25, 0.85, 0.10, 0.35, 0.5, 0.1, pesos,
				ParametrosDisciplina.padrao(), ParametrosPenalti.padrao(), ParametrosSorte.padrao(),
				ParametrosAsterisco.padrao(), ParametrosLesao.padrao(), ABERTURA_PADRAO, ajustesPadrao());
	}

	/** Jogo aberto (rev. 107): até 2 atacantes nada muda; 3 e 4 fazem e sofrem mais gols. */
	public static final List<Double> ABERTURA_PADRAO = List.of(1.0, 1.0, 1.0, 1.15, 1.30);

	/** Ajustes de equilíbrio das formações (SPEC §7.2, rev. 113; docs/simulacoes/calibracao-motor.md). */
	public static Map<Formacao, AjusteFormacao> ajustesPadrao() {
		Map<Formacao, AjusteFormacao> ajustes = new EnumMap<>(Formacao.class);
		for (Formacao formacao : Formacao.values()) {
			ajustes.put(formacao, AjusteFormacao.NENHUM);
		}
		// pelo usuário: 3 zagueiros ganham meio (3-4-3 menos), 4-2-4 ataque, 1 atacante com defesa forte ganha
		// defesa; os valores são do agente, pela simulação de todas contra todas
		ajustes.put(Formacao.F_3_5_2, new AjusteFormacao(1.07, 1, 1));
		ajustes.put(Formacao.F_3_6_1, new AjusteFormacao(1.22, 1, 1));
		ajustes.put(Formacao.F_3_4_3, new AjusteFormacao(1.04, 1, 1));
		ajustes.put(Formacao.F_4_2_4, new AjusteFormacao(1, 1.07, 1));
		ajustes.put(Formacao.F_4_5_1, new AjusteFormacao(1, 1, 1.10));
		ajustes.put(Formacao.F_5_4_1, new AjusteFormacao(1, 1, 1.05));
		return ajustes;
	}

	/** Ajuste de equilíbrio de uma formação. */
	public AjusteFormacao ajuste(Formacao formacao) {
		return ajustesFormacao.get(formacao);
	}

	/** Cópia com outros ajustes das formações (calibração e testes). */
	public ParametrosMotor comAjustes(Map<Formacao, AjusteFormacao> novosAjustes) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, disciplina, penalti, sorte,
				asterisco, lesao, aberturaPorAtacantes, novosAjustes);
	}

	/** Fator de jogo aberto de um time com esta quantidade de atacantes em campo. */
	public double abertura(int atacantes) {
		return aberturaPorAtacantes.get(Math.min(atacantes, aberturaPorAtacantes.size() - 1));
	}

	/** Cópia com outro jogo aberto (calibração e testes). */
	public ParametrosMotor comAbertura(List<Double> novaAbertura) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, disciplina, penalti, sorte,
				asterisco, lesao, novaAbertura, ajustesFormacao);
	}

	/** Cópia com outros cartões e expulsões (calibração e testes). */
	public ParametrosMotor comDisciplina(ParametrosDisciplina novaDisciplina) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, novaDisciplina, penalti, sorte,
				asterisco, lesao, aberturaPorAtacantes, ajustesFormacao);
	}

	/** Cópia com outro efeito de sorte (calibração e testes). */
	public ParametrosMotor comSorte(ParametrosSorte novaSorte) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, disciplina, penalti, novaSorte,
				asterisco, lesao, aberturaPorAtacantes, ajustesFormacao);
	}

	/** Cópia com outros efeitos do asterisco (calibração e testes). */
	public ParametrosMotor comAsterisco(ParametrosAsterisco novoAsterisco) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, disciplina, penalti, sorte,
				novoAsterisco, lesao, aberturaPorAtacantes, ajustesFormacao);
	}

	/** Cópia com outras lesões (simulação e testes). */
	public ParametrosMotor comLesao(ParametrosLesao novaLesao) {
		return new ParametrosMotor(minutos, chancePorMinuto, fatorMando, conversaoBase, sensibilidadeForca,
				expoenteMeias, expoenteAtacantes, expoenteDefensores, fatorPorGolMarcado, conversaoMinima,
				conversaoMaxima, proporcaoDefesa, proporcaoTrave, pesosFinalizador, disciplina, penalti, sorte,
				asterisco, novaLesao, aberturaPorAtacantes, ajustesFormacao);
	}

	private static void exigirProbabilidade(String nome, double valor) {
		if (valor < 0 || valor > 1) {
			throw new IllegalArgumentException(nome + " deve estar entre 0 e 1, mas foi " + valor);
		}
	}
}
