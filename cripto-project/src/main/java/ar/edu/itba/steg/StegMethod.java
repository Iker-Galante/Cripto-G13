package ar.edu.itba.steg;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/** Métodos de esteganografiado disponibles por línea de comandos ({@code -steg}). */
public enum StegMethod {
    LSB1(() -> new LsbN(1)),
    LSB4(() -> new LsbN(4));

    private final Supplier<StegAlgorithm> factory;

    StegMethod(Supplier<StegAlgorithm> factory) {
        this.factory = factory;
    }

    public StegAlgorithm algorithm() {
        return factory.get();
    }

    public static Optional<StegMethod> fromName(String name) {
        return Arrays.stream(values()).filter(method -> method.name().equalsIgnoreCase(name)).findFirst();
    }
}
