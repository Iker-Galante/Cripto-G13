package ar.edu.itba.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CipherOptionsTest {

    @Test
    void acceptsTheFourAlgorithms() {
        assertEquals(CipherAlgorithm.AES128, CipherAlgorithm.fromOption("aes128"));
        assertEquals(CipherAlgorithm.AES192, CipherAlgorithm.fromOption("aes192"));
        assertEquals(CipherAlgorithm.AES256, CipherAlgorithm.fromOption("aes256"));
        assertEquals(CipherAlgorithm.TRIPLE_DES, CipherAlgorithm.fromOption("3des"));
    }

    @Test
    void acceptsTheFourModes() {
        assertEquals(CipherMode.ECB, CipherMode.fromOption("ecb"));
        assertEquals(CipherMode.CFB, CipherMode.fromOption("cfb"));
        assertEquals(CipherMode.OFB, CipherMode.fromOption("ofb"));
        assertEquals(CipherMode.CBC, CipherMode.fromOption("cbc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AES128", "des", "aes", "TRIPLE_DES"})
    void rejectsOtherAlgorithms(String option) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CipherAlgorithm.fromOption(option));
        assertEquals("Invalid encryption algorithm: " + option, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CBC", "ctr", "cfb8"})
    void rejectsOtherModes(String option) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CipherMode.fromOption(option));
        assertEquals("Invalid encryption mode: " + option, e.getMessage());
    }
}
