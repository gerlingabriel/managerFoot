package br.com.managerfoot.economia;

import java.util.Objects;

import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.LinhaClassificacao;

/**
 * A meta de um patrocínio na liga (SPEC §9.6, rev. 85 e 97; modelo 20a): vencer N jogos ou terminar entre
 * os N primeiros (N = 1 é ser campeão). A meta é paga assim que fica garantida: as vitórias, ao chegar a
 * N; a posição, quando não há mais clubes suficientes que possam alcançar o clube em pontos. Empate
 * possível em pontos ainda não garante, porque o desempate só se decide no fim.
 *
 * @param alvo vitórias pedidas ou a posição máxima
 * @param bonus em centavos de Real
 */
public record MetaPatrocinio(Tipo tipo, int alvo, long bonus) {

	public MetaPatrocinio {
		if (tipo != Tipo.META_VITORIAS && tipo != Tipo.META_POSICAO) {
			throw new IllegalArgumentException("o patrocínio " + tipo + " não tem meta");
		}
		if (alvo < 1) {
			throw new IllegalArgumentException("a meta precisa ser de pelo menos 1");
		}
	}

	public enum Estado {
		EM_ANDAMENTO, GARANTIDA, FORA_DE_ALCANCE
	}

	/**
	 * Onde o clube está em relação à meta.
	 *
	 * @param percentual vitórias: quanto da meta já tem; posição: 100 dentro da meta, menos quanto mais longe
	 * @param restantes jogos que faltam ao clube na liga
	 * @param posicao lugar na classificação, a partir de 1
	 * @param pontosAtras do clube no último lugar da meta, se está fora dela; 0 se está dentro
	 */
	public record Situacao(Estado estado, int percentual, int vitorias, int jogos, int restantes, int posicao,
			int pontosAtras) {
	}

	public boolean campeao() {
		return tipo == Tipo.META_POSICAO && alvo == 1;
	}

	/**
	 * @param clubeId o clube do patrocínio, que precisa estar na classificação
	 * @param jogosPorClube jogos de cada clube na liga da temporada
	 */
	public Situacao situacao(Classificacao classificacao, long clubeId, int jogosPorClube) {
		Objects.requireNonNull(classificacao);
		// pelo id: a força do clube na classificação pode ser a do início da temporada
		int indice = 0;
		while (indice < classificacao.linhas().size() && classificacao.linhas().get(indice).clube().id() != clubeId) {
			indice++;
		}
		if (indice == classificacao.linhas().size()) {
			throw new IllegalArgumentException("o clube " + clubeId + " não está nesta classificação");
		}
		LinhaClassificacao minha = classificacao.linhas().get(indice);
		int posicao = indice + 1;
		int restantes = Math.max(0, jogosPorClube - minha.jogos());
		if (tipo == Tipo.META_VITORIAS) {
			Estado estado = minha.vitorias() >= alvo ? Estado.GARANTIDA
					: minha.vitorias() + restantes < alvo ? Estado.FORA_DE_ALCANCE : Estado.EM_ANDAMENTO;
			return new Situacao(estado, Math.min(100, minha.vitorias() * 100 / alvo), minha.vitorias(), minha.jogos(),
					restantes, posicao, 0);
		}
		int pontos = minha.pontos();
		int maximo = pontos + restantes * Classificacao.PONTOS_VITORIA;
		// quem ainda pode chegar aos meus pontos e quem já passou do máximo que eu posso fazer
		long podemAlcancar = classificacao.linhas().stream().filter(l -> l.clube().id() != clubeId)
			.filter(l -> l.pontos() + Math.max(0, jogosPorClube - l.jogos()) * Classificacao.PONTOS_VITORIA >= pontos).count();
		long jaPassaram = classificacao.linhas().stream().filter(l -> l.clube().id() != clubeId)
			.filter(l -> l.pontos() > maximo).count();
		Estado estado = podemAlcancar < alvo ? Estado.GARANTIDA : jaPassaram >= alvo ? Estado.FORA_DE_ALCANCE : Estado.EM_ANDAMENTO;
		int clubes = classificacao.linhas().size();
		int percentual = posicao <= alvo || clubes <= alvo ? 100 : Math.max(0, 100 * (clubes - posicao) / (clubes - alvo));
		int atras = posicao <= alvo ? 0 : classificacao.linhas().get(alvo - 1).pontos() - pontos;
		return new Situacao(estado, percentual, minha.vitorias(), minha.jogos(), restantes, posicao, atras);
	}
}
