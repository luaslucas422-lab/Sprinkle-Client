package fr.darkerdawn.sprinkle;

import fr.darkerdawn.sprinkle.screen.JoinLoadingScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Every Donut SMP button ends up here. */
public final class Join {
    private Join() {}

    public static void press(Screen parent) {
        MinecraftClient c = MinecraftClient.getInstance();
        switch (Config.get().joinStyle) {
            case INSTANT -> connect(parent);
            case LOADING -> c.setScreen(new JoinLoadingScreen(parent));
            case CONFIRM -> c.setScreen(new ConfirmScreen(
                    yes -> { if (yes) connect(parent); else c.setScreen(parent); },
                    Text.literal(Jokes.pick(Jokes.CONFIRM_LINES)),
                    Text.literal("You're about to join " + Config.get().serverIp + "."),
                    Text.literal("Let's go"),
                    Text.literal("I'm scared")));
        }
    }

    public static void connect(Screen parent) {
        MinecraftClient c = MinecraftClient.getInstance();
        String ip = Config.get().serverIp;
        ServerInfo info = new ServerInfo("Donut SMP", ip, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(parent, c, ServerAddress.parse(ip), info, false, null);
    }
}
