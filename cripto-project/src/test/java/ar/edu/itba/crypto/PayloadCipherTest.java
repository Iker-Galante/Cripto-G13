package ar.edu.itba.crypto;

import ar.edu.itba.StegoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadCipherTest {

    private static final HexFormat HEX = HexFormat.of();
    private static final String PASSWORD = "margarita";
    private static final byte[] PLAIN = "Criptografia y Seguridad 72.04".getBytes(StandardCharsets.US_ASCII);

    /** Valores del README de los archivos de ejemplo de la cátedra. */
    @ParameterizedTest
    @CsvSource({
            "AES128, 03db0a157acfe8de523760aa731d8122, b25f8d99f3173ec0b52849f459a4c20d",
            "AES256, 03db0a157acfe8de523760aa731d8122b25f8d99f3173ec0b52849f459a4c20d, 212420edc583a686a94d19a3497363a2",
            "DES3, 03db0a157acfe8de523760aa731d8122b25f8d99f3173ec0, b52849f459a4c20d",
    })
    void derivesKeyAndIvWithPbkdf2(CipherAlgorithm algorithm, String key, String iv) {
        PayloadCipher cipher = new PayloadCipher(algorithm, CipherMode.CBC, PASSWORD);

        assertEquals(key, HEX.formatHex(cipher.key()));
        assertEquals(iv, HEX.formatHex(cipher.iv()));
    }

    /** Generados con openssl enc usando la misma clave e IV. */
    @ParameterizedTest
    @CsvSource({
            "AES192, ECB, 95c0e9118e87d74b5c0b9ae5af31cb033a3631abfa9487ab55610bcffb4d152f",
            "AES256, CFB, 2bef19e236c838b7036c8c994bc8823c0fcc64e8f0d24c2d300e266898eb",
            "AES128, OFB, 754d6a864303c9db98e0f6285a816aa9c71cd6892eba8d4ca1c48e6f94f0",
            "DES3, OFB, 683c30132f88eed6ea55d7a30b580598b82bbfa05ba15e292ee4ffc2d8d8",
            "DES3, CBC, a20129f4e97277ab7212c8cf23ce86e876834327f58143c5fac63c9f01af22a2",
    })
    void matchesOpenssl(CipherAlgorithm algorithm, CipherMode mode, String expected) {
        PayloadCipher cipher = new PayloadCipher(algorithm, mode, PASSWORD);

        assertEquals(expected, HEX.formatHex(cipher.encrypt(PLAIN)));
    }

    @Test
    void roundTripForEveryAlgorithmAndMode() {
        byte[] plain = new byte[423];
        new Random(7).nextBytes(plain);
        for (CipherAlgorithm algorithm : CipherAlgorithm.values()) {
            for (CipherMode mode : CipherMode.values()) {
                PayloadCipher cipher = new PayloadCipher(algorithm, mode, "oculto");
                byte[] encrypted = cipher.encrypt(plain);

                int block = algorithm.ivLength();
                int expectedLength = mode.padding().equals("NoPadding") ? 423 : (423 / block + 1) * block;
                assertEquals(expectedLength, encrypted.length, algorithm + " " + mode);
                assertArrayEquals(plain, cipher.decrypt(encrypted), algorithm + " " + mode);
            }
        }
    }

    @Test
    void wrongPasswordFailsWithReadableError() {
        byte[] encrypted = new PayloadCipher(CipherAlgorithm.AES128, CipherMode.CBC, "oculto").encrypt(PLAIN);
        PayloadCipher wrong = new PayloadCipher(CipherAlgorithm.AES128, CipherMode.CBC, "otra");

        assertThrows(StegoException.class, () -> wrong.decrypt(encrypted));
    }
}
