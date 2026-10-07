package com.swaphat.spoofified.util;

import java.net.URI;
import java.util.regex.Pattern;

public class NetworkUtils {

    // Matches 'localhost' plus standard private IPv4 and IPv6 subnets
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "(^localhost$)|" +
                    "(^127\\.\\d+\\.\\d+\\.\\d+$)|" +       // Loopback (127.0.0.0/8)
                    "(^10\\.\\d+\\.\\d+\\.\\d+$)|" +        // Class A private (10.0.0.0/8)
                    "(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d+\\.\\d+$)|" + // Class B private (172.16.0.0/12)
                    "(^192\\.168\\.\\d+\\.\\d+$)|" +        // Class C private (192.168.0.0/16)
                    "(^169\\.254\\.\\d+\\.\\d+$)|" +        // Link-local (169.254.0.0/16)
                    "(^0\\.0\\.0\\.0$)|" +                  // Current network (0.0.0.0/8)
                    "(^\\[?::1]?$)|" +                    // IPv6 loopback
                    "(^\\[?[fF][cCdD].*]?$)|" +           // IPv6 ULA (fc00::/7)
                    "(^\\[?[fF][eE][89aAbB].*]?$)"        // IPv6 Link-local (fe80::/10)
    );

    public static boolean isUrlSafe(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();

            if (host == null) return false;

            // Strip brackets from raw IPv6 addresses
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }

            // If the host matches any private subnet, it's hostile
            return !PRIVATE_IP_PATTERN.matcher(host).matches();
        } catch (Exception e) {
            // If the URI is malformed, block it safely
            return false;
        }
    }
}