package ar.edu.itba.command;

import ar.edu.itba.FileIO;
import ar.edu.itba.StegoException;
import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.Encryption;
import ar.edu.itba.cli.Options;
import ar.edu.itba.payload.HiddenFile;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.steg.StegAlgorithm;
import ar.edu.itba.steg.StegMethod;

import java.io.PrintStream;
import java.nio.file.Path;

/** {@code -embed}: oculta un archivo en un bmp portador. */
public final class EmbedCommand {

    private EmbedCommand() {
    }

    public static void run(Options options, PrintStream out) {
        StegMethod method = options.steg().orElseThrow();
        String output = options.output().orElseThrow();
        Path input = Path.of(options.input().orElseThrow());
        Path fileName = input.getFileName();
        HiddenFile file = HiddenFile.of(fileName == null ? "" : fileName.toString(), FileIO.read(input));
        BmpImage carrier = Bitmaps.read(options.bitmap());
        byte[] payload = PayloadCodec.encode(file, options.encryption().map(Encryption::cipher));

        byte[] pixels = carrier.pixels();
        StegAlgorithm algorithm = method.algorithm();
        int capacity = algorithm.capacity(pixels.length);
        if (payload.length > capacity) {
            String contents = options.encryption().isPresent()
                    ? "tamaño del cifrado + cifrado"
                    : "tamaño + archivo + extensión";
            throw new StegoException("el portador " + options.bitmap() + " no tiene capacidad suficiente con "
                    + method + ": su capacidad máxima es de " + capacity + " bytes y se necesitan "
                    + payload.length + " bytes (" + contents + ")");
        }
        algorithm.embed(pixels, payload);
        FileIO.write(Path.of(output), carrier.withPixels(pixels));

        out.println("Se ocultó " + input + " (" + file.data().length + " bytes) en " + output + " con " + method
                + options.encryption().map(encryption -> ", encriptado con " + encryption).orElse("") + ".");
    }
}
