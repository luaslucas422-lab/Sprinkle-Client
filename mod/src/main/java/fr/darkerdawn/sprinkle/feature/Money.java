package fr.darkerdawn.sprinkle.feature;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure helpers for reading and showing Donut SMP money (no Minecraft classes, unit tested). */
public final class Money {
    private Money() {}

    private static final Pattern AMOUNT = Pattern.compile("\\$?\\s*(-?\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmMbBtTqQ])?(?![\\w])");
    private static final String[] SUFFIX = {"", "K", "M", "B", "T", "Q"};
    public static final double[] MILESTONES = {1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12};

    /** True when a sidebar line looks like the money line. */
    public static boolean isMoneyLine(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return l.contains("money") || l.contains("balance") || l.contains("bal:") || l.contains("$");
    }

    /** Reads "$1.25M", "Money: 12,500", "1.2b" ... Returns -1 when there's no number. */
    public static double parse(String line) {
        String clean = line.replaceAll("§.", "");
        int dollar = clean.indexOf('$');
        String search = dollar >= 0 ? clean.substring(dollar) : clean;
        Matcher m = AMOUNT.matcher(search);
        if (!m.find()) return -1;
        try {
            double v = Double.parseDouble(m.group(1).replace(",", ""));
            String s = m.group(2);
            if (s != null) {
                switch (Character.toUpperCase(s.charAt(0))) {
                    case 'K' -> v *= 1e3;
                    case 'M' -> v *= 1e6;
                    case 'B' -> v *= 1e9;
                    case 'T' -> v *= 1e12;
                    case 'Q' -> v *= 1e15;
                    default -> { }
                }
            }
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 1234567 -> "1.23M". */
    public static String format(double v) {
        boolean neg = v < 0;
        double a = Math.abs(v);
        int i = 0;
        while (a >= 1000 && i < SUFFIX.length - 1) {
            a /= 1000;
            i++;
        }
        String num = i == 0 ? String.valueOf((long) Math.floor(a)) : (a >= 100 ? String.format(Locale.ROOT, "%.0f", a) : a >= 10 ? String.format(Locale.ROOT, "%.1f", a) : String.format(Locale.ROOT, "%.2f", a));
        if (num.contains(".")) num = num.replaceAll("0+$", "").replaceAll("\\.$", "");
        return (neg ? "-" : "") + "$" + num + SUFFIX[i];
    }

    /** The biggest milestone at or below this amount (0 if none). */
    public static double milestone(double v) {
        double best = 0;
        for (double m : MILESTONES) if (v >= m) best = m;
        return best;
    }

    public static String milestoneTitle(double m) {
        if (m >= 1e12) return "TRILLIONAIRE!";
        if (m >= 1e9) return "BILLIONAIRE!";
        if (m >= 1e6) return "MILLIONAIRE!";
        return format(m) + " CLUB!";
    }
}
