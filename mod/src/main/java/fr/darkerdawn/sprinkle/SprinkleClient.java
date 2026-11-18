package fr.darkerdawn.sprinkle;

import fr.darkerdawn.sprinkle.feature.AuctionPrices;
import fr.darkerdawn.sprinkle.feature.Features;
import fr.darkerdawn.sprinkle.feature.SpawnerCalc;
import fr.darkerdawn.sprinkle.feature.Tooltips;
import fr.darkerdawn.sprinkle.feature.Tracker;
import fr.darkerdawn.sprinkle.screen.ModsScreen;
import fr.darkerdawn.sprinkle.screen.ScoreboardEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SprinkleClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Sprinkle Client");
    public static final String VERSION = FabricLoader.getInstance().getModContainer("sprinkleclient")
            .map(m -> m.getMetadata().getVersion().getFriendlyString().replaceAll("\\+.*", "")).orElse("1.0.0");

    @Override
    public void onInitializeClient() {
        Config.load();
        Features.registerKeys();

        fr.darkerdawn.sprinkle.compat.Hooks.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Tracker.tick(client);
            if (client.player != null) {
                Features.handleKeys(client);
                while (Features.openMenu.wasPressed()) if (client.currentScreen == null) client.setScreen(new ModsScreen(null));
                while (Features.editScoreboard.wasPressed()) if (client.currentScreen == null) client.setScreen(new ScoreboardEditorScreen(null));
                while (Features.setBase.wasPressed()) setBaseHere(client);
                while (Features.soundboard.wasPressed()) if (client.currentScreen == null) client.setScreen(new fr.darkerdawn.sprinkle.screen.SoundboardScreen(null));
            }
            if (Smoke.ON) Smoke.tick(client);
        });
        ClientReceiveMessageEvents.GAME.register(Tracker::onGameMessage);
        ClientReceiveMessageEvents.GAME.register(AuctionPrices::onGameMessage);
        ClientSendMessageEvents.COMMAND.register(AuctionPrices::onCommand);
        TooltipComponentCallback.EVENT.register(Tooltips::component);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Tracker.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Tracker.reset());
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (fr.darkerdawn.sprinkle.compat.Compat.isClient(world) && entity instanceof PlayerEntity) Tracker.tag();
            return ActionResult.PASS;
        });
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            SpawnerCalc.tooltip(stack, lines);
            Tooltips.hint(stack, lines);
            AuctionPrices.tooltip(stack, lines);
        });
        PauseMenu.register();

        ModContainer me = FabricLoader.getInstance().getModContainer("sprinkleclient").orElseThrow();
        fr.darkerdawn.sprinkle.compat.Compat.registerPack(me, "donut_smp", "Donut SMP");
        fr.darkerdawn.sprinkle.compat.Compat.registerPack(me, "donut_stone", "Donut Stone");
        fr.darkerdawn.sprinkle.compat.Compat.registerPack(me, "donut_voicechat", "Donut Voice Chat");
        fr.darkerdawn.sprinkle.compat.Compat.registerPack(me, "sprinkle_blue", "Sprinkle Blue Buttons");
        LOG.info("Sprinkle Client {} ready. Every button leads to Donut SMP.", VERSION);
    }

    public static void openDiscord(Screen parent) {
        Util.getOperatingSystem().open(Config.get().discordUrl);
    }

    private static void setBaseHere(MinecraftClient client) {
        Config c = Config.get();
        c.baseX = (int) Math.floor(client.player.getX());
        c.baseZ = (int) Math.floor(client.player.getZ());
        c.baseDimension = client.world.getRegistryKey().getValue().toString();
        Config.save();
        client.inGameHud.setOverlayMessage(Text.literal("§aBase Alarm: base saved here."), false);
    }
}
