package fr.darkerdawn.sprinkle.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;
import fr.darkerdawn.sprinkle.feature.Soundboard;

/** Simple Voice Chat plugin: only loaded by Simple Voice Chat, so the client works fine without it. */
public class DonutVoicePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "sprinkleclient";
    }

    @Override
    public void initialize(VoicechatApi api) {
        Soundboard.voiceAvailable = true;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MergeClientSoundEvent.class, event -> {
            short[] frame = Soundboard.VOICE.poll();
            if (frame != null) event.mergeAudio(frame);
        });
    }
}
