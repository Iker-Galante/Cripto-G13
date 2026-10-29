package ar.edu.itba.steg;

/**
 * Algoritmo de esteganografiado sobre el bloque de píxeles de un BMP.
 * Los bytes se ocultan del bit más significativo al menos significativo.
 */
public interface StegAlgorithm {

    /** Cantidad máxima de bytes que se pueden ocultar en un portador de {@code carrierLength} bytes. */
    int capacity(int carrierLength);

    /** Oculta {@code data} en {@code carrier}, modificándolo. */
    void embed(byte[] carrier, byte[] data);

    /** Recupera todos los bytes que el portador puede contener, en el orden en que se ocultaron. */
    byte[] extract(byte[] carrier);
}
