package br.com.managerfoot.web;

import org.springframework.stereotype.Component;

import br.com.managerfoot.dominio.Escudo;

/**
 * Desenho do escudo em CSS, como nos modelos aprovados (SPEC §10.2 e §10.5). Usado nas telas como
 * {@code @escudos.estilo(clube.escudo(), 36, 44)}.
 */
@Component("escudos")
public class Escudos {

	public String estilo(Escudo escudo, int largura, int altura) {
		String a = escudo.corPrimaria();
		String b = escudo.corSecundaria();
		String fundo = switch (escudo.modelo()) {
			case METADE_VERTICAL -> "linear-gradient(90deg, " + a + " 50%, " + b + " 50%)";
			case METADE_HORIZONTAL -> "linear-gradient(180deg, " + a + " 50%, " + b + " 50%)";
			case LISTRAS_VERTICAIS -> "repeating-linear-gradient(90deg, " + a + " 0 20%, " + b + " 20% 40%)";
			case LISTRAS_HORIZONTAIS -> "repeating-linear-gradient(180deg, " + a + " 0 20%, " + b + " 20% 40%)";
			case CORTE_DIAGONAL -> "linear-gradient(135deg, " + a + " 50%, " + b + " 50%)";
			case FAIXA_DIAGONAL -> "linear-gradient(135deg, " + a + " 36%, " + b + " 36% 64%, " + a + " 64%)";
			case FAIXA_HORIZONTAL -> "linear-gradient(180deg, " + a + " 36%, " + b + " 36% 64%, " + a + " 64%)";
			case QUARTOS -> "conic-gradient(" + a + " 0 25%, " + b + " 0 50%, " + a + " 0 75%, " + b + " 0)";
			case CRUZ -> "linear-gradient(90deg, transparent 40%, " + b + " 40% 60%, transparent 60%), "
					+ "linear-gradient(180deg, transparent 30%, " + b + " 30% 50%, transparent 50%), " + a;
			case LISO_COM_BORDA -> a;
		};
		String borda = switch (escudo.modelo()) {
			case LISO_COM_BORDA -> "inset 0 0 0 " + Math.max(2, Math.round(largura / 10f)) + "px " + b + ", ";
			default -> "";
		};
		int topo = Math.round(largura / 6f);
		int base = Math.round(largura / 2f);
		return "width: " + largura + "px; height: " + altura + "px; flex: none; border-radius: " + topo + "px " + topo
				+ "px " + base + "px " + base + "px; background: " + fundo + "; box-shadow: " + borda
				+ "0 0 0 1px rgba(255,255,255,0.35)";
	}
}
