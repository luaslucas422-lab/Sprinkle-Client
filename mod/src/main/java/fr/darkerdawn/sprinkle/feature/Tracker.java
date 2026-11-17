package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Jokes;
import fr.darkerdawn.sprinkle.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Text;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Everything the Donut mods keep track of while you play. */
public final class Tracker {
    private Tracker() {}

    // Money HUD / Rich Flex
    public static double money = -1;
    public static long flexUntil;
    public static String flexTitle = "";

    // PvP HUD
    public static long combatUntil;
    public static final Map<String, Integer> pops = new LinkedHashMap<>();
    public static final Map<String, Long> popTime = new HashMap<>();

    // Death Saver
    private static boolean deathRecorded;
    public static String deathLine = "";

    // Base Alarm
    private static final Map<String, Long> alarmCooldown = new HashMap<>();
    private static int tickCount;

    public static void reset() {
        money = -1;
        combatUntil = 0;
        pops.clear();
        popTime.clear();
        alarmCooldown.clear();
    }

    public static void tick(MinecraftClient c) {
        if (c.player == null || c.world == null) return;
        tickCount++;
        Config cfg = Config.get();
        long now = System.currentTimeMillis();

        // ---------------- money
        if (tickCount % 10 == 0) {
            double m = readMoney(c);
            if (m >= 0) onMoney(cfg, m, now);
        }

        // ---------------- combat tag: you got hit by a player
        if (c.player.hurtTime == 9) {
            var src = c.player.getRecentDamageSource();
            if (src != null && src.getAttacker() instanceof PlayerEntity) tag();
        }
        // forget old totem pops
        popTime.entrySet().removeIf(e -> {
            boolean old = now - e.getValue() > 60_000;
            if (old) pops.remove(e.getKey());
            return old;
        });

        // ---------------- death saver
        if (c.currentScreen instanceof DeathScreen) {
            if (!deathRecorded && cfg.deathSaver) {
                deathRecorded = true;
                Config.DeathSaverRecord r = new Config.DeathSaverRecord();
                r.x = (int) Math.floor(c.player.getX());
                r.y = (int) Math.floor(c.player.getY());
                r.z = (int) Math.floor(c.player.getZ());
                r.dimension = c.world.getRegistryKey().getValue().toString();
                r.time = now;
                cfg.deaths.add(0, r);
                while (cfg.deaths.size() > 10) cfg.deaths.remove(cfg.deaths.size() - 1);
                Config.save();
                deathLine = Jokes.pick(Jokes.DEATH_LINES);
                combatUntil = 0;
            }
        } else if (c.player.isAlive()) {
            deathRecorded = false;
        }
        // reached your death spot: clear the waypoint
        Config.DeathSaverRecord last = lastDeathHere(c);
        if (last != null && tickCount % 20 == 0) {
            double dx = last.x + 0.5 - c.player.getX(), dz = last.z + 0.5 - c.player.getZ();
            if (dx * dx + dz * dz < 16) {
                cfg.deaths.remove(last);
                Config.save();
                c.inGameHud.setOverlayMessage(Text.literal("§bYou made it back to where you died. Good luck finding your stuff."), false);
            }
        }

        // ---------------- base alarm
        if (cfg.baseAlarm && cfg.baseX != null && tickCount % 10 == 0
                && c.world.getRegistryKey().getValue().toString().equals(cfg.baseDimension)) {
            double r2 = (double) cfg.baseAlarmRadius * cfg.baseAlarmRadius;
            for (AbstractClientPlayerEntity p : c.world.getPlayers()) {
                if (p == c.player) continue;
                String name = p.getName().getString();
                if (cfg.baseAlarmIgnore.stream().anyMatch(n -> n.equalsIgnoreCase(name))) continue;
                double dx = p.getX() - (cfg.baseX + 0.5), dz = p.getZ() - (cfg.baseZ + 0.5);
                if (dx * dx + dz * dz > r2) continue;
                Long last2 = alarmCooldown.get(name);
                if (last2 != null && now - last2 < 60_000) continue;
                alarmCooldown.put(name, now);
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                Sounds.play(Sounds.ALARM, 1.0f, 1.0f);
                c.inGameHud.setOverlayMessage(Text.literal("§c§l⚠ " + name + " is near your base (" + dist + "m)"), false);
                c.inGameHud.getChatHud().addMessage(Text.literal("§9[Sprinkle] §cBase Alarm: §f" + name + " §7is " + dist + "m from your base."));
            }
        }
    }

    private static void onMoney(Config cfg, double m, long now) {
        String today = LocalDate.now().toString();
        if (!today.equals(cfg.moneyDay) || cfg.moneyDayStart < 0) {
            cfg.moneyDay = today;
            cfg.moneyDayStart = m;
            Config.save();
        }
        double best = Money.milestone(m);
        if (money >= 0 && best > cfg.moneyBestMilestone && best > Money.milestone(money)) {
            if (cfg.richFlex) {
                flexTitle = Money.milestoneTitle(best);
                flexUntil = now + 5000;
                Sounds.play(Sounds.FLEX, 1.0f, 1.0f);
            }
        }
        if (best > cfg.moneyBestMilestone) {
            cfg.moneyBestMilestone = best;
            Config.save();
        }
        money = m;
    }

    /** Finds the money line on the real sidebar (the server's scoreboard). */
    public static double readMoney(MinecraftClient c) {
        Scoreboard sb = c.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return -1;
        for (ScoreboardEntry e : sb.getScoreboardEntries(obj)) {
            if (e.hidden()) continue;
            Text name = e.name();
            Team team = sb.getScoreHolderTeam(name.getString());
            String line = Team.decorateName(team, name).getString() + " " + e.formatted(obj.getNumberFormatOr(BlankNumberFormat.INSTANCE)).getString();
            if (Money.isMoneyLine(line)) {
                double v = Money.parse(line);
                if (v >= 0) return v;
            }
        }
        return -1;
    }

    public static void tag() {
        combatUntil = System.currentTimeMillis() + Config.get().combatTagSeconds * 1000L;
    }

    /** Called from the packet mixin when any entity uses a totem. */
    public static void totemPop(Entity e) {
        if (!(e instanceof PlayerEntity p)) return;
        String name = p.getName().getString();
        pops.merge(name, 1, Integer::sum);
        popTime.put(name, System.currentTimeMillis());
        MinecraftClient c = MinecraftClient.getInstance();
        if (Config.get().pvpHud && p != c.player) Sounds.play(Sounds.POP, 1.4f, 0.5f);
    }

    public static Config.DeathSaverRecord lastDeathHere(MinecraftClient c) {
        Config cfg = Config.get();
        if (!cfg.deathSaver || cfg.deaths.isEmpty() || c.world == null) return null;
        Config.DeathSaverRecord r = cfg.deaths.get(0);
        return c.world.getRegistryKey().getValue().toString().equals(r.dimension) ? r : null;
    }

    /** Game messages from the server: Donut's action-bar combat message also starts the timer. */
    public static void onGameMessage(Text message, boolean overlay) {
        String s = message.getString().toLowerCase();
        if (s.contains("combat") && !s.contains("no longer") && !s.contains("left combat") && !s.contains("out of combat")) tag();
        if (s.contains("no longer in combat") || s.contains("out of combat") || s.contains("left combat")) combatUntil = 0;
    }
}
