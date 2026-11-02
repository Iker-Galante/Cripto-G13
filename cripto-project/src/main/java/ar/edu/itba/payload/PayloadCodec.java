package ar.edu.itba.payload;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Block layout: size (4 bytes, big-endian) || file bytes || ".ext" || 0
// Encrypted layout: cipherLength (4 bytes, big-endian) || ciphertext, where the ciphertext decrypts to the plain layout.
public final class PayloadCodec {

    private static final int SIZE_FIELD_LENGTH = 4;
    private static final int MIN_EXTENSION_LENGTH = 2;  // "." plus the NUL

    private PayloadCodec() {
    }

    public static byte[] encode(byte[] data, String extension) {
        if (!extension.startsWith(".")) {
            throw new IllegalArgumentException("Extension must start with '.', got \"" + extension + "\"");
        }
        if (!isAsciiWithoutNul(extension)) {
            throw new IllegalArgumentException("Extension must be ASCII without NUL, got \"" + extension + "\"");
        }
        if (hasPathSeparator(extension)) {
            throw new IllegalArgumentException("Extension must not contain '/' or '\\', got \"" + extension + "\"");
        }
        byte[] extensionBytes = extension.getBytes(StandardCharsets.US_ASCII);

        // ByteBuffer writes ints big-endian by default.
        return ByteBuffer.allocate(SIZE_FIELD_LENGTH + data.length + extensionBytes.length + 1)
                .putInt(data.length)
                .put(data)
                .put(extensionBytes)
                .put((byte) 0)
                .array();
    }

    public static byte[] prependLength(byte[] block) {
        return ByteBuffer.allocate(SIZE_FIELD_LENGTH + block.length)
                .putInt(block.length)
                .put(block)
                .array();
    }

    // Returns the bytes the length prefix announces; anything after them is ignored.
    // The length is checked against the block before copying, so garbage cannot ask for a huge array.
    public static byte[] readLengthPrefixed(byte[] block) {
        if (block.length < SIZE_FIELD_LENGTH) {
            throw new PayloadException("Payload too short: " + block.length + " bytes");
        }
        int length = ByteBuffer.wrap(block).getInt();
        if (length < 0 || length > block.length - SIZE_FIELD_LENGTH) {
            throw new PayloadException("Invalid cipher length " + length
                    + " for a block of " + block.length + " bytes");
        }
        return Arrays.copyOfRange(block, SIZE_FIELD_LENGTH, SIZE_FIELD_LENGTH + length);
    }

    // Bytes after the extension's NUL are ignored, so callers can pass everything extracted.
    public static Decoded decode(byte[] block) {
        if (block.length < SIZE_FIELD_LENGTH) {
            throw new PayloadException("Payload too short: " + block.length + " bytes");
        }

        int size = ByteBuffer.wrap(block).getInt();
        if (size < 0 || size > block.length - SIZE_FIELD_LENGTH - MIN_EXTENSION_LENGTH) {
            throw new PayloadException("Invalid payload size " + size
                    + " for a block of " + block.length + " bytes");
        }

        int extensionStart = SIZE_FIELD_LENGTH + size;
        int nul = indexOfZero(block, extensionStart);
        if (nul < 0) {
            throw new PayloadException("Payload extension has no terminating NUL byte");
        }
        if (block[extensionStart] != '.') {
            throw new PayloadException("Payload extension does not start with '.'");
        }

        byte[] data = Arrays.copyOfRange(block, SIZE_FIELD_LENGTH, extensionStart);
        String extension = new String(block, extensionStart, nul - extensionStart, StandardCharsets.US_ASCII);
        // The extension becomes part of the output file name, so a crafted "/../" must not get through.
        if (!isAsciiWithoutNul(extension) || hasPathSeparator(extension)) {
            throw new PayloadException("Payload extension has invalid characters");
        }
        return new Decoded(data, extension);
    }

    // Bytes above 127 decode as U+FFFD, so this also rejects them on decode.
    private static boolean isAsciiWithoutNul(String text) {
        return text.chars().allMatch(c -> c >= 1 && c <= 127);
    }

    private static boolean hasPathSeparator(String text) {
        return text.indexOf('/') >= 0 || text.indexOf('\\') >= 0;
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
