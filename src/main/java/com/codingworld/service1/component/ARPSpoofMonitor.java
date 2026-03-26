package com.codingworld.service1.component;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ARPSpoofMonitor {

    private static final String GATEWAY_IP = "192.168.1.1";
    private static final String GATEWAY_MAC = "00-1e-a6-81-b8-98";
    private static final Set<String> trustedMACs = new HashSet<>(Arrays.asList("00:1e:a6:81:b8:98"));

    private static final Pattern ARP_PATTERN =
            Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+([0-9a-fA-F-]+)");

    // Uncomment @Scheduled to enable periodic ARP cache monitoring
    // @Scheduled(fixedRate = 5000)
    public void monitorARPCache() throws IOException {
        if (checkARPCache()) {
            System.out.println("[ALERT] ARP Spoofing Detected!");
        }
    }

    private static boolean checkARPCache() throws IOException {
        Process proc = Runtime.getRuntime().exec("arp -a");
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        boolean isSpoofed = false;

        while ((line = reader.readLine()) != null) {
            try {
                if (line.contains(GATEWAY_IP)) {
                    String mac = extractMAC(line);
                    if (mac != null && !mac.equalsIgnoreCase(GATEWAY_MAC.replace("-", ":"))) {
                        isSpoofed = true;
                        System.out.println("Spoofed MAC: " + mac);
                    }
                }

                String mac = extractMAC(line);
                if (mac != null && !trustedMACs.contains(mac)) {
                    System.out.println("[WARNING] Unknown device: " + line);
                }
            } catch (Exception e) {
                System.err.println("Error parsing ARP line: " + line);
            }
        }
        return isSpoofed;
    }

    private static String extractMAC(String arpLine) {
        Matcher matcher = ARP_PATTERN.matcher(arpLine);
        if (matcher.find()) {
            return matcher.group(2).replace("-", ":").toLowerCase();
        }
        return null;
    }
}

