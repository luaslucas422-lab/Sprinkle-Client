package fr.darkerdawn.sprinkle.feature;

import java.util.regex.Pattern;

/** Hides coordinates in text for Streamer mode (no Minecraft classes, unit tested). */
public final class Coords {
    private Coords() {}

    private static final String N = "-?\\d+(?:\\.\\d+)?";
    // "x: 12 y: 64 z: -30", "X 12 Z -30", "12, 64, -30", "12 / 64 / -30", "12 64 -30"
    private static final Pattern LABELED = Pattern.compile("(?i)\\b([xyz])\\s*[:=]?\\s*(" + N + ")");
    private static final Pattern TRIPLE = Pattern.compile("(" + N + ")(\\s*[,/ ]\\s*)(" + N + ")(\\s*[,/ ]\\s*)(" + N + ")");
    private static final Pattern PAIR = Pattern.compile("(" + N + ")(\\s*,\\s*)(" + N + ")");

    public static boolean hasCoords(String s) {
        return LABELED.matcher(s).find() || TRIPLE.matcher(s).find();
    }

    public static String censor(String s) {
        String out = TRIPLE.matcher(s).replaceAll("???$2???$4???");
        out = LABELED.matcher(out).replaceAll("$1: ???");
        if (!out.equals(s)) out = PAIR.matcher(out).replaceAll("???$2???");
        return out;
    }
}
