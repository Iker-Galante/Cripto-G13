package ar.edu.itba.crypto;

public record CipherSpec(CipherAlgorithm algorithm, CipherMode mode, String password) {
}
