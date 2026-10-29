package ar.edu.itba.command;

import ar.edu.itba.FileIO;
import ar.edu.itba.StegoException;
import ar.edu.itba.cli.Encryption;
import ar.edu.itba.cli.Options;
import ar.edu.itba.payload.HiddenFile;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.steg.StegMethod;

import java.io.PrintStream;
import java.nio.file.Path;

/** {@code -extract}: recupera el archivo oculto en un bmp. La extensión recuperada se agrega a {@code -out}. */
public final class ExtractCommand {

    private ExtractCommand() {
    }

    public static void run(Options options, PrintStream out) {
        StegMethod method = options.steg().orElseThrow();
        byte[] pixels = Bitmaps.read(options.bitmap()).pixels();
        byte[] hidden = method.algorithm().extract(pixels);

        HiddenFile file;
        try {
            file = PayloadCodec.decode(hidden, options.encryption().map(Encryption::cipher));
        } catch (StegoException e) {
            throw new StegoException("no se pudo recuperar un archivo oculto con " + method + " en "
                    + options.bitmap() + ": " + e.getMessage(), e);
        }
        Path output = Path.of(file.fileName(options.output().orElseThrow()));
        FileIO.write(output, file.data());

        out.println("Se extrajo " + output + " (" + file.data().length + " bytes) de " + options.bitmap()
                + " con " + method + ".");
    }
}
