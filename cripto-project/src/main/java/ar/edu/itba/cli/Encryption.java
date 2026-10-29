package ar.edu.itba.cli;

import ar.edu.itba.crypto.CipherAlgorithm;
import ar.edu.itba.crypto.CipherMode;
import ar.edu.itba.crypto.PayloadCipher;

/** Parámetros de encripción indicados por línea de comandos. */
public record Encryption(CipherAlgorithm algorithm, CipherMode mode, String password) {

    public PayloadCipher cipher() {
        return new PayloadCipher(algorithm, mode, password);
    }

    @Override
    public String toString() {
        return algorithm.argument() + " " + mode.argument();
    }
}
