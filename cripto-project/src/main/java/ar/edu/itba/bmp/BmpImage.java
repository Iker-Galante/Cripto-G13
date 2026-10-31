package ar.edu.itba.bmp;

import java.util.Arrays;

// A 24-bit uncompressed BMP split into header and pixels (row padding included).
public final class BmpImage {

    private static final int MIN_HEADER_SIZE = 54;  // 14-byte file header + 40-byte info header

    private static final int PIXEL_OFFSET_POSITION = 0x0A;
    private static final int BITS_PER_PIXEL_POSITION = 0x1C;
    private static final int COMPRESSION_POSITION = 0x1E;

    private final byte[] header;
    private final byte[] pixels;

    private BmpImage(byte[] header, byte[] pixels) {
        this.header = header;
        this.pixels = pixels;
    }

    public static BmpImage parse(byte[] file) {
        if (file.length < MIN_HEADER_SIZE || file[0] != 'B' || file[1] != 'M') {
            throw new BmpException("Not a BMP file");
        }

        int offset = readIntLE(file, PIXEL_OFFSET_POSITION);
        int bitsPerPixel = readShortLE(file, BITS_PER_PIXEL_POSITION);
        int compression = readIntLE(file, COMPRESSION_POSITION);

        if (bitsPerPixel != 24) {
            throw new BmpException("Only 24-bit BMPs are supported, got " + bitsPerPixel);
        }
        if (compression != 0) {
            throw new BmpException("Compressed BMPs are not supported");
        }
        // Read the offset instead of assuming 54: GIMP's V5 export puts pixels at 138.
        if (offset < MIN_HEADER_SIZE || offset > file.length) {
            throw new BmpException("Corrupt header: invalid pixel data offset " + offset);
        }

        return new BmpImage(Arrays.copyOfRange(file, 0, offset),
                            Arrays.copyOfRange(file, offset, file.length));
    }

    public byte[] header() {
        return header.clone();
    }

    public byte[] pixels() {
        return pixels.clone();
    }

    public byte[] toBytes() {
        byte[] file = Arrays.copyOf(header, header.length + pixels.length);
        System.arraycopy(pixels, 0, file, header.length, pixels.length);
        return file;
    }

    // Header numbers are little-endian. "& 0xFF" reads each signed byte as 0..255.
    private static int readIntLE(byte[] bytes, int position) {
        return (bytes[position] & 0xFF)
                | (bytes[position + 1] & 0xFF) << 8
                | (bytes[position + 2] & 0xFF) << 16
                | (bytes[position + 3] & 0xFF) << 24;
    }

    private static int readShortLE(byte[] bytes, int position) {
        return (bytes[position] & 0xFF)
                | (bytes[position + 1] & 0xFF) << 8;
    }
}
