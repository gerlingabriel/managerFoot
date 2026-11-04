package br.com.managerfoot.web;

import br.com.managerfoot.economia.MetaPatrocinio;
import br.com.managerfoot.economia.MetaPatrocinio.Estado;
import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;
import br.com.managerfoot.persistencia.PatrocinioRepositorio.MetaNaTela;

/**
 * A meta do patrocínio em palavras (modelo 20a): a linha curta do topo do plantel e a faixa de Finanças.
 *
 * @param curto "9 de 14 vitórias · 64%" ou "Campeão da liga · hoje 2º, 3 pontos atrás"
 * @param pede "Vencer 14 jogos na liga" ou "Ser campeão da liga"
 * @param situacao a frase de baixo da faixa
 * @param nota embaixo do valor: quando o bônus entra
 * @param classe da faixa: vazia, "cumprida" ou "perdida"
 */
record TextosMeta(String patrocinador, String curto, String pede, String bonus, int percentual, String situacao, String nota,
		String classe) {

	static TextosMeta de(MetaNaTela m) {
		MetaPatrocinio meta = m.meta();
		MetaPatrocinio.Situacao s = m.situacao();
		String bonus = m.moeda().formatar(meta.bonus());
		boolean vitorias = meta.tipo() == Tipo.META_VITORIAS;
		String pede = vitorias ? "Vencer " + meta.alvo() + " jogos na liga"
				: meta.campeao() ? "Ser campeão da liga" : "Terminar entre os " + meta.alvo() + " primeiros da liga";
		String classe = switch (s.estado()) {
			case GARANTIDA -> "cumprida";
			case FORA_DE_ALCANCE -> "perdida";
			case EM_ANDAMENTO -> "";
		};
		String hoje = "hoje " + s.posicao() + "º" + (s.pontosAtras() > 0 ? ", " + s.pontosAtras() + (s.pontosAtras() == 1 ? " ponto" : " pontos") + " atrás" : "");
		String curto = switch (s.estado()) {
			case GARANTIDA -> (vitorias ? meta.alvo() + " vitórias" : meta.campeao() ? "Campeão da liga" : "Entre os " + meta.alvo() + " primeiros")
					+ " · cumprida";
			case FORA_DE_ALCANCE -> pede + " · fora de alcance";
			case EM_ANDAMENTO -> vitorias ? s.vitorias() + " de " + meta.alvo() + " vitórias · " + s.percentual() + "%"
					: (meta.campeao() ? "Campeão da liga" : "Entre os " + meta.alvo() + " primeiros") + " · " + hoje;
		};
		String jogos = s.restantes() == 1 ? "1 jogo" : s.restantes() + " jogos";
		String situacao = switch (s.estado()) {
			case GARANTIDA -> (vitorias ? s.vitorias() + " vitórias." : meta.campeao() ? "Campeão garantido." : "Posição garantida.")
					+ (m.pagaNaRodada() != null ? " O bônus entrou no caixa na rodada " + m.pagaNaRodada() + "." : " O bônus entra no caixa no fim da liga.");
			case FORA_DE_ALCANCE -> vitorias ? "Não dá mais: " + s.vitorias() + " vitórias e restam " + jogos + "."
					: "Não dá mais: mesmo vencendo tudo, não chega entre os " + meta.alvo() + " primeiros.";
			case EM_ANDAMENTO -> vitorias
					? s.percentual() + "% · " + s.vitorias() + " vitórias em " + s.jogos() + (s.jogos() == 1 ? " jogo" : " jogos") + ". Faltam "
							+ (meta.alvo() - s.vitorias()) + " e restam " + jogos + "."
					: "Hoje: " + s.posicao() + "º lugar" + (s.pontosAtras() > 0 ? ", " + s.pontosAtras() + (s.pontosAtras() == 1 ? " ponto" : " pontos") + " atrás do " + meta.alvo() + "º" : ", dentro da meta")
							+ ", com " + jogos + " para jogar.";
		};
		String nota = switch (s.estado()) {
			case GARANTIDA -> m.pagaNaRodada() != null ? "pago na rodada " + m.pagaNaRodada() : "garantido";
			case FORA_DE_ALCANCE -> "meta fora de alcance";
			case EM_ANDAMENTO -> vitorias ? "ao chegar a " + meta.alvo() + " vitórias"
					: meta.campeao() ? "quando o título estiver garantido" : "quando a posição estiver garantida";
		};
		return new TextosMeta(m.patrocinador(), curto, pede, bonus, s.estado() == Estado.GARANTIDA ? 100 : s.percentual(), situacao,
				nota, classe);
	}
}
