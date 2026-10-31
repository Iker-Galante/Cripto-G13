package ar.edu.itba.bmp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BMP files built in memory for tests, so no binary fixtures are needed in the repo.
 */
final class TestBmps {

    static final int WIDTH = 6;
    static final int HEIGHT = 5;
    static final int ROW_SIZE = 20;          // 6 pixels * 3 bytes = 18, padded to a multiple of 4
    static final int PIXEL_DATA_SIZE = 100;  // 5 rows * 20 bytes
    static final int V3_HEADER_SIZE = 54;

    // Colors in BMP byte order: blue, green, red.
    private static final byte[] BLACK = {0x00, 0x00, 0x00};
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] BLUE = {(byte) 0xFF, 0x00, 0x00};
    private static final byte[] GREEN = {0x00, (byte) 0xFF, 0x00};

    private TestBmps() {
    }

    /**
     * The 6x5 image from Nick's GIMP export, byte for byte: a V3 header (file size 154, offset
     * 54) and five rows stored bottom-up: black, white, blue, white, green.
     */
    static byte[] sixByFive() {
        return withOffset(V3_HEADER_SIZE);
    }

    /**
     * The same pixels, with the header padded with zeros up to {@code offset} bytes and the
     * offset and file size fields updated to match. 138 mimics GIMP's V5 export.
     */
    static byte[] withOffset(int offset) {
        ByteBuffer file = ByteBuffer.allocate(offset + PIXEL_DATA_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        // File header (14 bytes)
        file.put((byte) 'B').put((byte) 'M');
        file.putInt(offset + PIXEL_DATA_SIZE);  // total file size
        file.putInt(0);                          // reserved
        file.putInt(offset);                     // pixel data offset

        // Info header (40 bytes, V3)
        file.putInt(40);                         // info header size
        file.putInt(WIDTH);
        file.putInt(HEIGHT);
        file.putShort((short) 1);                // color planes
        file.putShort((short) 24);               // bits per pixel
        file.putInt(0);                          // compression: none
        file.putInt(PIXEL_DATA_SIZE);
        file.putInt(2835).putInt(2835);          // resolution, 72 DPI in pixels per meter
        file.putInt(0).putInt(0);                // palette colors, important colors

        file.position(offset);
        for (byte[] color : new byte[][] {BLACK, WHITE, BLUE, WHITE, GREEN}) {
            for (int x = 0; x < WIDTH; x++) {
                file.put(color);
            }
            file.put(new byte[ROW_SIZE - WIDTH * 3]);  // row padding
        }
        return file.array();
    }

    static byte[] withBitsPerPixel(int bitsPerPixel) {
        byte[] file = sixByFive();
        ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).putShort(0x1C, (short) bitsPerPixel);
        return file;
    }

    static byte[] withCompression(int compression) {
        byte[] file = sixByFive();
        ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).putInt(0x1E, compression);
        return file;
    }
}
