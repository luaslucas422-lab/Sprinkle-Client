package fr.darkerdawn.sprinkle.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.zip.ZipOutputStream;

/** Fabric-installer-style installer: adds Fabric 1.21.11, Sprinkle Client, the speed mods and a launcher profile. */
public final class Installer {
    static final String MC = "1.21.11";
    static final String PROFILE_KEY = "sprinkleclient";
    static final String UA = "DarkerDawnFR/SprinkleClient-Installer/1.0.0";

    record ExtraMod(String slug, String name, String desc) {}

    static final List<ExtraMod> CATALOG = List.of(
            new ExtraMod("sodium", "Sodium", "Huge FPS boost"),
            new ExtraMod("lithium", "Lithium", "Faster game logic"),
            new ExtraMod("ferrite-core", "FerriteCore", "Uses less memory"),
            new ExtraMod("immediatelyfast", "ImmediatelyFast", "Faster HUD and text"),
            new ExtraMod("entityculling", "Entity Culling", "Skips hidden mobs"),
            new ExtraMod("modernfix", "ModernFix", "Faster loading"),
            new ExtraMod("iris", "Iris Shaders", "Shader support"),
            new ExtraMod("zoomify", "Zoomify", "Zoom with C"),
            new ExtraMod("modmenu", "Mod Menu", "See your mods"));

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final HttpClient HTTP = HttpClient.newBuilder().proxy(java.net.ProxySelector.getDefault()).connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();

    // ------------------------------------------------------------------ paths

