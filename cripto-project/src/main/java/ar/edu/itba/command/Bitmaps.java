package ar.edu.itba.command;

import ar.edu.itba.FileIO;
import ar.edu.itba.StegoException;
import ar.edu.itba.bmp.BmpImage;

import java.nio.file.Path;

final class Bitmaps {

    private Bitmaps() {
    }

    static BmpImage read(String path) {
        byte[] file = FileIO.read(Path.of(path));
        try {
            return BmpImage.parse(file);
        } catch (StegoException e) {
            throw new StegoException(path + ": " + e.getMessage(), e);
        }
    }
}
