package fr.darkerdawn.sprinkle.compat;

import fr.darkerdawn.sprinkle.Sounds;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;


/** Everything else that differs between Minecraft versions (this copy: 1.21.8). */
public final class Compat {
    private Compat() {}

    public static final String MC = "1.21.8";

    public interface Painter { void paint(DrawContext g); }

    public static void registerPack(ModContainer me, String path, String name) {
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(Sounds.id(path), me, Text.literal(name),
                net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED);
    }

    public static SoundInstance ui(SoundEvent e, float pitch, float volume) {
        return PositionedSoundInstance.master(e, pitch, volume);
    }

    public static SoundInstance click() {
        return PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f);
    }

    public static NativeImageBackedTexture texture(String name, NativeImage img) {
        return new NativeImageBackedTexture(() -> name, img);
    }

    public static void setPixel(NativeImage img, int x, int y, int argb) {
        img.setColorArgb(x, y, argb);
    }

    public static boolean keyDown(int key) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key);
    }

    public static KeyBinding key(String id, int code) {
        return new KeyBinding(id, code, "key.category.sprinkleclient.sprinkle");
    }

    /** Drawn after a screen's background but before its buttons. */
    public static void afterBackground(Screen screen, Painter p) {
        ScreenEvents.afterBackground(screen).register((s, g, mx, my, d) -> p.paint(g));
    }

    public static void beforeClick(Screen screen, Runnable r) {
        ScreenMouseEvents.beforeMouseClick(screen).register((s, mx, my, button) -> r.run());
    }

    /** Smoke test helpers. */
    public static net.minecraft.entity.Entity spawnTestPig(net.minecraft.server.world.ServerWorld w) {
        return net.minecraft.entity.EntityType.PIG.create(w, net.minecraft.entity.SpawnReason.COMMAND);
    }

    public static void leaveWorld(MinecraftClient c) {
        c.world.disconnect(net.minecraft.client.world.ClientWorld.QUITTING_MULTIPLAYER_TEXT);
        c.disconnectWithSavingScreen();
    }

    public static boolean isClient(World w) {
        return w.isClient();
    }

}
