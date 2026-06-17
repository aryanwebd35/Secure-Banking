package net.javaguides.Banking_app.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Temporary utility to generate bcrypt hash for admin password.
 * Run this class's main method to get the hash, then put it in application.properties.
 */
public class BcryptHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "adminPassword123";
        String hash = encoder.encode(password);
        System.out.println("Hash for '" + password + "':");
        System.out.println(hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
        
        // Verify the old hash from application.properties
        String oldHash = "$2a$10$tM2xK5Qc1kUvGvJ8Qx4y1OqF3a1v.LgH9qYfE613s6lq85C5pQc0u";
        System.out.println("Old hash match: " + encoder.matches(password, oldHash));
    }
}
