package br.com.managerfoot.motor;

/**
 * Partida da copa nacional, onde vale a zebra (SPEC §8.2, rev. 117; {@link ParametrosZebraCopa}).
 *
 * @param primeirasFases 16 avos ou oitavas: o mais fraco pode ter o super bônus
 */
public record CopaNacional(boolean primeirasFases) {
}
