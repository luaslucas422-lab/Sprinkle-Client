package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/** Shulker Preview (hold Shift) and Map Preview tooltips. Bundles already show their items in vanilla 1.21.11. */
public final class Tooltips {
    private Tooltips() {}

    public record ShulkerData(DefaultedList<ItemStack> items) implements TooltipData {}

    public record MapData(MapIdComponent id) implements TooltipData {}

    public static boolean shiftDown() {
        var w = MinecraftClient.getInstance().getWindow();
        return InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    /** Called from the ItemStack mixin: returns our own tooltip picture, or null for vanilla. */
    public static Optional<TooltipData> data(ItemStack stack) {
        Config c = Config.get();
        if (c.shulkerTooltip && isShulker(stack) && shiftDown()) {
            ContainerComponent box = stack.get(DataComponentTypes.CONTAINER);
            if (box != null && box.streamNonEmpty().findAny().isPresent()) {
                DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
                box.copyTo(items);
                return Optional.of(new ShulkerData(items));
            }
        }
        if (c.mapTooltip && stack.isOf(Items.FILLED_MAP)) {
            MapIdComponent id = stack.get(DataComponentTypes.MAP_ID);
            if (id != null) return Optional.of(new MapData(id));
        }
        return null;
    }

    public static void hint(ItemStack stack, List<Text> lines) {
        if (Config.get().shulkerTooltip && isShulker(stack) && !shiftDown()) {
            ContainerComponent box = stack.get(DataComponentTypes.CONTAINER);
            if (box != null && box.streamNonEmpty().findAny().isPresent()) lines.add(Text.literal("§9Hold Shift to look inside"));
        }
    }

    public static TooltipComponent component(TooltipData data) {
        if (data instanceof ShulkerData s) return new ShulkerComponent(s.items());
        if (data instanceof MapData m) return new MapComponent(m.id());
        return null;
    }

    static final class ShulkerComponent implements TooltipComponent {
        private final DefaultedList<ItemStack> items;

        ShulkerComponent(DefaultedList<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int getHeight(TextRenderer tr) {
            return 3 * 18 + 6;
        }

        @Override
        public int getWidth(TextRenderer tr) {
            return 9 * 18 + 2;
        }

        @Override
        public void drawItems(TextRenderer tr, int x, int y, int width, int height, DrawContext g) {
            g.fill(x, y, x + 9 * 18 + 2, y + 3 * 18 + 2, 0xFF020B35);
            for (int i = 0; i < 27; i++) {
                int sx = x + 1 + (i % 9) * 18, sy = y + 1 + (i / 9) * 18;
                g.fill(sx, sy, sx + 18, sy + 18, (i + i / 9) % 2 == 0 ? 0xFF0A2A5C : 0xFF0C3370);
                ItemStack s = i < items.size() ? items.get(i) : ItemStack.EMPTY;
                if (!s.isEmpty()) {
                    g.drawItem(s, sx + 1, sy + 1);
                    g.drawStackOverlay(tr, s, sx + 1, sy + 1);
                }
            }
        }
    }

    static final class MapComponent implements TooltipComponent {
        private static final int SIZE = 96;
        private final MapIdComponent id;

        MapComponent(MapIdComponent id) {
            this.id = id;
        }

        @Override
        public int getHeight(TextRenderer tr) {
            return SIZE + 4;
        }

        @Override
        public int getWidth(TextRenderer tr) {
            return SIZE;
        }

        @Override
        public void drawItems(TextRenderer tr, int x, int y, int width, int height, DrawContext g) {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.world == null) return;
            MapState state = FilledMapItem.getMapState(id, c.world);
            if (state == null) {
                g.drawTextWithShadow(tr, "§7Map not loaded yet", x, y + SIZE / 2 - 4, 0xFFFFFFFF);
                return;
            }
            MapRenderState rs = new MapRenderState();
            c.getMapRenderer().update(id, state, rs);
            g.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, 0xFF020B35);
            g.getMatrices().pushMatrix();
            g.getMatrices().translate(x, y);
            g.getMatrices().scale(SIZE / 128f, SIZE / 128f);
            g.drawMap(rs);
            g.getMatrices().popMatrix();
        }
    }
}
