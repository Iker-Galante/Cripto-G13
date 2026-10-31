package ar.edu.itba.bmp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// BMP files built in memory, so the repo needs no binary fixtures.
final class TestBmps {

    private static final int WIDTH = 6;
    private static final int HEIGHT = 5;
    private static final int ROW_SIZE = 20;          // 18 bytes of pixels, padded to a multiple of 4
    private static final int PIXEL_DATA_SIZE = 100;  // 5 rows * 20 bytes
    private static final int V3_HEADER_SIZE = 54;

    // BMP stores colors as blue, green, red.
    private static final byte[] BLACK = {0x00, 0x00, 0x00};
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] BLUE = {(byte) 0xFF, 0x00, 0x00};
    private static final byte[] GREEN = {0x00, (byte) 0xFF, 0x00};

    private TestBmps() {
    }

    // Nick's 6x5 GIMP export, byte for byte.
    static byte[] sixByFive() {
        return withOffset(V3_HEADER_SIZE);
    }

    // Same pixels with the header zero-padded to offset bytes; 138 mimics GIMP's V5 export.
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
        file.putInt(2835).putInt(2835);          // 72 DPI in pixels per meter
        file.putInt(0).putInt(0);                // palette colors, important colors

        // Rows are stored bottom-up.
        file.position(offset);
        for (byte[] color : new byte[][] {BLACK, WHITE, BLUE, WHITE, GREEN}) {
            for (int x = 0; x < WIDTH; x++) {
                file.put(color);
            }
            file.put(new byte[ROW_SIZE - WIDTH * 3]);
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
