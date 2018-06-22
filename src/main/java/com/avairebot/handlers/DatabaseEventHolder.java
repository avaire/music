package com.avairebot.handlers;

import com.avairebot.database.transformers.GuildTransformer;

public class DatabaseEventHolder {

    private final GuildTransformer guild;

    public DatabaseEventHolder(GuildTransformer guild) {
        this.guild = guild;
    }

    public GuildTransformer getGuild() {
        return guild;
    }
}
