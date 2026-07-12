package service;

import util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SystemInfoService {

    private static final long APP_START_TIME = System.currentTimeMillis();

    private String cachedPublicIP;
    private String cachedLocalIP;
    private String cachedMac;
    private String cachedGatewayIp;
    private String cachedDnsServers;
    private boolean isPrepared;

    public void prepare() {
        if (isPrepared) return;
        isPrepared = true;

        new Thread(() -> {
            try {
                cachedPublicIP = fetchPublicIP();
                cachedLocalIP = fetchLocalIP();
                cachedMac = fetchMac();
                cachedGatewayIp = fetchGatewayIp();
                cachedDnsServers = fetchDnsServers();
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

    public String getCachedGatewayIp() {
        return cachedGatewayIp;
    }

    public String getCachedDnsServers() {
        return cachedDnsServers;
    }

    public String getArchitecture() {
        return System.getProperty("os.arch");
    }

    public String getAppUptime() {
        long elapsed = System.currentTimeMillis() - APP_START_TIME;
        return formatUptime(elapsed);
    }

    public String getSystemUptime() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            Method m = osBean.getClass().getMethod("getSystemUptime");
            long uptime = (long) m.invoke(osBean);
            return formatUptime(uptime);
        } catch (Exception e1) {
            try {
                Process process = new ProcessBuilder(
                        "cmd.exe", "/c", "wmic", "os", "get", "lastbootuptime"
                ).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.contains("LastBootUpTime") || line.contains("lastbootuptime")) continue;
                    if (line.length() >= 14) {
                        try {
                            java.time.LocalDateTime boot = java.time.LocalDateTime.of(
                                    Integer.parseInt(line.substring(0, 4)),
                                    Integer.parseInt(line.substring(4, 6)),
                                    Integer.parseInt(line.substring(6, 8)),
                                    Integer.parseInt(line.substring(8, 10)),
                                    Integer.parseInt(line.substring(10, 12)),
                                    Integer.parseInt(line.substring(12, 14))
                            );
                            long diff = java.time.Duration.between(boot, java.time.LocalDateTime.now()).toMillis();
                            return formatUptime(diff);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e2) {
                return "Unavailable";
            }
            return "Unavailable";
        }
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

    private String fetchGatewayIp() {
        try {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "wmic", "path",
                    "Win32_NetworkAdapterConfiguration",
                    "where", "IPEnabled=true",
                    "get", "DefaultIPGateway", "/format:csv"
            ).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Node") || line.startsWith("DefaultIPGateway")) continue;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String gw = parts[1].trim();
                    if (!gw.isEmpty()) return gw;
                }
            }
            return "Unavailable";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String fetchDnsServers() {
        try {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "wmic", "path",
                    "Win32_NetworkAdapterConfiguration",
                    "where", "IPEnabled=true",
                    "get", "DNSServerSearchOrder", "/format:csv"
            ).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Node") || line.startsWith("DNSServerSearchOrder")) continue;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String dns = parts[1].trim();
                    if (!dns.isEmpty()) return dns.replaceAll("\\{([^}]+)\\}", "$1");
                }
            }
            return "Unavailable";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String formatUptime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
