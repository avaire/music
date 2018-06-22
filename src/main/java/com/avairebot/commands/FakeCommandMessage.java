package com.avairebot.commands;

import com.avairebot.config.YamlConfiguration;
import com.avairebot.contracts.commands.CommandContext;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.handlers.DatabaseEventHolder;
import net.dv8tion.jda.core.entities.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FakeCommandMessage implements CommandContext {

    @Override
    public Guild getGuild() {
        return null;
    }

    @Override
    public Member getMember() {
        return null;
    }

    @Override
    public User getAuthor() {
        return null;
    }

    @Override
    public TextChannel getChannel() {
        return null;
    }

    @Override
    public MessageChannel getMessageChannel() {
        return null;
    }

    @Override
    public Message getMessage() {
        return null;
    }

    @Override
    public GuildTransformer getGuildTransformer() {
        return null;
    }

    @Override
    public DatabaseEventHolder getDatabaseEventHolder() {
        return null;
    }

    @Override
    public List<User> getMentionedUsers() {
        return new ArrayList<>();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return new ArrayList<>();
    }

    @Override
    public boolean isMentionableCommand() {
        return false;
    }

    @Override
    public boolean isGuildMessage() {
        return false;
    }

    @Nonnull
    @Override
    public YamlConfiguration getI18n() {
        throw new UnsupportedOperationException("Invoking the i18n method on a fake command message context is not supported!");
    }

    @Override
    public String i18n(@Nonnull String key) {
        return "fake-" + key;
    }

    @Override
    public String i18n(@Nonnull String key, Object... args) {
        return i18n(key);
    }

    @Override
    public String i18nRaw(@Nonnull String key) {
        return "fake-" + key;
    }

    @Override
    public String i18nRaw(@Nonnull String key, Object... args) {
        return i18nRaw(key);
    }

    @Override
    public void setI18nPrefix(@Nullable String i18nPrefix) {
        // This does nothing
    }

    @Override
    public String getI18nCommandPrefix() {
        return "fake-prefix";
    }

    @Override
    public void setI18nCommandPrefix(@Nonnull CommandContainer container) {
        // This does nothing
    }
}
