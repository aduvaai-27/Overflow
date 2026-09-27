package com.orderflow.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small helper for hashing passwords with SHA-256 so that plain text
 * passwords are never stored in the database.
 *
 * (Introduction to Java Syntax topic in practice: this class shows basic
 * Java syntax such as static methods, loops, arrays and try/catch.)
 */
public class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes("UTF-8"));

            // Convert the raw bytes into a readable hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to hash password", e);
        }
    }

    public static boolean matches(String plainText, String storedHash) {
        return hash(plainText).equals(storedHash);
    }
}
