package ar.edu.itba;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Lectura y escritura de archivos con errores legibles para el usuario. */
public final class FileIO {

    private FileIO() {
    }

    public static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (NoSuchFileException e) {
            throw new StegoException("no existe el archivo " + path, e);
        } catch (AccessDeniedException e) {
            throw new StegoException("no hay permisos para leer el archivo " + path, e);
        } catch (IOException e) {
            throw new StegoException("no se pudo leer el archivo " + path + ": " + e.getMessage(), e);
        }
    }

    public static void write(Path path, byte[] data) {
        try {
            Files.write(path, data);
        } catch (NoSuchFileException e) {
            throw new StegoException("no existe el directorio para escribir " + path, e);
        } catch (AccessDeniedException e) {
            throw new StegoException("no hay permisos para escribir el archivo " + path, e);
        } catch (IOException e) {
            throw new StegoException("no se pudo escribir el archivo " + path + ": " + e.getMessage(), e);
        }
    }
}
