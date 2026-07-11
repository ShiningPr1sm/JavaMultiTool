package service;

import util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QuoteService {

    private List<String> quotes;

    public String getDailyQuote() {
        if (quotes == null) loadQuotes();
        if (quotes.isEmpty()) return null;
        int dayOfYear = LocalDate.now().getDayOfYear();
        return quotes.get(dayOfYear % quotes.size());
    }

    private void loadQuotes() {
        quotes = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/quotes.txt")) {
            if (in == null) {
                AppLogger.info("QuoteService: quotes.txt not found");
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) quotes.add(line);
            }
            AppLogger.info("QuoteService: loaded " + quotes.size() + " quotes");
        } catch (Exception e) {
            AppLogger.error("QuoteService: failed to load quotes: " + e.getMessage());
        }
    }
}
