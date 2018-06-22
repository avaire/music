package com.avairebot.commands.music.playlist;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.VoiceConnectStatus;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.music.PlaylistCommand;
import com.avairebot.contracts.commands.playlist.PlaylistSubCommand;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.database.transformers.PlaylistTransformer;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoadPlaylist extends PlaylistSubCommand {

    public LoadPlaylist(AvaIre avaire, PlaylistCommand command) {
        super(avaire, command);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args, GuildTransformer guild, PlaylistTransformer playlist) {
        VoiceConnectStatus voiceConnectStatus = AudioHandler.getDefaultAudioHandler().connectToVoiceChannel(context);
        if (!voiceConnectStatus.isSuccess()) {
            context.makeWarning(voiceConnectStatus.getErrorMessage()).queue();
            return false;
        }

        List<AudioTrack> tracks = new ArrayList<>();
        AudioHandler.getDefaultAudioHandler().getGuildAudioPlayer(context.getGuild()).setLastActiveMessage(context);

        int index = 0;
        for (PlaylistTransformer.PlaylistSong song : playlist.getSongs()) {
            if (index++ == playlist.getSongs().size() - 1) {
                loadSong(song, tracks, audioTracks -> {
                    loadedPlaylist(context, playlist, tracks);
                });

                return true;
            }
            loadSong(song, tracks, null);
        }

        context.makeWarning(context.i18n("failedToLoadPlaylist"))
            .set("playlist", playlist.getName())
            .queue();

        return true;
    }

    private void loadedPlaylist(CommandMessage context, PlaylistTransformer playlist, List<AudioTrack> tracks) {
        if (tracks.isEmpty()) {
            context.makeWarning(context.i18n("failedToLoadPlaylist"))
                .set("playlist", playlist.getName())
                .queue();

            return;
        }

        AudioHandler.getDefaultAudioHandler().getGuildAudioPlayer(context.getGuild())
            .getScheduler().queue(playlist, tracks, context.getAuthor());
    }

    private void loadSong(PlaylistTransformer.PlaylistSong song, final List<AudioTrack> tracks, Consumer<List<AudioTrack>> success) {
        AudioHandler.getDefaultAudioHandler().getPlayerManager().loadItemOrdered(AudioHandler.getDefaultAudioHandler().musicManagers, song.getLink(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                tracks.add(track);

                if (success != null) {
                    success.accept(tracks);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                trackLoaded(playlist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                //
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                //
            }
        });
    }
}
