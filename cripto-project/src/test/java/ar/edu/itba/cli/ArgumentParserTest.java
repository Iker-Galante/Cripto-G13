package ar.edu.itba.cli;

import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.steg.StegMethod;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentParserTest {

    private static Options parse(String line) {
        return ArgumentParser.parse(line.split(" "));
    }

    private static void assertUsageError(String line) {
        assertThrows(UsageException.class, () -> parse(line));
    }

    @Test
    void parsesEmbedWithEncryption() {
        Options options = parse("-embed -in mensaje1.txt -p imagen1.bmp -out imagenmas1.bmp -steg LSBI -a 3des -m cbc -pass oculto");

        assertEquals(Command.EMBED, options.command());
        assertEquals(Optional.of("mensaje1.txt"), options.input());
        assertEquals("imagen1.bmp", options.bitmap());
        assertEquals(Optional.of("imagenmas1.bmp"), options.output());
        assertEquals(Optional.of(StegMethod.LSBI), options.steg());
        assertEquals(Optional.of(new Encryption(CipherAlgorithm.DES3, CipherMode.CBC, "oculto")), options.encryption());
        assertTrue(options.warnings().isEmpty());
    }

    @Test
    void parsesExtractWithoutEncryption() {
        Options options = parse("-extract -p imagenmas1.bmp -out mensaje1 -steg LSB4");

        assertEquals(Command.EXTRACT, options.command());
        assertEquals(Optional.empty(), options.input());
        assertEquals(Optional.of(StegMethod.LSB4), options.steg());
        assertEquals(Optional.empty(), options.encryption());
    }

    @Test
    void acceptsLongDashesCopiedFromTheAssignment() {
        Options options = parse("–extract –p imagenmas1.bmp -out mensaje1 –steg LSBI –a 3des –m cbc -pass oculto");

        assertEquals(Command.EXTRACT, options.command());
        assertEquals(Optional.of(new Encryption(CipherAlgorithm.DES3, CipherMode.CBC, "oculto")), options.encryption());
    }

    @Test
    void passwordOnlyDefaultsToAes128Cbc() {
        Options options = parse("-extract -p a.bmp -out b -steg LSB1 -pass secreto");

        assertEquals(Optional.of(new Encryption(CipherAlgorithm.AES128, CipherMode.CBC, "secreto")), options.encryption());
    }

    @Test
    void algorithmAndPasswordDefaultsToCbc() {
        Options options = parse("-extract -p a.bmp -out b -steg LSB1 -a aes256 -pass secreto");

        assertEquals(CipherMode.CBC, options.encryption().orElseThrow().mode());
    }

    @Test
    void modeAndPasswordDefaultsToAes128() {
        Options options = parse("-extract -p a.bmp -out b -steg LSB1 -m ofb -pass secreto");

        assertEquals(CipherAlgorithm.AES128, options.encryption().orElseThrow().algorithm());
    }

    @Test
    void algorithmWithoutPasswordOnlyWarns() {
        Options options = parse("-embed -in a.txt -p a.bmp -out b.bmp -steg LSB1 -a aes128 -m ecb");

        assertEquals(Optional.empty(), options.encryption());
        assertFalse(options.warnings().isEmpty());
    }

    @Test
    void analyzeOnlyRequiresTheBitmap() {
        Options options = parse("-analyze -p a.bmp");

        assertEquals(Command.ANALYZE, options.command());
        assertEquals(Optional.empty(), options.steg());
        assertEquals(Optional.empty(), options.output());
        assertEquals(Optional.empty(), options.encryption());
    }

    @Test
    void analyzeAcceptsMethodPrefixAndEncryption() {
        Options options = parse("-analyze -p a.bmp -steg LSBI -out encontrado -a aes256 -m ofb -pass margarita");

        assertEquals(Optional.of(StegMethod.LSBI), options.steg());
        assertEquals(Optional.of("encontrado"), options.output());
        assertEquals(Optional.of(new Encryption(CipherAlgorithm.AES256, CipherMode.OFB, "margarita")), options.encryption());
    }

    @Test
    void rejectsInvalidInput() {
        assertUsageError("-embed -in a.txt -p a.bmp -out b.bmp");
        assertUsageError("-embed -in a.txt -p a.bmp -out b.bmp -steg LSB2");
        assertUsageError("-embed -in a.txt -p a.bmp -out b.bmp -steg LSB1 -a des");
        assertUsageError("-embed -in a.txt -p a.bmp -out b.bmp -steg LSB1 -m ctr -pass x");
        assertUsageError("-embed -in a.txt -p a.bmp -out b.bmp -steg LSB1 -p c.bmp");
        assertUsageError("-embed -extract -p a.bmp -out b -steg LSB1");
        assertUsageError("-extract -in a.txt -p a.bmp -out b -steg LSB1");
        assertUsageError("-extract -p a.bmp -out -steg LSB1");
        assertUsageError("-extract -p a.bmp -out b -steg LSB1 -pass");
        assertUsageError("-p a.bmp -out b -steg LSB1");
        assertUsageError("-EMBED -in a.txt -p a.bmp -out b.bmp -steg LSB1");
        assertUsageError("-extract -p a.bmp -out b -steg LSB1 -verbose");
        assertUsageError("-analyze -out encontrado");
        assertUsageError("-analyze -in a.txt -p a.bmp");
        assertUsageError("-analyze -extract -p a.bmp -out b -steg LSB1");
        assertThrows(UsageException.class, () -> ArgumentParser.parse(new String[0]));
    }
}
