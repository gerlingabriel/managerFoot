package br.com.managerfoot.temporada;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.ParametrosEvolucao.Desfecho;

/**
 * Evolução de força depois de uma partida (SPEC §6.1, rev. 54): quem jogou se desgasta (mais na
 * derrota), quem não jogou pode crescer (mais na vitória). O desgaste não leva abaixo de um piso
 * pela força do clube (como o "nível base do clube" − 5 do Elifoot 98). Quem passa do teto da divisão
 * não cresce e perde força aos poucos (§4.2). A força fica entre 1 e 50. Domínio puro.
 */
public final class EvolucaoForca {

	private final ParametrosEvolucao parametros;

	public EvolucaoForca(ParametrosEvolucao parametros) {
		this.parametros = parametros;
	}

	public static EvolucaoForca padrao() {
		return new EvolucaoForca(ParametrosEvolucao.padrao());
	}

	/**
	 * O plantel depois da partida, na mesma ordem.
	 *
	 * @param jogaram titulares e quem entrou no segundo tempo
	 * @param banco reservas que não entraram; os outros do plantel estão fora da escalação
	 * @param teto limite de força da divisão do clube
	 * @param forcaDoClube força cadastrada do clube: com ela e o piso dos parâmetros, ninguém que joga cai
	 *        abaixo de força do clube − piso
	 */
	public List<Jogador> aposPartida(List<Jogador> plantel, Set<Long> jogaram, Set<Long> banco, Desfecho desfecho, int teto,
			int forcaDoClube, RandomGenerator aleatorio) {
		int piso = forcaDoClube - parametros.abaixoDoClube();
		double perda = parametros.perdaDeQuemJogou().get(desfecho) / ParametrosEvolucao.EM_CAMPO;
		Map<Long, Integer> novas = new HashMap<>();
		// ordem fixa antes dos sorteios, para a mesma semente dar o mesmo resultado
		for (Jogador j : plantel.stream().sorted(Comparator.comparingLong(Jogador::id)).toList()) {
			int forca = j.forca();
			if (jogaram.contains(j.id())) {
				if (aleatorio.nextDouble() < perda && forca > piso) {
					forca--;
				}
			}
			else {
				double chance = (banco.contains(j.id()) ? parametros.crescimentoNoBanco() : parametros.crescimentoFora())
					.get(desfecho);
				if (aleatorio.nextDouble() < chance && forca < teto) {
					forca++;
				}
			}
			if (j.forca() > teto && aleatorio.nextDouble() < parametros.perdaAcimaDoTeto()) {
				forca--;
			}
			novas.put(j.id(), Math.clamp(forca, Jogador.FORCA_MINIMA, Jogador.FORCA_MAXIMA));
		}
		return plantel.stream().map(j -> novas.get(j.id()) == j.forca() ? j : j.comForca(novas.get(j.id()))).toList();
	}
}
