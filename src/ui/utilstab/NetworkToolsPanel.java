package ui.utilstab;

import ui.UIStyle;
import util.AppLogger;
import util.ConfigManager;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
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

        tabs.addTab(" " + I18n.t("network.tab.myIp") + " ", createMyIpPanel());
        tabs.addTab(" " + I18n.t("network.tab.ping") + " ", createPingPanel());
        tabs.addTab(" " + I18n.t("network.tab.portCheck") + " ", createPortPanel());
        tabs.addTab(" " + I18n.t("network.tab.whois") + " ", createWhoisPanel());

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
        JScrollPane scrollPane = new JScrollPane(outputArea);
        UIStyle.styleScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    private ActionListener clearOutputArea(ActionListener action) {
        return e -> {
            outputArea.setText("");
            action.actionPerformed(e);
        };
    }

    private JPanel createMyIpPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        JButton checkBtn = new JButton(I18n.t("network.checkIP"));
        UIStyle.styleButton(checkBtn);
        checkBtn.addActionListener(clearOutputArea(e -> {
            if (!ConfigManager.isInternetEnabled()) { appendOutput(I18n.t("network.internetDisabled")); return; }
            appendOutput(I18n.t("network.checkingIP"));
            new Thread(() -> {
                try {
                    URL url = new URL("https://api.ipify.org");
                    String ip = new String(url.openStream().readAllBytes());
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.yourIP", ip)));
                    AppLogger.info("Public IP fetched: " + ip);
                } catch (Exception ex) {
                    AppLogger.error("Failed to get public IP: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.ipFail", ex.getMessage())));
                }
            }).start();
        }));
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
        pingBtn.addActionListener(clearOutputArea(e -> {
            if (!ConfigManager.isInternetEnabled()) { appendOutput(I18n.t("network.internetDisabled")); return; }
            String host = hostField.getText().trim();
            int count = (int) countSpinner.getValue();
            appendOutput(I18n.t("network.pinging", host, String.valueOf(count)));
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
                                appendOutput(I18n.t("network.reply", host, String.valueOf(time)));
                            } else {
                                appendOutput(I18n.t("network.timeout", String.valueOf(idx + 1)));
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
                        appendOutput(I18n.t("network.stats", host));
                        appendOutput(I18n.t("network.packets", String.valueOf(s), String.valueOf((count - s) * 100 / count)));
                        if (s > 0) appendOutput(I18n.t("network.avg", String.valueOf(avg)));
                    });
                    AppLogger.info("Ping " + host + " completed: " + s + "/" + count
                            + " received, avg " + avg + "ms");
                } catch (Exception ex) {
                    AppLogger.error("Ping " + host + " failed: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.pingFail", ex.getMessage())));
                }
            }).start();
        }));
        JLabel host = new JLabel(I18n.t("network.host"));
        host.setForeground(Color.white);
        p.add(host);
        p.add(hostField);
        JLabel count = new JLabel(I18n.t("network.count"));
        count.setForeground(Color.white);
        p.add(count);
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
        checkBtn.addActionListener(clearOutputArea(e -> {
            if (!ConfigManager.isInternetEnabled()) { appendOutput(I18n.t("network.internetDisabled")); return; }
            String host = hostField.getText().trim();
            int port = (int) portSpinner.getValue();
            appendOutput(I18n.t("network.checkingPort", host, String.valueOf(port)));
            new Thread(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 5000);
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.portOpen", String.valueOf(port))));
                    AppLogger.info("Port " + port + " on " + host + " is OPEN");
                } catch (Exception ex) {
                    AppLogger.info("Port " + port + " on " + host + " is CLOSED or filtered");
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.portClosed", String.valueOf(port))));
                }
            }).start();
        }));
        JLabel host = new JLabel(I18n.t("network.host"));
        host.setForeground(Color.WHITE);
        p.add(host);
        p.add(hostField);
        JLabel port = new JLabel(I18n.t("network.portLabel"));
        port.setForeground(Color.white);
        p.add(port);
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
        whoisBtn.addActionListener(clearOutputArea(e -> {
            if (!ConfigManager.isInternetEnabled()) { appendOutput(I18n.t("network.internetDisabled")); return; }
            String domain = domainField.getText().trim();
            appendOutput(I18n.t("network.whoisLookup", domain));
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
                    AppLogger.info("Whois lookup for " + domain + " completed ("
                            + whois.length() + " chars)");
                } catch (Exception ex) {
                    AppLogger.error("Whois lookup for " + domain + " failed: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> appendOutput(I18n.t("network.whoisFail", ex.getMessage())));
                }
            }).start();
        }));
        JLabel domain = new JLabel(I18n.t("network.domain"));
        domain.setForeground(Color.white);
        p.add(domain);
        p.add(domainField);
        p.add(whoisBtn);
        return p;
    }

    private void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}
