package ar.edu.itba.cli;

import ar.edu.itba.cli.Arguments.Command;
import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.crypto.CipherSpec;
import ar.edu.itba.steg.StegMethod;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ArgumentParser {

    private static final Set<String> VALUE_FLAGS = Set.of("-in", "-p", "-out", "-steg", "-a", "-m", "-pass");

    private ArgumentParser() {
    }

    public static Arguments parse(String[] args) {
        Command command = null;
        Map<String, String> values = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            if (flag.equals("-embed") || flag.equals("-extract")) {
                if (command != null) {
                    throw new UsageException("Give only one of -embed or -extract");
                }
                command = flag.equals("-embed") ? Command.EMBED : Command.EXTRACT;
            } else if (VALUE_FLAGS.contains(flag)) {
                if (i + 1 == args.length || args[i + 1].startsWith("-")) {
                    throw new UsageException("Missing value for " + flag);
                }
                i++;
                if (values.put(flag, args[i]) != null) {
                    throw new UsageException("Repeated option: " + flag);
                }
            } else {
                throw new UsageException("Unknown option: " + flag);
            }
        }

        if (command == null) {
            throw new UsageException("Missing -embed or -extract");
        }
        if (command == Command.EMBED) {
            require(values, "-in");
        } else if (values.containsKey("-in")) {
            throw new UsageException("Option -in is not used with -extract");
        }
        require(values, "-p");
        require(values, "-out");
        require(values, "-steg");
        StegMethod method = stegMethod(values.get("-steg"));
        CipherSpec cipher = cipherSpec(values);

        Path inFile = command == Command.EMBED ? Path.of(values.get("-in")) : null;
        return new Arguments(command, inFile, Path.of(values.get("-p")),
                Path.of(values.get("-out")), method, cipher);
    }

    private static void require(Map<String, String> values, String flag) {
        if (!values.containsKey(flag)) {
            throw new UsageException("Missing option " + flag);
        }
    }

    // -a and -m are checked even without -pass, so a typo is never silently ignored.
    // Without -pass there is no encryption; -pass alone means aes128 with cbc.
    private static CipherSpec cipherSpec(Map<String, String> values) {
        CipherAlgorithm algorithm = CipherAlgorithm.AES128;
        CipherMode mode = CipherMode.CBC;
        try {
            if (values.containsKey("-a")) {
                algorithm = CipherAlgorithm.fromOption(values.get("-a"));
            }
            if (values.containsKey("-m")) {
                mode = CipherMode.fromOption(values.get("-m"));
            }
        } catch (IllegalArgumentException e) {
            throw new UsageException(e.getMessage());
        }

        String password = values.get("-pass");
        if (password == null) {
            return null;
        }
        if (password.isEmpty()) {
            throw new UsageException("Password must not be empty");
        }
        return new CipherSpec(algorithm, mode, password);
    }

    private static StegMethod stegMethod(String value) {
        try {
            return StegMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new UsageException("Invalid steganography method: " + value);
        }
    }
}
