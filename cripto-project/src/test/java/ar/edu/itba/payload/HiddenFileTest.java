package ar.edu.itba.payload;

import ar.edu.itba.StegoException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HiddenFileTest {

    private static final HexFormat HEX = HexFormat.of();

    @Test
    void serializesSizeDataAndExtension() {
        HiddenFile file = HiddenFile.of("saco.txt", new byte[]{1, 2, 3});

        assertEquals("00000003010203" + "2e74787400", HEX.formatHex(file.serialize()));
    }

    @Test
    void takesExtensionFromLastDot() {
        assertEquals(".gz", HiddenFile.of("backup.tar.gz", new byte[0]).extension());
        assertEquals("", HiddenFile.of("README", new byte[0]).extension());
        assertEquals("", HiddenFile.of(".bashrc", new byte[0]).extension());
    }

    @Test
    void fileWithoutExtensionIsStoredAsDot() {
        HiddenFile file = HiddenFile.of("README", new byte[]{9});

        assertEquals("00000001092e00", HEX.formatHex(file.serialize()));
        assertEquals("", HiddenFile.parse(file.serialize()).extension());
        assertEquals("salida", HiddenFile.parse(file.serialize()).fileName("salida"));
    }

    @Test
    void parseIgnoresTrailingBytes() {
        byte[] serialized = HiddenFile.of("foto.png", new byte[]{4, 5}).serialize();
        byte[] extracted = Arrays.copyOf(serialized, serialized.length + 20);

        HiddenFile parsed = HiddenFile.parse(extracted);

        assertArrayEquals(new byte[]{4, 5}, parsed.data());
        assertEquals(".png", parsed.extension());
        assertEquals("salida.png", parsed.fileName("salida"));
    }

    @Test
    void rejectsSizeLargerThanAvailableData() {
        assertThrows(StegoException.class, () -> HiddenFile.parse(HEX.parseHex("000000ff0102")));
    }

    @Test
    void rejectsMissingTerminator() {
        assertThrows(StegoException.class, () -> HiddenFile.parse(HEX.parseHex("00000001012e747874")));
    }

    @Test
    void rejectsExtensionWithoutDot() {
        assertThrows(StegoException.class, () -> HiddenFile.parse(HEX.parseHex("000000010174787400")));
    }

    @Test
    void rejectsExtensionWithPathSeparator() {
        assertThrows(StegoException.class, () -> HiddenFile.parse(HEX.parseHex("00000001012e2f6100")));
    }
}
