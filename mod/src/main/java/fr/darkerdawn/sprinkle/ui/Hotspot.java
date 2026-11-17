package fr.darkerdawn.sprinkle.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

/** An invisible clickable area (used for the logo easter egg). */
public class Hotspot extends ClickableWidget {
    private final Runnable action;

    public Hotspot(int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, Text.literal("Logo"));
        this.action = action;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        action.run();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
