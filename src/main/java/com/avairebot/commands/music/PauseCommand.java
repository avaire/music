package com.avairebot.commands.music;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.GuildMusicManager;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.utilities.RestActionUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PauseCommand extends Command {

    public PauseCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Pause Music Command";
    }

    @Override
    public String getDescription() {
        return "Pauses the music currently playing";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command` - Pauses the music");
    }

    @Override
    public List<Class<? extends Command>> getRelations() {
        return Collections.singletonList(ResumeCommand.class);
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("pause");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "hasDJLevel:normal",
            "throttle:guild,1,4",
            "musicChannel"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        GuildMusicManager musicManager = AudioHandler.getDefaultAudioHandler().getGuildAudioPlayer(context.getGuild());

        if (musicManager.getPlayer().getPlayingTrack() == null) {
            return sendErrorMessage(context, context.i18n("error", generateCommandPrefix()));
        }

        if (musicManager.getPlayer().isPaused()) {
            context.makeWarning(context.i18n("alreadyPaused"))
                .queue(message -> message.delete().queueAfter(1, TimeUnit.MINUTES, null, RestActionUtil.IGNORE));
            return true;
        }

        musicManager.getPlayer().setPaused(true);
        context.makeSuccess(context.i18n("paused"))
            .queue(message -> message.delete().queueAfter(1, TimeUnit.MINUTES, null, RestActionUtil.IGNORE));

        return true;
    }
}
