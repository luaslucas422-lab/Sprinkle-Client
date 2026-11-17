package fr.darkerdawn.sprinkle;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Images loaded from disk: the optional logo.png and the main-menu background slideshow. */
public final class Textures {
    private Textures() {}

    public record Img(Identifier id, int w, int h) {}

    public static final Identifier DONUT = Sounds.id("textures/gui/donut.png");
    public static final int DONUT_SIZE = 128;
    public static final Identifier DISCORD = Sounds.id("textures/gui/discord.png");
    public static final Identifier CREDITS = Sounds.id("textures/gui/credits.png");
    public static final Identifier GEAR = Sounds.id("textures/gui/gear.png");
    public static final int ICON_SIZE = 32;

    private static boolean loaded;
    private static Img customLogo;
    private static final List<Img> backgrounds = new ArrayList<>();
    private static int counter;

    public static Path backgroundsDir() {
        return Config.dir().resolve("backgrounds");
    }

    public static Path logoFile() {
        return Config.dir().resolve("logo.png");
    }

    /** (Re)loads the logo and backgrounds from disk. */
    public static void reload() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (customLogo != null) c.getTextureManager().destroyTexture(customLogo.id());
        for (Img b : backgrounds) c.getTextureManager().destroyTexture(b.id());
        customLogo = null;
        backgrounds.clear();
        loaded = true;

        if (Files.isRegularFile(logoFile())) customLogo = load(logoFile(), 1024);

        List<Path> files = new ArrayList<>(list(backgroundsDir()));
        if (files.isEmpty()) {
            // Nothing in the backgrounds folder yet: use screenshots taken in this client.
            files.addAll(list(FabricLoader.getInstance().getGameDir().resolve("screenshots")));
        }
        files.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        for (Path p : files) {
            if (backgrounds.size() >= 24) break;
            Img img = load(p, 1600);
            if (img != null) backgrounds.add(img);
        }
        SprinkleClient.LOG.info("Sprinkle: {} background(s), custom logo: {}", backgrounds.size(), customLogo != null);
    }

    private static List<Path> list(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp") || n.endsWith(".gif");
            }).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Reads any common image format, shrinks big ones, and uploads it as a texture. */
    private static Img load(Path file, int maxWidth) {
        try {
            BufferedImage src = ImageIO.read(file.toFile());
            if (src == null) return null;
            int w = src.getWidth(), h = src.getHeight();
            if (w > maxWidth) {
                h = Math.max(1, h * maxWidth / w);
                w = maxWidth;
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            NativeImage image = NativeImage.read(bytes.toByteArray());
            Identifier id = Sounds.id("dynamic/img_" + (counter++));
            String name = "sprinkle " + file.getFileName();
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> name, image));
            return new Img(id, w, h);
        } catch (Throwable t) {
            SprinkleClient.LOG.warn("Sprinkle could not load image {}: {}", file, t.toString());
            return null;
        }
    }

    public static Img customLogo() {
        if (!loaded) reload();
        return customLogo;
    }

    public static List<Img> backgrounds() {
        if (!loaded) reload();
        return backgrounds;
    }

    /** Draws an image scaled to cover the box (cropping the edges), with a slow zoom. */
    public static void drawCover(DrawContext g, Img img, int bw, int bh, float zoom, float panX, int color) {
        float scale = Math.max(bw / (float) img.w(), bh / (float) img.h()) * zoom;
        int regionW = Math.max(1, Math.round(bw / scale));
        int regionH = Math.max(1, Math.round(bh / scale));
        int u = Math.max(0, Math.min(img.w() - regionW, Math.round((img.w() - regionW) * panX)));
        int v = Math.max(0, (img.h() - regionH) / 2);
        g.drawTexture(RenderPipelines.GUI_TEXTURED, img.id(), 0, 0, u, v, bw, bh, regionW, regionH, img.w(), img.h(), color);
    }

    /** Draws a whole image scaled into a box. */
    public static void draw(DrawContext g, Identifier id, int x, int y, int w, int h, int texW, int texH, int color) {
        g.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, w, h, texW, texH, texW, texH, color);
    }
}
