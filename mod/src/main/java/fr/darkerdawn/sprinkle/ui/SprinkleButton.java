package fr.darkerdawn.sprinkle.ui;

import fr.darkerdawn.sprinkle.Sounds;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/** Frosting-blue button with sprinkles on hover. Can show text, an icon, or both. */
public class SprinkleButton extends ClickableWidget {
    private final Consumer<SprinkleButton> action;
    private Identifier icon;
    private int iconSize = Textures.ICON_SIZE;
    private boolean silent;
    private final long seed;

    public SprinkleButton(int x, int y, int w, int h, Text label, Consumer<SprinkleButton> action) {
        super(x, y, w, h, label);
        this.action = action;
        this.seed = label.getString().hashCode() * 31L + x * 7L + y;
    }

    public SprinkleButton icon(Identifier id, int texSize) {
        this.icon = id;
        this.iconSize = texSize;
        return this;
    }

    /** Buttons that play their own sound (Rage Quit) skip the click. */
    public SprinkleButton silent() {
        this.silent = true;
        return this;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        action.accept(this);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!silent) Sounds.click();
    }

    @Override
    protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
        boolean hov = isHovered() && active;
        int lift = hov ? -1 : 0;
        int x = getX(), y = getY() + lift, w = getWidth(), h = getHeight();
        Theme.button(g, x, y, w, h, hov, active, seed);
        var tr = MinecraftClient.getInstance().textRenderer;
        String label = getMessage().getString();
        if (icon != null) {
            int s = Math.min(w, h) - 6;
            int ix = label.isEmpty() ? x + (w - s) / 2 : x + 4;
            Textures.draw(g, icon, ix, y + (h - s) / 2 - 1, s, s, iconSize, iconSize, 0xFFFFFFFF);
            if (!label.isEmpty()) g.drawTextWithShadow(tr, getMessage(), ix + s + 4, y + (h - 8) / 2 - 1, active ? Theme.WHITE : Theme.MUTED);
        } else {
            g.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2 - 1, active ? (hov ? Theme.GOLD : Theme.WHITE) : Theme.MUTED);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
