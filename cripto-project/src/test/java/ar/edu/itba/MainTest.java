package ar.edu.itba;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.bmp.TestBmps;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    private final PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

    @Test
    void embedThenExtractGivesTheSameFile() throws IOException {
        // The 6x5 image holds 12 bytes: 4 (size) + 3 (data) + 5 (".txt" and 0).
        byte[] secret = "hey".getBytes(StandardCharsets.US_ASCII);
        Path carrier = write("6x5.bmp", TestBmps.sixByFive());
        write("hola.txt", secret);

        assertEquals(0, run("-embed", "-in", path("hola.txt"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSB1"));
        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertArrayEquals(secret, Files.readAllBytes(dir.resolve("back.txt")));
        assertArrayEquals(BmpImage.parse(Files.readAllBytes(carrier)).header(),
                BmpImage.parse(Files.readAllBytes(dir.resolve("out.bmp"))).header());
        assertEquals("", errText());
    }

    @Test
    void embedFailsWhenFileDoesNotFit() throws IOException {
        write("6x5.bmp", TestBmps.sixByFive());
        write("hola.txt", "hola mund".getBytes(StandardCharsets.US_ASCII));

        assertEquals(1, run("-embed", "-in", path("hola.txt"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSB1"));

        assertTrue(errText().contains("maximum capacity with LSB1 is 12 bytes"), errText());
    }

    @Test
    void embedFailsForFileWithoutExtension() throws IOException {
        write("6x5.bmp", TestBmps.sixByFive());
        write("hola", new byte[] {1});

        assertEquals(1, run("-embed", "-in", path("hola"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSB1"));

        assertEquals("Error: File to hide has no extension: hola", errText().strip());
    }

    @Test
    void usageErrorPrintsReasonAndUsage() {
        assertEquals(1, run("-extract", "-p", "out.bmp", "-out", "back", "-steg", "LSB1", "-pass", "x"));

        assertEquals("Error: Unknown option: -pass\n" + Main.USAGE, errText().strip());
    }

    @Test
    void missingCarrierPrintsOneLine() {
        assertEquals(1, run("-extract", "-p", path("nope.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertEquals("Error: File not found: " + path("nope.bmp"), errText().strip());
    }

    private int run(String... args) {
        return Main.run(args, err);
    }

    private Path write(String name, byte[] content) throws IOException {
        return Files.write(dir.resolve(name), content);
    }

    private String path(String name) {
        return dir.resolve(name).toString();
    }

    private String errText() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }
}
