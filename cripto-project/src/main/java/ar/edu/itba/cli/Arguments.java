package ar.edu.itba.cli;

import ar.edu.itba.crypto.CipherSpec;
import ar.edu.itba.steg.StegMethod;
import java.nio.file.Path;

// inFile is null for -extract; cipher is null without -pass.
public record Arguments(Command command, Path inFile, Path carrier, Path outFile, StegMethod stegMethod,
                        CipherSpec cipher) {

    public enum Command { EMBED, EXTRACT }
}
