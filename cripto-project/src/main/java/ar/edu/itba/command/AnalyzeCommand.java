package ar.edu.itba.command;

import ar.edu.itba.FileIO;
import ar.edu.itba.StegoException;
import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.Encryption;
import ar.edu.itba.cli.Options;
import ar.edu.itba.crypto.PayloadCipher;
import ar.edu.itba.payload.HiddenFile;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.steg.StegMethod;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * {@code -analyze}: estegoanálisis. Intenta recuperar un archivo oculto con cada método y, si no lo
 * logra, indica si el tamaño oculto es compatible con información encriptada.
 */
public final class AnalyzeCommand {

    private AnalyzeCommand() {
    }

    public static void run(Options options, PrintStream out) {
        BmpImage carrier = Bitmaps.read(options.bitmap());
        byte[] pixels = carrier.pixels();
        Optional<PayloadCipher> cipher = options.encryption().map(Encryption::cipher);

        out.println("Análisis de " + options.bitmap() + " (" + carrier.width() + "x" + carrier.height() + ", "
                + pixels.length + " bytes de píxeles"
                + options.encryption().map(encryption -> ", desencriptando con " + encryption).orElse("") + "):");
        List<StegMethod> methods = options.steg().map(List::of).orElse(List.of(StegMethod.values()));
        for (StegMethod method : methods) {
            out.println("  " + method + ": " + analyze(method, pixels, cipher, options.output()));
        }

        byte[] trailing = carrier.trailingBytes();
        if (trailing.length == 0) {
            out.println("  No hay bytes después del bloque de píxeles.");
        } else {
            out.println("  Hay " + trailing.length + " bytes después del bloque de píxeles"
                    + save(options.output(), "_extra.bin", trailing) + ".");
        }
    }

    private static String analyze(StegMethod method, byte[] pixels, Optional<PayloadCipher> cipher,
                                  Optional<String> prefix) {
        byte[] hidden = method.algorithm().extract(pixels);
        try {
            HiddenFile file = PayloadCodec.decode(hidden, cipher);
            String extension = file.extension().isEmpty() ? "sin extensión" : "con extensión " + file.extension();
            return "se encontró un archivo de " + file.data().length + " bytes " + extension
                    + save(prefix, "_" + method + file.extension(), file.data());
        } catch (StegoException e) {
            if (cipher.isEmpty()) {
                Optional<String> encrypted = possiblyEncrypted(method, hidden, prefix);
                if (encrypted.isPresent()) {
                    return encrypted.get();
                }
            }
            return "no se encontró un archivo oculto (" + e.getMessage() + ")";
        }
    }

    /**
     * Sin encripción, el tamaño declarado puede ser válido pero sin una extensión después de los datos:
     * es lo que se ve cuando lo oculto es {@code tamaño del cifrado || cifrado}.
     */
    private static Optional<String> possiblyEncrypted(StegMethod method, byte[] hidden, Optional<String> prefix) {
        long size = PayloadCodec.declaredSize(hidden);
        if (size == 0 || size > hidden.length - PayloadCodec.SIZE_BYTES) {
            return Optional.empty();
        }
        String blocks;
        if (size % 16 == 0) {
            blocks = "múltiplo de 16: compatible con cualquier algoritmo y modo";
        } else if (size % 8 == 0) {
            blocks = "múltiplo de 8: compatible con 3des en cualquier modo, o con aes en cfb/ofb";
        } else {
            blocks = "no es múltiplo de bloque: sólo modos cfb u ofb";
        }
        byte[] encrypted = Arrays.copyOfRange(hidden, PayloadCodec.SIZE_BYTES, PayloadCodec.SIZE_BYTES + (int) size);
        return Optional.of("no hay un archivo sin encriptar, pero el tamaño oculto es válido (" + size + " bytes, "
                + blocks + "): puede estar encriptado, probar con -pass"
                + save(prefix, "_" + method + ".enc", encrypted));
    }

    private static String save(Optional<String> prefix, String suffix, byte[] data) {
        if (prefix.isEmpty()) {
            return "";
        }
        Path path = Path.of(prefix.get() + suffix);
        FileIO.write(path, data);
        return " -> guardado en " + path;
    }
}
