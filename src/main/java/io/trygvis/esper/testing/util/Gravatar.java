package io.trygvis.esper.testing.util;

import java.io.*;
import java.security.*;

public class Gravatar {
    public static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    public static String gravatar(String mail) {
        return "http://www.gravatar.com/avatar/" + md5Hex(mail.trim().toLowerCase());
    }
}
