package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

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
			.filter(j -> podeSair(plantel, j))
			.max(Comparator.comparingLong((Jogador j) -> preco.preco(j, paisDaLiga)).thenComparingLong(Jogador::id));
	}

	/**
	 * A saída do jogador não deixa a posição dele abaixo do elenco mínimo (SPEC §9.3) nem o plantel
	 * abaixo do plantel mínimo (rev. 59).
	 */
	public boolean podeSair(List<Jogador> plantel, Jogador jogador) {
		return plantel.size() > parametros.plantelMinimo()
				&& plantel.stream().filter(o -> o.posicao() == jogador.posicao()).count()
						> parametros.elencoMinimo().getOrDefault(jogador.posicao(), 0);
	}

	/**
	 * Jogadores que o clube da máquina passa a deixar à venda para compra direta (SPEC §9.4, rev. 43):
	 * com menos que o mínimo à venda, sorteia até completar entre o mínimo e o máximo, sem olhar a
	 * posição, mas sem que a venda de todos os anunciados deixe uma posição abaixo do elenco mínimo.
	 *
	 * @param jaAVenda ids dos jogadores do plantel que já estão à venda
	 * @return os novos anunciados, possivelmente nenhum
	 */
	public List<Jogador> sortearAVenda(List<Jogador> plantel, Set<Long> jaAVenda, RandomGenerator aleatorio) {
		if (jaAVenda.size() >= parametros.aVendaMinimo()) {
			return List.of();
		}
		int alvo = parametros.aVendaMinimo() + aleatorio.nextInt(parametros.aVendaMaximo() - parametros.aVendaMinimo() + 1);
		Map<Posicao, Integer> sobra = new EnumMap<>(Posicao.class);
		for (Jogador j : plantel) {
			sobra.merge(j.posicao(), jaAVenda.contains(j.id()) ? 0 : 1, Integer::sum);
		}
		sobra.replaceAll((posicao, livres) -> livres - parametros.elencoMinimo().getOrDefault(posicao, 0));
		// ordem fixa antes do sorteio, para a mesma semente dar os mesmos jogadores
		List<Jogador> candidatos = new ArrayList<>(plantel.stream().filter(j -> !jaAVenda.contains(j.id()))
			.sorted(Comparator.comparingLong(Jogador::id)).toList());
		List<Jogador> novos = new ArrayList<>();
		// vendidos todos os anunciados, o plantel não fica abaixo do mínimo
		alvo = Math.min(alvo, plantel.size() - parametros.plantelMinimo());
		while (jaAVenda.size() + novos.size() < alvo && !candidatos.isEmpty()) {
			Jogador j = candidatos.remove(aleatorio.nextInt(candidatos.size()));
			if (sobra.get(j.posicao()) > 0) {
				sobra.merge(j.posicao(), -1, Integer::sum);
				novos.add(j);
			}
		}
		return List.copyOf(novos);
	}

	/** Força em campo: média dos 11 jogadores mais fortes do plantel. */
	public static double forcaEmCampo(List<Jogador> plantel) {
		return Clube.forcaEmCampo(plantel);
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
