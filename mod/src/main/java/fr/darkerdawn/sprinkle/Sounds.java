package fr.darkerdawn.sprinkle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/** Original synthesized sounds (tools/gen_sounds.py). Client-only, so no registry needed. */
public final class Sounds {
    private Sounds() {}

    public static final SoundEvent CASH = SoundEvent.of(id("cash"));
    public static final SoundEvent RAGE_QUIT = SoundEvent.of(id("rage_quit"));
    public static final SoundEvent MENU_MUSIC = SoundEvent.of(id("menu_music"));
    public static final SoundEvent ALARM = SoundEvent.of(id("alarm"));
    public static final SoundEvent FLEX = SoundEvent.of(id("flex"));
    public static final SoundEvent POP = SoundEvent.of(id("pop"));

    public static final MusicSound MENU = new MusicSound(RegistryEntry.of(MENU_MUSIC), 20, 200, true);

    public static Identifier id(String path) {
        return Identifier.of("sprinkleclient", path);
    }

    public static void play(SoundEvent e, float pitch, float volume) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c != null && c.getSoundManager() != null) c.getSoundManager().play(PositionedSoundInstance.ui(e, pitch, volume));
    }

    /** The button click: ka-ching, or the normal click if turned off. */
    public static void click() {
        if (Config.get().cashClick) play(CASH, 0.95f + (float) Math.random() * 0.1f, 0.8f);
        else MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
