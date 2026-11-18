package fr.darkerdawn.sprinkle;

import fr.darkerdawn.sprinkle.feature.Features;
import fr.darkerdawn.sprinkle.feature.Money;
import fr.darkerdawn.sprinkle.feature.Tracker;
import fr.darkerdawn.sprinkle.screen.AntiLeakScreen;
import fr.darkerdawn.sprinkle.screen.CreditsScreen;
import fr.darkerdawn.sprinkle.screen.SoundboardScreen;
import fr.darkerdawn.sprinkle.screen.JoinLoadingScreen;
import fr.darkerdawn.sprinkle.screen.ModsScreen;
import fr.darkerdawn.sprinkle.screen.ScoreboardEditorScreen;
import fr.darkerdawn.sprinkle.screen.SettingsScreen;
import fr.darkerdawn.sprinkle.screen.SprinkleTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;

/** Automatic test run used by the build server only (-Dsprinkle.smoke=true). Never runs for players. */
final class Smoke {
    private Smoke() {}

    static final boolean ON = Boolean.getBoolean("sprinkle.smoke");
    private static int ticks, worldTicks, titleTicks = -1, fails;
    private static boolean loadingShot;

    private static void shot(MinecraftClient c, String what) {
        System.out.println("[SPRINKLE-SMOKE] screenshot " + what);
        ScreenshotRecorder.saveScreenshot(c.runDirectory, c.getFramebuffer(), t -> {});
    }

    private static void check(String what, boolean ok) {
        System.out.println("[SPRINKLE-SMOKE] " + (ok ? "PASS " : "FAIL ") + what);
        if (!ok) fails++;
    }

    private static void makeTestSky() {
        try {
            Files.createDirectories(SkyChanger.dir());
            BufferedImage img = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setPaint(new GradientPaint(0, 0, new Color(255, 120, 200), 0, 512, new Color(40, 0, 120)));
            g.fillRect(0, 0, 1024, 512);
            g.setColor(Color.WHITE);
            for (int x = 0; x < 1024; x += 64) g.fillRect(x, 0, 6, 512);
            g.dispose();
            ImageIO.write(img, "png", SkyChanger.dir().resolve("smoke_sky.png").toFile());
        } catch (Exception e) {
            check("make test sky: " + e, false);
        }
    }

