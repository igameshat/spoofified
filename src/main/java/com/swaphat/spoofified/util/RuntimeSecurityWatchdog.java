package com.swaphat.spoofified.util;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.security.MessageDigest;


public class RuntimeSecurityWatchdog {

    private static String initialJarHash = null;

    public static void initializeGuard() {
        ClientSpoofer.LOGGER.info(" Initializing Runtime Security & Integrity Guard...");

        try {
            // Capture our own CodeSource location (the running JAR file)
            CodeSource codeSource = ClientSpoofer.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                File jarFile = new File(codeSource.getLocation().toURI());

                if (jarFile.isFile()) {
                    validateJarFile(jarFile);
                }
            }
        } catch (Exception e) {
            ClientSpoofer.LOGGER.error("[Spoofified Security] Integrity check failed: {}", e.getMessage());
            crashGame("Critical Integrity Violation: Environment compromised.");
        }

        // Launch background continuous monitoring watchdog
        Thread watchdogThread = new Thread(RuntimeSecurityWatchdog::runContinuousWatchdog, "Spoofified-Watchdog");
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }

    private static void validateJarFile(File jarFile) throws IOException {
        // Check last modified timestamp anomalies (Requirement 2)
        Path path = jarFile.toPath();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        long lastModified = attrs.lastModifiedTime().toMillis();
        long currentTime = System.currentTimeMillis();

        // If file timestamp is in the future or manipulated unexpectedly during runtime
        if (lastModified > currentTime + 60000) {
            crashGame("Security Violation: File timestamp anomaly detected on mod container.");
        }

        // Compute baseline SHA-256 hash of our own JAR
        initialJarHash = computeFileHash(jarFile);
        ClientSpoofer.LOGGER.info(" Base JAR checksum secured. Hash verification active.");
    }

    private static String computeFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void runContinuousWatchdog() {
        while (ClientSpooferOptions.RUN_CONTINUOUS_WATCHDOG) {
            try {
                Thread.sleep(120000); // Check every 120 seconds

                CodeSource codeSource = ClientSpoofer.class.getProtectionDomain().getCodeSource();
                if (codeSource != null && codeSource.getLocation() != null) {
                    File jarFile = new File(codeSource.getLocation().toURI());
                    if (jarFile.isFile()) {
                        String currentHash = computeFileHash(jarFile);
                        if (initialJarHash != null && !initialJarHash.equals(currentHash)) {
                            crashGame("Fatal Security Alert: Mod JAR file was modified or overwritten at runtime!");
                        }
                    }
                }

                ClassLoader loader = ClientSpoofer.class.getClassLoader();
                if (loader == null) {
                    crashGame("Fatal Security Alert: ClassLoader stripped or hijacked.");
                }

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                ClientSpoofer.LOGGER.error("[Watchdog Error] {}", e.getMessage());
            }
        }
    }

    private static void crashGame(String reason) {
        ClientSpoofer.LOGGER.error("[Security Guard] {}", reason);
        Runtime.getRuntime().halt(-1);
    }
}