package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.feature.FakeScoreboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Edit what the Fake Scoreboard says, with a live preview on the right. */
public class ScoreboardEditorScreen extends SprinkleScreen {
    private final List<Object[]> labels = new ArrayList<>();

    public ScoreboardEditorScreen(Screen parent) {
        super("Fake Scoreboard Editor", parent);
    }

    @Override
    protected void init() {
        labels.clear();
        Config c = Config.get();
        Config.FakeBoard f = c.fake;
        pw = Math.min(250, width / 2 + 40);
        ph = Math.min(height - 16, 280);
        px = 8;
        py = (height - ph) / 2;
        int y = py + 24;
        y = field(y, "Title", () -> f.title, v -> f.title = v);
        y = field(y, "Name", () -> f.name, v -> f.name = v);
        y = field(y, "Money", () -> f.money, v -> f.money = v);
        y = field(y, "Shards", () -> f.shards, v -> f.shards = v);
        y = field(y, "Kills", () -> f.kills, v -> f.kills = v);
        y = field(y, "Deaths", () -> f.deaths, v -> f.deaths = v);
        y = field(y, "Keyall", () -> f.keyall, v -> f.keyall = v);
        y = field(y, "Playtime", () -> f.playtime, v -> f.playtime = v);
        y = field(y, "Team", () -> f.team, v -> f.team = v);
        y = field(y, "Bottom line", () -> f.footer, v -> f.footer = v);
        int bw = (pw - 24) / 3;
        int by = py + ph - 26;
        button(px + 8, by, bw, "Board: " + onOff(c.fakeScoreboard), b -> { c.fakeScoreboard = !c.fakeScoreboard; Config.save(); clearAndInit(); });
        button(px + 12 + bw, by, bw, "Style: " + (c.fakeOldStyle ? "§7Old" : "§bNew"), b -> { c.fakeOldStyle = !c.fakeOldStyle; Config.save(); clearAndInit(); });
        button(px + 16 + 2 * bw, by, bw, "Done", b -> close());
    }

    private int field(int y, String label, Supplier<String> get, Consumer<String> set) {
        int fx = px + 72;
        TextFieldWidget t = new TextFieldWidget(textRenderer, fx, y, pw - 80, 16, Text.literal(label));
        t.setMaxLength(40);
        t.setText(get.get());
        t.setChangedListener(s -> { set.accept(s); Config.save(); });
        addDrawableChild(t);
        labels.add(new Object[]{px + 10, y + 4, label});
        return y + 19;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        for (Object[] l : labels) g.drawTextWithShadow(textRenderer, (String) l[2], (int) l[0], (int) l[1], Theme.MUTED);
        FakeScoreboard.render(g);
        String hint = Config.get().fakeScoreboard ? "§aShowing in game. Turn it off and the real one comes back."
                : "§7Preview only. Switch Board ON to show it in game.";
        g.drawTextWithShadow(textRenderer, hint, px + pw + 10, height - 14, 0xFFFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
