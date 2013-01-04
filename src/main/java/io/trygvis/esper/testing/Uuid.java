package io.trygvis.esper.testing;

import java.util.*;

public class Uuid {
    private transient final UUID uuid;

    public Uuid(UUID uuid) {
        this.uuid = uuid;
    }

    public static Uuid randomUuid() {
        return new Uuid(UUID.randomUUID());
    }

    public String toUuidString() {
        return uuid.toString();
    }

    public String toString() {
        return toStringBase64();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Uuid that = (Uuid) o;

        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }

    public String toStringBase64() {
        char[] chars = new char[22];

        int i = 0;
        long most = uuid.getMostSignificantBits();
        int j;

        // The first 10 characters (60 bits) are easy
        for (j = 64 - 6; j >= 0; j -= 6) {
            long l = most >> j;
            long y = 0x3f & l;
            int x = (int) y;
            char c = alphabet[x];
            chars[i++] = c;
        }

        long least = uuid.getLeastSignificantBits();

        // Use the four last digits from most and two from least
        {
            long y = (0xf & most) << 2;
            long yy = 0xc000000000000000L & least;

            int z = (int) (yy >> 62);

            int x = (int) y + (0x3 & z);
            char c = alphabet[x];
            chars[i++] = c;
        }

        // Start from the 56th bit and generate 9 characters
        for (j = 62 - 6; j >= 0; j -= 6) {
            long l = least >> j;
            long y = 0x3f & l;
            int x = (int) y;
            char c = alphabet[x];
            chars[i++] = c;
        }

        // Use the last two bits for the last character
        chars[i] = alphabet[(int)least & 0x3];

        return new String(chars);
    }

    public static Uuid fromString(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        if (s.length() == 36) {
            return new Uuid(UUID.fromString(s));
        }

        if (s.length() == 22) {
            long most = 0;
            int i = 0;
            int shift = 64;
            for(; i < 10; i++) {
                char c = s.charAt(i);
                long b = alphabetR[c];

                if(b == 0) {
                    throw new IllegalArgumentException(s);
                }

                b--;

                shift -= 6;

                long l = b << shift;

                most |= l;
            }

            long least;

            {
                char c = s.charAt(i++);
                long b = alphabetR[c];

                if (b == 0) {
                    throw new IllegalArgumentException(s);
                }

                b--;

                long l = b >> 2;

                most |= l;

                shift = 64 - 2;

                least = (b & 0x03) << shift;
            }

            for(; i < 22; i++) {
                char c = s.charAt(i);
                long b = alphabetR[c];

                if(b == 0) {
                    throw new IllegalArgumentException(s);
                }

                b--;

                shift -= 6;

                long l = b << shift;
                least |= l;
            }

            return new Uuid(new UUID(most, least));
        }

        throw new IllegalArgumentException("Illegal: " + s);
    }

    // http://en.wikipedia.org/wiki/Base64
    public final static char[] alphabet = new char[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'/* ,'+', '/'*/, '-', '_'
    };

    public final static byte[] alphabetR;

    static {
        alphabetR = new byte[256];
        for (int i = 0; i < alphabet.length; i++) {
            // plus one so it's possible to know if the lookup failed ('A' would normally be 0).
            alphabetR[alphabet[i]] = (byte)(i + 1);
        }

//        for (int i = 0; i < alphabetR.length; i++) {
//            int v = alphabetR[i];
//            if (v == 0) {
//                continue;
//            }
//            System.out.println(String.format("alphabetR[%3d / %c] = %d", i, (char) i, v));
//        }
//
//        System.out.println("alphabetR = " + alphabetR['A']);
    }
}
