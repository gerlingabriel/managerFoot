package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ResultadoPartida;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.ParametrosEvolucao.Desfecho;

/**
 * Evolução de força depois de uma partida (SPEC §6.1, rev. 54 e 55): cada jogador muda no máximo 1
 * ponto. Entre os que jogaram, alguns sorteados perdem 1 (mais na derrota); entre os que não jogaram,
 * alguns sorteados ganham 1 (mais na vitória). O desgaste não leva abaixo de um piso pela força do
 * clube (como o "nível base do clube" − 5 do Elifoot 98). Quem passa do teto da divisão não cresce e
 * perde força aos poucos (§4.2). A força fica entre 1 e 50. Domínio puro.
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
	 * @param forcaDoClube força cadastrada do clube: o desgaste não leva abaixo dela menos o piso
	 */
	public List<Jogador> aposPartida(List<Jogador> plantel, Set<Long> jogaram, Set<Long> banco, Desfecho desfecho, int teto,
			int forcaDoClube, RandomGenerator aleatorio) {
		int piso = forcaDoClube - Math.min(parametros.abaixoDoClube(), forcaDoClube);
		// ordem fixa antes dos sorteios, para a mesma semente dar o mesmo resultado
		List<Jogador> ordem = plantel.stream().sorted(Comparator.comparingLong(Jogador::id)).toList();
		Map<Long, Integer> variacao = new HashMap<>();
		List<Jogador> podemPerder = ordem.stream().filter(j -> jogaram.contains(j.id()) && j.forca() > piso).toList();
		List<Jogador> podemCrescer = ordem.stream().filter(j -> !jogaram.contains(j.id()) && j.forca() < teto).toList();
		sortear(podemPerder, parametros.perdem().get(desfecho), aleatorio).forEach(j -> variacao.put(j.id(), -1));
		sortear(podemCrescer, parametros.crescem().get(desfecho), aleatorio).forEach(j -> variacao.put(j.id(), 1));
		for (Jogador j : ordem) {
			if (j.forca() > teto && !variacao.containsKey(j.id()) && aleatorio.nextDouble() < parametros.perdaAcimaDoTeto()) {
				variacao.put(j.id(), -1);
			}
		}
		return plantel.stream().map(j -> {
			int delta = variacao.getOrDefault(j.id(), 0);
			return delta == 0 ? j : j.comForca(Math.clamp(j.forca() + delta, Jogador.FORCA_MINIMA, Jogador.FORCA_MAXIMA));
		}).toList();
	}

	/**
	 * O plantel do clube que jogou no {@code lado} da partida: jogaram os titulares e quem entrou; o
	 * banco são as reservas que não entraram. Depois da evolução, o lesionado perde a força sorteada
	 * na lesão (SPEC §7.5), sem passar da força mínima.
	 */
	public List<Jogador> aposPartida(List<Jogador> plantel, Escalacao escalacao, ResultadoPartida resultado, Lado lado,
			int teto, int forcaDoClube, RandomGenerator aleatorio) {
		Set<Long> jogaram = new HashSet<>();
		escalacao.titulares().forEach(j -> jogaram.add(j.id()));
		for (EventoPartida e : resultado.eventos()) {
			if (e.tipo() == TipoEvento.SUBSTITUICAO && e.lado() == lado) {
				jogaram.add(e.jogador().id());
			}
		}
		Set<Long> banco = new HashSet<>();
		escalacao.reservas().forEach(j -> banco.add(j.id()));
		banco.removeAll(jogaram);
		int golsPro = lado == Lado.CASA ? resultado.golsCasa() : resultado.golsFora();
		int golsContra = lado == Lado.CASA ? resultado.golsFora() : resultado.golsCasa();
		Map<Long, Integer> perdas = new HashMap<>();
		for (EventoPartida e : resultado.eventos()) {
			if (e.tipo() == TipoEvento.LESAO && e.lado() == lado) {
				perdas.merge(e.jogador().id(), e.lesao().perdaForca(), Integer::sum);
			}
		}
		return aposPartida(plantel, jogaram, banco, Desfecho.de(golsPro, golsContra), teto, forcaDoClube, aleatorio)
			.stream()
			.map(j -> perdas.containsKey(j.id())
					? j.comForca(Math.max(Jogador.FORCA_MINIMA, j.forca() - perdas.get(j.id())))
					: j)
			.toList();
	}

	/** Sorteia quantos: a parte inteira certa e mais um com a chance da parte fracionária. */
	private static List<Jogador> sortear(List<Jogador> candidatos, double quantos, RandomGenerator aleatorio) {
		int n = (int) Math.floor(quantos);
		if (aleatorio.nextDouble() < quantos - n) {
			n++;
		}
		List<Jogador> restantes = new ArrayList<>(candidatos);
		List<Jogador> sorteados = new ArrayList<>();
		while (sorteados.size() < n && !restantes.isEmpty()) {
			sorteados.add(restantes.remove(aleatorio.nextInt(restantes.size())));
		}
		return sorteados;
	}
}
