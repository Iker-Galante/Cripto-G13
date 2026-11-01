package ar.edu.itba.cli;

import java.nio.file.Path;

// inFile is null for -extract.
public record Arguments(Command command, Path inFile, Path carrier, Path outFile, String stegMethod) {

    public enum Command { EMBED, EXTRACT }
}
