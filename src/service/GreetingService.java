package service;

import util.AppLogger;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreetingService {

    private static final String[] GREETINGS = {
            "Hello", "Privet", "Hola", "Bonjour", "Konnichiwa",
            "Hallo", "Ola", "Hei", "Salut", "Ahoj", "Sveiki",
            "Annyeonghaseyo", "Kamusta", "Aloha", "Jambo"
    };

    public Font[] loadGreetingFonts() {
        Font[] base = {
                new Font("SansSerif", Font.BOLD, 24),
                new Font("Serif", Font.ITALIC, 24),
                new Font("Monospaced", Font.PLAIN, 24),
                new Font("Dialog", Font.PLAIN, 24)
        };

        try (InputStream is = GreetingService.class.getResourceAsStream("/fonts/minecraft.ttf")) {
            if (is != null) {
                Font custom = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(24f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(custom);
                Font[] result = new Font[base.length + 1];
                System.arraycopy(base, 0, result, 0, base.length);
                result[base.length] = custom;
                AppLogger.info("Successfully loaded Minecraft font!");
                return result;
            }
        } catch (Exception e) {
            AppLogger.error("Minecraft FONT error: " + e.getMessage());
        }

        return base;
    }

    public Font selectBestFontForGreeting(Font[] fonts, String text) {
        List<Font> candidates = new ArrayList<>();
        for (Font font : fonts) {
            if (font.canDisplayUpTo(text) == -1) {
                candidates.add(font.deriveFont(24f));
            }
        }
        if (candidates.isEmpty()) {
            return new Font("Dialog", Font.PLAIN, 24);
        }
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    public String randomGreeting() {
        return GREETINGS[new Random().nextInt(GREETINGS.length)];
    }
}