    static Path minecraftDir() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        String override = System.getProperty("sprinkle.mcdir");
        if (override != null) return Paths.get(override);
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            return Paths.get(appdata != null ? appdata : home, ".minecraft");
        }
        if (os.contains("mac")) return Paths.get(home, "Library", "Application Support", "minecraft");
        return Paths.get(home, ".minecraft");
    }

    static Path gameDir(Path mc) {
        String n = mc.getFileName().toString().startsWith(".") ? ".sprinkleclient" : "sprinkleclient";
        return mc.resolveSibling(n);
    }

    // ------------------------------------------------------------------ http

    static JsonElement getJson(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", UA).timeout(Duration.ofSeconds(60)).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IOException(url + " returned " + res.statusCode());
        return JsonParser.parseString(res.body());
    }

    static void download(String url, Path dest) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", UA).timeout(Duration.ofMinutes(3)).build();
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        HttpResponse<Path> res = HTTP.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() != 200) {
            Files.deleteIfExists(tmp);
            throw new IOException("download " + url + " returned " + res.statusCode());
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    // ------------------------------------------------------------------ install

    static final class Report {
        String fabricId, modJar;
        Map<String, String> installed = new LinkedHashMap<>();
        Map<String, String> skipped = new LinkedHashMap<>();
    }

    static Report install(Path mc, Set<String> chosen, int ramGb, BiConsumer<Double, String> progress) throws Exception {
        Report r = new Report();
        if (!Files.isDirectory(mc)) throw new IOException("Minecraft isn't installed yet. Install the Minecraft Launcher and press Play once, then try again.");
        Path game = gameDir(mc);
        Path mods = game.resolve("mods");
        Files.createDirectories(mods);

        progress.accept(0.04, "Getting Fabric for Minecraft " + MC + "...");
        JsonArray loaders = getJson("https://meta.fabricmc.net/v2/versions/loader/" + MC).getAsJsonArray();
        String loader = null;
        for (JsonElement e : loaders) {
            JsonObject l = e.getAsJsonObject().getAsJsonObject("loader");
            if (l.get("stable").getAsBoolean()) { loader = l.get("version").getAsString(); break; }
        }
        if (loader == null && !loaders.isEmpty()) loader = loaders.get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();
        if (loader == null) throw new IOException("No Fabric loader found for " + MC);
        JsonObject profile = getJson("https://meta.fabricmc.net/v2/versions/loader/" + MC + "/" + loader + "/profile/json").getAsJsonObject();
        String id = profile.get("id").getAsString();
        Path vdir = mc.resolve("versions").resolve(id);
        Files.createDirectories(vdir);
        Files.writeString(vdir.resolve(id + ".json"), GSON.toJson(profile));
        Path vjar = vdir.resolve(id + ".jar");
        if (!Files.exists(vjar)) {
            try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(vjar))) { /* empty jar, like the Fabric installer */ }
        }
        r.fabricId = id;

        progress.accept(0.12, "Adding sprinkles...");
        try (InputStream in = Installer.class.getResourceAsStream("/sprinkle/SprinkleClient-mod.jar")) {
            if (in == null) throw new IOException("This installer has no Sprinkle Client mod inside.");
            try (var list = Files.list(mods)) {
                for (Path p : list.toList()) if (p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("sprinkleclient")) Files.delete(p);
            }
            Path dest = mods.resolve("SprinkleClient.jar");
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            r.modJar = dest.getFileName().toString();
        }

        Path manifestFile = game.resolve("sprinkle-installed.json");
        Map<String, String> manifest = new LinkedHashMap<>();
        if (Files.exists(manifestFile)) {
            JsonObject o = JsonParser.parseString(Files.readString(manifestFile)).getAsJsonObject();
            for (var e : o.entrySet()) manifest.put(e.getKey(), e.getValue().getAsString());
        }
        List<String> want = new ArrayList<>();
        want.add("fabric-api");
        for (ExtraMod m : CATALOG) if (chosen.contains(m.slug())) want.add(m.slug());
        Set<String> seen = new HashSet<>(), keep = new HashSet<>();
        String q = "?loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8) + "&game_versions=" + URLEncoder.encode("[\"" + MC + "\"]", StandardCharsets.UTF_8);
        for (int i = 0; i < want.size(); i++) {
            String slug = want.get(i);
            progress.accept(0.15 + 0.75 * i / (want.size() + 1), "Downloading " + pretty(slug) + "...");
            try {
                JsonArray vs = getJson("https://api.modrinth.com/v2/project/" + URLEncoder.encode(slug, StandardCharsets.UTF_8) + "/version" + q).getAsJsonArray();
                if (vs.isEmpty()) { r.skipped.put(slug, "no version for " + MC); continue; }
                JsonObject v = vs.get(0).getAsJsonObject();
                String project = v.get("project_id").getAsString();
                if (!seen.add(project)) continue;
                JsonArray files = v.getAsJsonArray("files");
                if (files.isEmpty()) { r.skipped.put(slug, "no file"); continue; }
                JsonObject f = files.get(0).getAsJsonObject();
                for (JsonElement fe : files) if (fe.getAsJsonObject().get("primary").getAsBoolean()) f = fe.getAsJsonObject();
                String fname = f.get("filename").getAsString();
                Path dest = mods.resolve(fname);
                if (!Files.exists(dest)) download(f.get("url").getAsString(), dest);
                String old = manifest.get(slug);
                if (old != null && !old.equals(fname)) Files.deleteIfExists(mods.resolve(old));
                manifest.put(slug, fname);
                keep.add(slug);
                r.installed.put(slug, fname);
                for (JsonElement de : v.getAsJsonArray("dependencies")) {
                    JsonObject d = de.getAsJsonObject();
                    if ("required".equals(d.get("dependency_type").getAsString()) && d.has("project_id") && !d.get("project_id").isJsonNull()) {
                        String pid = d.get("project_id").getAsString();
                        if (!seen.contains(pid) && !want.contains(pid)) want.add(pid);
                    }
                }
            } catch (Exception e) {
                r.skipped.put(slug, e.getMessage());
            }
        }
        for (var it = manifest.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (!keep.contains(e.getKey()) && !r.skipped.containsKey(e.getKey())) {
                Files.deleteIfExists(mods.resolve(e.getValue()));
                it.remove();
            }
        }
        Files.writeString(manifestFile, GSON.toJson(manifest));

        progress.accept(0.93, "Adding Sprinkle Client to the Minecraft Launcher...");
        writeProfile(mc, game, id, ramGb);
        progress.accept(1.0, "Ready!");
        return r;
    }

    static String pretty(String slug) {
        for (ExtraMod m : CATALOG) if (m.slug().equals(slug)) return m.name();
        return slug.equals("fabric-api") ? "Fabric API" : "a helper mod";
    }

    static void writeProfile(Path mc, Path game, String versionId, int ramGb) throws IOException {
        Path file = mc.resolve("launcher_profiles.json");
        JsonObject root = new JsonObject();
        if (Files.exists(file)) {
            String text = Files.readString(file);
            Files.writeString(file.resolveSibling("launcher_profiles.json.sprinkle-backup"), text);
            root = JsonParser.parseString(text).getAsJsonObject();
        }
        JsonObject profiles = root.has("profiles") ? root.getAsJsonObject("profiles") : new JsonObject();
        JsonObject prof = profiles.has(PROFILE_KEY) ? profiles.getAsJsonObject(PROFILE_KEY) : new JsonObject();
        String now = Instant.now().toString();
        if (!prof.has("created")) prof.addProperty("created", now);
        prof.addProperty("name", "Sprinkle Client");
        prof.addProperty("type", "custom");
        prof.addProperty("lastVersionId", versionId);
        prof.addProperty("gameDir", game.toAbsolutePath().toString());
        prof.addProperty("icon", iconDataUri());
        prof.addProperty("lastUsed", now);
        prof.addProperty("javaArgs", "-Xmx" + ramGb + "G -Xms1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200");
        profiles.add(PROFILE_KEY, prof);
        root.add("profiles", profiles);
        if (!root.has("version")) root.addProperty("version", 3);
        Files.writeString(file, GSON.toJson(root));
    }

    static String iconDataUri() {
        try (InputStream in = Installer.class.getResourceAsStream("/profile.png")) {
            if (in == null) return "Furnace";
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            return "Furnace";
        }
    }

    // ------------------------------------------------------------------ entry

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("--selftest")) {
            System.exit(selfTest(Paths.get(args[1])));
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("No screen found. Installing with default options...");
            Report r = install(minecraftDir(), defaultMods(), 4, (p, m) -> System.out.printf("[%3.0f%%] %s%n", p * 100, m));
            System.out.println("Done. Skipped: " + r.skipped.keySet());
            return;
        }
        SwingUtilities.invokeLater(Window::open);
    }

    static Set<String> defaultMods() {
        Set<String> s = new LinkedHashSet<>();
        for (ExtraMod m : CATALOG) s.add(m.slug());
        return s;
    }

    static int selfTest(Path dir) throws Exception {
        Path mc = dir.resolve(".minecraft");
        Files.createDirectories(mc);
        Files.writeString(mc.resolve("launcher_profiles.json"), "{\"profiles\":{\"other\":{\"name\":\"Keep me\",\"type\":\"custom\"}},\"settings\":{\"keepThis\":true},\"version\":3}");
        Report r = install(mc, defaultMods(), 4, (p, m) -> System.out.printf("[%3.0f%%] %s%n", p * 100, m));
        System.out.println("REPORT " + GSON.toJson(r));
        JsonObject root = JsonParser.parseString(Files.readString(mc.resolve("launcher_profiles.json"))).getAsJsonObject();
        boolean ok = root.getAsJsonObject("profiles").has("other") && root.getAsJsonObject("profiles").has(PROFILE_KEY) && root.has("settings");
        if (!ok || !r.installed.containsKey("fabric-api") || r.modJar == null) {
            System.out.println("FAIL jar installer self-test");
            return 1;
        }
        System.out.println("PASS jar installer self-test");
        return 0;
    }

    // ------------------------------------------------------------------ window

    static final class Window {
        static final Color SKY = new Color(0x2EA1FC), DEEP = new Color(0x0B5CC2), FROSTING = new Color(0x0370FF),
                FROSTING_L = new Color(0x3D93FF), OUTLINE = new Color(0x020B35), DOUGH = new Color(0xAF7F52), CREAM = new Color(0xFFF3B0);

        static void open() {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
            JFrame f = new JFrame("Sprinkle Client Installer");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            BufferedImage donut = null;
            try (InputStream in = Installer.class.getResourceAsStream("/donut.png")) {
                if (in != null) donut = ImageIO.read(in);
            } catch (IOException ignored) { }
            if (donut != null) f.setIconImage(donut);
            BufferedImage logo = donut;

            JPanel root = new JPanel() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0;
                    g.setPaint(new GradientPaint(0, 0, SKY, 0, getHeight(), DEEP));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    Random r = new Random(7);
                    Color[] cols = {new Color(0xFF6FB1), new Color(0xFFE15C), new Color(0x7CFF8A), Color.WHITE, new Color(0xFF9F43)};
                    for (int i = 0; i < 60; i++) {
                        g.setColor(new Color(cols[i % cols.length].getRGB() & 0x00FFFFFF | 0x88000000, true));
                        int x = r.nextInt(Math.max(1, getWidth())), y = r.nextInt(Math.max(1, getHeight()));
                        if (i % 2 == 0) g.fillRect(x, y, 8, 2); else g.fillRect(x, y, 2, 8);
                    }
                    if (logo != null) {
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.drawImage(logo, getWidth() / 2 - 70, 20, 140, 140, null);
                    }
                }
            };
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(BorderFactory.createEmptyBorder(170, 36, 18, 36));

            JLabel title = label("Sprinkle Client", 32f, Font.BOLD, Color.WHITE);
            JLabel sub = label("Donut SMP client  ·  Minecraft " + MC + "  ·  Fabric", 13f, Font.PLAIN, new Color(0xD8ECFF));
            JButton install = new JButton("INSTALL");
            style(install, FROSTING, 24f);
            JProgressBar bar = new JProgressBar(0, 1000);
            bar.setForeground(FROSTING);
            bar.setBackground(DOUGH);
            bar.setBorder(BorderFactory.createLineBorder(OUTLINE, 3));
            bar.setMaximumSize(new Dimension(380, 18));
            bar.setAlignmentX(0.5f);
            bar.setVisible(false);
            JLabel msg = label(" ", 13f, Font.PLAIN, CREAM);
            JButton options = new JButton("Options");
            style(options, DOUGH, 13f);
            JLabel foot = label("Made by DarkerDawnFR · Fan-made, not affiliated with DrDonut or Donut SMP", 10f, Font.PLAIN, new Color(0xCFE6FF));

            Set<String> chosen = defaultMods();
            int[] ram = {4};
            Path mc = minecraftDir();
            if (!Files.isDirectory(mc)) msg.setText("Minecraft isn't installed yet. Install it and press Play once first.");

            options.addActionListener(e -> {
                JPanel p = new JPanel(new GridLayout(0, 1));
                Map<String, JCheckBox> boxes = new LinkedHashMap<>();
                for (ExtraMod m : CATALOG) {
                    JCheckBox b = new JCheckBox(m.name() + "  (" + m.desc() + ")", chosen.contains(m.slug()));
                    boxes.put(m.slug(), b);
                    p.add(b);
                }
                JComboBox<String> ramBox = new JComboBox<>(new String[]{"2 GB", "3 GB", "4 GB", "6 GB", "8 GB", "12 GB"});
                ramBox.setSelectedItem(ram[0] + " GB");
                p.add(new JLabel("RAM for Minecraft:"));
                p.add(ramBox);
                if (JOptionPane.showConfirmDialog(f, p, "Sprinkle Client options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    chosen.clear();
                    boxes.forEach((k, b) -> { if (b.isSelected()) chosen.add(k); });
                    ram[0] = Integer.parseInt(((String) ramBox.getSelectedItem()).split(" ")[0]);
                }
            });

            install.addActionListener(e -> {
                install.setEnabled(false);
                install.setText("INSTALLING...");
                bar.setVisible(true);
                new Thread(() -> {
                    try {
                        Report r = install(mc, chosen, ram[0], (p, m) -> SwingUtilities.invokeLater(() -> { bar.setValue((int) (p * 1000)); msg.setText(m); }));
                        SwingUtilities.invokeLater(() -> {
                            install.setText("REINSTALL");
                            install.setEnabled(true);
                            String text = "<html><center>Installed! Open the Minecraft Launcher,<br>pick <b>Sprinkle Client</b> and press Play.";
                            if (!r.skipped.isEmpty()) text += "<br>Skipped (not out for " + MC + " yet): " + String.join(", ", r.skipped.keySet());
                            msg.setText(text + "</center></html>");
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            install.setText("TRY AGAIN");
                            install.setEnabled(true);
                            msg.setText("<html><center>" + ex.getMessage() + "</center></html>");
                        });
                    }
                }, "sprinkle-install").start();
            });

            root.add(title);
            root.add(sub);
            root.add(Box.createVerticalStrut(16));
            root.add(install);
            root.add(Box.createVerticalStrut(14));
            root.add(bar);
            root.add(Box.createVerticalStrut(8));
            root.add(msg);
            root.add(Box.createVerticalGlue());
            root.add(options);
            root.add(Box.createVerticalStrut(10));
            root.add(foot);
            f.setContentPane(root);
            f.setSize(480, 560);
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        }

        static JLabel label(String text, float size, int style, Color c) {
            JLabel l = new JLabel(text, SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(style, size));
            l.setForeground(c);
            l.setAlignmentX(0.5f);
            return l;
        }

        static void style(JButton b, Color bg, float size) {
            b.setFont(b.getFont().deriveFont(Font.BOLD, size));
            b.setForeground(Color.WHITE);
            b.setBackground(bg);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE, 3), BorderFactory.createEmptyBorder(10, 40, 10, 40)));
            b.setAlignmentX(0.5f);
            b.setMaximumSize(new Dimension(380, size > 20 ? 64 : 40));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(bg == FROSTING ? FROSTING_L : bg.brighter()); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(bg); }
            });
        }
    }
}
