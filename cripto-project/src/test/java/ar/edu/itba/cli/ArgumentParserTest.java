package ar.edu.itba.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.cli.Arguments.Command;
import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.crypto.CipherSpec;
import ar.edu.itba.steg.StegMethod;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArgumentParserTest {

    @Test
    void parsesEmbed() {
        Arguments arguments = parse("-embed -in hola.txt -p carrier.bmp -out out.bmp -steg LSB1");

        assertEquals(Command.EMBED, arguments.command());
        assertEquals(Path.of("hola.txt"), arguments.inFile());
        assertEquals(Path.of("carrier.bmp"), arguments.carrier());
        assertEquals(Path.of("out.bmp"), arguments.outFile());
        assertEquals(StegMethod.LSB1, arguments.stegMethod());
    }

    @Test
    void parsesExtract() {
        Arguments arguments = parse("-extract -p out.bmp -out back -steg LSB1");

        assertEquals(Command.EXTRACT, arguments.command());
        assertNull(arguments.inFile());
        assertEquals(Path.of("out.bmp"), arguments.carrier());
        assertEquals(Path.of("back"), arguments.outFile());
    }

    @Test
    void acceptsFlagsInAnyOrder() {
        Arguments arguments = parse("-steg LSB1 -out out.bmp -p carrier.bmp -in hola.txt -embed");

        assertEquals(Command.EMBED, arguments.command());
        assertEquals(Path.of("hola.txt"), arguments.inFile());
    }

    @Test
    void embedWithoutInFails() {
        assertRejected("-embed -p carrier.bmp -out out.bmp -steg LSB1", "Missing option -in");
    }

    @Test
    void missingCommandFails() {
        assertRejected("-p carrier.bmp -out out.bmp -steg LSB1", "Missing -embed, -extract or -analyze");
    }

    @Test
    void missingValueFails() {
        assertRejected("-extract -p -out back -steg LSB1", "Missing value for -p");
    }

    @Test
    void unknownFlagFails() {
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -verbose", "Unknown option: -verbose");
    }

    @Test
    void noPassMeansNoEncryption() {
        assertNull(parse("-extract -p out.bmp -out back -steg LSB1").cipher());
        assertNull(parse("-extract -p out.bmp -out back -steg LSB1 -a aes256 -m ofb").cipher());
    }

    @Test
    void passAloneMeansAes128Cbc() {
        assertEquals(new CipherSpec(CipherAlgorithm.AES128, CipherMode.CBC, "secreto"),
                parse("-extract -p out.bmp -out back -steg LSB1 -pass secreto").cipher());
    }

    @Test
    void passWithAlgorithmMeansCbc() {
        assertEquals(new CipherSpec(CipherAlgorithm.TRIPLE_DES, CipherMode.CBC, "secreto"),
                parse("-extract -p out.bmp -out back -steg LSB1 -a 3des -pass secreto").cipher());
    }

    @Test
    void passWithModeMeansAes128() {
        assertEquals(new CipherSpec(CipherAlgorithm.AES128, CipherMode.OFB, "secreto"),
                parse("-extract -p out.bmp -out back -steg LSB1 -m ofb -pass secreto").cipher());
    }

    @Test
    void allThreeCipherOptionsAreUsedAsGiven() {
        assertEquals(new CipherSpec(CipherAlgorithm.AES192, CipherMode.ECB, "secreto"),
                parse("-extract -p out.bmp -out back -steg LSB1 -pass secreto -m ecb -a aes192").cipher());
    }

    @Test
    void invalidAlgorithmFails() {
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -a aes -pass x", "Invalid encryption algorithm: aes");
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -a AES128 -pass x", "Invalid encryption algorithm: AES128");
    }

    @Test
    void invalidModeFails() {
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -m ctr -pass x", "Invalid encryption mode: ctr");
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -m CBC -pass x", "Invalid encryption mode: CBC");
    }

    @Test
    void invalidAlgorithmFailsEvenWithoutPass() {
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -a aes", "Invalid encryption algorithm: aes");
    }

    @Test
    void emptyPasswordFails() {
        String[] args = {"-extract", "-p", "out.bmp", "-out", "back", "-steg", "LSB1", "-pass", ""};

        UsageException e = assertThrows(UsageException.class, () -> ArgumentParser.parse(args));
        assertEquals("Password must not be empty", e.getMessage());
    }

    @Test
    void parsesTheStatementExamples() {
        // Statement section 5, Example 1 (with encryption) and Example 2 (without).
        Arguments example1 = parse("-embed -in mensaje1.txt -p imagen1.bmp -out imagenmas1.bmp -steg LSBI -a 3des -m cbc -pass oculto");
        Arguments example2 = parse("-embed -in mensaje1.txt -p imagen1.bmp -out imagenmas1.bmp -steg LSBI");

        assertEquals(new CipherSpec(CipherAlgorithm.TRIPLE_DES, CipherMode.CBC, "oculto"), example1.cipher());
        assertEquals(StegMethod.LSBI, example1.stegMethod());
        assertNull(example2.cipher());
        assertEquals(Path.of("imagenmas1.bmp"), example2.outFile());
    }

    @Test
    void acceptsLsb4ForEmbedAndExtract() {
        assertEquals(StegMethod.LSB4, parse("-embed -in hola.txt -p carrier.bmp -out out.bmp -steg LSB4").stegMethod());
        assertEquals(StegMethod.LSB4, parse("-extract -p out.bmp -out back -steg LSB4").stegMethod());
    }

    @Test
    void acceptsLsbiForEmbedAndExtract() {
        assertEquals(StegMethod.LSBI, parse("-embed -in hola.txt -p carrier.bmp -out out.bmp -steg LSBI").stegMethod());
        assertEquals(StegMethod.LSBI, parse("-extract -p out.bmp -out back -steg LSBI").stegMethod());
    }

    @Test
    void unknownMethodFails() {
        assertRejected("-extract -p out.bmp -out back -steg LSB2", "Invalid steganography method: LSB2");
    }

    @Test
    void flagsAreCaseSensitive() {
        assertRejected("-extract -p out.bmp -out back -STEG LSB1", "Unknown option: -STEG");
    }

    @Test
    void methodIsCaseSensitive() {
        assertRejected("-extract -p out.bmp -out back -steg lsb1", "Invalid steganography method: lsb1");
        assertRejected("-extract -p out.bmp -out back -steg lsb4", "Invalid steganography method: lsb4");
        assertRejected("-extract -p out.bmp -out back -steg lsbi", "Invalid steganography method: lsbi");
    }

    @Test
    void bothCommandsFail() {
        assertRejected("-embed -extract -in hola.txt -p carrier.bmp -out out.bmp -steg LSB1",
                "Give only one of -embed, -extract or -analyze");
        assertRejected("-extract -analyze -p out.bmp -out back -steg LSB1",
                "Give only one of -embed, -extract or -analyze");
    }

    @Test
    void repeatedFlagFails() {
        assertRejected("-extract -p a.bmp -p b.bmp -out back -steg LSB1", "Repeated option: -p");
    }

    @Test
    void inWithExtractFails() {
        assertRejected("-extract -in hola.txt -p out.bmp -out back -steg LSB1",
                "Option -in is not used with -extract");
    }

    @Test
    void analyzeNeedsOnlyTheCarrier() {
        Arguments arguments = parse("-analyze -p suspect.bmp");

        assertEquals(Command.ANALYZE, arguments.command());
        assertEquals(Path.of("suspect.bmp"), arguments.carrier());
        assertNull(arguments.outFile());
        assertNull(arguments.stegMethod());
        assertNull(arguments.cipher());
    }

    @Test
    void analyzeAcceptsMethodPrefixAndCipher() {
        Arguments arguments = parse("-analyze -p suspect.bmp -steg LSB4 -out found -a aes256 -m ecb -pass gloria");

        assertEquals(StegMethod.LSB4, arguments.stegMethod());
        assertEquals(Path.of("found"), arguments.outFile());
        assertEquals(new CipherSpec(CipherAlgorithm.AES256, CipherMode.ECB, "gloria"), arguments.cipher());
    }

    @Test
    void analyzeWithoutCarrierFails() {
        assertRejected("-analyze -out found", "Missing option -p");
    }

    @Test
    void inWithAnalyzeFails() {
        assertRejected("-analyze -in hola.txt -p suspect.bmp", "Option -in is not used with -analyze");
    }

    @Test
    void analyzeChecksTheMethod() {
        assertRejected("-analyze -p suspect.bmp -steg LSB2", "Invalid steganography method: LSB2");
    }

    private static Arguments parse(String commandLine) {
        return ArgumentParser.parse(commandLine.split(" "));
    }

    private static void assertRejected(String commandLine, String expectedMessage) {
        UsageException e = assertThrows(UsageException.class, () -> parse(commandLine));
        assertEquals(expectedMessage, e.getMessage());
    }
}
