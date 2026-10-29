package ar.edu.itba.cli;

import ar.edu.itba.steg.StegMethod;

import java.util.List;
import java.util.Optional;

/**
 * Parámetros ya validados. Los opcionales siempre están presentes cuando la operación los requiere.
 *
 * @param input      archivo a ocultar ({@code -in}); sólo en {@code -embed}
 * @param bitmap     bmp portador ({@code -p})
 * @param output     archivo de salida ({@code -out}); opcional en {@code -analyze}
 * @param steg       método de esteganografiado ({@code -steg}); opcional en {@code -analyze}
 * @param encryption encripción, presente sólo si se indicó {@code -pass}
 * @param warnings   advertencias para mostrarle al usuario
 */
public record Options(Command command, Optional<String> input, String bitmap, Optional<String> output,
                      Optional<StegMethod> steg, Optional<Encryption> encryption, List<String> warnings) {
}
