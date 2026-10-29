package ar.edu.itba.command;

import ar.edu.itba.StegoException;
import ar.edu.itba.TestBitmaps;
import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.ArgumentParser;
import ar.edu.itba.cli.Options;
import ar.edu.itba.steg.StegMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbedExtractTest {

    private static final PrintStream SILENT = new PrintStream(new ByteArrayOutputStream());

    @TempDir
    Path dir;

    static Stream<Arguments> combinations() {
        List<String> encryptions = List.of("", "-pass oculto", "-a aes192 -m ecb -pass oculto",
                "-a 3des -m ofb -pass oculto", "-a aes256 -m cfb -pass oculto");
        return Arrays.stream(StegMethod.values())
                .flatMap(method -> encryptions.stream().map(encryption -> Arguments.of(method, encryption)));
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void embedThenExtractRecoversTheFile(StegMethod method, String encryption) throws IOException {
        // 101 px de ancho: 303 bytes por fila más 1 byte de padding.
        Path carrier = write("portador.bmp", TestBitmaps.random(101, 60, 1));
        byte[] secret = randomBytes(500, 2);
        Path input = write("mensaje.txt", secret);
        Path stego = dir.resolve("salida.bmp");

        run("-embed -in %s -p %s -out %s -steg %s " + encryption, input, carrier, stego, method);
        run("-extract -p %s -out %s -steg %s " + encryption, stego, dir.resolve("recuperado"), method);

        assertArrayEquals(secret, Files.readAllBytes(dir.resolve("recuperado.txt")));
        byte[] original = Files.readAllBytes(carrier);
        byte[] result = Files.readAllBytes(stego);
        assertArrayEquals(Arrays.copyOf(original, BmpImage.HEADER_SIZE), Arrays.copyOf(result, BmpImage.HEADER_SIZE));
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void fileWithoutExtensionIsExtractedWithoutExtension(StegMethod method, String encryption) throws IOException {
        Path carrier = write("portador.bmp", TestBitmaps.random(64, 64, 3));
        Path input = write("LEEME", new byte[]{1, 2, 3});
        Path stego = dir.resolve("salida.bmp");

        run("-embed -in %s -p %s -out %s -steg %s " + encryption, input, carrier, stego, method);
        run("-extract -p %s -out %s -steg %s " + encryption, stego, dir.resolve("recuperado"), method);

        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(dir.resolve("recuperado")));
    }

    @Test
    void reportsCarrierCapacityWhenTheFileDoesNotFit() throws IOException {
        // 16x16: 768 bytes de píxeles -> 96 bytes con LSB1.
        Path carrier = write("chico.bmp", TestBitmaps.random(16, 16, 4));
        Path input = write("grande.txt", new byte[200]);

        StegoException error = assertThrows(StegoException.class, () -> run(
                "-embed -in %s -p %s -out %s -steg LSB1", input, carrier, dir.resolve("x.bmp")));

        assertTrue(error.getMessage().contains("96 bytes"), error.getMessage());
        assertFalse(Files.exists(dir.resolve("x.bmp")));
    }

    @Test
    void extractingFromACleanImageFails() throws IOException {
        Path carrier = write("limpio.bmp", TestBitmaps.random(64, 64, 5));

        assertThrows(StegoException.class, () -> run(
                "-extract -p %s -out %s -steg LSB1", carrier, dir.resolve("nada")));
    }

    @Test
    void wrongPasswordFails() throws IOException {
        Path carrier = write("portador.bmp", TestBitmaps.random(64, 64, 6));
        Path input = write("mensaje.txt", randomBytes(100, 7));
        Path stego = dir.resolve("salida.bmp");
        run("-embed -in %s -p %s -out %s -steg LSBI -pass correcta", input, carrier, stego);

        assertThrows(StegoException.class, () -> run(
                "-extract -p %s -out %s -steg LSBI -pass incorrecta", stego, dir.resolve("x")));
    }

    private Path write(String name, byte[] data) throws IOException {
        return Files.write(dir.resolve(name), data);
    }

    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    /** Ejecuta el comando; cada "%s" del template se reemplaza por un argumento entero (las rutas pueden tener espacios). */
    private static void run(String template, Object... values) {
        Iterator<Object> next = Arrays.asList(values).iterator();
        String[] args = Arrays.stream(template.trim().split(" +"))
                .map(token -> token.equals("%s") ? next.next().toString() : token)
                .toArray(String[]::new);
        Options options = ArgumentParser.parse(args);
        switch (options.command()) {
            case EMBED -> EmbedCommand.run(options, SILENT);
            case EXTRACT -> ExtractCommand.run(options, SILENT);
        }
    }
}
