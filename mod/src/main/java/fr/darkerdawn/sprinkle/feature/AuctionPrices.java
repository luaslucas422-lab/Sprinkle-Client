package fr.darkerdawn.sprinkle.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.SprinkleClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auction Prices: adds "Auction: $X" under an item's tooltip, using the official DonutSMP API
 * (cheapest active listing of the same item, per item). Needs your API key from /api in game.
 */
public final class AuctionPrices {
    private AuctionPrices() {}

    private static final String API = "https://api.donutsmp.net/v1/auction/list/1";
    private static final long CACHE_MS = 5 * 60_000, FAIL_MS = 60_000;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private record Entry(double perItem, long at, boolean loading, boolean failed) {}

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static long apiArmedUntil;
    private static final Pattern KEY = Pattern.compile("\\b([A-Za-z0-9_-]{24,})\\b");

    public static void tooltip(ItemStack stack, List<Text> lines) {
        Config c = Config.get();
        if (!c.auctionPrices || stack.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (c.donutApiKey == null || c.donutApiKey.isBlank()) {
            lines.add(Text.literal("§6Auction: §7type /api on Donut SMP to turn on"));
            return;
        }
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        Entry e = CACHE.get(id);
        long now = System.currentTimeMillis();
        if (e == null || (!e.loading && now - e.at > (e.failed ? FAIL_MS : CACHE_MS))) {
            CACHE.put(id, new Entry(-1, now, true, false));
            fetch(id, c.donutApiKey);
            e = CACHE.get(id);
        }
        if (e.loading) {
            lines.add(Text.literal("§6Auction: §7checking..."));
        } else if (e.failed) {
            lines.add(Text.literal("§6Auction: §7couldn't reach the API"));
        } else if (e.perItem < 0) {
            lines.add(Text.literal("§6Auction: §7none listed"));
        } else {
            int n = stack.getCount();
            String text = "§6Auction: §f" + Money.format(e.perItem) + (n > 1 ? " each §7(" + Money.format(e.perItem * n) + " for " + n + ")" : "") + " §8lowest";
            lines.add(Text.literal(text));
        }
    }

    private static void fetch(String id, String key) {
        String search = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        String body = "{\"search\":\"" + search.replace("\"", "") + "\",\"sort\":\"lowest_price\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(API))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + key.trim())
                .header("Content-Type", "application/json")
                .header("User-Agent", "SprinkleClient/" + SprinkleClient.VERSION)
                .method("GET", HttpRequest.BodyPublishers.ofString(body))
                .build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((res, err) -> {
            long now = System.currentTimeMillis();
            if (err != null || res.statusCode() != 200) {
                SprinkleClient.LOG.debug("Auction lookup for {} failed: {}", id, err != null ? err.toString() : res.statusCode());
                CACHE.put(id, new Entry(-1, now, false, true));
                return;
            }
            try {
                CACHE.put(id, new Entry(lowest(res.body(), id), now, false, false));
            } catch (Exception ex) {
                CACHE.put(id, new Entry(-1, now, false, true));
            }
        });
    }

    /** Cheapest price per item among listings of exactly this item id. Package-visible for tests. */
    static double lowest(String json, String id) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray result = root.getAsJsonArray("result");
        double best = -1;
        if (result == null) return best;
        for (JsonElement el : result) {
            if (el == null || el.isJsonNull()) continue;
            JsonObject o = el.getAsJsonObject();
            JsonObject item = o.getAsJsonObject("item");
            if (item == null || !item.has("id") || !id.equals(item.get("id").getAsString())) continue;
            double price = o.get("price").getAsDouble();
            int count = item.has("count") ? Math.max(1, item.get("count").getAsInt()) : 1;
            double per = price / count;
            if (best < 0 || per < best) best = per;
        }
        return best;
    }

    /** You typed /api: grab the key from the server's reply. */
    public static void onCommand(String command) {
        if (command.trim().equalsIgnoreCase("api") || command.trim().toLowerCase().startsWith("api ")) {
            apiArmedUntil = System.currentTimeMillis() + 15_000;
        }
    }

    public static void onGameMessage(Text message, boolean overlay) {
        if (System.currentTimeMillis() > apiArmedUntil) return;
        String s = message.getString();
        Matcher m = KEY.matcher(s);
        while (m.find()) {
            String k = m.group(1);
            if (k.chars().anyMatch(Character::isDigit) && k.chars().anyMatch(Character::isLetter)) {
                Config.get().donutApiKey = k;
                Config.save();
                CACHE.clear();
                apiArmedUntil = 0;
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§9[Sprinkle] §aAuction Prices is set up! Hover over items to see what they go for."));
                return;
            }
        }
    }
}