    static void tick(MinecraftClient c) {
        ticks++;
        if (c.getOverlay() != null) {
            if (!loadingShot && ticks > 30) { loadingShot = true; shot(c, "loading"); }
            return;
        }
        if (titleTicks >= 0) { title(c); return; }
        if (c.player == null || c.world == null) {
            if (ticks == 1500) { System.out.println("[SPRINKLE-SMOKE] no world"); titleTicks = 0; }
            return;
        }
        worldTicks++;
        Config cfg = Config.get();
        switch (worldTicks) {
            case 20 -> {
                check("Donut SMP pack enabled", c.getResourcePackManager().getEnabledIds().stream().anyMatch(id -> id.contains("donut_smp")));
                check("Donut Stone pack enabled", Packs.enabled("donut_stone"));
                check("Anti-Leak blocks stone", fr.darkerdawn.sprinkle.feature.AntiLeak.covers(net.minecraft.block.Blocks.STONE));
                check("Blue buttons pack enabled", c.getResourcePackManager().getEnabledIds().stream().anyMatch(id -> id.contains("sprinkle_blue")));
                check("all Donut mods registered", Features.ALL.size() == 13);
                check("money parser", Money.parse("Money $1.5M") == 1_500_000);
                makeTestSky();
                SkyChanger.reload();
                check("sky picture loaded", SkyChanger.hasSky());
                cfg.fakeScoreboard = true;
                cfg.streamerMode = true;
                cfg.moneyDay = java.time.LocalDate.now().toString();
                cfg.moneyDayStart = 900_000;
                Tracker.money = 3_400_000;
                Tracker.tag();
                Tracker.pops.put("Jettism", 2);
                Tracker.popTime.put("Jettism", System.currentTimeMillis());
                Config.DeathSaverRecord r = new Config.DeathSaverRecord();
                r.x = (int) c.player.getX() + 40; r.y = (int) c.player.getY(); r.z = (int) c.player.getZ() + 80;
                r.dimension = c.world.getRegistryKey().getValue().toString();
                r.time = System.currentTimeMillis();
                cfg.deaths.add(0, r);
                c.player.setYaw(0);
                c.player.setPitch(0);
                var srv = c.getServer();
                if (srv != null) srv.execute(() -> {
                    for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
                        p.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                        p.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                        p.getInventory().setStack(0, new ItemStack(Items.SPAWNER, 12));
                        ServerWorld w = srv.getOverworld();
                        w.setTimeOfDay(6000);
                        for (int i = -3; i <= 3; i++) {
                            w.setBlockState(net.minecraft.util.math.BlockPos.ofFloored(p.getX() + i, p.getY(), p.getZ() + 7), net.minecraft.block.Blocks.STONE.getDefaultState());
                            w.setBlockState(net.minecraft.util.math.BlockPos.ofFloored(p.getX() + i, p.getY() + 1, p.getZ() + 7), net.minecraft.block.Blocks.DEEPSLATE.getDefaultState());
                        }
                        for (int i = 0; i < 3; i++) {
                            var pig = fr.darkerdawn.sprinkle.compat.Compat.spawnTestPig(w);
                            if (pig instanceof MobEntity m) {
                                m.refreshPositionAndAngles(p.getX() - 2 + i * 2, p.getY(), p.getZ() + 4, 180, 0);
                                m.setAiDisabled(true);
                                w.spawnEntity(m);
                            }
                        }
                    }
                });
            }
            case 80 -> shot(c, "in-game hud with fake scoreboard and donut pigs");
            case 82 -> c.player.setPitch(-60);
            case 95 -> shot(c, "sky changer");
            case 97 -> { c.player.setPitch(0); c.setScreen(new ModsScreen(null)); }
            case 115 -> shot(c, "donut mods menu");
            case 117 -> c.setScreen(new ScoreboardEditorScreen(null));
            case 135 -> shot(c, "fake scoreboard editor");
            case 137 -> { cfg.fakeOldStyle = true; c.setScreen(null); }
            case 150 -> shot(c, "old style scoreboard");
            case 152 -> c.setScreen(new GameMenuScreen(true));
            case 170 -> shot(c, "pause menu");
            case 171 -> c.setScreen(new TooltipTest());
            case 178 -> shot(c, "shulker preview tooltip");
            case 179 -> { c.setScreen(null); Tracker.flexTitle = Money.milestoneTitle(1e6); Tracker.flexUntil = System.currentTimeMillis() + 4000; }
            case 186 -> shot(c, "rich flex");
            case 190 -> {
                cfg.fakeScoreboard = false;
                cfg.fakeOldStyle = false;
                cfg.streamerMode = false;
                cfg.deaths.clear();
                Config.save();
                try { Files.deleteIfExists(SkyChanger.dir().resolve("smoke_sky.png")); } catch (Exception ignored) { }
                c.setScreen(null);
                fr.darkerdawn.sprinkle.compat.Compat.leaveWorld(c);
                c.setScreen(new TitleScreen());
                titleTicks = 0;
            }
            default -> { }
        }
    }

    /** Draws the Shulker Preview tooltip in the middle of the screen. */
    static final class TooltipTest extends net.minecraft.client.gui.screen.Screen {
        TooltipTest() { super(net.minecraft.text.Text.literal("tooltip test")); }

        @Override
        public void render(net.minecraft.client.gui.DrawContext g, int mx, int my, float d) {
            super.render(g, mx, my, d);
            var items = net.minecraft.util.collection.DefaultedList.ofSize(27, ItemStack.EMPTY);
            items.set(0, new ItemStack(Items.DIAMOND, 64));
            items.set(4, new ItemStack(Items.TOTEM_OF_UNDYING));
            items.set(13, new ItemStack(Items.SPAWNER, 8));
            items.set(26, new ItemStack(Items.NETHERITE_SWORD));
            g.drawTooltip(textRenderer, java.util.List.of(net.minecraft.text.Text.literal("Shulker Box"), net.minecraft.text.Text.literal("§6Auction: §f$1.2M §8lowest")),
                    java.util.Optional.of(new fr.darkerdawn.sprinkle.feature.Tooltips.ShulkerData(items)), width / 2 - 90, height / 2 - 40);
        }
    }

    /** Opens another mod's screen if that mod is installed (Mod Menu, Simple Voice Chat). */
    private static void open(MinecraftClient c, String cls, boolean withParent) {
        try {
            Class<?> k = Class.forName(cls);
            Object s = withParent ? k.getConstructor(net.minecraft.client.gui.screen.Screen.class).newInstance(c.currentScreen) : k.getConstructor().newInstance();
            c.setScreen((net.minecraft.client.gui.screen.Screen) s);
            System.out.println("[SPRINKLE-SMOKE] opened " + cls);
        } catch (Throwable t) {
            System.out.println("[SPRINKLE-SMOKE] could not open " + cls + ": " + t);
        }
    }

    private static void title(MinecraftClient c) {
        titleTicks++;
        switch (titleTicks) {
            case 160 -> {
                check("title screen is the Donut menu", c.currentScreen instanceof SprinkleTitleScreen);
                check("menu video has frames", MenuVideo.ready());
                shot(c, "main menu");
            }
            case 162 -> c.setScreen(new CreditsScreen(c.currentScreen));
            case 180 -> shot(c, "credits");
            case 182 -> { SettingsScreen.showTab(0); c.setScreen(new SettingsScreen(null)); }
            case 200 -> shot(c, "settings menu tab");
            case 202 -> { SettingsScreen.showTab(2); c.setScreen(new SettingsScreen(null)); }
            case 220 -> shot(c, "settings donut mods tab");
            case 222 -> c.setScreen(new SoundboardScreen(null));
            case 238 -> shot(c, "soundboard");
            case 240 -> c.setScreen(new AntiLeakScreen(null));
            case 256 -> shot(c, "anti-leak blocks");
            case 258 -> open(c, "com.terraformersmc.modmenu.gui.ModsScreen", true);
            case 276 -> shot(c, "mod menu");
            case 278 -> open(c, "de.maxhenkel.voicechat.gui.VoiceChatScreen", false);
            case 296 -> shot(c, "voice chat");
            case 298 -> {
                check("soundboard voice plugin loaded", !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("voicechat")
                        || fr.darkerdawn.sprinkle.feature.Soundboard.voiceAvailable);
                c.setScreen(new JoinLoadingScreen(new SprinkleTitleScreen()));
            }
            case 314 -> shot(c, "join loading screen");
            case 316 -> {
                c.setScreen(new SprinkleTitleScreen());
                check("config file saved", Files.exists(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("sprinkleclient.json")));
                System.out.println("[SPRINKLE-SMOKE] done, failures=" + fails);
                c.scheduleStop();
            }
            default -> { }
        }
    }
}
