package ar.edu.itba.steg;

/**
 * LSB Improved (Majeed y Sulaiman): LSB1 sobre los canales azul y verde, invirtiendo el LSB de los
 * bytes cuyo patrón (bits 2 y 1) tuvo más cambios que no cambios.
 * <p>
 * Los primeros 4 bytes del portador guardan con LSB1 si se invirtió cada patrón (00, 01, 10, 11).
 * El mensaje se oculta a partir del byte 4, salteando los bytes de rojo. El bloque de píxeles se
 * recorre como un único flujo (padding incluido), así que el rojo es todo byte con índice % 3 == 2.
 */
public final class Lsbi implements StegAlgorithm {

    private static final int PATTERNS = 4;
    private static final int FIRST_DATA_BYTE = PATTERNS;

    @Override
    public int capacity(int carrierLength) {
        if (carrierLength <= FIRST_DATA_BYTE) {
            return 0;
        }
        return (nonRedBytes(carrierLength) - nonRedBytes(FIRST_DATA_BYTE)) / 8;
    }

    @Override
    public void embed(byte[] carrier, byte[] data) {
        if (data.length > capacity(carrier.length)) {
            throw new IllegalArgumentException("data does not fit in carrier");
        }
        int[] changed = new int[PATTERNS];
        int[] unchanged = new int[PATTERNS];
        int position = FIRST_DATA_BYTE;
        for (byte value : data) {
            for (int shift = 7; shift >= 0; shift--) {
                position = skipRed(position);
                int original = carrier[position];
                int stego = (original & ~1) | ((value >> shift) & 1);
                carrier[position] = (byte) stego;
                if (stego == original) {
                    unchanged[pattern(stego)]++;
                } else {
                    changed[pattern(stego)]++;
                }
                position++;
            }
        }

        boolean[] inverted = new boolean[PATTERNS];
        for (int pattern = 0; pattern < PATTERNS; pattern++) {
            inverted[pattern] = changed[pattern] > unchanged[pattern];
        }
        for (int i = FIRST_DATA_BYTE; i < position; i++) {
            if (!isRed(i) && inverted[pattern(carrier[i])]) {
                carrier[i] ^= 1;
            }
        }
        for (int pattern = 0; pattern < PATTERNS; pattern++) {
            carrier[pattern] = (byte) ((carrier[pattern] & ~1) | (inverted[pattern] ? 1 : 0));
        }
    }

    @Override
    public byte[] extract(byte[] carrier) {
        byte[] data = new byte[capacity(carrier.length)];
        if (data.length == 0) {
            return data;
        }
        boolean[] inverted = new boolean[PATTERNS];
        for (int pattern = 0; pattern < PATTERNS; pattern++) {
            inverted[pattern] = (carrier[pattern] & 1) == 1;
        }
        int position = FIRST_DATA_BYTE;
        for (int i = 0; i < data.length; i++) {
            int value = 0;
            for (int bit = 0; bit < 8; bit++) {
                position = skipRed(position);
                int lsb = carrier[position] & 1;
                if (inverted[pattern(carrier[position])]) {
                    lsb ^= 1;
                }
                value = (value << 1) | lsb;
                position++;
            }
            data[i] = (byte) value;
        }
        return data;
    }

    /** Patrón formado por el 3er y 2do bit menos significativos. */
    private static int pattern(int value) {
        return (value >> 1) & 0b11;
    }

    /** Los píxeles se guardan en orden BGR: el tercer byte de cada terna es el rojo. */
    private static boolean isRed(int index) {
        return index % 3 == 2;
    }

    private static int skipRed(int index) {
        return isRed(index) ? index + 1 : index;
    }

    /** Cantidad de bytes que no son rojo entre los índices [0, length). */
    private static int nonRedBytes(int length) {
        return length - length / 3;
    }
}
