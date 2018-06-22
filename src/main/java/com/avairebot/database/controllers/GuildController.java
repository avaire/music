package com.avairebot.database.controllers;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.CacheUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import javax.annotation.CheckReturnValue;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class GuildController {

    public static final Cache<Object, Object> cache = CacheBuilder.newBuilder()
        .recordStats()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .expireAfterWrite(4, TimeUnit.MINUTES)
        .build();

    private static final String[] REQUIRED_GUILD_ITEMS = new String[]{
        "guild_types.name as type_name", "guild_types.limits as type_limits", "guilds.id", "guilds.name", "guilds.icon", "guilds.locale",
        "guilds.default_volume", "guilds.dj_level", "guilds.music_channel_text", "guilds.music_channel_voice"
    };

    /**
     * Fetches the guild transformer from the cache, if it doesn't exist in the
     * cache it will be loaded into the cache and then returned afterwords.
     *
     * @param avaire  The avaire instance, used to talking to the database.
     * @param message The JDA message instance for the current message.
     * @return Possibly null, the guild transformer instance for the current guild, or null.
     */
    @CheckReturnValue
    public static GuildTransformer fetchGuild(AvaIre avaire, Message message) {
        if (!message.getChannelType().isGuild()) {
            return null;
        }
        return fetchGuild(avaire, message.getGuild());
    }

    /**
     * Fetches the guild transformer from the cache, if it doesn't exist in the
     * cache it will be loaded into the cache and then returned afterwords.
     *
     * @param avaire The avaire instance, used to talking to the database.
     * @param guild  The JDA guild instance for the current guild.
     * @return Possibly null, the guild transformer instance for the current guild, or null.
     */
    @CheckReturnValue
    public static GuildTransformer fetchGuild(AvaIre avaire, Guild guild) {
        return (GuildTransformer) CacheUtil.getUncheckedUnwrapped(cache, guild.getIdLong(), () -> loadGuildFromDatabase(avaire, guild));
    }

    /**
     * Fetches the guild transformer from the cache, if it doesn't exist in the
     * cache it will be loaded into the cache and then returned afterwords.
     * <p>
     * If the entry wasn't found in the cache, the channel object will also be used to
     * send a typing event to the channel, to let people know that Ava is working,
     * but is loading the data from the database, so it might be slow for a
     * second or two while everything is loaded into the cache.
     *
     * @param avaire  The avaire instance, used to talking to the database.
     * @param message The JDA message instance for the current message.
     * @param channel The JDA channel instance for the channel the message was sent in.
     * @return Possibly null, the guild transformer instance for the current guild, or null.
     */
    @CheckReturnValue
    public static GuildTransformer fetchGuild(AvaIre avaire, Message message, MessageChannel channel) {
        if (!message.getChannelType().isGuild()) {
            return null;
        }

        return (GuildTransformer) CacheUtil.getUncheckedUnwrapped(cache, message.getGuild().getIdLong(), () -> {
            channel.sendTyping().queue();

            return loadGuildFromDatabase(avaire, message.getGuild());
        });
    }

    public static void forgetCache(long guildId) {
        cache.invalidate(guildId);
    }

    private static GuildTransformer loadGuildFromDatabase(AvaIre avaire, Guild guild) {
        try {
            GuildTransformer transformer = new GuildTransformer(avaire.getDatabase()
                .newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .select(REQUIRED_GUILD_ITEMS)
                .leftJoin("guild_types", "guilds.type", "guild_types.id")
                .where("guilds.id", guild.getId())
                .get().first());

            if (!transformer.hasData()) {
                try {
                    avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                        .insert(statement -> {
                            statement.set("id", guild.getId())
                                .set("owner", guild.getOwner().getUser().getId())
                                .set("name", guild.getName(), true);

                            if (guild.getIconId() != null) {
                                statement.set("icon", guild.getIconId());
                            }
                        });
                } catch (Exception ex) {
                    AvaIre.getLogger().error(ex.getMessage(), ex);
                }
                return new GuildTransformer(guild);
            }

            return transformer;
        } catch (SQLException ex) {
            AvaIre.getLogger().error(ex.getMessage(), ex);
            return null;
        }
    }
}
