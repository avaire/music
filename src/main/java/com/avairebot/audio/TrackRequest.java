package com.avairebot.audio;

import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.async.Future;
import com.avairebot.exceptions.NoMatchFoundException;
import com.avairebot.exceptions.TrackLoadFailedException;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.function.Consumer;

public class TrackRequest extends Future {

    private final GuildMusicManager musicManager;
    private final CommandMessage context;
    private final String trackUrl;

    TrackRequest(GuildMusicManager musicManager, CommandMessage context, String trackUrl) {
        this.musicManager = musicManager;
        this.context = context;
        this.trackUrl = trackUrl;

        musicManager.setLastActiveMessage(context);
    }

    @Override
    public void handle(final Consumer success, final Consumer<Throwable> failure) {
        handle(success, failure, null);
    }

    public void handle(final Consumer success, final Consumer<Throwable> failure, final Consumer<AudioSession> sessionConsumer) {
        AudioHandler.getDefaultAudioHandler().getPlayerManager().loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                success.accept(new TrackResponse(musicManager, track, trackUrl));

                AudioHandler.getDefaultAudioHandler().play(context, musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    noMatches();
                    return;
                }

                if (trackUrl.startsWith("ytsearch:") || trackUrl.startsWith("scsearch:")) {
                    if (sessionConsumer == null) {
                        trackLoaded(playlist.getTracks().get(0));
                        return;
                    }

                    sessionConsumer.accept(AudioHandler.getDefaultAudioHandler().createAudioSession(context, playlist));
                    return;
                }

                if (playlist.getTracks().isEmpty()) {
                    noMatches();
                    return;
                }

                success.accept(new TrackResponse(musicManager, playlist, trackUrl));
                AudioHandler.getDefaultAudioHandler().play(context, musicManager, playlist);
            }

            @Override
            public void noMatches() {
                failure.accept(new NoMatchFoundException(
                    context.i18nRaw("music.internal.noMatchFound"),
                    trackUrl
                ));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                failure.accept(new TrackLoadFailedException(
                    context.i18nRaw("music.internal.trackLoadFailed"),
                    exception.getMessage(),
                    exception
                ));
            }
        });
    }
}
