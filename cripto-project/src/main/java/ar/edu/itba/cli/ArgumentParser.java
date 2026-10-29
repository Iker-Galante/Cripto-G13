package ar.edu.itba.cli;

import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.steg.StegMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ArgumentParser {

    public static final String USAGE = """
            Uso:
              stegobmp -embed -in <archivo> -p <bitmap portador> -out <bitmap salida> -steg <LSB1 | LSB4 | LSBI>
                       [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]

              stegobmp -extract -p <bitmap portador> -out <archivo salida> -steg <LSB1 | LSB4 | LSBI>
                       [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]

              stegobmp -analyze -p <bitmap> [-steg <LSB1 | LSB4 | LSBI>] [-out <prefijo salida>]
                       [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]

            Sin -pass no se encripta. Con -pass, si no se indica -a se usa aes128 y si no se indica -m se usa cbc.
            Al extraer, se agrega al nombre indicado en -out la extensión del archivo oculto.
            -analyze busca un archivo oculto con LSB1, LSB4 y LSBI (o sólo con el método de -steg); si se indica
            -out, guarda lo encontrado como <prefijo>_<método><extensión>.""";

    private static final String IN = "-in";
    private static final String BITMAP = "-p";
    private static final String OUT = "-out";
    private static final String STEG = "-steg";
    private static final String ALGORITHM = "-a";
    private static final String MODE = "-m";
    private static final String PASSWORD = "-pass";
    private static final Set<String> VALUED_FLAGS = Set.of(IN, BITMAP, OUT, STEG, ALGORITHM, MODE, PASSWORD);

    private ArgumentParser() {
    }

    public static boolean isHelp(String[] args) {
        return args.length == 1 && (args[0].equals("-h") || args[0].equals("-help"));
    }

    public static Options parse(String[] args) {
        if (args.length == 0) {
            throw new UsageException("no se indicaron parámetros");
        }
        Command command = null;
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String flag = normalizeDash(args[i]);
            Optional<Command> commandFlag = commandOf(flag);
            if (commandFlag.isPresent()) {
                if (command != null) {
                    throw new UsageException("sólo se puede indicar una operación (" + command.flag() + " o "
                            + commandFlag.get().flag() + ")");
                }
                command = commandFlag.get();
            } else if (VALUED_FLAGS.contains(flag)) {
                if (i + 1 >= args.length || isFlag(args[i + 1])) {
                    throw new UsageException("falta el valor del parámetro " + flag);
                }
                if (values.putIfAbsent(flag, args[++i]) != null) {
                    throw new UsageException("el parámetro " + flag + " se indicó más de una vez");
                }
            } else {
                throw new UsageException("parámetro desconocido: " + args[i]);
            }
        }
        if (command == null) {
            throw new UsageException("debe indicarse la operación: -embed, -extract o -analyze");
        }

        List<String> warnings = new ArrayList<>();
        switch (command) {
            case EMBED -> requireAll(values, command, IN, BITMAP, OUT, STEG);
            case EXTRACT -> {
                requireAll(values, command, BITMAP, OUT, STEG);
                rejectAll(values, command, IN);
            }
            case ANALYZE -> {
                requireAll(values, command, BITMAP);
                rejectAll(values, command, IN);
            }
        }
        return new Options(command, Optional.ofNullable(values.get(IN)), values.get(BITMAP),
                Optional.ofNullable(values.get(OUT)), Optional.ofNullable(values.get(STEG)).map(ArgumentParser::parseSteg),
                parseEncryption(values, warnings), List.copyOf(warnings));
    }

    private static StegMethod parseSteg(String value) {
        return StegMethod.fromName(value).orElseThrow(() -> new UsageException(
                "método de esteganografiado inválido: " + value + " (debe ser LSB1, LSB4 o LSBI)"));
    }

    private static Optional<Encryption> parseEncryption(Map<String, String> values, List<String> warnings) {
        CipherAlgorithm algorithm = Optional.ofNullable(values.get(ALGORITHM))
                .map(value -> CipherAlgorithm.fromName(value).orElseThrow(() -> new UsageException(
                        "algoritmo de encripción inválido: " + value + " (debe ser aes128, aes192, aes256 o 3des)")))
                .orElse(null);
        CipherMode mode = Optional.ofNullable(values.get(MODE))
                .map(value -> CipherMode.fromName(value).orElseThrow(() -> new UsageException(
                        "modo de encripción inválido: " + value + " (debe ser ecb, cfb, ofb o cbc)")))
                .orElse(null);
        String password = values.get(PASSWORD);
        if (password == null) {
            if (algorithm != null || mode != null) {
                warnings.add("se indicó -a o -m sin -pass: no se usa encripción");
            }
            return Optional.empty();
        }
        if (password.isEmpty()) {
            throw new UsageException("la password no puede estar vacía");
        }
        return Optional.of(new Encryption(
                algorithm != null ? algorithm : CipherAlgorithm.AES128,
                mode != null ? mode : CipherMode.CBC,
                password));
    }

    private static void requireAll(Map<String, String> values, Command command, String... flags) {
        for (String flag : flags) {
            if (!values.containsKey(flag)) {
                throw new UsageException("falta el parámetro obligatorio " + flag + " para " + command.flag());
            }
        }
    }

    private static void rejectAll(Map<String, String> values, Command command, String... flags) {
        for (String flag : flags) {
            if (values.containsKey(flag)) {
                throw new UsageException("el parámetro " + flag + " no se usa con " + command.flag());
            }
        }
    }

    private static Optional<Command> commandOf(String flag) {
        for (Command command : Command.values()) {
            if (command.flag().equals(flag)) {
                return Optional.of(command);
            }
        }
        return Optional.empty();
    }

    private static boolean isFlag(String arg) {
        String flag = normalizeDash(arg);
        return VALUED_FLAGS.contains(flag) || commandOf(flag).isPresent();
    }

    /** Acepta los guiones largos (–, —) que aparecen al copiar los ejemplos del enunciado. */
    private static String normalizeDash(String arg) {
        if (arg.startsWith("–") || arg.startsWith("—")) {
            return "-" + arg.substring(1);
        }
        return arg;
    }
}
