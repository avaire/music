package com.avairebot.scheduler.tasks;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.GuildMusicManager;
import com.avairebot.audio.LavalinkManager;
import com.avairebot.contracts.scheduler.Task;
import lavalink.client.io.Link;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MusicActivityTask implements Task {

    public final static Map<Long, Integer> MISSING_LISTENERS = new HashMap<>();
    public final static Map<Long, Integer> EMPTY_QUEUE = new HashMap<>();
    public final static Map<Long, Integer> PLAYER_PAUSED = new HashMap<>();

    @Override
    public void handle(AvaIre avaire) {
        if (!avaire.areWeReadyYet() || !avaire.getConfig().getBoolean("music-activity.enabled", true)) {
            return;
        }

        if (LavalinkManager.LavalinkManagerHolder.LAVALINK.isEnabled()) {
            handleLavalinkNodes(avaire);
        } else {
            handleInternalLavaplayer(avaire);
        }
    }


    private void handleInternalLavaplayer(AvaIre avaire) {
        for (JDA shard : avaire.getShardManager().getShards()) {
            Iterator<AudioManager> iterator = shard.getAudioManagers().iterator();

            try {
                while (iterator.hasNext()) {
                    AudioManager manager = iterator.next();

                    if (!manager.isConnected()) {
                        continue;
                    }

                    long guildId = manager.getGuild().getIdLong();

                    if (!AudioHandler.getDefaultAudioHandler().musicManagers.containsKey(guildId)) {
                        handleEmptyMusic(avaire, manager, null, null, guildId);
                        continue;
                    }

                    GuildMusicManager guildMusicManager = AudioHandler.getDefaultAudioHandler().musicManagers.get(guildId);

                    if (guildMusicManager.getScheduler().getQueue().isEmpty() && guildMusicManager.getPlayer().getPlayingTrack() == null) {
                        handleEmptyMusic(avaire, manager, null, guildMusicManager, guildId);
                        continue;
                    }

                    if (EMPTY_QUEUE.containsKey(guildId)) {
                        EMPTY_QUEUE.remove(guildId);
                    }

                    if (guildMusicManager.getPlayer().isPaused()) {
                        handlePausedMusic(avaire, manager, null, guildMusicManager, guildId);
                        continue;
                    }

                    VoiceChannel voiceChannel = manager.getConnectedChannel();

                    boolean hasListeners = false;
                    for (Member member : voiceChannel.getMembers()) {
                        if (member.getUser().isBot()) {
                            continue;
                        }

                        if (member.getVoiceState().isDeafened()) {
                            continue;
                        }

                        hasListeners = true;
                        break;
                    }

                    if (hasListeners && !manager.getGuild().getSelfMember().getVoiceState().isMuted()) {
                        MISSING_LISTENERS.remove(guildId);
                        continue;
                    }

                    int times = MISSING_LISTENERS.getOrDefault(guildId, 0) + 1;

                    if (times <= getValue(avaire, "missing-listeners", 5)) {
                        MISSING_LISTENERS.put(guildId, times);
                        continue;
                    }

                    clearItems(manager, null, guildMusicManager, guildId);
                }
            } catch (Exception e) {
                AvaIre.getLogger().error("An exception occurred during music activity job: " + e.getMessage(), e);
            }
        }
    }

    private void handleLavalinkNodes(AvaIre avaire) {
        for (Link link : LavalinkManager.LavalinkManagerHolder.LAVALINK.getLavalink().getLinks()) {
            long guildId = link.getGuildIdLong();

            try {
                if (!AudioHandler.getDefaultAudioHandler().musicManagers.containsKey(guildId)) {
                    handleEmptyMusic(avaire, null, link, null, guildId);
                    continue;
                }

                GuildMusicManager guildMusicManager = AudioHandler.getDefaultAudioHandler().musicManagers.get(guildId);
                if (guildMusicManager.getLastActiveMessage() == null) {
                    continue;
                }

                if (guildMusicManager.getScheduler().getQueue().isEmpty() && guildMusicManager.getPlayer().getPlayingTrack() == null) {
                    handleEmptyMusic(avaire, null, link, guildMusicManager, guildId);
                    continue;
                }

                if (EMPTY_QUEUE.containsKey(guildId)) {
                    EMPTY_QUEUE.remove(guildId);
                }

                if (guildMusicManager.getPlayer().isPaused()) {
                    handlePausedMusic(avaire, null, link, guildMusicManager, guildId);
                    continue;
                }

                VoiceChannel voiceChannel = link.getChannel();

                if (voiceChannel != null) {
                    boolean hasListeners = false;
                    for (Member member : voiceChannel.getMembers()) {
                        if (member.getUser().isBot()) {
                            continue;
                        }

                        if (member.getVoiceState().isDeafened()) {
                            continue;
                        }

                        hasListeners = true;
                        break;
                    }

                    if (hasListeners && !guildMusicManager.getLastActiveMessage().getGuild().getSelfMember().getVoiceState().isMuted()) {
                        MISSING_LISTENERS.remove(guildId);
                        continue;
                    }

                    int times = MISSING_LISTENERS.getOrDefault(guildId, 0) + 1;

                    if (times <= getValue(avaire, "missing-listeners", 5)) {
                        MISSING_LISTENERS.put(guildId, times);
                        continue;
                    }
                }

                clearItems(null, link, guildMusicManager, guildId);
            } catch (Exception e) {
                AvaIre.getLogger().error("An exception occurred during music activity job for ID: {} - Message: " + e.getMessage(), guildId, e);
            }
        }
    }

    private void handleEmptyMusic(AvaIre avaire, @Nullable AudioManager manager, @Nullable Link link, @Nullable GuildMusicManager guildMusicManager, long guildId) {
        int times = EMPTY_QUEUE.getOrDefault(guildId, 0) + 1;

        if (times <= getValue(avaire, "empty-queue-timeout", 2)) {
            EMPTY_QUEUE.put(guildId, times);
            return;
        }

        clearItems(manager, link, guildMusicManager, guildId);
    }

    private void handlePausedMusic(AvaIre avaire, @Nullable AudioManager manager, @Nullable Link link, @Nullable GuildMusicManager guildMusicManager, long guildId) {
        int times = PLAYER_PAUSED.getOrDefault(guildId, 0) + 1;

        if (times <= getValue(avaire, "paused-music-timeout", 10)) {
            PLAYER_PAUSED.put(guildId, times);
            return;
        }

        clearItems(manager, link, guildMusicManager, guildId);
    }

    private void clearItems(@Nullable AudioManager manager, @Nullable Link link, @Nullable GuildMusicManager guildMusicManager, long guildId) {
        if (guildMusicManager != null) {
            guildMusicManager.getScheduler().getQueue().clear();
            if (LavalinkManager.LavalinkManagerHolder.LAVALINK.isEnabled()) {
                if (guildMusicManager.getLastActiveMessage() != null) {
                    LavalinkManager.LavalinkManagerHolder.LAVALINK.getLavalink()
                        .getLink(guildMusicManager.getLastActiveMessage().getGuild())
                        .destroy();
                }
            }

            if (guildMusicManager.getLastActiveMessage() != null && guildMusicManager.getLastActiveMessage().getChannel().canTalk()) {
                guildMusicManager.getLastActiveMessage().makeInfo("The music has ended due to inactivity.").queue();
            }
        }

        MISSING_LISTENERS.remove(guildId);
        PLAYER_PAUSED.remove(guildId);
        EMPTY_QUEUE.remove(guildId);

        if (guildMusicManager == null) {
            if (manager != null) {
                LavalinkManager.LavalinkManagerHolder.LAVALINK.closeConnection(manager.getGuild());
            } else if (link != null) {
                link.disconnect();
            }

            if (LavalinkManager.LavalinkManagerHolder.LAVALINK.isEnabled()) {
                if (manager != null) {
                    manager.getGuild().getAudioManager().setSendingHandler(null);
                }
            }

            AudioHandler.getDefaultAudioHandler().musicManagers.remove(guildId);
        } else {
            guildMusicManager.getScheduler().handleEndOfQueueWithLastActiveMessage(false);
        }
    }

    private int getValue(AvaIre avaire, String path, int def) {
        return Math.max(1, avaire.getConfig().getInt("music-activity." + path, def) * 2);
    }
}
