package ar.edu.itba;

import ar.edu.itba.bmp.BmpException;
import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.cli.ArgumentParser;
import ar.edu.itba.cli.Arguments;
import ar.edu.itba.cli.Arguments.Command;
import ar.edu.itba.cli.UsageException;
import ar.edu.itba.crypto.CryptoException;
import ar.edu.itba.crypto.PayloadCipher;
import ar.edu.itba.payload.Decoded;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.payload.PayloadException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class Main {

    static final String USAGE = """
            Usage:
              stegobmp -embed -in <file> -p <bitmap> -out <bitmap> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
              stegobmp -extract -p <bitmap> -out <file> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]""";

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
        } catch (BmpException | CryptoException | IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
        } catch (PayloadException e) {
            err.println("Error: No hidden data found in the carrier (or wrong -steg method, -a, -m or -pass)");
        }
        return 1;
    }

    private static void embed(Arguments arguments) throws IOException {
        String extension = extensionOf(arguments.inFile());
        BmpImage carrier = BmpImage.parse(Files.readAllBytes(arguments.carrier()));
        byte[] payload = PayloadCodec.encode(Files.readAllBytes(arguments.inFile()), extension);
        if (arguments.cipher() != null) {
            payload = PayloadCodec.prependLength(new PayloadCipher(arguments.cipher()).encrypt(payload));
        }

        byte[] pixels = carrier.pixels();
        arguments.stegMethod().create().embed(pixels, payload);

        try (OutputStream out = Files.newOutputStream(arguments.outFile())) {
            out.write(carrier.header());
            out.write(pixels);
        }
    }

    private static void extract(Arguments arguments) throws IOException {
        BmpImage carrier = BmpImage.parse(Files.readAllBytes(arguments.carrier()));
        byte[] hidden = arguments.stegMethod().create().extract(carrier.pixels());
        if (arguments.cipher() != null) {
            hidden = new PayloadCipher(arguments.cipher()).decrypt(PayloadCodec.readLengthPrefixed(hidden));
        }
        Decoded decoded = PayloadCodec.decode(hidden);

        // -out is a name without extension; the extension comes from the hidden payload.
        Files.write(Path.of(arguments.outFile() + decoded.extension()), decoded.data());
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
