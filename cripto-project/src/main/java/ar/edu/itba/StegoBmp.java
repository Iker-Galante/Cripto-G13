package ar.edu.itba;

import ar.edu.itba.cli.ArgumentParser;
import ar.edu.itba.cli.Options;
import ar.edu.itba.cli.UsageException;
import ar.edu.itba.command.AnalyzeCommand;
import ar.edu.itba.command.EmbedCommand;
import ar.edu.itba.command.ExtractCommand;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class StegoBmp {

    private StegoBmp() {
    }

    public static void main(String[] args) {
        // Los mensajes tienen acentos: no depender del locale de la terminal.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        if (ArgumentParser.isHelp(args)) {
            System.out.println(ArgumentParser.USAGE);
            return;
        }
        try {
            Options options = ArgumentParser.parse(args);
            options.warnings().forEach(warning -> System.err.println("Advertencia: " + warning));
            switch (options.command()) {
                case EMBED -> EmbedCommand.run(options, System.out);
                case EXTRACT -> ExtractCommand.run(options, System.out);
                case ANALYZE -> AnalyzeCommand.run(options, System.out);
            }
        } catch (UsageException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(ArgumentParser.USAGE);
            System.exit(1);
        } catch (StegoException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
