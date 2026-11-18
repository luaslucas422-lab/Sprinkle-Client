package fr.darkerdawn.sprinkle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * The looping video next to Rage Quit. Drop an .mp4 (H.264) or .gif into config/sprinkleclient/menu_video/.
 * Frames are decoded once in the background (no sound) and then loop on the main menu.
 */
public final class MenuVideo {
    private MenuVideo() {}

    private static final int MAX_FRAMES = 180, TARGET_FPS = 15, MAX_W = 360;
    private static final List<Identifier> frames = new ArrayList<>();
    private static final ConcurrentLinkedQueue<BufferedImage> pending = new ConcurrentLinkedQueue<>();
    private static volatile boolean started, decoding;
    public static volatile String status = "";
    private static int w, h;
    private static float fps = TARGET_FPS;

    public static Path dir() {
        return Config.dir().resolve("menu_video");
    }

    public static boolean hasVideoFile() {
        return file() != null;
    }

    private static Path file() {
        if (!Files.isDirectory(dir())) return null;
        try (Stream<Path> s = Files.list(dir())) {
            return s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return n.endsWith(".mp4") || n.endsWith(".m4v") || n.endsWith(".mov") || n.endsWith(".gif");
            }).sorted().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Starts decoding the first time the menu asks for it. */
    public static void ensureStarted() {
        if (started) return;
        started = true;
        Path f = file();
        if (f == null) return;
        decoding = true;
        status = "Loading video...";
        Thread t = new Thread(() -> {
            try {
                if (f.toString().toLowerCase().endsWith(".gif")) decodeGif(f); else decodeMp4(f);
                status = "";
            } catch (Throwable e) {
                status = "Video not supported";
                SprinkleClient.LOG.warn("Sprinkle menu video {} failed: {}", f.getFileName(), e.toString());
            } finally {
                decoding = false;
            }
        }, "sprinkle-menu-video");
        t.setDaemon(true);
        t.start();
    }

    public static void reload() {
        MinecraftClient c = MinecraftClient.getInstance();
        for (Identifier id : frames) c.getTextureManager().destroyTexture(id);
        frames.clear();
        pending.clear();
        started = false;
        status = "";
    }

    private static BufferedImage shrink(BufferedImage src) {
        int tw = Math.min(MAX_W, src.getWidth()), th = Math.max(1, src.getHeight() * tw / src.getWidth());
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, tw, th, null);
        g.dispose();
        return out;
    }

    private static void decodeMp4(Path f) throws Exception {
        try (var ch = NIOUtils.readableChannel(f.toFile())) {
            FrameGrab grab = FrameGrab.createFrameGrab(ch);
            double srcFps = 30;
            try {
                var meta = grab.getVideoTrack().getMeta();
                if (meta.getTotalDuration() > 0 && meta.getTotalFrames() > 0) srcFps = meta.getTotalFrames() / meta.getTotalDuration();
            } catch (Throwable ignored) { }
            int step = Math.max(1, (int) Math.round(srcFps / TARGET_FPS));
            fps = (float) (srcFps / step);
            Picture p;
            int i = 0, kept = 0;
            while ((p = grab.getNativeFrame()) != null && kept < MAX_FRAMES) {
                if (i++ % step != 0) continue;
                pending.add(shrink(AWTUtil.toBufferedImage(p)));
                kept++;
            }
        }
    }

    private static void decodeGif(Path f) throws Exception {
        try (ImageInputStream in = ImageIO.createImageInputStream(f.toFile())) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) throw new IllegalStateException("no gif reader");
            ImageReader r = it.next();
            r.setInput(in);
            int n = Math.min(MAX_FRAMES, r.getNumImages(true));
            BufferedImage canvas = null;
            for (int i = 0; i < n; i++) {
                BufferedImage fr = r.read(i);
                if (canvas == null) canvas = new BufferedImage(r.getWidth(0), r.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                canvas.createGraphics().drawImage(fr, 0, 0, null);
                pending.add(shrink(canvas));
            }
            fps = 12;
        }
    }

    /** Uploads decoded frames to the GPU, a few per frame, on the render thread. */
    private static void upload() {
        MinecraftClient c = MinecraftClient.getInstance();
        for (int k = 0; k < 6; k++) {
            BufferedImage img = pending.poll();
            if (img == null) return;
            try {
                NativeImage ni = new NativeImage(img.getWidth(), img.getHeight(), false);
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) fr.darkerdawn.sprinkle.compat.Compat.setPixel(ni, x, y, img.getRGB(x, y) | 0xFF000000);
                }
                Identifier id = Sounds.id("dynamic/menu_video_" + frames.size());
                String name = "sprinkle menu video " + frames.size();
                c.getTextureManager().registerTexture(id, fr.darkerdawn.sprinkle.compat.Compat.texture(name, ni));
                frames.add(id);
                w = img.getWidth();
                h = img.getHeight();
            } catch (Throwable t) {
                SprinkleClient.LOG.warn("Sprinkle menu video frame failed: {}", t.toString());
                return;
            }
        }
    }

    /** True once there's at least one frame to show. */
    public static boolean ready() {
        upload();
        return !frames.isEmpty();
    }

    public static boolean busy() {
        return decoding || !pending.isEmpty();
    }

    public static int width() { return w; }

    public static int height() { return h; }

    /** The frame to show now (loops; while still loading it loops over what's ready). */
    public static Identifier frame(long millis) {
        if (frames.isEmpty()) return null;
        int idx = (int) ((millis / 1000.0) * fps) % frames.size();
        return frames.get(idx);
    }
}
