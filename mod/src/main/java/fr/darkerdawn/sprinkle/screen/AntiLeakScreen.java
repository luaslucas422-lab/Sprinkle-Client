package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Packs;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.feature.AntiLeak;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.List;

/** Anti-Leak settings: add and remove the blocks that never rotate. */
public class AntiLeakScreen extends SprinkleScreen {
    private static final int ROW = 22;
    private int page, perPage, listTop;
    private TextFieldWidget input;
    private String note = "";

    public AntiLeakScreen(Screen parent) {
        super("Anti-Leak Blocks", parent);
    }

    @Override
    protected void init() {
        layoutPanel(330, 300);
        Config c = Config.get();
        int left = px + 12, w = pw - 24;
        button(left, py + 22, w / 2 - 2, "Anti-Leak: " + onOff(c.antiLeak), b -> { c.antiLeak = !c.antiLeak; AntiLeak.changed(); clearAndInit(); });
        button(left + w / 2 + 2, py + 22, w / 2 - 2, "Donut stone: " + onOff(Packs.enabled("donut_stone")), b -> { Packs.set("donut_stone", !Packs.enabled("donut_stone")); clearAndInit(); });

        input = new TextFieldWidget(textRenderer, left, py + 48, w - 112, 18, Text.literal("Block id"));
        input.setMaxLength(80);
        input.setPlaceholder(Text.literal("e.g. tuff or minecraft:gravel"));
        addDrawableChild(input);
        button(left + w - 108, py + 47, 50, "Add", b -> add(input.getText()));
        button(left + w - 54, py + 47, 54, "Looking", b -> addLookedAt());

        listTop = py + 74;
        perPage = Math.max(2, (py + ph - 34 - listTop) / ROW);
        List<String> ids = c.antiLeakBlocks;
        int pages = Math.max(1, (ids.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int i = page * perPage; i < Math.min(ids.size(), (page + 1) * perPage); i++) {
            String id = ids.get(i);
            int y = listTop + (i - page * perPage) * ROW;
            button(left + w - 24, y, 24, "§cX", b -> { c.antiLeakBlocks.remove(id); AntiLeak.changed(); clearAndInit(); });
        }
        int by = py + ph - 26;
        if (pages > 1) {
            button(left, by, 20, "<", b -> { page = (page + pages - 1) % pages; clearAndInit(); });
            button(left + 22, by, 20, ">", b -> { page = (page + 1) % pages; clearAndInit(); });
        }
        button(width / 2 - 64, by, 60, "Reset", b -> { c.antiLeakBlocks = new ArrayList<>(Config.DEFAULT_ANTI_LEAK); AntiLeak.changed(); clearAndInit(); });
        button(width / 2, by, 70, "Done", b -> close());
    }

    private void add(String raw) {
        if (raw == null || raw.isBlank()) return;
        String id = AntiLeak.normalize(raw);
        if (!AntiLeak.valid(id)) { note = "§cThere's no block called " + raw.trim(); return; }
        if (!Config.get().antiLeakBlocks.contains(id)) Config.get().antiLeakBlocks.add(id);
        AntiLeak.changed();
        note = "§aAdded " + id;
        clearAndInit();
    }

    private void addLookedAt() {
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            note = "§7Look at a block in game first.";
            return;
        }
        add(Registries.BLOCK.getId(client.world.getBlockState(hit.getBlockPos()).getBlock()).toString());
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        List<String> ids = Config.get().antiLeakBlocks;
        for (int i = page * perPage; i < Math.min(ids.size(), (page + 1) * perPage); i++) {
            int y = listTop + (i - page * perPage) * ROW;
            g.fill(px + 12, y, px + pw - 40, y + 20, 0x44000000);
            g.drawTextWithShadow(textRenderer, ids.get(i).replace("minecraft:", ""), px + 18, y + 6, Theme.WHITE);
        }
        if (ids.isEmpty()) g.drawCenteredTextWithShadow(textRenderer, "§7No blocks. Add some above.", width / 2, listTop + 6, Theme.WHITE);
        if (!note.isEmpty()) g.drawCenteredTextWithShadow(textRenderer, note, width / 2, py + ph - 40, Theme.WHITE);
    }
}
