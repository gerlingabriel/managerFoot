package br.com.managerfoot.economia;

import java.util.List;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Ampliação do estádio (SPEC §9.2, rev. 54 a 56): blocos de lugares pelo preço de um jogador de
 * força 50, que valem depois da obra, até o máximo. A máquina amplia quando a torcida cresceu (o
 * clube ficou mais forte) e o caixa aguenta, ou um pouco quando está com muito dinheiro. Domínio puro.
 */
public final class AmpliacaoEstadio {

	/** Jogador de referência do preço: força 50, nacional, sem asterisco. */
	private static final Jogador REFERENCIA = new Jogador(0, "Referência", Posicao.MEIO_CAMPISTA, Jogador.FORCA_MAXIMA,
			Temperamento.NORMAL, "BRA");

	/** Folga de caixa da máquina para ampliar quando a torcida cresceu, em limites. */
	private static final double FOLGA_CRESCEU = 1.5;
	/** Caixa da máquina "com muito dinheiro", em limites, depois de pagar a obra. */
	private static final double MUITO_DINHEIRO = 3;
	/** Até onde a máquina amplia com muito dinheiro, em fração da torcida. */
	private static final double TETO_COM_DINHEIRO = 1.05;

	private final ParametrosPublico parametros;
	private final PublicoDoJogo publico;
	private final PrecoJogador preco;
	private final MercadoDaMaquina mercado;

	public AmpliacaoEstadio(ParametrosPublico parametros, PublicoDoJogo publico, PrecoJogador preco, MercadoDaMaquina mercado) {
		this.parametros = parametros;
		this.publico = publico;
		this.preco = preco;
		this.mercado = mercado;
	}

	public static AmpliacaoEstadio padrao() {
		return new AmpliacaoEstadio(ParametrosPublico.padrao(), PublicoDoJogo.padrao(), PrecoJogador.padrao(),
				MercadoDaMaquina.padrao());
	}

	/** Preço de um bloco: o de um jogador de força 50 nacional. */
	public long custo() {
		return preco.preco(REFERENCIA, REFERENCIA.nacionalidade());
	}

	public boolean cabe(int capacidade) {
		return capacidade + parametros.bloco() <= parametros.capacidadeMaxima();
	}

	/**
	 * A máquina decide ampliar, sem obra em andamento: quando a torcida dos jogos grandes (líder contra
	 * o 2º) já passa do estádio com o bloco novo e sobra caixa; ou, com muito dinheiro, até um pouco
	 * acima da torcida.
	 */
	public boolean maquinaAmplia(int capacidade, double forcaEmCampo, long saldo, List<Jogador> plantel) {
		if (!cabe(capacidade)) {
			return false;
		}
		int torcida = publico.torcida(forcaEmCampo);
		long limite = mercado.limite(plantel);
		long depois = saldo - custo();
		double jogoGrande = torcida * (parametros.ocupacaoLider() + parametros.bonusAdversario());
		boolean cresceu = capacidade + parametros.bloco() <= jogoGrande + parametros.bloco() / 2.0
				&& depois >= limite * FOLGA_CRESCEU;
		boolean rico = depois >= limite * MUITO_DINHEIRO && capacidade + parametros.bloco() <= torcida * TETO_COM_DINHEIRO;
		return cresceu || rico;
	}
}
