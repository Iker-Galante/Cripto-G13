package ar.edu.itba.cli;

import ar.edu.itba.steg.StegMethod;

import java.util.List;
import java.util.Optional;

/**
 * Parámetros ya validados.
 *
 * @param input      archivo a ocultar ({@code -in}); vacío salvo en {@code -embed}
 * @param bitmap     bmp portador ({@code -p})
 * @param output     archivo de salida ({@code -out})
 * @param steg       método de esteganografiado ({@code -steg})
 * @param encryption encripción, presente sólo si se indicó {@code -pass}
 * @param warnings   advertencias para mostrarle al usuario
 */
public record Options(Command command, Optional<String> input, String bitmap, String output, StegMethod steg,
                      Optional<Encryption> encryption, List<String> warnings) {
}
