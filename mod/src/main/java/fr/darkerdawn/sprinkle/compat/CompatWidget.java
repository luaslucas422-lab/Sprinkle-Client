package fr.darkerdawn.sprinkle.compat;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** A clickable widget whose click handler is the same on every version: override pressed(). */
public abstract class CompatWidget extends ClickableWidget {
    protected CompatWidget(int x, int y, int w, int h, Text message) {
        super(x, y, w, h, message);
    }

    protected abstract void pressed();

    @Override
    public void onClick(Click click, boolean doubled) {
        pressed();
    }
}
