package fr.darkerdawn.sprinkle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything the settings gear can change. Saved in config/sprinkleclient.json. */
public final class Config {
    public enum JoinStyle {
        LOADING("Funny loading screen"), INSTANT("Instant join"), CONFIRM("Are you sure? popup");
        public final String label;
        JoinStyle(String label) { this.label = label; }
        public JoinStyle next() { return values()[(ordinal() + 1) % values().length]; }
    }

    // Menu
    public boolean vanillaMenu = false;
    public JoinStyle joinStyle = JoinStyle.LOADING;
    public boolean splashes = true;
    public boolean themedPause = true;
    public boolean backgroundSlideshow = true;
    public String serverIp = "donutsmp.net";
    public String discordUrl = "https://discord.gg/donutsmp";

    // Sounds
    public boolean cashClick = true;
    public boolean menuMusic = true;
    public boolean rageQuitSound = true;

    // Donut mods
    public boolean moneyHud = true;
    public boolean streamerMode = false;
    public boolean pvpHud = true;
    public boolean deathSaver = true;
    public boolean spawnerCalc = true;
    public boolean baseAlarm = false; // off: Donut SMP may count proximity alerts as a radar
    public boolean richFlex = true;
    public boolean showDeathWaypoint = true;
    public boolean skyChanger = true; // only does something once a picture is in the sky folder
    public boolean fakeScoreboard = false;
    public boolean antiLeak = true;
    public List<String> antiLeakBlocks = new ArrayList<>(DEFAULT_ANTI_LEAK);
    public boolean menuVideo = true;
    public boolean auctionPrices = true;
    public boolean shulkerTooltip = true;
    public boolean mapTooltip = true;
    public String donutApiKey = "";
    public boolean fakeOldStyle = false;
    public FakeBoard fake = new FakeBoard();
    public int combatTagSeconds = 15;
    public int baseAlarmRadius = 96;
    public Integer baseX, baseZ;
    public String baseDimension;
    public List<String> baseAlarmIgnore = new ArrayList<>();

    // Money tracking
    public String moneyDay = "";
    public double moneyDayStart = -1;
    public double moneyBestMilestone = 0;

    /** Spawner sell prices per spawner, editable in the json. These are guesses, not official prices. */
    public Map<String, Double> spawnerPrices = defaultPrices();

    public List<DeathSaverRecord> deaths = new ArrayList<>();
    public int logoClicksFound = 0;

    /** What the Fake Scoreboard shows. Only drawn on your screen; nothing is sent anywhere. */
    public static final class FakeBoard {
        public String title = "DONUT SMP";
        public String name = "DrDonut";
        public String money = "$1.5T";
        public String shards = "99,999";
        public String kills = "6,767";
        public String deaths = "0";
        public String keyall = "59m 59s";
        public String playtime = "420d 6h";
        public String team = "Donut Gang";
        public String footer = "donutsmp.net";
    }

    public static final List<String> DEFAULT_ANTI_LEAK = List.of(
            "minecraft:stone", "minecraft:deepslate", "minecraft:grass_block", "minecraft:dirt", "minecraft:sand",
            "minecraft:red_sand", "minecraft:netherrack", "minecraft:bedrock", "minecraft:mycelium", "minecraft:podzol");

    public static final class DeathSaverRecord {
        public int x, y, z;
        public String dimension;
        public long time;
        public String cause;
    }

    static Map<String, Double> defaultPrices() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("skeleton", 50_000d);
        m.put("zombie", 30_000d);
        m.put("spider", 30_000d);
        m.put("cave_spider", 30_000d);
        m.put("creeper", 75_000d);
        m.put("blaze", 100_000d);
        m.put("iron_golem", 250_000d);
        m.put("pig", 10_000d);
        m.put("cow", 15_000d);
        m.put("chicken", 10_000d);
        m.put("sheep", 10_000d);
        m.put("zombified_piglin", 60_000d);
        m.put("magma_cube", 60_000d);
        m.put("slime", 60_000d);
        m.put("witch", 80_000d);
        m.put("default", 25_000d);
        return m;
    }

    // ------------------------------------------------------------------ storage

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("sprinkleclient");
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("sprinkleclient.json");
    }

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Config c = null;
        try {
            if (Files.exists(file())) c = GSON.fromJson(Files.readString(file()), Config.class);
        } catch (Exception e) {
            SprinkleClient.LOG.warn("Sprinkle config was broken, using defaults", e);
        }
        if (c == null) c = new Config();
        if (c.joinStyle == null) c.joinStyle = JoinStyle.LOADING;
        if (c.spawnerPrices == null || c.spawnerPrices.isEmpty()) c.spawnerPrices = defaultPrices();
        if (c.deaths == null) c.deaths = new ArrayList<>();
        if (c.fake == null) c.fake = new FakeBoard();
        if (c.antiLeakBlocks == null) c.antiLeakBlocks = new ArrayList<>(DEFAULT_ANTI_LEAK);
        if (c.baseAlarmIgnore == null) c.baseAlarmIgnore = new ArrayList<>();
        if (c.serverIp == null || c.serverIp.isBlank()) c.serverIp = "donutsmp.net";
        if (c.discordUrl == null || c.discordUrl.isBlank()) c.discordUrl = "https://discord.gg/donutsmp";
        instance = c;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.createDirectories(dir().resolve("backgrounds"));
            Files.createDirectories(dir().resolve("sky"));
            Files.writeString(file(), GSON.toJson(instance));
        } catch (Exception e) {
            SprinkleClient.LOG.warn("Could not save Sprinkle config", e);
        }
    }
}
