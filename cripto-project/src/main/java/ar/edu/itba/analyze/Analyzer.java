package ar.edu.itba.analyze;

import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.crypto.CipherSpec;
import ar.edu.itba.crypto.CryptoException;
import ar.edu.itba.crypto.PayloadCipher;
import ar.edu.itba.payload.Decoded;
import ar.edu.itba.payload.PayloadCodec;
import ar.edu.itba.payload.PayloadException;
import ar.edu.itba.steg.StegMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// -analyze: looks for a hidden file with each method and reports what it finds, one line per check.
// Given an output prefix, it also saves each finding as <prefix>_<method><extension>.
public final class Analyzer {

    private static final int MAX_TEXT_SHOWN = 200;

    private final BmpImage carrier;
    private final PayloadCipher cipher;  // null without -pass
    private final Path outPrefix;        // null when nothing is saved

    public Analyzer(BmpImage carrier, CipherSpec cipher, Path outPrefix) {
        this.carrier = carrier;
        this.cipher = cipher == null ? null : new PayloadCipher(cipher);  // derives the key once for all methods
        this.outPrefix = outPrefix;
    }

    public List<String> analyze(List<StegMethod> methods) throws IOException {
        List<String> report = new ArrayList<>();
        byte[] pixels = carrier.pixels();
        for (StegMethod method : methods) {
            report.add(method + ": " + check(method, method.create().extract(pixels)));
        }
        report.add(trailingData());
        return report;
    }

    private String check(StegMethod method, byte[] hidden) throws IOException {
        try {
            byte[] block = cipher == null ? hidden : cipher.decrypt(PayloadCodec.readLengthPrefixed(hidden));
            Decoded found = PayloadCodec.decode(block);
            return "found a " + found.data().length + "-byte " + found.extension() + " file"
                    + save("_" + method + found.extension(), found.data());
        } catch (PayloadException | CryptoException e) {
            return cipher == null ? maybeEncrypted(method, hidden) : "nothing found with this -a, -m and -pass";
        }
    }

    // An encrypted payload is cipherLength || ciphertext: the length fits, but no extension follows the data.
    private String maybeEncrypted(StegMethod method, byte[] hidden) throws IOException {
        byte[] ciphertext;
        try {
            ciphertext = PayloadCodec.readLengthPrefixed(hidden);
        } catch (PayloadException e) {
            return "nothing found";
        }
        if (ciphertext.length == 0) {
            return "nothing found";
        }
        return "no plain file, but a valid length of " + ciphertext.length + " bytes ("
                + blockHint(ciphertext.length) + "): it may be encrypted, try -a, -m and -pass"
                + save("_" + method + ".enc", ciphertext);
    }

    // ECB and CBC pad to whole blocks (16 bytes for AES, 8 for 3DES); CFB and OFB keep any length.
    private static String blockHint(int length) {
        if (length % 16 == 0) {
            return "a multiple of 16: any algorithm and mode";
        }
        if (length % 8 == 0) {
            return "a multiple of 8: 3des in any mode, or aes with cfb or ofb";
        }
        return "not a multiple of 8: only cfb or ofb";
    }

    private String trailingData() throws IOException {
        byte[] trailing = carrier.trailingBytes();
        if (trailing.length == 0) {
            return "No data after the pixel array";
        }
        String text = isPrintable(trailing) && trailing.length <= MAX_TEXT_SHOWN
                ? ": \"" + new String(trailing, StandardCharsets.US_ASCII) + "\""
                : "";
        return trailing.length + " bytes after the pixel array" + text + save("_trailing.bin", trailing);
    }

    private static boolean isPrintable(byte[] bytes) {
        for (byte b : bytes) {
            if ((b < 0x20 || b > 0x7E) && b != '\n' && b != '\r' && b != '\t') {
                return false;
            }
        }
        return true;
    }

    private String save(String suffix, byte[] data) throws IOException {
        if (outPrefix == null) {
            return "";
        }
        Path path = Path.of(outPrefix + suffix);
        Files.write(path, data);
        return " -> saved to " + path;
    }
}
