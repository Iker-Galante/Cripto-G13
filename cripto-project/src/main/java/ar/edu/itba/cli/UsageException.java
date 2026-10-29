package ar.edu.itba.cli;

import ar.edu.itba.StegoException;

/** Error en los parámetros de entrada: además del mensaje se le muestra al usuario la sintaxis correcta. */
public class UsageException extends StegoException {

    public UsageException(String message) {
        super(message);
    }
}
