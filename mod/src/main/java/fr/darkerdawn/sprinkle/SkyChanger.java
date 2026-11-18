package fr.darkerdawn.sprinkle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Sky Changer: wraps a picture from config/sprinkleclient/sky around the whole sky (overworld only).
 * The picture is drawn on a big sphere just inside the fog, so terrain still covers it.
 */
public final class SkyChanger {
    private SkyChanger() {}

    private static final Identifier SKY = Sounds.id("dynamic/sky");
    private static boolean loaded, has;

    public static Path dir() {
        return Config.dir().resolve("sky");
    }

    public static boolean hasSky() {
        if (!loaded) reload();
        return has;
    }

    public static void reload() {
        loaded = true;
        MinecraftClient c = MinecraftClient.getInstance();
        if (has) c.getTextureManager().destroyTexture(SKY);
        has = false;
        if (!Files.isDirectory(dir())) return;
        try (Stream<Path> s = Files.list(dir())) {
            Path file = s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
            }).sorted().findFirst().orElse(null);
            if (file == null) return;
            BufferedImage src = ImageIO.read(file.toFile());
            if (src == null) return;
            int w = Math.min(4096, src.getWidth());
            int h = Math.max(1, src.getHeight() * w / src.getWidth());
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            out.createGraphics().drawImage(src, 0, 0, w, h, null);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            NativeImage img = NativeImage.read(bytes.toByteArray());
            c.getTextureManager().registerTexture(SKY, fr.darkerdawn.sprinkle.compat.Compat.texture("sprinkle sky", img));
            has = true;
            SprinkleClient.LOG.info("Sprinkle sky: {}", file.getFileName());
        } catch (Throwable t) {
            SprinkleClient.LOG.warn("Sprinkle could not load the sky picture: {}", t.toString());
        }
    }

    /** Runs before entities are drawn: terrain is already there, so the sphere only fills the sky. */
    public static void render(MatrixStack matrices, VertexConsumerProvider consumers) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (!Config.get().skyChanger || c.world == null || !hasSky()) return;
        if (!c.world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (consumers == null || matrices == null) return;

        // darker at night
        double day = (c.world.getTimeOfDay() % 24000L) / 24000.0; // 0 = sunrise-ish (6am)
        float light = (float) (0.6 + 0.4 * Math.cos((day - 0.25) * Math.PI * 2));
        light = Math.max(0.3f, Math.min(1f, light));
        int v = (int) (light * 255);
        int color = 0xFF000000 | (v << 16) | (v << 8) | v;

        float r = Math.max(48, c.options.getClampedViewDistance() * 16 * 0.78f);
        VertexConsumer vc = consumers.getBuffer(fr.darkerdawn.sprinkle.compat.Hooks.skyLayer(SKY));
        MatrixStack.Entry e = matrices.peek();
        int lon = 48, lat = 24;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            float v0 = 1f - (float) i / lat, v1 = 1f - (float) (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
                float u0 = (float) j / lon, u1 = (float) (j + 1) / lon;
                float[] a = point(r, t0, p0), b = point(r, t0, p1), cc = point(r, t1, p1), d = point(r, t1, p0);
                // both windings so it shows whichever way the layer culls
                quad(vc, e, a, u0, v0, b, u1, v0, cc, u1, v1, d, u0, v1, color);
                quad(vc, e, d, u0, v1, cc, u1, v1, b, u1, v0, a, u0, v0, color);
            }
        }
    }

    private static float[] point(float r, double lat, double lon) {
        return new float[]{(float) (r * Math.cos(lat) * Math.sin(lon)), (float) (r * Math.sin(lat)), (float) (-r * Math.cos(lat) * Math.cos(lon))};
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e,
                             float[] a, float ua, float va, float[] b, float ub, float vb,
                             float[] c, float uc, float vc2, float[] d, float ud, float vd, int color) {
        vert(vc, e, a, ua, va, color);
        vert(vc, e, b, ub, vb, color);
        vert(vc, e, c, uc, vc2, color);
        vert(vc, e, d, ud, vd, color);
    }

    private static void vert(VertexConsumer vc, MatrixStack.Entry e, float[] p, float u, float v, int color) {
        vc.vertex(e, p[0], p[1], p[2]).color(color).texture(u, v).light(0xF000F0);
    }
}
