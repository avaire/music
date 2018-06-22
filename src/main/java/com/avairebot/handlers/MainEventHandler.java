package com.avairebot.handlers;

import com.avairebot.AvaIre;
import com.avairebot.handlers.adapter.ChannelEventAdapter;
import com.avairebot.handlers.adapter.GuildStateEventAdapter;
import com.avairebot.handlers.adapter.MessageEventAdapter;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MainEventHandler extends ListenerAdapter {

    protected final AvaIre avaire;
    private final ChannelEventAdapter channelEvent;
    private final GuildStateEventAdapter guildStateEvent;
    private final MessageEventAdapter messageEvent;

    /**
     * Instantiates the event handler and sets the avaire class instance.
     *
     * @param avaire The AvaIre application class instance.
     */
    public MainEventHandler(AvaIre avaire) {
        this.avaire = avaire;
        this.channelEvent = new ChannelEventAdapter(avaire);
        this.messageEvent = new MessageEventAdapter(avaire);
        this.guildStateEvent = new GuildStateEventAdapter(avaire);
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        guildStateEvent.onGuildUpdateName(event);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildStateEvent.onGuildJoin(event);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        guildStateEvent.onGuildLeave(event);
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        channelEvent.onVoiceChannelDelete(event);
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        channelEvent.onTextChannelDelete(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        messageEvent.onMessageReceived(event);
    }
}
