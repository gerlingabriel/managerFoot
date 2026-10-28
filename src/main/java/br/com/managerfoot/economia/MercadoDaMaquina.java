package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/**
 * Decisões de mercado de um clube da máquina (SPEC §9.3 e §9.4, rev. 43): o caixa é comparado a um
 * limite de rodadas da folha; abaixo dele o clube vende, acima pode comprar por leilão. Domínio puro;
 * dinheiro em centavos de Real.
 */
public final class MercadoDaMaquina {

	private final ParametrosMercadoMaquina parametros;
	private final PrecoJogador preco;
	private final CaixaDaRodada caixa;

	public MercadoDaMaquina(ParametrosMercadoMaquina parametros, PrecoJogador preco, CaixaDaRodada caixa) {
		this.parametros = parametros;
		this.preco = preco;
		this.caixa = caixa;
	}

	public static MercadoDaMaquina padrao() {
		return new MercadoDaMaquina(ParametrosMercadoMaquina.padrao(), PrecoJogador.padrao(), CaixaDaRodada.padrao());
	}

	/** Caixa que o clube quer manter: rodadas da folha. */
	public long limite(List<Jogador> plantel) {
		return caixa.folha(plantel) * parametros.rodadasDeFolhaNoLimite();
	}

	public boolean precisaVender(long saldo, List<Jogador> plantel) {
		return saldo < limite(plantel);
	}

	/**
	 * Quem a máquina põe em leilão quando precisa vender: o jogador mais caro sem asterisco cuja saída
	 * não deixa a posição abaixo do elenco mínimo. Vazio se ninguém pode sair.
	 */
	public Optional<Jogador> jogadorParaVender(List<Jogador> plantel, String paisDaLiga) {
		return plantel.stream()
			.filter(j -> !j.asterisco())
			.filter(j -> plantel.stream().filter(o -> o.posicao() == j.posicao()).count()
					> parametros.elencoMinimo().getOrDefault(j.posicao(), 0))
			.max(Comparator.comparingLong((Jogador j) -> preco.preco(j, paisDaLiga)).thenComparingLong(Jogador::id));
	}

	/** Força em campo: média dos 11 jogadores mais fortes do plantel. */
	public static double forcaEmCampo(List<Jogador> plantel) {
		return plantel.stream().map(Jogador::forca).sorted(Comparator.reverseOrder()).limit(11)
			.mapToInt(Integer::intValue).average().orElse(0);
	}

	/**
	 * Lance da máquina num leilão: um pouco abaixo do preço, um pouco mais para o clube mais forte.
	 * Vazio se o jogador é fraco demais para o time, se o plantel está cheio, se o lance fica abaixo
	 * do mínimo ou se, pago o lance, o caixa não ficaria acima do limite com a folga, já com o novo
	 * salário.
	 *
	 * @param lanceMinimo lance inicial do leilão (zero no leilão sem lance mínimo)
	 */
	public Optional<Long> lance(Clube clube, long saldo, List<Jogador> plantel, Jogador alvo, String paisDaLiga,
			long lanceMinimo) {
		if (plantel.size() >= Clube.MAXIMO_JOGADORES
				|| alvo.forca() < forcaEmCampo(plantel) - parametros.interesseAbaixoDoTime()) {
			return Optional.empty();
		}
		double fator = 1 - parametros.descontoLance()
				+ parametros.acrescimoPelaForca() * clube.forca() / Jogador.FORCA_MAXIMA;
		long valor = preco.redondo(Math.round(preco.preco(alvo, paisDaLiga) * fator));
		List<Jogador> depois = new ArrayList<>(plantel);
		depois.add(alvo);
		long reserva = limite(depois) + caixa.folha(depois) * parametros.folgaNaCompra();
		if (valor < lanceMinimo || saldo - valor < reserva) {
			return Optional.empty();
		}
		return Optional.of(valor);
	}
}
