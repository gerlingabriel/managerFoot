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
 * @param interesseAbaixoDoTime a máquina só dá lance em jogador com força de pelo menos a força em
 *        campo do clube menos este valor (proposta: 5)
 * @param folgaNaCompra rodadas da folha que precisam sobrar acima do limite depois de pagar uma
 *        compra, para o clube não vender logo em seguida (proposta: 5)
 */
public record ParametrosMercadoMaquina(int rodadasDeFolhaNoLimite, double descontoLance, double acrescimoPelaForca,
		Map<Posicao, Integer> elencoMinimo, int interesseAbaixoDoTime, int folgaNaCompra) {

	public ParametrosMercadoMaquina {
		if (rodadasDeFolhaNoLimite < 0 || descontoLance < 0 || descontoLance >= 1 || acrescimoPelaForca < 0
				|| interesseAbaixoDoTime < 0 || folgaNaCompra < 0) {
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
		return new ParametrosMercadoMaquina(10, 0.10, 0.05, minimo, 5, 5);
	}
}
