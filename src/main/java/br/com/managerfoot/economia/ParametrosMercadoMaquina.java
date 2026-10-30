package br.com.managerfoot.economia;

import java.util.EnumMap;
import java.util.Map;

import br.com.managerfoot.dominio.Posicao;

/**
 * Valores do mercado da máquina (SPEC §9.3 e §9.4, rev. 43).
 *
 * @param rodadasDeFolhaNoLimite o caixa que a máquina quer manter, em rodadas da folha: acima compra,
 *        abaixo vende (definido pelo usuário: 10)
 * @param descontoLance quanto abaixo do preço a máquina oferece no leilão (usuário: "um pouco abaixo";
 *        proposta: 10%)
 * @param acrescimoPelaForca quanto a mais oferece o clube mais forte, proporcional à força dele na
 *        escala até a força máxima (usuário: preferência "pela força do time"; proposta: até 5%)
 * @param elencoMinimo jogadores por posição que a máquina não vende (proposta do §9.3)
 * @param plantelMinimo nenhuma venda deixa o plantel com menos que isto: 11 mais reservas para as
 *        suspensões, enquanto o time com um a menos (§9.3) não existe (proposta do agente, rev. 59: 14)
 * @param interesseAbaixoDoTime a máquina só dá lance em jogador com força de pelo menos a força em
 *        campo do clube menos este valor (proposta: 5)
 * @param folgaNaCompra rodadas da folha que precisam sobrar acima do limite depois de pagar uma
 *        compra, para o clube não vender logo em seguida (proposta: 5)
 * @param aVendaMinimo quando o clube tem menos que isto à venda para compra direta, sorteia mais
 *        (usuário: "uns 5 ou 6")
 * @param aVendaMaximo até quantos o sorteio completa
 * @param plantelAlvo abaixo disto o clube com caixa compra até jogador mais fraco que o time, para ter
 *        um plantel maior (usuário, rev. 66: "entre 23 e 25 se o caixa permitir"); 0 desliga
 * @param aVendaDeFora quantos o clube de fora do campeonato deixa à venda por temporada (usuário, rev. 84: "uns dois")
 * @param reposicaoAbaixoDaMedia quantos pontos abaixo da média do clube vem o jogador criado na virada para o clube
 *        de fora (proposta do agente, rev. 84: 2)
 */
public record ParametrosMercadoMaquina(int rodadasDeFolhaNoLimite, double descontoLance, double acrescimoPelaForca,
		Map<Posicao, Integer> elencoMinimo, int plantelMinimo, int interesseAbaixoDoTime, int folgaNaCompra,
		int aVendaMinimo, int aVendaMaximo, int plantelAlvo, int aVendaDeFora, int reposicaoAbaixoDaMedia) {

	public ParametrosMercadoMaquina {
		if (rodadasDeFolhaNoLimite < 0 || descontoLance < 0 || descontoLance >= 1 || acrescimoPelaForca < 0
				|| plantelMinimo < 0 || interesseAbaixoDoTime < 0 || folgaNaCompra < 0 || aVendaMinimo < 0 || aVendaMaximo < aVendaMinimo
				|| aVendaDeFora < 0 || reposicaoAbaixoDaMedia < 0) {
			throw new IllegalArgumentException("parâmetros do mercado da máquina inválidos");
		}
		elencoMinimo = Map.copyOf(elencoMinimo);
	}

	public static ParametrosMercadoMaquina padrao() {
		Map<Posicao, Integer> minimo = new EnumMap<>(Posicao.class);
		minimo.put(Posicao.GOLEIRO, 1);
		minimo.put(Posicao.DEFENSOR, 4);
		minimo.put(Posicao.MEIO_CAMPISTA, 4);
		minimo.put(Posicao.ATACANTE, 2);
		return new ParametrosMercadoMaquina(10, 0.10, 0.05, minimo, 14, 5, 5, 5, 6, 23, 2, 2);
	}

	/** Os mesmos valores com outro plantel alvo (rev. 66). */
	public ParametrosMercadoMaquina comPlantelAlvo(int alvo) {
		return new ParametrosMercadoMaquina(rodadasDeFolhaNoLimite, descontoLance, acrescimoPelaForca, elencoMinimo,
				plantelMinimo, interesseAbaixoDoTime, folgaNaCompra, aVendaMinimo, aVendaMaximo, alvo, aVendaDeFora, reposicaoAbaixoDaMedia);
	}
}
