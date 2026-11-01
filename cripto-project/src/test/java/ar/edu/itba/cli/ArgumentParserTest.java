package ar.edu.itba.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.cli.Arguments.Command;
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
        assertEquals("LSB1", arguments.stegMethod());
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
        assertRejected("-p carrier.bmp -out out.bmp -steg LSB1", "Missing -embed or -extract");
    }

    @Test
    void missingValueFails() {
        assertRejected("-extract -p -out back -steg LSB1", "Missing value for -p");
    }

    @Test
    void passIsUnknownForNow() {
        assertRejected("-extract -p out.bmp -out back -steg LSB1 -pass secreto", "Unknown option: -pass");
    }

    @Test
    void lsb4IsNotImplementedYet() {
        assertRejected("-extract -p out.bmp -out back -steg LSB4", "Method LSB4 is not implemented yet");
    }

    @Test
    void flagsAreCaseSensitive() {
        assertRejected("-extract -p out.bmp -out back -STEG LSB1", "Unknown option: -STEG");
    }

    @Test
    void methodIsCaseSensitive() {
        assertRejected("-extract -p out.bmp -out back -steg lsb1", "Invalid steganography method: lsb1");
    }

    private static Arguments parse(String commandLine) {
        return ArgumentParser.parse(commandLine.split(" "));
    }

    private static void assertRejected(String commandLine, String expectedMessage) {
        UsageException e = assertThrows(UsageException.class, () -> parse(commandLine));
        assertEquals(expectedMessage, e.getMessage());
    }
}
