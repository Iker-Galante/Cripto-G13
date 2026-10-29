package ar.edu.itba.payload;

import ar.edu.itba.StegoException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Archivo a ocultar. Se serializa como {@code tamaño (4 bytes big endian) || datos || extensión || '\0'},
 * donde la extensión empieza con '.'. Un archivo sin extensión se guarda como ".\0".
 *
 * @param data      contenido del archivo
 * @param extension extensión con el punto (".png"), o vacía si el archivo no tiene
 */
public record HiddenFile(byte[] data, String extension) {

    static final int SIZE_BYTES = 4;

    public static HiddenFile of(String fileName, byte[] data) {
        int dot = fileName.lastIndexOf('.');
        return new HiddenFile(data, dot > 0 ? fileName.substring(dot) : "");
    }

    /** Nombre del archivo a escribir al extraer: el nombre base con la extensión recuperada. */
    public String fileName(String base) {
        return base + extension;
    }

    public byte[] serialize() {
        byte[] ext = (extension.isEmpty() ? "." : extension).getBytes(StandardCharsets.ISO_8859_1);
        return ByteBuffer.allocate(SIZE_BYTES + data.length + ext.length + 1)
                .putInt(data.length)
                .put(data)
                .put(ext)
                .put((byte) 0)
                .array();
    }

    public static HiddenFile parse(byte[] bytes) {
        long size = readSize(bytes);
        long available = bytes.length - SIZE_BYTES;
        if (size > available) {
            throw new StegoException("el tamaño oculto (" + size + " bytes) supera lo que puede contener el portador ("
                    + available + " bytes)");
        }
        int extensionStart = SIZE_BYTES + (int) size;
        int extensionEnd = indexOfZero(bytes, extensionStart);
        if (extensionEnd < 0) {
            throw new StegoException("no se encontró el '\\0' que termina la extensión del archivo oculto");
        }
        String extension = new String(bytes, extensionStart, extensionEnd - extensionStart, StandardCharsets.ISO_8859_1);
        if (!isValidExtension(extension)) {
            throw new StegoException("la extensión del archivo oculto no es válida");
        }
        byte[] data = Arrays.copyOfRange(bytes, SIZE_BYTES, extensionStart);
        return new HiddenFile(data, extension.equals(".") ? "" : extension);
    }

    /** Lee el tamaño (4 bytes big endian, sin signo) del comienzo de {@code bytes}. */
    static long readSize(byte[] bytes) {
        if (bytes.length < SIZE_BYTES) {
            throw new StegoException("el portador no tiene capacidad ni para el tamaño del archivo oculto");
        }
        return Integer.toUnsignedLong(ByteBuffer.wrap(bytes).getInt());
    }

    /** Una extensión válida empieza con '.' y sólo tiene caracteres ASCII imprimibles que no son separadores de ruta. */
    static boolean isValidExtension(String extension) {
        if (!extension.startsWith(".")) {
            return false;
        }
        return extension.chars().allMatch(c -> c > ' ' && c < 0x7F && c != '/' && c != '\\');
    }

    private static int indexOfZero(byte[] bytes, int from) {
        for (int i = from; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                return i;
            }
        }
        return -1;
    }
}
