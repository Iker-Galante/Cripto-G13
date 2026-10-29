package ar.edu.itba;

/**
 * Error que se le informa al usuario. El mensaje está pensado para mostrarse tal cual por consola.
 */
public class StegoException extends RuntimeException {

    public StegoException(String message) {
        super(message);
    }

    public StegoException(String message, Throwable cause) {
        super(message, cause);
    }
}
