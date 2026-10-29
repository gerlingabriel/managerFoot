package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;

/**
 * Quem substitui o treinador demitido (SPEC §5.4, rev. 59 a 61). O chamado é o treinador de um clube
 * de força inferior, no máximo {@code diferencaMaxima} abaixo, com moral verde; os dois trocam de
 * clube e começam com a moral inicial. Entre os {@code principais} mais fortes desses clubes, o usuário
 * decide primeiro (convite); sem usuário, vai o de maior força. Força = força em campo, a média dos 11 mais fortes.
 */
public final class TrocaDeTreinador {

	/**
	 * Clube que pode ceder o treinador.
	 *
	 * @param usuario o treinador é um usuário, que recebe convite em vez de ser chamado direto
	 */
	public record Candidato(long clubeId, double forca, Moral moral, boolean usuario) {

		public Candidato {
			Objects.requireNonNull(moral);
		}
	}

	private final ParametrosMoral moral;
	private final int principais;
	private final double forcaComTreinadorSemClube;
	private final double diferencaMaxima;

	/**
	 * @param principais quantos dos inferiores mais fortes entram na primeira chamada, com prioridade
	 *        para o usuário (3, rev. 61)
	 * @param forcaComTreinadorSemClube até esta força, o clube aceita um treinador sem clube quando
	 *        nenhum com clube pode vir (10, rev. 59 e 60)
	 * @param diferencaMaxima o clube que cede o treinador fica no máximo tantos pontos de força abaixo
	 *        (12, rev. 61: um clube de força 20 não assume um de força 50)
	 */
	public TrocaDeTreinador(ParametrosMoral moral, int principais, double forcaComTreinadorSemClube,
			double diferencaMaxima) {
		if (principais < 1) {
			throw new IllegalArgumentException("principais deve ser pelo menos 1, mas foi " + principais);
		}
		this.moral = Objects.requireNonNull(moral);
		this.principais = principais;
		this.forcaComTreinadorSemClube = forcaComTreinadorSemClube;
		this.diferencaMaxima = diferencaMaxima;
	}

	public static TrocaDeTreinador padrao() {
		return new TrocaDeTreinador(ParametrosMoral.padrao(), 3, 10, 12);
	}

	/**
	 * Ordem de chamada para o clube que demitiu: os {@code principais} inferiores de moral verde mais
	 * fortes (usuários antes, depois a máquina, cada grupo do mais forte ao mais fraco) e então os demais
	 * inferiores de moral verde, do mais forte ao mais fraco. Quem recusa (usuário) dá a vez ao seguinte;
	 * se todos recusam ou a lista é vazia (nenhum treinador com clube pode vir), o demitido continua
	 * (rev. 61).
	 *
	 * @param clubes os outros clubes do país; o próprio clube que demitiu não deve estar entre eles
	 */
	public List<Candidato> chamados(double forcaDoClube, Collection<Candidato> clubes) {
		Comparator<Candidato> maisForte = Comparator.comparingDouble(Candidato::forca).reversed()
			.thenComparingLong(Candidato::clubeId);
		List<Candidato> inferiores = clubes.stream()
			.filter(c -> c.forca() < forcaDoClube && c.forca() >= forcaDoClube - diferencaMaxima)
			.filter(c -> c.moral().faixa(moral) == Moral.Faixa.VERDE)
			.sorted(maisForte)
			.toList();
		List<Candidato> primeiros = inferiores.subList(0, Math.min(principais, inferiores.size()));
		List<Candidato> ordem = new ArrayList<>(primeiros.stream()
			.sorted(Comparator.comparing((Candidato c) -> !c.usuario()).thenComparing(maisForte))
			.toList());
		ordem.addAll(inferiores.subList(primeiros.size(), inferiores.size()));
		return List.copyOf(ordem);
	}

	/** Sem treinador com clube para chamar, um treinador sem clube pode assumir este clube. */
	public boolean aceitaTreinadorSemClube(double forcaDoClube) {
		return forcaDoClube <= forcaComTreinadorSemClube;
	}

	/** Moral dos dois clubes depois da troca (rev. 61). */
	public Moral moralDepoisDaTroca() {
		return Moral.inicial(moral);
	}
}
