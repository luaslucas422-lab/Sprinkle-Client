package fr.darkerdawn.sprinkle;

import java.util.List;
import java.util.Random;

/** All the Donut SMP jokes. Fan-made and friendly; the creators named here have nothing to do with Sprinkle Client. */
public final class Jokes {
    private Jokes() {}

    private static final Random RNG = new Random();

    public static final List<String> SPLASHES = List.of(
            "Selling dirt for $1!",
            "Who stole my spawners?!",
            "/ah is down again",
            "Now with 100% more donuts!",
            "Trust me bro, I'm not a scammer",
            "DrDonut's forehead has its own biome!",
            "Jettism: base bet? BASE BET!",
            "Farex pulled the pearl again!",
            "Jesticc built this splash text",
            "LethalLuke trapped this splash",
            "One more keyall, I promise",
            "My base is hidden. Totally.",
            "Rich? Check /baltop. Not you.",
            "Spawners go brrrr",
            "Glazed and confused",
            "Sprinkles > shards",
            "Don't trade with randoms!",
            "Pay me back, it's been 3 days",
            "Totem? Never heard of her",
            "Coords leaked. Again.",
            "Back to 0 on Monday!",
            "Ender pearls are a personality",
            "Made by DarkerDawnFR!",
            "The donut is a lie",
            "Rage quit speedrun any%"
    );

    public static final List<String> LOADING_LINES = List.of(
            "Selling your dirt...",
            "Glazing the donuts...",
            "Hiding your base coords...",
            "Counting DrDonut's money...",
            "Asking Jettism for a base bet...",
            "Letting Farex pull the pearl...",
            "Waiting for Jesticc to finish the build...",
            "Checking for LethalLuke traps...",
            "Stacking spawners...",
            "Paying back the guy you owe...",
            "Polishing your totems...",
            "Adding sprinkles..."
    );

    public static final List<String> TIPS = List.of(
            "Tip: Never tell anyone your base coords. Not even your mum.",
            "Tip: DrDonut owns the server. Be nice to the donut.",
            "Tip: Jettism would bet your base. Would you?",
            "Tip: Farex says always carry a pearl.",
            "Tip: Streamer mode (in Sprinkle settings) hides your coords.",
            "Tip: Spawners make money while you sleep.",
            "Tip: The Money HUD shows how much you made today.",
            "Tip: Rage quitting is a valid PvP strategy (it isn't).",
            "Tip: Double-check every trade. Scammers love new players."
    );

    public static final List<String> CONFIRM_LINES = List.of(
            "Are you ready to lose everything?",
            "Your base will be found. Continue?",
            "Spawners won't sell themselves. Join?",
            "DrDonut is waiting. Ready?"
    );

    public static final List<String> DEATH_LINES = List.of(
            "Your stuff is gone. DrDonut sends his condolences.",
            "Someone is wearing your armour right now.",
            "F in the chat.",
            "Should've carried a totem.",
            "Back to selling dirt.",
            "That's a rage quit if I've ever seen one."
    );

    /** Credits screen: name, line. */
    public static final List<String[]> THANKS = List.of(
            new String[]{"DrDonut", "Owner of Donut SMP. Without him, no donuts."},
            new String[]{"Jettism", "Base bets and giveaways."},
            new String[]{"Farex", "Professional pearl puller."},
            new String[]{"Jesticc", "Big builds, bigger streams."},
            new String[]{"LethalLuke", "Trapped everyone. Probably you."},
            new String[]{"You", "For losing everything and coming back."}
    );

    /** Logo easter egg lines. */
    public static final List<String> EGG_LINES = List.of(
            "DONUT RAIN!",
            "DrDonut approves!",
            "+$1,000,000 (not real)",
            "Jettism bet you couldn't find this"
    );

    public static String pick(List<String> list) {
        return list.get(RNG.nextInt(list.size()));
    }
}
