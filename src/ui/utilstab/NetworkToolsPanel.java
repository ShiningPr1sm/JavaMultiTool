package ui.utilstab;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class NetworkToolsPanel extends JPanel {
    private final JTextArea outputArea;

    public NetworkToolsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab(" My IP ", createMyIpPanel());
        tabs.addTab(" Ping ", createPingPanel());
        tabs.addTab(" Port Check ", createPortPanel());
        tabs.addTab(" Whois ", createWhoisPanel());

        outputArea = new JTextArea(12, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setBackground(UIStyle.SECONDARY_BG);
        outputArea.setForeground(UIStyle.TEXT_COLOR);
        outputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        add(tabs, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
    }

    private JPanel createMyIpPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        JButton checkBtn = new JButton("Check My Public IP");
        UIStyle.styleButton(checkBtn);
        checkBtn.addActionListener(e -> {
            appendOutput("Checking public IP...");
            new Thread(() -> {
                try {
                    URL url = new URL("https://api.ipify.org");
                    String ip = new String(url.openStream().readAllBytes());
                    SwingUtilities.invokeLater(() -> appendOutput("Your public IP: " + ip));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> appendOutput("Failed to get IP: " + ex.getMessage()));
                }
            }).start();
        });
        p.add(checkBtn);
        return p;
    }

    private JPanel createPingPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        JTextField hostField = new JTextField(15);
        UIStyle.styleTextField(hostField);
        hostField.setText("google.com");
        JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
        UIStyle.styleSpinner(countSpinner);
        JButton pingBtn = new JButton("Ping");
        UIStyle.styleButton(pingBtn);
        pingBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            int count = (int) countSpinner.getValue();
            appendOutput("Pinging " + host + " (" + count + " times)...");
            new Thread(() -> {
                try {
                    InetAddress addr = InetAddress.getByName(host);
                    int success = 0;
                    long totalTime = 0;
                    for (int i = 0; i < count; i++) {
                        long start = System.currentTimeMillis();
                        boolean reachable = addr.isReachable(3000);
                        long ms = System.currentTimeMillis() - start;
                        final boolean ok = reachable;
                        final long time = ms;
                        final int idx = i;
                        SwingUtilities.invokeLater(() -> {
                            if (ok) {
                                appendOutput("Reply from " + host + ": time=" + time + "ms");
                            } else {
                                appendOutput("Request timed out (" + (idx + 1) + ")");
                            }
                        });
                        if (reachable) {
                            success++;
                            totalTime += ms;
                        }
                    }
                    final int s = success;
                    final long avg = success > 0 ? totalTime / success : 0;
                    SwingUtilities.invokeLater(() -> {
                        appendOutput("--- " + host + " ping statistics ---");
                        appendOutput(s + " packets transmitted, " + s + " received, "
                                + ((count - s) * 100 / count) + "% packet loss");
                        if (s > 0) appendOutput("avg time: " + avg + "ms");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> appendOutput("Ping failed: " + ex.getMessage()));
                }
            }).start();
        });
        p.add(new JLabel("Host:"));
        p.add(hostField);
        p.add(new JLabel("Count:"));
        p.add(countSpinner);
        p.add(pingBtn);
        return p;
    }

    private JPanel createPortPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        JTextField hostField = new JTextField(15);
        UIStyle.styleTextField(hostField);
        hostField.setText("google.com");
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(80, 1, 65535, 1));
        UIStyle.styleSpinner(portSpinner);
        JButton checkBtn = new JButton("Check Port");
        UIStyle.styleButton(checkBtn);
        checkBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port = (int) portSpinner.getValue();
            appendOutput("Checking " + host + ":" + port + "...");
            new Thread(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 5000);
                    SwingUtilities.invokeLater(() -> appendOutput("Port " + port + " is OPEN"));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> appendOutput("Port " + port + " is CLOSED or filtered"));
                }
            }).start();
        });
        p.add(new JLabel("Host:"));
        p.add(hostField);
        p.add(new JLabel("Port:"));
        p.add(portSpinner);
        p.add(checkBtn);
        return p;
    }

    private JPanel createWhoisPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        JTextField domainField = new JTextField(15);
        UIStyle.styleTextField(domainField);
        domainField.setText("google.com");
        JButton whoisBtn = new JButton("Whois");
        UIStyle.styleButton(whoisBtn);
        whoisBtn.addActionListener(e -> {
            String domain = domainField.getText().trim();
            appendOutput("Looking up whois for " + domain + "...");
            new Thread(() -> {
                try (Socket s = new Socket("whois.iana.org", 43)) {
                    s.setSoTimeout(10000);
                    PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                    pw.println(domain);
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                    String whois = result.toString();
                    SwingUtilities.invokeLater(() -> appendOutput(whois));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> appendOutput("Whois failed: " + ex.getMessage()));
                }
            }).start();
        });
        p.add(new JLabel("Domain:"));
        p.add(domainField);
        p.add(whoisBtn);
        return p;
    }

    private void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}
