package net.javaguides.Banking_app.util;
// ↑ Belongs to the "util" package — utility/helper classes.

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// ↑ BCryptPasswordEncoder is Spring Security's tool to hash passwords using BCrypt algorithm.

/**
 * ============================================================
 * WHAT IS BcryptHashGenerator?
 * A simple ONE-TIME utility tool (NOT used in production flow).
 *
 * PURPOSE:
 * When the Admin password needs to be changed, you can't store
 * plain-text passwords. This tool generates the BCrypt hash of
 * a password so you can safely copy-paste it into application.properties.
 *
 * HOW TO USE:
 * 1. Change the "password" variable below to your desired admin password.
 * 2. Run ONLY this class's main() method (right-click → Run 'main()' in IntelliJ).
 * 3. Copy the printed hash from the console.
 * 4. Paste it as the value of "app.admin.password" in application.properties.
 *
 * This tool is NOT a REST endpoint — it's just a standalone Java program
 * that you run manually when needed.
 *
 * BCrypt properties:
 *   - The same password generates a DIFFERENT hash each time (due to random "salt").
 *   - You cannot reverse a BCrypt hash to get the original password.
 *   - BCryptPasswordEncoder.matches(raw, hash) returns true if they match.
 * ============================================================
 */
public class BcryptHashGenerator {

    // This is a standalone main() method — not the same as BankingAppApplication.main()!
    // Running this method does NOT start the whole Spring Boot app.
    // It just creates a BCryptPasswordEncoder and hashes a password.
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // ↑ Create a BCrypt password encoder with default strength (10 rounds).

        String password = "adminPassword123"; // ← Change this to your desired admin password

        String hash = encoder.encode(password); // Hash the password using BCrypt algorithm.
        // Example output: "$2a$10$voRCWJN8VukUWyqZoqr/O.khtZ8cEidangb8WvmrMi2LtdaFS9zS."
        // This hash is safe to store in application.properties.

        System.out.println("Hash for '" + password + "':");
        System.out.println(hash); // Print the hash to copy into application.properties

        System.out.println("Verification: " + encoder.matches(password, hash));
        // ↑ Verifies the hash works. Should always print "true".

        // Verify the existing hash from application.properties (sanity check).
        String oldHash = "$2a$10$tM2xK5Qc1kUvGvJ8Qx4y1OqF3a1v.LgH9qYfE613s6lq85C5pQc0u";
        System.out.println("Old hash match: " + encoder.matches(password, oldHash));
        // ↑ Checks if the old hash still works for this password.
        //   Useful to verify if the hash in application.properties is correct.
    }
}
