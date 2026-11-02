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
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    private static final String NO_HIDDEN_DATA =
            "Error: No hidden data found in the carrier (or wrong -steg method, -a, -m or -pass)";
    private static final byte[] SECRET = "hey".getBytes(StandardCharsets.US_ASCII);

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
    void lsb4EmbedThenExtractGivesTheSameFile() throws IOException {
        byte[] secret = "hey".getBytes(StandardCharsets.US_ASCII);
        write("6x5.bmp", TestBmps.sixByFive());
        write("hola.txt", secret);

        assertEquals(0, run("-embed", "-in", path("hola.txt"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSB4"));
        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB4"));

        assertArrayEquals(secret, Files.readAllBytes(dir.resolve("back.txt")));
        assertEquals("", errText());
    }

    @Test
    void lsb4EmbedFailsWhenFileDoesNotFit() throws IOException {
        // 4 + 42 + 5 = 51 bytes, one more than the 50 that LSB4 fits in the 6x5 image.
        write("6x5.bmp", TestBmps.sixByFive());
        write("big.txt", new byte[42]);

        assertEquals(1, run("-embed", "-in", path("big.txt"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSB4"));

        assertTrue(errText().contains("maximum capacity with LSB4 is 50 bytes"), errText());
    }

    @Test
    void lsbiEmbedThenExtractGivesTheSameFile() throws IOException {
        // 10x5 blank image: 160 pixel bytes hold 13 LSBI bytes; "hey" needs 4 + 3 + 5 = 12.
        byte[] secret = "hey".getBytes(StandardCharsets.US_ASCII);
        write("10x5.bmp", TestBmps.blank(10, 5));
        write("hola.txt", secret);

        assertEquals(0, run("-embed", "-in", path("hola.txt"), "-p", path("10x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSBI"));
        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSBI"));

        assertArrayEquals(secret, Files.readAllBytes(dir.resolve("back.txt")));
        assertEquals("", errText());
    }

    @Test
    void lsbiEmbedFailsWhenFileDoesNotFit() throws IOException {
        write("6x5.bmp", TestBmps.sixByFive());
        write("hola.txt", "hey".getBytes(StandardCharsets.US_ASCII));

        assertEquals(1, run("-embed", "-in", path("hola.txt"), "-p", path("6x5.bmp"),
                "-out", path("out.bmp"), "-steg", "LSBI"));

        assertTrue(errText().contains("maximum capacity with LSBI is 8 bytes"), errText());
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
        assertEquals(1, run("-extract", "-p", "out.bmp", "-out", "back", "-steg", "LSB1", "-analyze"));

        assertEquals("Error: Unknown option: -analyze\n" + Main.USAGE, errText().strip());
    }

    @Test
    void missingCarrierPrintsOneLine() {
        assertEquals(1, run("-extract", "-p", path("nope.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertEquals("Error: File not found: " + path("nope.bmp"), errText().strip());
    }

    @Test
    void extractFromCleanCarrierSaysNothingIsHidden() throws IOException {
        write("6x5.bmp", TestBmps.sixByFive());

        assertEquals(1, run("-extract", "-p", path("6x5.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertEquals(NO_HIDDEN_DATA, errText().strip());
    }

    @Test
    void encryptedRoundTripWithDefaults() throws IOException {
        embedHeyIntoBlankImage("-pass", "secret");

        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1", "-pass", "secret"));

        assertArrayEquals(SECRET, Files.readAllBytes(dir.resolve("back.txt")));
        assertEquals("", errText());
    }

    @Test
    void encryptedRoundTripWith3desCfb() throws IOException {
        embedHeyIntoBlankImage("-a", "3des", "-m", "cfb", "-pass", "secret");

        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1",
                "-a", "3des", "-m", "cfb", "-pass", "secret"));

        assertArrayEquals(SECRET, Files.readAllBytes(dir.resolve("back.txt")));
    }

    @Test
    void wrongPasswordWithCbcSaysItCouldNotDecrypt() throws IOException {
        embedHeyIntoBlankImage("-pass", "secret");

        assertEquals(1, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1", "-pass", "wrong"));

        assertEquals("Error: Could not decrypt: wrong password, algorithm or mode", errText().strip());
        assertTrue(Files.notExists(dir.resolve("back.txt")));
    }

    @Test
    void wrongPasswordWithOfbSaysNoHiddenData() throws IOException {
        embedHeyIntoBlankImage("-m", "ofb", "-pass", "secret");

        assertEquals(1, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1",
                "-m", "ofb", "-pass", "wrong"));

        assertEquals(NO_HIDDEN_DATA, errText().strip());
    }

    @Test
    void extractWithoutPassFromEncryptedCarrierSaysNoHiddenData() throws IOException {
        embedHeyIntoBlankImage("-pass", "secret");

        assertEquals(1, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertEquals(NO_HIDDEN_DATA, errText().strip());
    }

    @Test
    void algorithmWithoutPassHidesWithoutEncryption() throws IOException {
        embedHeyIntoBlankImage("-a", "aes256");

        assertEquals(0, run("-extract", "-p", path("out.bmp"), "-out", path("back"), "-steg", "LSB1"));

        assertArrayEquals(SECRET, Files.readAllBytes(dir.resolve("back.txt")));
        assertEquals("", errText());
    }

    // 16x4 blank image: 192 pixel bytes hold 24 LSB1 bytes. Encrypted "hey" needs 4 + 16 with cbc.
    private void embedHeyIntoBlankImage(String... cipherOptions) throws IOException {
        write("16x4.bmp", TestBmps.blank(16, 4));
        write("hola.txt", SECRET);
        String[] args = {"-embed", "-in", path("hola.txt"), "-p", path("16x4.bmp"), "-out", path("out.bmp"), "-steg", "LSB1"};
        String[] all = Arrays.copyOf(args, args.length + cipherOptions.length);
        System.arraycopy(cipherOptions, 0, all, args.length, cipherOptions.length);

        assertEquals(0, run(all));
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
