package io.clamped.cli;

import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Interactive setup wizard that creates ~/.clamped/config.properties.
 *
 * Usage: clamped setup
 */
public final class SetupCommand {

    private SetupCommand() {}

    public static void run() {
        Console console = System.console();

        System.out.println("Clamped setup — configures your connection to PostgreSQL.");
        System.out.println("Values will be saved to ~/.clamped/config.properties");
        System.out.println("Press Enter to accept the default shown in brackets.");
        System.out.println();

        String url      = prompt(console, "JDBC URL",  "jdbc:postgresql://localhost:5432/clamped_db");
        String username = prompt(console, "Username",  "postgres");
        String password = promptPassword(console,      "Password");

        Path dir        = Paths.get(System.getProperty("user.home"), ".clamped");
        Path configFile = dir.resolve("config.properties");

        try {
            Files.createDirectories(dir);

            Properties props = new Properties();
            props.setProperty("jdbcUrl",   url);
            props.setProperty("username",  username);
            props.setProperty("password",  password);

            try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
                props.store(fos, "Clamped CLI configuration");
            }

            System.out.println();
            System.out.println("Config saved to: " + configFile);
            System.out.println("Run 'clamped list' to verify the connection.");

        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String prompt(Console console, String label, String defaultValue) {
        String display = label + " [" + defaultValue + "]: ";
        if (console != null) {
            String input = console.readLine(display);
            return (input == null || input.trim().isEmpty()) ? defaultValue : input.trim();
        }
        // Fallback if no console (e.g. IDE terminal)
        System.out.print(display);
        try {
            String input = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
            return (input == null || input.trim().isEmpty()) ? defaultValue : input.trim();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    private static String promptPassword(Console console, String label) {
        if (console != null) {
            char[] pwd = console.readPassword(label + ": ");
            return pwd == null ? "" : new String(pwd);
        }
        // Fallback — visible input (no Console available)
        System.out.print(label + " (input visible — run from a real terminal to hide): ");
        try {
            return new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            return "";
        }
    }
}
