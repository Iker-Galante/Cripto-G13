package ar.edu.itba;

import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.Encryption;
import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.payload.HiddenFile;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.steg.StegMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Archivos de ejemplo de la cátedra (TP/ejemploTP.zip): lado.bmp con una imagen .png oculta de distintas
 * formas. Si el zip no está, los tests se saltean.
 */
class CourseExamplesTest {

    private static final Path ZIP = Path.of("..", "TP", "ejemploTP.zip");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final String PASSWORD = "margarita";

    private static final Map<String, byte[]> files = new HashMap<>();

    @BeforeAll
    static void loadExamples() throws IOException {
        assumeTrue(Files.exists(ZIP), "no se encontró " + ZIP.toAbsolutePath());
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(ZIP))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    files.put(Path.of(entry.getName()).getFileName().toString(), zip.readAllBytes());
                }
            }
        }
    }

    static Stream<Arguments> examples() {
        return Stream.of(
                Arguments.of("ladoLSB1.bmp", StegMethod.LSB1, Optional.empty()),
                Arguments.of("ladoLSB4.bmp", StegMethod.LSB4, Optional.empty()),
                Arguments.of("ladoLSBI.bmp", StegMethod.LSBI, Optional.empty()),
                Arguments.of("ladoLSB1aes128cbc.bmp", StegMethod.LSB1,
                        Optional.of(new Encryption(CipherAlgorithm.AES128, CipherMode.CBC, PASSWORD))),
                Arguments.of("ladoLSBIaes256ofb.bmp", StegMethod.LSBI,
                        Optional.of(new Encryption(CipherAlgorithm.AES256, CipherMode.OFB, PASSWORD))),
                Arguments.of("ladoLSBIdescfb.bmp", StegMethod.LSBI,
                        Optional.of(new Encryption(CipherAlgorithm.DES3, CipherMode.CFB, PASSWORD))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void extractsTheHiddenPng(String name, StegMethod method, Optional<Encryption> encryption) {
        HiddenFile file = extract(name, method, encryption);

        assertEquals(".png", file.extension());
        assertEquals(44886, file.data().length);
        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(file.data(), PNG_SIGNATURE.length));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void embeddingReproducesTheCourseFileExactly(String name, StegMethod method, Optional<Encryption> encryption) {
        HiddenFile png = extract("ladoLSB1.bmp", StegMethod.LSB1, Optional.empty());
        BmpImage carrier = BmpImage.parse(files.get("lado.bmp"));
        byte[] pixels = carrier.pixels();

        method.algorithm().embed(pixels, PayloadCodec.encode(png, encryption.map(Encryption::cipher)));

        assertArrayEquals(files.get(name), carrier.withPixels(pixels));
    }

    private static HiddenFile extract(String name, StegMethod method, Optional<Encryption> encryption) {
        byte[] pixels = BmpImage.parse(files.get(name)).pixels();
        return PayloadCodec.decode(method.algorithm().extract(pixels), encryption.map(Encryption::cipher));
    }
}
