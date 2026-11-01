package ar.edu.itba;

import ar.edu.itba.bmp.BmpException;
import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.ArgumentParser;
import ar.edu.itba.cli.Arguments;
import ar.edu.itba.cli.Arguments.Command;
import ar.edu.itba.cli.UsageException;
import ar.edu.itba.payload.Decoded;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.payload.PayloadException;
import ar.edu.itba.steg.Lsb1Steganographer;
import ar.edu.itba.steg.Steganographer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class Main {

    static final String USAGE = """
            Usage:
              stegobmp -embed -in <file> -p <bitmap> -out <bitmap> -steg LSB1
              stegobmp -extract -p <bitmap> -out <file> -steg LSB1""";

    private Main() {
    }

    public static void main(String[] args) {
        int status = run(args, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    // Returns the exit status; errors go to err as one line, never as a stack trace.
    static int run(String[] args, PrintStream err) {
        try {
            Arguments arguments = ArgumentParser.parse(args);
            if (arguments.command() == Command.EMBED) {
                embed(arguments);
            } else {
                extract(arguments);
            }
            return 0;
        } catch (UsageException e) {
            err.println("Error: " + e.getMessage());
            err.println(USAGE);
        } catch (NoSuchFileException e) {
            err.println("Error: File not found: " + e.getFile());
        } catch (IOException e) {
            err.println("Error: Cannot read or write " + e.getMessage());
        } catch (BmpException | IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
        } catch (PayloadException e) {
            err.println("Error: No hidden data found in the carrier (or wrong -steg method)");
        }
        return 1;
    }

    private static void embed(Arguments arguments) throws IOException {
        String extension = extensionOf(arguments.inFile());
        BmpImage carrier = BmpImage.parse(Files.readAllBytes(arguments.carrier()));
        byte[] payload = PayloadCodec.encode(Files.readAllBytes(arguments.inFile()), extension);

        byte[] pixels = carrier.pixels();
        steganographer().embed(pixels, payload);

        try (OutputStream out = Files.newOutputStream(arguments.outFile())) {
            out.write(carrier.header());
            out.write(pixels);
        }
    }

    private static void extract(Arguments arguments) throws IOException {
        BmpImage carrier = BmpImage.parse(Files.readAllBytes(arguments.carrier()));
        Decoded decoded = PayloadCodec.decode(steganographer().extract(carrier.pixels()));

        // -out is a name without extension; the extension comes from the hidden payload.
        Files.write(Path.of(arguments.outFile() + decoded.extension()), decoded.data());
    }

    // Only LSB1 exists so far; ArgumentParser rejects every other method.
    private static Steganographer steganographer() {
        return new Lsb1Steganographer();
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            throw new IllegalArgumentException("File to hide has no extension: " + name);
        }
        return name.substring(dot);
    }
}
