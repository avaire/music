package com.avairebot.handlers.adapter;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.contracts.handlers.EventAdapter;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent;

import java.sql.SQLException;

public class GuildStateEventAdapter extends EventAdapter {

    /**
     * Instantiates the event adapter and sets the avaire class instance.
     *
     * @param avaire The AvaIre application class instance.
     */
    public GuildStateEventAdapter(AvaIre avaire) {
        super(avaire);
    }

    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        try {
            avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .useAsync(true)
                .where("id", event.getGuild().getId())
                .update(statement -> statement.set("name", event.getGuild().getName(), true));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onGuildJoin(GuildJoinEvent event) {
        AvaIre.getLogger().info("Joined guild with an ID of " + event.getGuild().getId() + " called: " + event.getGuild().getName());
    }

    public void onGuildLeave(GuildLeaveEvent event) {
        AvaIre.getLogger().info("Left guild with an ID of " + event.getGuild().getId() + " called: " + event.getGuild().getName());
    }
}
