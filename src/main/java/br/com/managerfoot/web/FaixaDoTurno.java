package br.com.managerfoot.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.multiplayer.Turno.Passo;
import br.com.managerfoot.persistencia.SalaRepositorio;
import br.com.managerfoot.persistencia.TurnoRepositorio;

/**
 * Faixa "Aguardando" do jogo em rede (SPEC §3.3 itens 4 a 7, rev. 92; modelo 18e): quem já deu OK no
 * plantel e nos leilões, montada para as telas que a mostram.
 */
@Component
class FaixaDoTurno {

	private final TurnoRepositorio turnos;
	private final SalaRepositorio salas;
	private final Clock relogio;
	private final Duration semConexaoApos;

	FaixaDoTurno(TurnoRepositorio turnos, SalaRepositorio salas, Clock relogio,
			@Value("${managerfoot.rede.sem-conexao-apos}") Duration semConexaoApos) {
		this.turnos = turnos;
		this.salas = salas;
		this.relogio = relogio;
		this.semConexaoApos = semConexaoApos;
	}

	/**
	 * Põe a faixa no modelo da tela, se o jogo é em rede, e marca o treinador como conectado.
	 *
	 * @param rotulo o que o OK confirma: "rodada 6", "Copa · 2ª fase", "fim da temporada"
	 * @return o turno, vazio fora do jogo em rede começado
	 */
	Optional<Turno> preencher(Model model, long jogoId, Usuario usuario, String tela, String rotulo) {
		Optional<Turno> turno = turnos.turno(jogoId);
		turno.ifPresent(t -> {
			salas.marcarVisto(jogoId, usuario.id(), relogio.instant());
			model.addAttribute("painel", painel(jogoId, usuario.id(), t, tela, rotulo));
		});
		return turno;
	}

	private Painel painel(long jogoId, long eu, Turno turno, String tela, String rotulo) {
		Instant agora = relogio.instant();
		// quem criou o jogo pode tirar os outros entre as rodadas (SPEC §11.4, rev. 104)
		boolean souAdministrador = turnos.administrador(jogoId).filter(d -> d == eu).isPresent();
		List<Linha> linhas = turno.treinadores().stream().map(t -> {
			Turno.Situacao situacao = t.situacao(agora, semConexaoApos);
			return new Linha(t.nome(), t.usuarioId() == eu ? "(você)" : "", t.clube() == null ? "" : t.clube(),
					switch (situacao) {
						case OK -> "OK";
						case MONTANDO -> switch (turno.passo()) {
							case LANCES -> "Dando lances";
							case RESULTADO -> "Vendo o resultado";
							case PLANTEL -> "Montando o time";
						};
						case SEM_CONEXAO -> "Sem conexão há " + SalaController.tempo(Duration.between(t.vistoEm(), agora));
					}, switch (situacao) {
						case OK -> "estado-pronto";
						case MONTANDO -> "estado-mexendo";
						case SEM_CONEXAO -> "estado-fora";
					}, souAdministrador && t.usuarioId() != eu ? t.usuarioId() : null);
		}).toList();
		String segue = switch (turno.passo()) {
			case LANCES -> "Os lances são secretos: ninguém vê o lance de ninguém até o resultado.";
			case RESULTADO -> "A rodada começa quando todos derem OK.";
			case PLANTEL -> turno.momento().endsWith("/fim") ? "A próxima temporada começa quando todos derem OK."
					: "O jogo começa quando todos derem OK.";
		};
		return new Painel(jogoId, turno.momento(), tela, rotulo, turno.comOk() + " de " + turno.treinadores().size() + " com OK",
				linhas, turno.deuOk(eu), turno.faltam(), segue,
				turno.passo() == Passo.LANCES ? "Mudar meus lances" : "Tirar meu OK e mexer");
	}

	/**
	 * Um treinador na faixa.
	 *
	 * @param tirar para o administrador, o treinador que ele pode tirar do jogo; nulo nos outros casos
	 */
	record Linha(String nome, String nota, String clube, String estado, String classe, Long tirar) {
	}

	/**
	 * A faixa na tela.
	 *
	 * @param faltam "Lúcia e Renata"
	 * @param segue o que acontece quando todos derem OK
	 */
	record Painel(long jogoId, String momento, String tela, String rotulo, String contagem, List<Linha> linhas, boolean deuOk,
			String faltam, String segue, String tirar) {
	}
}
