package org.qubership.itool.modules.diagram;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class UMLDiagramEncoder {

    static final char[] encode6bit = new char[64];
    static final byte[] decode6bit = new byte[128];

    public static String encodeDiagram(String generatedText) {
        String uncommentData = trim(generatedText);
        //Encoded in UTF-8
        byte[] data = uncommentData.getBytes(StandardCharsets.UTF_8);
        //Compressed using Deflate algorithm
        byte[] compressedData = compress(data);
        //Re-encoded in ASCII using a transformation close to base64
        String encodedData= encode(compressedData);
        return encodedData;
    }

    private static byte[] compress(byte[] in) {

        int len = in.length * 2;
        if (len < 1000) {
            len = 1000;
        }

        Deflater compressor = new Deflater(9, true);
        compressor.setInput(in);
        compressor.finish();
        byte[] output = new byte[len];
        int compressedDataLength = compressor.deflate(output);
        return !compressor.finished() ? null : copyArray(output, compressedDataLength);
    }

    private static byte[] copyArray(byte[] data, int len) {
        byte[] result = new byte[len];
        System.arraycopy(data, 0, result, 0, len);
        return result;
    }

    private static String encode(byte[] data) {
        if (data == null) {
            return "";
        } else {
            StringBuilder result = new StringBuilder((data.length * 4 + 2) / 3);

            for (int i = 0; i < data.length; i += 3) {
                append3bytes(result, data[i] & 255, i + 1 < data.length ? data[i + 1] & 255 : 0, i + 2 < data.length ? data[i + 2] & 255 : 0);
            }

            return result.toString();
        }
    }

    private static void append3bytes(StringBuilder sb, int b1, int b2, int b3) {
        int c1 = b1 >> 2;
        int c2 = (b1 & 3) << 4 | b2 >> 4;
        int c3 = (b2 & 15) << 2 | b3 >> 6;
        int c4 = b3 & 63;
        sb.append(encode6bit[c1 & 63]);
        sb.append(encode6bit[c2 & 63]);
        sb.append(encode6bit[c3 & 63]);
        sb.append(encode6bit[c4 & 63]);
    }

    // trim @start & @end annotated lines
    private static String trim(String arg) {
        return arg.length() == 0 ? arg : trimEndingInternal(arg, getPositionStartNonSpace(arg));
    }

    private static String trimEndingInternal(String arg, int from) {
        int j;
        for (j = arg.length() - 1; j >= from && isSpaceOrTabOrNull(arg.charAt(j)); --j) {
        }

        return from == 0 && j == arg.length() - 1 ? arg : arg.substring(from, j + 1);
    }

    private static boolean isSpaceOrTabOrNull(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == 0;
    }

    private static int getPositionStartNonSpace(String arg) {
        int i;
        for (i = 0; i < arg.length() && isSpaceOrTabOrNull(arg.charAt(i)); ++i) {
        }

        return i;
    }

    //mechanism to build 64bit char encoding

    static {
        for (byte b = 0; b < 64; decode6bit[encode6bit[b]] = b++) {
            encode6bit[b] = encode6bit(b);
        }
    }

    private static char encode6bit(byte b) {
        assert b >= 0 && b < 64;

        if (b < 10) {
            return (char) (48 + b);
        } else {
            b = (byte) (b - 10);
            if (b < 26) {
                return (char) (65 + b);
            } else {
                b = (byte) (b - 26);
                if (b < 26) {
                    return (char) (97 + b);
                } else {
                    b = (byte) (b - 26);
                    if (b == 0) {
                        return '-';
                    } else if (b == 1) {
                        return '_';
                    } else {
                        assert false;

                        return '?';
                    }
                }
            }
        }
    }

}
