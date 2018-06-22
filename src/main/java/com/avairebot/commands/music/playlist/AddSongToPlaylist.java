package com.avairebot.commands.music.playlist;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.audio.AudioHandler;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.music.PlaylistCommand;
import com.avairebot.contracts.commands.playlist.PlaylistSubCommand;
import com.avairebot.database.controllers.PlaylistController;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.database.transformers.PlaylistTransformer;
import com.avairebot.utilities.NumberUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;

public class AddSongToPlaylist extends PlaylistSubCommand {

    public AddSongToPlaylist(AvaIre avaire, PlaylistCommand command) {
        super(avaire, command);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args, GuildTransformer guild, PlaylistTransformer playlist) {
        String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (query.trim().length() == 0) {
            context.makeWarning(context.i18n("invalidFormat"))
                .set("command", command.generateCommandTrigger() + " " + playlist.getName() + " add <song title / link>")
                .set("type", "song")
                .queue();

            return false;
        }

        if (playlist.getSongs().size() >= guild.getType().getLimits().getPlaylist().getSongs()) {
            context.makeWarning(context.i18n("noMoreSongSlots"))
                .set("playlist", playlist.getName())
                .queue();

            return false;
        }

        try {
            new URL(query);
        } catch (MalformedURLException ex) {
            query = "ytsearch:" + query;
        }

        String finalQuery = query;
        context.getChannel().sendTyping().queue(v -> loadSong(context, finalQuery, guild, playlist));
        return true;
    }

    private void loadSong(CommandMessage context, String query, GuildTransformer guild, PlaylistTransformer playlist) {
        AudioHandler.getDefaultAudioHandler().getPlayerManager().loadItemOrdered(AudioHandler.getDefaultAudioHandler().musicManagers, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                handleTrackLoadedEvent(context, guild, playlist, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                trackLoaded(playlist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                context.makeWarning(context.i18n("noMatches")).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                context.makeWarning(String.format(
                    context.i18n("failedToLoad"),
                    e.getMessage()
                )).queue();
            }
        });
    }

    private void handleTrackLoadedEvent(CommandMessage context, GuildTransformer guild, PlaylistTransformer playlist, AudioTrack track) {
        if (track.getInfo().isStream) {
            context.makeWarning(context.i18n("attemptingToAddLivestreamToPlaylist")).queue();
            return;
        }

        playlist.addSong(
            track.getInfo().title,
            NumberUtil.formatTime(track.getDuration()),
            track.getInfo().uri
        );

        try {
            avaire.getDatabase().newQueryBuilder(Constants.MUSIC_PLAYLIST_TABLE_NAME)
                .where("id", playlist.getId()).andWhere("guild_id", context.getGuild().getId())
                .update(statement -> {
                    statement.set("songs", AvaIre.GSON.toJson(playlist.getSongs()), true);
                    statement.set("amount", playlist.getSongs().size());
                });

            PlaylistController.forgetCache(context.getGuild().getIdLong());

            context.makeSuccess(context.i18n("userHasAddedSong"))
                .set("name", track.getInfo().title)
                .set("url", track.getInfo().uri)
                .set("playlist", playlist.getName())
                .set("slots", guild.getType().getLimits().getPlaylist().getSongs() - playlist.getSongs().size())
                .queue();
        } catch (SQLException e) {
            e.printStackTrace();

            context.makeError(String.format(
                context.i18n("failedToSavePlaylist"),
                e.getMessage()
            )).queue();
        }
    }
}
