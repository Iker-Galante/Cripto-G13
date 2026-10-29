package ar.edu.itba.payload;

import ar.edu.itba.StegoException;
import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.crypto.PayloadCipher;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadCodecTest {

    private static final HiddenFile SACO = HiddenFile.of("saco.txt", new byte[414]);

    @Test
    void plainPayloadIsTheSerializedFile() {
        assertArrayEquals(SACO.serialize(), PayloadCodec.encode(SACO, Optional.empty()));
    }

    @Test
    void encryptedPayloadIsPrefixedWithCipherSize() {
        // Ejemplo del enunciado: 4 + 414 + 5 = 423 bytes, que con 3DES CBC se rellenan a 424.
        PayloadCipher cipher = new PayloadCipher(CipherAlgorithm.DES3, CipherMode.CBC, "oculto");

        byte[] payload = PayloadCodec.encode(SACO, Optional.of(cipher));

        assertEquals(4 + 424, payload.length);
        assertEquals(424, ByteBuffer.wrap(payload).getInt());
    }

    @Test
    void decodesEncryptedPayloadWithTrailingBytes() {
        Optional<PayloadCipher> cipher = Optional.of(new PayloadCipher(CipherAlgorithm.AES256, CipherMode.OFB, "oculto"));
        byte[] payload = PayloadCodec.encode(SACO, cipher);
        byte[] extracted = Arrays.copyOf(payload, payload.length + 100);

        HiddenFile decoded = PayloadCodec.decode(extracted, cipher);

        assertArrayEquals(SACO.data(), decoded.data());
        assertEquals(".txt", decoded.extension());
    }

    @Test
    void rejectsCipherSizeLargerThanAvailableData() {
        Optional<PayloadCipher> cipher = Optional.of(new PayloadCipher(CipherAlgorithm.AES128, CipherMode.CBC, "oculto"));
        byte[] payload = PayloadCodec.encode(SACO, cipher);
        byte[] truncated = Arrays.copyOf(payload, payload.length - 1);

        assertThrows(StegoException.class, () -> PayloadCodec.decode(truncated, cipher));
    }
}
