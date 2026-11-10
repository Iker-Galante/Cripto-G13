package ar.edu.itba;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// The course's sample images stay outside the repo. Point STEGOBMP_SAMPLES at their folder to run these.
class CatedraSamplesTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int HIDDEN_PNG_SIZE = 44886;
    private static final String HIDDEN_PNG_SHA256_PREFIX = "b93be15382f35f09";

    @TempDir
    Path dir;

    private Path samples;

    @BeforeEach
    void findSamples() {
        String folder = System.getenv("STEGOBMP_SAMPLES");
        assumeTrue(folder != null && Files.isDirectory(Path.of(folder)),
                "Set STEGOBMP_SAMPLES to the folder with the catedra samples");
        samples = Path.of(folder);
    }

    @Test
    void lsb1SampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSB1.bmp", "LSB1");
    }

    @Test
    void lsb4SampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSB4.bmp", "LSB4");
    }

    @Test
    void lsbiSampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSBI.bmp", "LSBI");
    }

    @Test
    void lsb1Aes128CbcSampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSB1aes128cbc.bmp", "LSB1", "-a", "aes128", "-m", "cbc", "-pass", "margarita");
    }

    @Test
    void lsbiAes256OfbSampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSBIaes256ofb.bmp", "LSBI", "-a", "aes256", "-m", "ofb", "-pass", "margarita");
    }

    @Test
    void lsbi3desCfbSampleExtractsAndReembedsIdentically() throws Exception {
        checkSample("ladoLSBIdescfb.bmp", "LSBI", "-a", "3des", "-m", "cfb", "-pass", "margarita");
    }

    @Test
    void allThreeSamplesHideTheSameFile() throws IOException {
        assertEquals(0, run("-extract", "-p", sample("ladoLSB1.bmp"), "-out", path("from1"), "-steg", "LSB1"));
        assertEquals(0, run("-extract", "-p", sample("ladoLSB4.bmp"), "-out", path("from4"), "-steg", "LSB4"));
        assertEquals(0, run("-extract", "-p", sample("ladoLSBI.bmp"), "-out", path("fromI"), "-steg", "LSBI"));

        byte[] fromLsb1 = Files.readAllBytes(dir.resolve("from1.png"));
        assertArrayEquals(fromLsb1, Files.readAllBytes(dir.resolve("from4.png")));
        assertArrayEquals(fromLsb1, Files.readAllBytes(dir.resolve("fromI.png")));
    }

    // Extract the sample, then embed what we got into the clean carrier: we must rebuild the sample byte for byte.
    // cipherOptions are the -a, -m and -pass arguments, used the same way for extract and embed.
    private void checkSample(String sampleName, String method, String... cipherOptions)
            throws IOException, NoSuchAlgorithmException {
        Path stego = samples.resolve(sampleName);

        assertEquals(0, run(withOptions(cipherOptions,
                "-extract", "-p", stego.toString(), "-out", path("hidden"), "-steg", method)));
        byte[] png = Files.readAllBytes(dir.resolve("hidden.png"));
        assertEquals(HIDDEN_PNG_SIZE, png.length);
        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(png, PNG_SIGNATURE.length));
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
        assertTrue(sha256.startsWith(HIDDEN_PNG_SHA256_PREFIX), sha256);

        assertEquals(0, run(withOptions(cipherOptions, "-embed", "-in", path("hidden.png"), "-p", sample("lado.bmp"),
                "-out", path("ours.bmp"), "-steg", method)));
        assertArrayEquals(Files.readAllBytes(stego), Files.readAllBytes(dir.resolve("ours.bmp")));
    }

    private static String[] withOptions(String[] options, String... args) {
        List<String> all = new ArrayList<>(List.of(args));
        all.addAll(List.of(options));
        return all.toArray(String[]::new);
    }

    private int run(String... args) {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int status = Main.run(args, System.out, new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
        return status;
    }

    private String sample(String name) {
        return samples.resolve(name).toString();
    }

    private String path(String name) {
        return dir.resolve(name).toString();
    }
}
