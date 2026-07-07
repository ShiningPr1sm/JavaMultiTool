package service;

import util.AppLogger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SystemInfoService {

    private String cachedPublicIP;
    private String cachedLocalIP;
    private String cachedMac;
    private boolean isPrepared;

    public void prepare() {
        if (isPrepared) return;
        isPrepared = true;

        new Thread(() -> {
            try {
                cachedPublicIP = fetchPublicIP();
                cachedLocalIP = fetchLocalIP();
                cachedMac = fetchMac();
                AppLogger.info("System info cached successfully.");
            } catch (Exception e) {
                AppLogger.error("Failed to cache system info: " + e.getMessage());
            }
        }).start();
    }

    public String getCachedPublicIP() {
        return cachedPublicIP;
    }

    public String getCachedLocalIP() {
        return cachedLocalIP;
    }

    public String getCachedMac() {
        return cachedMac;
    }

    public String fetchPublicIP() {
        try (Scanner s = new Scanner(new URL("https://api.ipify.org").openStream(), StandardCharsets.UTF_8)) {
            return s.useDelimiter("\\A").next();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String fetchLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String fetchMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception e) {
            return "Unavailable";
        }
        return "Unavailable";
    }
}
