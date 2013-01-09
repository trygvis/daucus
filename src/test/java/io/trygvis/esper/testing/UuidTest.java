package io.trygvis.esper.testing;

import org.apache.commons.lang.*;
import org.junit.*;

import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class UuidTest {

    boolean silent = System.getProperty("idea.launcher") != null;

    @Test
    public void testToString() {
        String s = "fedcba98-7654-3210-fedc-ba9876543210";
        Uuid uuid = Uuid.fromString(s);

        String r = toBase64(0xfedcba9876543210L, 0xfedcba9876543210L);

        assertEquals(s, uuid.toUuidString());
//        assertEquals(uuid.toUuidString(), Uuid.fromString(uuid.toUuidString()).toUuidString());
//        assertEquals(uuid.toStringBase64(), Uuid.fromString(uuid.toStringBase64()).toStringBase64());

        assertEquals(r, uuid.toStringBase64());
    }

    @Test
    public void test2() {
        Uuid uuid = Uuid.fromString("fedcba98-7654-3210-fedc-ba9876543210");

        assertEquals(uuid, Uuid.fromString(uuid.toUuidString()));

        assertEquals(uuid.toUuidString(), Uuid.fromString(uuid.toStringBase64()).toUuidString());
    }

    @Test
    public void test2_() {
        UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

        String base64 = new Uuid(uuid).toStringBase64();
        
        assertEquals(toBase64(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()), base64);

        assertEquals(uuid, Uuid.parseBase64(base64).toUUID());
    }

    @Test
    public void test3() {
        Uuid uuid = Uuid.fromString("3f4fc3bc-967a-40a7-9fc2-46398d35fa25");

        UUID x = UUID.fromString(uuid.toUuidString());
        String s = toBase64(x.getMostSignificantBits(), x.getLeastSignificantBits());
        System.out.println(s);

        System.out.println("uuid.toUuidString() = " + uuid.toUuidString());
        System.out.println("uuid.toStringBase64() = " + uuid.toStringBase64());

        assertEquals(uuid, Uuid.fromString(uuid.toUuidString()));

        assertEquals(uuid.toUuidString(), Uuid.fromString(uuid.toStringBase64()).toUuidString());
    }

    @Test
    public void test4() {
        Uuid uuid = Uuid.fromString("c541ff9e-e0a6-4eda-8eea-3f19e5ef893a");

        System.out.println(System.getProperties());

        assertEquals(uuid, Uuid.fromString(uuid.toUuidString()));

        assertEquals(uuid.toUuidString(), Uuid.fromString(uuid.toStringBase64()).toUuidString());
    }

    @Test
    public void random() {
        Random random = new Random(0);

        for(int i = 0; i < 100; i++) {
            long most = random.nextLong();
            long least = random.nextLong();
            if(!silent)
                System.out.println(format("i=%2d, most=%08x, least=%08x", i, most, least));

            UUID s = new UUID(most, least);
            System.out.println("s = " + s);
            Uuid uuid = Uuid.fromString(s.toString());

            assertEquals(s.toString(), uuid.toUuidString());
            assertEquals(toBase64(most, least), uuid.toStringBase64());
        }
    }

    private String toBase64(long most, long least) {
        String x = StringUtils.leftPad(Long.toBinaryString(most), 64, '0') + StringUtils.leftPad(Long.toBinaryString(least), 64, '0');
        if(!silent)
            System.out.println(x);

        String r = "";
        for (int i = 0; i < 22; i++) {
            int end = Math.min((i + 1) * 6, x.length());
            String y = x.substring(i * 6, end);
            int number = Integer.parseInt(y, 2);
            if(!silent)
                System.out.println(format("% 4d % 4d binary=%8s, dec=%2d %c", i, end, y, number, Uuid.alphabet[number]));
            r += Uuid.alphabet[number];
        }
        return r;
    }

    /*
    private UUID fromBase64(String s) {
        assertEquals(22, s.length());

        int shift = 60;
        long most = 0, least = 0;

        for (int i = 0; i < 22; i += 6) {
            String x = s.substring(i, Math.min(s.length(), i + 6));
            System.out.println("x = " + x);
            Integer.parseInt(x);

        }
    }
    */
}
