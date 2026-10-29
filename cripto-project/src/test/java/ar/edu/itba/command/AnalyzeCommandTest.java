package ar.edu.itba.command;

import ar.edu.itba.TestBitmaps;
import ar.edu.itba.cli.ArgumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeCommandTest {

    @TempDir
    Path dir;

    @Test
    void findsThePlainFileAndSavesItWithThePrefix() throws IOException {
        byte[] secret = randomBytes(300, 1);
        Path stego = embed(secret, "LSB4");

        String report = analyze("-analyze", "-p", stego.toString(), "-out", dir.resolve("hallado").toString());

        assertTrue(lineFor(report, "LSB4").contains("se encontró un archivo de 300 bytes con extensión .txt"), report);
        assertTrue(lineFor(report, "LSB1").contains("no "), report);
        assertTrue(lineFor(report, "LSBI").contains("no "), report);
        assertArrayEquals(secret, Files.readAllBytes(dir.resolve("hallado_LSB4.txt")));
    }

    @Test
    void flagsEncryptedContentWithoutPassword() throws IOException {
        Path stego = embed(randomBytes(300, 2), "LSBI", "-a", "3des", "-m", "cbc", "-pass", "oculto");

        String report = analyze("-analyze", "-p", stego.toString(), "-steg", "LSBI", "-out", dir.resolve("hallado").toString());

        // 4 + 300 + 5 = 309 bytes -> 312 con el padding de 3DES.
        assertTrue(lineFor(report, "LSBI").contains("puede estar encriptado"), report);
        assertTrue(lineFor(report, "LSBI").contains("312 bytes"), report);
        assertEquals(312, Files.size(dir.resolve("hallado_LSBI.enc")));
    }

    @Test
    void decryptsWhenPasswordIsGiven() throws IOException {
        byte[] secret = randomBytes(300, 3);
        Path stego = embed(secret, "LSB1", "-a", "aes192", "-m", "cfb", "-pass", "oculto");

        String report = analyze("-analyze", "-p", stego.toString(), "-steg", "LSB1", "-a", "aes192", "-m", "cfb", "-pass", "oculto");

        assertTrue(lineFor(report, "LSB1").contains("se encontró un archivo de 300 bytes"), report);
    }

    @Test
    void reportsBytesAfterThePixelBlock() throws IOException {
        Path bitmap = Files.write(dir.resolve("extra.bmp"), TestBitmaps.bmp(32, 32, 24, 0, 10, 4));

        String report = analyze("-analyze", "-p", bitmap.toString());

        assertTrue(report.contains("Hay 10 bytes después del bloque de píxeles"), report);
    }

    private Path embed(byte[] secret, String method, String... encryption) throws IOException {
        Path carrier = Files.write(dir.resolve("portador.bmp"), TestBitmaps.random(80, 80, 5));
        Path input = Files.write(dir.resolve("secreto.txt"), secret);
        Path stego = dir.resolve("stego.bmp");
        String[] args = {"-embed", "-in", input.toString(), "-p", carrier.toString(), "-out", stego.toString(),
                "-steg", method};
        String[] all = Arrays.copyOf(args, args.length + encryption.length);
        System.arraycopy(encryption, 0, all, args.length, encryption.length);
        EmbedCommand.run(ArgumentParser.parse(all), new PrintStream(new ByteArrayOutputStream()));
        return stego;
    }

    private static String analyze(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        AnalyzeCommand.run(ArgumentParser.parse(args), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String lineFor(String report, String method) {
        return report.lines().filter(line -> line.trim().startsWith(method + ":")).findFirst().orElse("");
    }

    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
