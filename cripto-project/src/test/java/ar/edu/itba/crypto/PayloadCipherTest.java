package ar.edu.itba.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.BadPaddingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Vectors from OpenSSL 4.0.3 (password "oculto") and from the course's samples (password "margarita").
class PayloadCipherTest {

    private static final HexFormat HEX = HexFormat.of();
    private static final byte[] PLAINTEXT = "hola mundo stegobmp".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({
        "AES128, E5FB03E4ABD3D239CBB4F0AAA3572401, 20EFCD7A8C7D9986B0BF3089EFF2D9B7",
        "AES192, E5FB03E4ABD3D239CBB4F0AAA357240120EFCD7A8C7D9986, B0BF3089EFF2D9B74D908F992BE5FDDF",
        "AES256, E5FB03E4ABD3D239CBB4F0AAA357240120EFCD7A8C7D9986B0BF3089EFF2D9B7, 4D908F992BE5FDDF985C7B0C567655B6",
        "TRIPLE_DES, E5FB03E4ABD3D239CBB4F0AAA357240120EFCD7A8C7D9986, B0BF3089EFF2D9B7",
    })
    void derivesOpensslKeyAndIv(CipherAlgorithm algorithm, String key, String iv) {
        byte[] keyAndIv = PayloadCipher.deriveKeyAndIv(new CipherSpec(algorithm, CipherMode.CBC, "oculto"));

        assertEquals((key + iv).toLowerCase(), HEX.formatHex(keyAndIv));
    }

    @Test
    void modeDoesNotChangeTheDerivation() {
        assertArrayEquals(PayloadCipher.deriveKeyAndIv(spec(CipherAlgorithm.AES128, CipherMode.CBC, "oculto")),
                PayloadCipher.deriveKeyAndIv(spec(CipherAlgorithm.AES128, CipherMode.ECB, "oculto")));
    }

    @ParameterizedTest
    @CsvSource({
        "AES128, 32, b25f8d99f3173ec0b52849f459a4c20d",
        "AES256, 48, 212420edc583a686a94d19a3497363a2",
        "TRIPLE_DES, 32, b52849f459a4c20d",
    })
    void derivesCatedraKeyAndIv(CipherAlgorithm algorithm, int length, String iv) {
        String stream = "03db0a157acfe8de523760aa731d8122b25f8d99f3173ec0b52849f459a4c20d"
                + "212420edc583a686a94d19a3497363a2219d404e53549976f67501b4b0e60c57";

        byte[] keyAndIv = PayloadCipher.deriveKeyAndIv(spec(algorithm, CipherMode.CBC, "margarita"));

        assertEquals(stream.substring(0, length * 2), HEX.formatHex(keyAndIv));
        assertEquals(iv, HEX.formatHex(keyAndIv, algorithm.keyBytes(), keyAndIv.length));
    }

    @ParameterizedTest
    @CsvSource({
        "AES128, CBC, 1d63e164284fe24f11374a5b1fe7f1ea9b19a2503f62dec7fae35c8685b7a052",
        "AES128, ECB, f2de484dd1e5ea7c53d7c5a41d90e89710d97d6b40a4fcf920d97a23a231b1aa",
        "AES192, OFB, 6bf37ba4c9c9a4305c5cf1a9d21f972dc88ba2",
        "AES256, CFB, 4bdf4577b8f90205ef71401f2294c55a3be48d",
        "TRIPLE_DES, CBC, 7ea823d660e04344e92d860b2b8bb801619cba062803c45f",
        "TRIPLE_DES, CFB, 7cd0a4e1a062331b3d1a0c9317392ac76b08e4",
        "TRIPLE_DES, OFB, 7c0c7a391a44484a9385d01573ef0a6998a2bd",
    })
    void encryptsLikeOpenssl(CipherAlgorithm algorithm, CipherMode mode, String ciphertext) {
        byte[] encrypted = new PayloadCipher(spec(algorithm, mode, "oculto")).encrypt(PLAINTEXT);

        assertEquals(ciphertext, HEX.formatHex(encrypted));
    }

    @Test
    void allSixteenPairsRoundTrip() {
        for (CipherAlgorithm algorithm : CipherAlgorithm.values()) {
            for (CipherMode mode : CipherMode.values()) {
                PayloadCipher cipher = new PayloadCipher(spec(algorithm, mode, "secret"));
                String pair = algorithm + " " + mode;

                byte[] encrypted = cipher.encrypt(PLAINTEXT);
                assertArrayEquals(PLAINTEXT, cipher.decrypt(encrypted), pair);
                assertArrayEquals(new byte[0], cipher.decrypt(cipher.encrypt(new byte[0])), pair);
                assertEquals(expectedLength(algorithm, mode, PLAINTEXT.length), encrypted.length, pair);
            }
        }
    }

    @Test
    void wrongPasswordWithPaddingThrows() {
        byte[] encrypted = new PayloadCipher(spec(CipherAlgorithm.AES128, CipherMode.CBC, "right")).encrypt(PLAINTEXT);
        PayloadCipher wrong = new PayloadCipher(spec(CipherAlgorithm.AES128, CipherMode.CBC, "wrong"));

        CryptoException e = assertThrows(CryptoException.class, () -> wrong.decrypt(encrypted));
        assertEquals("Could not decrypt: wrong password, algorithm or mode", e.getMessage());
        assertInstanceOf(BadPaddingException.class, e.getCause());
    }

    @Test
    void wrongPasswordWithStreamModeGivesGarbage() {
        // No padding to check, so nothing fails here. Main reports it when the garbage does not decode.
        byte[] encrypted = new PayloadCipher(spec(CipherAlgorithm.AES128, CipherMode.OFB, "right")).encrypt(PLAINTEXT);

        byte[] decrypted = new PayloadCipher(spec(CipherAlgorithm.AES128, CipherMode.OFB, "wrong")).decrypt(encrypted);

        assertEquals(PLAINTEXT.length, decrypted.length);
        assertFalse(Arrays.equals(PLAINTEXT, decrypted));
    }

    // ECB and CBC pad to the next full block (16 bytes for AES, 8 for 3DES); CFB and OFB keep the length.
    private static int expectedLength(CipherAlgorithm algorithm, CipherMode mode, int plainLength) {
        if (mode == CipherMode.CFB || mode == CipherMode.OFB) {
            return plainLength;
        }
        int block = algorithm == CipherAlgorithm.TRIPLE_DES ? 8 : 16;
        return (plainLength / block + 1) * block;
    }

    private static CipherSpec spec(CipherAlgorithm algorithm, CipherMode mode, String password) {
        return new CipherSpec(algorithm, mode, password);
    }
}
