package com.avairebot.commands.music;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.AudioSession;
import com.avairebot.audio.TrackResponse;
import com.avairebot.audio.VoiceConnectStatus;
import com.avairebot.commands.CommandHandler;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.ThreadCommand;
import com.avairebot.utilities.NumberUtil;
import com.avairebot.utilities.RestActionUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PlayCommand extends ThreadCommand {

    public PlayCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Play Music Command";
    }

    @Override
    public String getDescription() {
        return "Plays the provided song for you, if just the song title is given the bot will search YouTube for your song and give you some suggestions, you can also use YouTube, SoundCloud, TwitchTV, Bandcamp, and Vimeo link, or raw sound file, mp3, flac, wav, webm, mp4, ogg, aac, m3u and pls formats.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <song>` - Plays the given song");
    }

    @Override
    public List<Class<? extends Command>> getRelations() {
        return Collections.singletonList(SoundcloudCommand.class);
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("play", "request");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "hasDJLevel:none",
            "throttle:guild,2,4",
            "musicChannel"
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command A cool song` - Finds songs with the name \"A cool song\".",
            "`:command https://www.youtube.com/watch?v=dQw4w9WgXcQ` - Plays the song off a link"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "errors.missingMusicQueue");
        }

        boolean shouldLeaveMessage = false;
        if (args[args.length - 1].equals("---leave-message")) {
            shouldLeaveMessage = true;
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }

        VoiceConnectStatus voiceConnectStatus = AudioHandler.getDefaultAudioHandler().connectToVoiceChannel(context);
        if (!voiceConnectStatus.isSuccess()) {
            context.makeWarning(voiceConnectStatus.getErrorMessage()).queue();
            return false;
        }

        if (AudioHandler.getDefaultAudioHandler().hasAudioSession(context) && NumberUtil.isNumeric(args[0])) {
            return loadSongFromSession(context, args);
        }

        AudioHandler.getDefaultAudioHandler().loadAndPlay(context, buildTrackRequestString(args)).handle(
            musicSuccess(context, shouldLeaveMessage),
            musicFailure(context),
            musicSession(context, args)
        );

        return true;
    }

    boolean loadSongFromSession(CommandMessage context, String[] args) {
        int songIndex = NumberUtil.parseInt(args[0], 1) - 1;
        AudioSession session = AudioHandler.getDefaultAudioHandler().getAudioSession(context);

        int index = NumberUtil.getBetween(songIndex, 0, session.getSongs().getTracks().size() - 1);
        AudioTrack track = session.getSongs().getTracks().get(index);

        musicSuccess(context, false).accept(
            new TrackResponse(AudioHandler.getDefaultAudioHandler().getGuildAudioPlayer(context.getGuild()),
                track,
                track.getInfo().uri
            )
        );
        AudioHandler.getDefaultAudioHandler().play(context, AudioHandler.getDefaultAudioHandler().getGuildAudioPlayer(context.getGuild()), track);

        if (session.getMessage() != null) {
            session.getMessage().delete().queue(null, RestActionUtil.IGNORE);
        }

        AudioHandler.getDefaultAudioHandler().removeAudioSession(context);
        return false;
    }

    private void sendPlaylistResponse(CommandMessage context, TrackResponse response) {
        AudioPlaylist playlist = (AudioPlaylist) response.getAudioItem();

        context.makeSuccess(context.i18n("addedSongsFromPlaylist"))
            .set("songs", NumberUtil.formatNicely(playlist.getTracks().size()))
            .set("title", playlist.getName())
            .set("url", response.getTrackUrl())
            .set("queueSize", NumberUtil.formatNicely(
                AudioHandler.getDefaultAudioHandler().getQueueSize(response.getMusicManager())
            ))
            .queue();
    }

    private void sendTrackResponse(CommandMessage context, TrackResponse response) {
        AudioTrack track = (AudioTrack) response.getAudioItem();

        context.makeSuccess(context.i18n("addedSong"))
            .set("title", track.getInfo().title)
            .set("url", track.getInfo().uri)
            .set("queueSize", NumberUtil.formatNicely(
                AudioHandler.getDefaultAudioHandler().getQueueSize(response.getMusicManager())
            ))
            .queue();
    }

    private String buildTrackRequestString(String[] args) {
        String string = String.join(" ", args);

        if (string.startsWith("scsearch:")) {
            return string;
        }

        try {
            new URL(string);

            return string;
        } catch (MalformedURLException ex) {
            return "ytsearch:" + string;
        }
    }

    private Consumer<TrackResponse> musicSuccess(final CommandMessage context, final boolean finalShouldLeaveMessage) {
        return (TrackResponse response) -> {
            if (!finalShouldLeaveMessage && canDeleteMessage(context)) {
                context.delete().reason("Song request, removing song to cleanup chat").queue(null, RestActionUtil.IGNORE);
            }

            response.getMusicManager().registerDefaultVolume();

            if (response.getMusicManager().getPlayer().isPaused()) {
                response.getMusicManager().getPlayer().setPaused(false);
            }

            if (response.getMusicManager().getPlayer().getPlayingTrack() != null) {
                if (response.isPlaylist()) sendPlaylistResponse(context, response);
                else sendTrackResponse(context, response);
            }
        };
    }

    private boolean canDeleteMessage(CommandMessage context) {
        return context.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)
            && context.getGuild().getSelfMember().hasPermission(context.getChannel(), Permission.MESSAGE_MANAGE);
    }

    private Consumer<Throwable> musicFailure(final CommandMessage context) {
        return throwable -> context.makeError(throwable.getMessage()).queue();
    }

    @SuppressWarnings("ConstantConditions")
    private Consumer<AudioSession> musicSession(final CommandMessage context, final String[] args) {
        return (AudioSession audioSession) -> {
            List<String> songs = new ArrayList<>();
            List<AudioTrack> tracks = audioSession.getSongs().getTracks();
            for (int i = 0; i < 9; i++) {
                if (tracks.size() <= i) {
                    break;
                }

                AudioTrack track = tracks.get(i);

                songs.add(String.format("`%s` [%s](%s)",
                    (i + 1), track.getInfo().title, track.getInfo().uri
                ));
            }

            String command = generateCommandTrigger();
            if (args[0].startsWith("scsearch:")) {
                command = CommandHandler.getCommand(SoundcloudCommand.class)
                    .getCommand().generateCommandTrigger();
            }

            context.makeSuccess(String.join("\n", songs))
                .setTitle(String.format(
                    context.i18n("session.title"),
                    String.join(" ", args)
                ))
                .setFooter(String.format(
                    context.i18n("session.footer"),
                    command
                ))
                .queue(audioSession::setMessage);
        };
    }
}
