package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.economia.MetaPatrocinio.Estado;
import br.com.managerfoot.economia.MetaPatrocinio.Situacao;
import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.LinhaClassificacao;

class MetaPatrocinioTest {

	/** 4 clubes, 6 jogos cada na liga. */
	private static final int JOGOS = 6;

	@Test
	void metaDeVitoriasGarantidaAoChegarAoNumeroEForaDeAlcanceSemJogosQueBastem() {
		MetaPatrocinio meta = new MetaPatrocinio(Tipo.META_VITORIAS, 4, 100);

		Situacao andamento = meta.situacao(tabela(linha(1, 3, 2, 0), linha(2, 3, 1, 1), linha(3, 3, 1, 0), linha(4, 3, 0, 0)), 1, JOGOS);
		assertThat(andamento.estado()).isEqualTo(Estado.EM_ANDAMENTO);
		assertThat(andamento.percentual()).isEqualTo(50);
		assertThat(andamento.restantes()).isEqualTo(3);

		assertThat(meta.situacao(tabela(linha(1, 5, 4, 0), linha(2, 5, 1, 1), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS).estado())
			.isEqualTo(Estado.GARANTIDA);
		assertThat(meta.situacao(tabela(linha(2, 5, 3, 0), linha(1, 5, 2, 0), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS).estado())
			.as("2 vitórias e só 1 jogo").isEqualTo(Estado.FORA_DE_ALCANCE);
	}

	@Test
	void campeaoGarantidoQuandoNinguemMaisAlcancaEmPontos() {
		MetaPatrocinio campeao = new MetaPatrocinio(Tipo.META_POSICAO, 1, 100);
		assertThat(campeao.campeao()).isTrue();

		// 5 jogos: o líder tem 15, o 2º tem 9 e só pode chegar a 12
		Situacao garantido = campeao.situacao(tabela(linha(1, 5, 5, 0), linha(2, 5, 3, 0), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS);
		assertThat(garantido.estado()).isEqualTo(Estado.GARANTIDA);

		// o 2º ainda pode empatar: empate possível não garante
		Situacao empate = campeao.situacao(tabela(linha(1, 5, 4, 0), linha(2, 5, 3, 0), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS);
		assertThat(empate.estado()).isEqualTo(Estado.EM_ANDAMENTO);
		assertThat(empate.posicao()).isEqualTo(1);
		assertThat(empate.pontosAtras()).isZero();
	}

	@Test
	void posicaoForaDeAlcanceEPontosAtrasDoUltimoLugarDaMeta() {
		MetaPatrocinio campeao = new MetaPatrocinio(Tipo.META_POSICAO, 1, 100);

		Situacao atras = campeao.situacao(tabela(linha(2, 4, 4, 0), linha(1, 4, 3, 0), linha(3, 4, 1, 0), linha(4, 4, 0, 0)), 1, JOGOS);
		assertThat(atras.estado()).isEqualTo(Estado.EM_ANDAMENTO);
		assertThat(atras.posicao()).isEqualTo(2);
		assertThat(atras.pontosAtras()).isEqualTo(3);
		assertThat(atras.percentual()).isEqualTo(66);

		// 1 jogo a fazer: no máximo 12, e o líder já tem 15
		assertThat(campeao.situacao(tabela(linha(2, 5, 5, 0), linha(1, 5, 3, 0), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS).estado())
			.isEqualTo(Estado.FORA_DE_ALCANCE);

		MetaPatrocinio entreOsDois = new MetaPatrocinio(Tipo.META_POSICAO, 2, 100);
		assertThat(entreOsDois.situacao(tabela(linha(2, 5, 5, 0), linha(1, 5, 4, 0), linha(3, 5, 1, 0), linha(4, 5, 0, 0)), 1, JOGOS).estado())
			.as("só o líder ainda pode passar").isEqualTo(Estado.GARANTIDA);
	}

	private static Classificacao tabela(LinhaClassificacao... linhas) {
		return new Classificacao(List.of(linhas));
	}

	private static LinhaClassificacao linha(long clube, int jogos, int vitorias, int empates) {
		return new LinhaClassificacao(new Clube(clube, "C" + clube, 20), jogos, vitorias, empates, jogos - vitorias - empates, 0, 0);
	}
}
