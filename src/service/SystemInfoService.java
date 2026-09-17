package service;

import util.AppLogger;
import util.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private volatile long systemBootMillis = -1;
    private volatile boolean systemBootFetched;

    public void prepare() {
        if (isPrepared) return;
        isPrepared = true;

        new Thread(() -> {
            systemBootMillis = fetchSystemBootMillis();
            systemBootFetched = true;
            if (!ConfigManager.isInternetEnabled()) return;
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
        if (!systemBootFetched) return "Loading...";
        if (systemBootMillis <= 0) return "Unavailable";
        return formatUptime(System.currentTimeMillis() - systemBootMillis);
    }

    private long fetchSystemBootMillis() {
        try {
            Process process = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-Command",
                    "(Get-CimInstance Win32_OperatingSystem).LastBootUpTime.ToFileTimeUtc()"
            ).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) {
                    return Long.parseLong(line) / 10000L - 11644473600000L;
                }
            }
        } catch (Exception e) {
            AppLogger.error("SystemInfoService: failed to fetch system boot time - " + e.getMessage());
        }
        return -1;
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
                    "powershell.exe", "-NoProfile", "-Command",
                    "(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null } | Select-Object -First 1).IPv4DefaultGateway.NextHop"
            ).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")) return line;
            }
        } catch (Exception e) {
            AppLogger.error("SystemInfoService: failed to fetch gateway IP - " + e.getMessage());
        }
        return "Unavailable";
    }

    private String fetchDnsServers() {
        try {
            Process process = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-Command",
                    "(Get-DnsClientServerAddress -AddressFamily IPv4 | Where-Object { $_.ServerAddresses.Count -gt 0 } | Select-Object -First 1).ServerAddresses -join ', '"
            ).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("[0-9a-fA-F:.]+(,\\s*[0-9a-fA-F:.]+)*")) return line;
            }
        } catch (Exception e) {
            AppLogger.error("SystemInfoService: failed to fetch DNS servers - " + e.getMessage());
        }
        return "Unavailable";
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