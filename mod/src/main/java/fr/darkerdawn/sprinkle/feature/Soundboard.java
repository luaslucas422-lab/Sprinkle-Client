package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.SprinkleClient;
import fr.darkerdawn.sprinkle.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Donut SMP Soundboard: plays a sound for you, and (when Simple Voice Chat is installed and the server has it)
 * mixes it into your voice so everyone nearby hears it too. Every sound is synthesized, no samples.
 */
public final class Soundboard {
    private Soundboard() {}

    public record Sound(String file, String label) {
        public SoundEvent event() {
            return SoundEvent.of(Sounds.id(file));
        }
    }

    public static final List<Sound> SOUNDS = List.of(
            new Sound("sb_pay", "/pay ding"),
            new Sound("sb_kill", "Kill"),
            new Sound("cash", "Ka-ching"),
            new Sound("sb_airhorn", "Air horn"),
            new Sound("sb_boom", "Boom"),
            new Sound("rage_quit", "Sad trombone"),
            new Sound("meow1", "Donut meow"),
            new Sound("sb_drumroll", "Drumroll"),
            new Sound("sb_crickets", "Crickets"),
            new Sound("sb_scratch", "Record scratch"),
            new Sound("sb_clown", "Clown honk"),
            new Sound("sb_dundun", "Dun dun DUNNN"),
            new Sound("alarm", "Base alarm"),
            new Sound("flex", "Rich flex"));

    public static final int RATE = 48_000, FRAME = 960;
    /** 20 ms frames waiting to go into your voice. */
    public static final ConcurrentLinkedQueue<short[]> VOICE = new ConcurrentLinkedQueue<>();
    public static volatile boolean voiceAvailable;
    private static final Map<String, short[]> PCM = new ConcurrentHashMap<>();

    public static void play(Sound s) {
        Sounds.play(s.event(), 1.0f, 1.0f);
        if (!voiceAvailable) return;
        short[] pcm = PCM.computeIfAbsent(s.file(), Soundboard::decode);
        if (pcm == null || pcm.length == 0) return;
        VOICE.clear();
        for (int i = 0; i < pcm.length; i += FRAME) {
            short[] f = new short[FRAME];
            System.arraycopy(pcm, i, f, 0, Math.min(FRAME, pcm.length - i));
            VOICE.add(f);
        }
    }

    public static void stop() {
        VOICE.clear();
    }

    /** OGG from our assets -> 48 kHz mono 16-bit, at 60% volume. */
    private static short[] decode(String file) {
        try (InputStream in = MinecraftClient.getInstance().getResourceManager()
                .getResource(Sounds.id("sounds/" + file + ".ogg")).orElseThrow().getInputStream()) {
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channels = stack.mallocInt(1), rate = stack.mallocInt(1);
                ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(buf, channels, rate);
                if (pcm == null) return null;
                try {
                    int ch = Math.max(1, channels.get(0)), sr = rate.get(0);
                    int frames = pcm.remaining() / ch;
                    int outLen = (int) ((long) frames * RATE / sr);
                    short[] out = new short[outLen];
                    for (int i = 0; i < outLen; i++) {
                        double pos = (double) i * sr / RATE;
                        int a = Math.min(frames - 1, (int) pos), b = Math.min(frames - 1, a + 1);
                        double t = pos - a;
                        double va = pcm.get(a * ch), vb = pcm.get(b * ch);
                        out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (va + (vb - va) * t) * 0.6));
                    }
                    return out;
                } finally {
                    LibCStdlib.free(pcm);
                }
            } finally {
                MemoryUtil.memFree(buf);
            }
        } catch (Throwable t) {
            SprinkleClient.LOG.warn("Soundboard could not load {}: {}", file, t.toString());
            return null;
        }
    }
}
