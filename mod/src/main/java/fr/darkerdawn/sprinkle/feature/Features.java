package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Sounds;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** The Donut mods: a name, what it does, an on/off switch and its own keybind. */
public final class Features {
    private Features() {}

    public static final class Feature {
        public final String id, name, desc;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        public KeyBinding key;

        Feature(String id, String name, String desc, BooleanSupplier get, Consumer<Boolean> set) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.get = get;
            this.set = set;
        }

        public boolean on() {
            return get.getAsBoolean();
        }

        public void toggle() {
            set.accept(!on());
            Config.save();
        }
    }

    public static final List<Feature> ALL = new ArrayList<>();
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Sounds.id("sprinkle"));
    public static KeyBinding openMenu, editScoreboard, setBase, soundboard;

    static {
        Config c = Config.get();
        add("money_hud", "Money HUD", "Shows your balance and how much you made today", () -> c.moneyHud, v -> c.moneyHud = v);
        add("fake_scoreboard", "Fake Scoreboard", "Your own Donut sidebar with any stats (not real!)", () -> c.fakeScoreboard, v -> c.fakeScoreboard = v);
        add("streamer_mode", "Streamer Mode", "Hides coords in F3, chat and on your HUD", () -> c.streamerMode, v -> c.streamerMode = v);
        add("pvp_hud", "PvP HUD", "Combat-tag timer, totem pops and armour durability", () -> c.pvpHud, v -> c.pvpHud = v);
        add("death_saver", "Death Saver", "Saves where you died and points you back", () -> c.deathSaver, v -> c.deathSaver = v);
        add("spawner_calc", "Spawner Calculator", "Shows what spawners in your inventory are worth", () -> c.spawnerCalc, v -> c.spawnerCalc = v);
        add("base_alarm", "Base Alarm", "Pings you when a player is near your base (may be seen as a radar!)", () -> c.baseAlarm, v -> c.baseAlarm = v);
        add("rich_flex", "Rich Flex", "Big celebration when you hit a money milestone", () -> c.richFlex, v -> c.richFlex = v);
        add("auction_prices", "Auction Prices", "Shows what an item goes for on the auction house", () -> c.auctionPrices, v -> c.auctionPrices = v);
        add("shulker_preview", "Shulker Preview", "Hold Shift over a shulker box to see what's inside", () -> c.shulkerTooltip, v -> c.shulkerTooltip = v);
        add("map_preview", "Map Preview", "Hover over a map to see it", () -> c.mapTooltip, v -> c.mapTooltip = v);
        add("anti_leak", "Anti-Leak", "No random block rotation, so screenshots can't find your base",
                () -> c.antiLeak, v -> { c.antiLeak = v; AntiLeak.changed(); });
        add("sky_changer", "Sky Changer", "Uses your own picture as the sky", () -> c.skyChanger, v -> c.skyChanger = v);
    }

    private static void add(String id, String name, String desc, BooleanSupplier get, Consumer<Boolean> set) {
        ALL.add(new Feature(id, name, desc, get, set));
    }

    public static Feature byId(String id) {
        for (Feature f : ALL) if (f.id.equals(id)) return f;
        throw new IllegalArgumentException(id);
    }

    public static void registerKeys() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sprinkleclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        editScoreboard = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sprinkleclient.edit_scoreboard", InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
        soundboard = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sprinkleclient.soundboard", InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
        setBase = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sprinkleclient.set_base", InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
        for (Feature f : ALL) {
            f.key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sprinkleclient.toggle." + f.id, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
        }
    }

    /** Called every client tick (in game). */
    public static void handleKeys(MinecraftClient client) {
        for (Feature f : ALL) {
            while (f.key.wasPressed()) {
                f.toggle();
                Sounds.play(Sounds.POP, f.on() ? 1.2f : 0.8f, 0.7f);
                if (client.inGameHud != null) {
                    client.inGameHud.setOverlayMessage(Text.literal(f.name + ": " + (f.on() ? "§aON" : "§cOFF")), false);
                }
            }
        }
    }

    /** "Key: R" or "No key (set in Controls)" for the mods menu. */
    public static String keyName(KeyBinding key) {
        if (key == null || key.isUnbound()) return "No key";
        return key.getBoundKeyLocalizedText().getString();
    }
}
