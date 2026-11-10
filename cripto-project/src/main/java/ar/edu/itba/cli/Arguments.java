package ar.edu.itba.cli;

import ar.edu.itba.crypto.CipherSpec;
import ar.edu.itba.steg.StegMethod;
import java.nio.file.Path;

// inFile is null unless -embed; cipher is null without -pass.
// With -analyze, outFile (a prefix for what is found) and stegMethod (null = try all) are optional.
public record Arguments(Command command, Path inFile, Path carrier, Path outFile, StegMethod stegMethod,
                        CipherSpec cipher) {

    public enum Command {
        EMBED("-embed"), EXTRACT("-extract"), ANALYZE("-analyze");

        private final String flag;

        Command(String flag) {
            this.flag = flag;
        }

        public String flag() {
            return flag;
        }
    }
}
