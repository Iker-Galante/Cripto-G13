package ar.edu.itba.payload;

import ar.edu.itba.StegoException;
import ar.edu.itba.crypto.PayloadCipher;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

/**
 * Arma los bytes que se esteganografían y los vuelve a interpretar al extraer.
 * <ul>
 *     <li>Sin encripción: {@code tamaño || datos || extensión}.</li>
 *     <li>Con encripción: {@code tamaño del cifrado || encripción(tamaño || datos || extensión)}.</li>
 * </ul>
 */
public final class PayloadCodec {

    /** Bytes que ocupa el tamaño al comienzo de lo oculto. */
    public static final int SIZE_BYTES = HiddenFile.SIZE_BYTES;

    private PayloadCodec() {
    }

    public static byte[] encode(HiddenFile file, Optional<PayloadCipher> cipher) {
        byte[] plain = file.serialize();
        if (cipher.isEmpty()) {
            return plain;
        }
        byte[] encrypted = cipher.get().encrypt(plain);
        return ByteBuffer.allocate(HiddenFile.SIZE_BYTES + encrypted.length)
                .putInt(encrypted.length)
                .put(encrypted)
                .array();
    }

    /** Tamaño declarado en los primeros 4 bytes extraídos (del archivo, o del cifrado si se encriptó). */
    public static long declaredSize(byte[] hidden) {
        return HiddenFile.readSize(hidden);
    }

    /** Interpreta los bytes extraídos del portador (pueden sobrar bytes al final). */
    public static HiddenFile decode(byte[] hidden, Optional<PayloadCipher> cipher) {
        if (cipher.isEmpty()) {
            return HiddenFile.parse(hidden);
        }
        long size = HiddenFile.readSize(hidden);
        long available = hidden.length - HiddenFile.SIZE_BYTES;
        if (size == 0 || size > available) {
            throw new StegoException("el tamaño del cifrado oculto (" + size + " bytes) no es válido para este portador"
                    + " (máximo " + available + " bytes)");
        }
        byte[] encrypted = Arrays.copyOfRange(hidden, HiddenFile.SIZE_BYTES, HiddenFile.SIZE_BYTES + (int) size);
        return HiddenFile.parse(cipher.get().decrypt(encrypted));
    }
}
