package br.com.managerfoot.web;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;

/** Campeonatos oferecidos no novo jogo: os do banco padrão que podem iniciar. */
@Component
class Campeonatos {

	private final MoldeRepositorio moldes;
	private final BancoPadraoRepositorio bancoPadrao;

	Campeonatos(MoldeRepositorio moldes, BancoPadraoRepositorio bancoPadrao) {
		this.moldes = moldes;
		this.bancoPadrao = bancoPadrao;
	}

	List<CampeonatoDisponivel> disponiveis() {
		return bancoPadrao.moldeId().map(moldes::campeonatosDisponiveis).orElse(List.of());
	}

	/** O campeonato, se ele está entre os oferecidos: o id enviado pelo navegador não é confiável. */
	Optional<CampeonatoDisponivel> buscar(long paisId) {
		return disponiveis().stream().filter(c -> c.paisId() == paisId).findFirst();
	}
}
