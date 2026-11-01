package ar.edu.itba.cli;

import ar.edu.itba.steg.StegMethod;
import java.nio.file.Path;

// inFile is null for -extract.
public record Arguments(Command command, Path inFile, Path carrier, Path outFile, StegMethod stegMethod) {

    public enum Command { EMBED, EXTRACT }
}
