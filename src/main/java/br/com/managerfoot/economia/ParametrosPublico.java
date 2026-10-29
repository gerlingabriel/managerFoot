package br.com.managerfoot.economia;

/**
 * Público e estádio (SPEC §9.2, rev. 56). Proposta do agente a partir das regras do usuário, simulada
 * em {@code docs/simulacoes/estadio-publico-patrocinio.md}. As ocupações são frações da torcida do
 * clube, que segue a força em campo (a curva de {@link CaixaDaRodada#capacidadeInicial(int)}).
 *
 * @param estadioInicialSobreTorcida o estádio do início do jogo, em fração da torcida (ninguém começa
 *        no máximo)
 * @param ocupacaoLider fração da torcida no jogo do líder (antes dos bônus)
 * @param ocupacaoUltimo fração da torcida no jogo do último colocado; entre os dois, linear pela posição
 * @param bonusAdversario até quanto soma receber o líder; cai com a posição do adversário e com a do
 *        mandante (o último não ganha nada)
 * @param bonusDecisivo soma no jogo entre os dois primeiros nas últimas {@code rodadasDecisivas}
 * @param rodadasDecisivas últimas rodadas da temporada em que vale o bônus decisivo
 * @param rodadaTabelaInteira a partir desta rodada a posição vale inteira; antes, mistura com a
 *        ocupação pela posição final da temporada passada
 * @param ocupacaoInicio fração da torcida antes de a tabela valer na 1ª temporada do jogo
 * @param variacao sorteio uniforme de ± esta fração na ocupação
 * @param ocupacaoMinima fração mínima do estádio
 * @param capacidadeMaxima limite do estádio para todos
 * @param bloco lugares de cada ampliação
 * @param rodadasDeObra rodadas até os lugares novos valerem
 * @param ocupacaoMinimaFinal fração das duas torcidas, no mínimo, na final da copa em campo neutro
 *        ("médio-alto", rev. 69; valor do agente, provisório)
 */
public record ParametrosPublico(double estadioInicialSobreTorcida, double ocupacaoLider, double ocupacaoUltimo,
		double bonusAdversario, double bonusDecisivo, int rodadasDecisivas, int rodadaTabelaInteira, double ocupacaoInicio,
		double variacao, double ocupacaoMinima, int capacidadeMaxima, int bloco, int rodadasDeObra,
		double ocupacaoMinimaFinal) {

	public ParametrosPublico {
		if (estadioInicialSobreTorcida <= 0 || ocupacaoLider < ocupacaoUltimo || rodadaTabelaInteira < 1 || bloco < 1
				|| capacidadeMaxima < bloco || rodadasDeObra < 0 || ocupacaoMinimaFinal < 0) {
			throw new IllegalArgumentException("parâmetros de público inválidos");
		}
	}

	public static ParametrosPublico padrao() {
		return new ParametrosPublico(0.75, 0.75, 0.15, 0.15, 0.15, 3, 5, 0.45, 0.08, 0.05, 80_000, 5_000, 6, 0.6);
	}
}
