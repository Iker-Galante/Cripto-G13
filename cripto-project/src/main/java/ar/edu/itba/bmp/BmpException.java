package ar.edu.itba.bmp;

/**
 * Thrown when a file is not a BMP we can use as a carrier: wrong signature, not 24 bits per
 * pixel, compressed, or a header whose pixel data offset points outside the file.
 */
public class BmpException extends RuntimeException {

    public BmpException(String message) {
        super(message);
    }
}
