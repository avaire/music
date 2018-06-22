package com.avairebot.handlers.adapter;

import com.avairebot.AppInfo;
import com.avairebot.AvaIre;
import com.avairebot.cache.CacheItem;
import com.avairebot.cache.CacheType;
import com.avairebot.commands.CommandContainer;
import com.avairebot.commands.CommandHandler;
import com.avairebot.contracts.commands.ThreadCommand;
import com.avairebot.contracts.handlers.EventAdapter;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.factories.MessageFactory;
import com.avairebot.handlers.DatabaseEventHolder;
import com.avairebot.middleware.MiddlewareStack;
import com.avairebot.shared.DiscordConstants;
import com.avairebot.utilities.ArrayUtil;
import com.avairebot.utilities.NumberUtil;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageEventAdapter extends EventAdapter {

    public static final Set<Long> hasReceivedInfoMessageInTheLastMinute = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageEventAdapter.class);
    private static final Pattern userRegEX = Pattern.compile("<@(!|)+[0-9]{16,}+>", Pattern.CASE_INSENSITIVE);
    private static final String mentionMessage = String.join("\n", Arrays.asList(
        "Hi there! I'm **%s**, a multipurpose Discord bot built for fun by %s!",
        "You can see what commands I have by using the `%s` command.",
        "",
        "I am currently running **AvaIre v%s**",
        "",
        "You can find all of my source code on github:",
        "https://github.com/avaire/avaire",
        "",
        "If you like me please vote for AvaIre to help me grow:",
        "https://discordbots.org/bot/avaire/vote"
    ));

    private static Pattern commandRegEx = null;
    private static int maxCommandTriggerSize = -1;

    /**
     * Instantiates the event adapter and sets the avaire class instance.
     *
     * @param avaire The AvaIre application class instance.
     */
    public MessageEventAdapter(AvaIre avaire) {
        super(avaire);
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (!isValidMessage(event.getAuthor())) {
            return;
        }

        if (event.getChannelType().isGuild() && !event.getTextChannel().canTalk()) {
            return;
        }

        loadDatabasePropertiesIntoMemory(event).thenAccept(databaseEventHolder -> {
            if (!avaire.areWeReadyYet()) {
                return;
            }

            CommandContainer container = CommandHandler.getCommand(avaire, event.getMessage(), event.getMessage().getContentRaw());
            if (container != null && canExecuteCommand(event, container)) {
                invokeMiddlewareStack(new MiddlewareStack(event.getMessage(), container, databaseEventHolder));
                return;
            }

            if (isMentionableAction(event)) {
                container = CommandHandler.getLazyCommand(ArrayUtil.toArguments(event.getMessage().getContentRaw())[1]);
                if (container != null && canExecuteCommand(event, container)) {
                    invokeMiddlewareStack(new MiddlewareStack(event.getMessage(), container, databaseEventHolder, true));
                    return;
                }
            }

            if (isSingleBotMention(event.getMessage().getContentRaw().trim())) {
                sendTagInformationMessage(event);
                return;
            }

            if (!event.getChannelType().isGuild()) {
                sendInformationMessage(event);
            }
        });
    }

    private boolean isValidMessage(User author) {
        return !author.isBot() || author.getIdLong() == DiscordConstants.SENITHER_BOT_ID;
    }

    private void invokeMiddlewareStack(MiddlewareStack stack) {
        if (stack.getCommand() instanceof ThreadCommand) {
            ((ThreadCommand) stack.getCommand()).submitTask(stack::next);
            return;
        }
        stack.next();
    }

    private boolean canExecuteCommand(MessageReceivedEvent event, CommandContainer container) {
        if (!container.getCommand().isAllowedInDM() && !event.getChannelType().isGuild()) {
            MessageFactory.makeWarning(event.getMessage(), ":warning: You can not use this command in direct messages!").queue();
            return false;
        }
        return true;
    }

    private boolean isMentionableAction(MessageReceivedEvent event) {
        if (!event.getMessage().isMentioned(avaire.getSelfUser())) {
            return false;
        }

        String[] args = event.getMessage().getContentRaw().split(" ");
        return args.length >= 2 &&
            userRegEX.matcher(args[0]).matches() &&
            event.getMessage().getMentionedUsers().get(0).getId().equals(avaire.getSelfUser().getId());

    }

    private boolean isSingleBotMention(String rawContent) {
        return rawContent.equals("<@" + avaire.getSelfUser().getId() + ">") ||
            rawContent.equals("<!@" + avaire.getSelfUser().getId() + ">");
    }


    private void sendTagInformationMessage(MessageReceivedEvent event) {
        String author = "**Senither#0001**";
        if (event.getMessage().getChannelType().isGuild() && event.getGuild().getMemberById(88739639380172800L) != null) {
            author = "<@88739639380172800>";
        }

        MessageFactory.makeEmbeddedMessage(event.getMessage().getChannel(), Color.decode("#E91E63"), String.format(mentionMessage,
            avaire.getSelfUser().getName(),
            author,
            CommandHandler.getLazyCommand("help").getCommand().generateCommandTrigger(),
            AppInfo.getAppInfo().VERSION
        ))
            .setFooter("This message will be automatically deleted in one minute.")
            .queue(message -> message.delete().queueAfter(1, TimeUnit.MINUTES, null, RestActionUtil.IGNORE));
    }

    private void sendInformationMessage(MessageReceivedEvent event) {
        LOGGER.info("Private message received from user(ID: {}) that does not match any commands!",
            event.getAuthor().getId()
        );

        if (hasReceivedInfoMessageInTheLastMinute.contains(event.getAuthor().getIdLong())) {
            return;
        }

        hasReceivedInfoMessageInTheLastMinute.add(event.getAuthor().getIdLong());

        try {
            ArrayList<String> strings = new ArrayList<>();
            strings.addAll(Arrays.asList(
                "To invite me to your server, use this link:",
                "*:oauth*",
                "",
                "You can use `!help` to see a list of all the categories of commands.",
                "You can use `!help category` to see a list of commands for that category.",
                "For specific command help, use `!help command` (for example `!help !ping`, `!help ping` also works)"
            ));

            strings.add("\n**Full list of commands**\n*https://avairebot.com/docs/commands*");
            strings.add("\nAvaIre Support Server:\n*https://avairebot.com/support*");

            MessageFactory.makeEmbeddedMessage(event.getMessage(), Color.decode("#E91E63"), String.join("\n", strings))
                .set("oauth", avaire.getConfig().getString("discord.oauth"))
                .set("botId", avaire.getSelfUser().getId())
                .queue();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private CompletableFuture<DatabaseEventHolder> loadDatabasePropertiesIntoMemory(final MessageReceivedEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            if (!event.getChannelType().isGuild()) {
                return new DatabaseEventHolder(null);
            }

            GuildTransformer guild = looksLikeCommand(event.getMessage())
                ? GuildController.fetchGuild(avaire, event.getMessage(), event.getChannel())
                : GuildController.fetchGuild(avaire, event.getMessage());

            return new DatabaseEventHolder(guild);
        });
    }

    private boolean isThrottled(AvaIre avaire, String fingerprint, int limit, int decay) {
        CacheItem cacheItem = avaire.getCache().getAdapter(CacheType.MEMORY).getRaw(fingerprint);

        if (cacheItem == null) {
            avaire.getCache().getAdapter(CacheType.MEMORY).put(fingerprint, 1, decay);
            return false;
        }

        int value = NumberUtil.parseInt(cacheItem.getValue().toString(), 0);
        if (value++ >= limit) {
            return true;
        }

        avaire.getCache().getAdapter(CacheType.MEMORY).put(fingerprint, value, decay);
        return false;
    }

    private boolean looksLikeCommand(Message message) {
        if (message.getGuild().getMembers().stream().filter(member -> member.getUser().isBot()).count() > 5) {
            return false;
        }

        String content = message.getContentRaw();
        if (commandRegEx == null) {
            maxCommandTriggerSize = -1;
            Set<String> commandTriggers = new HashSet<>();
            for (CommandContainer container : CommandHandler.getCommands()) {
                for (String trigger : container.getCommand().getTriggers()) {
                    if (trigger.length() > maxCommandTriggerSize) {
                        maxCommandTriggerSize = trigger.length();
                    }
                    commandTriggers.add(Matcher.quoteReplacement(trigger));
                }
            }
            maxCommandTriggerSize += 4;
            commandRegEx = Pattern.compile(String.format(
                "^(\\S){1,3}(%s)$", String.join("|", commandTriggers)),
                Pattern.CASE_INSENSITIVE
            );
        }

        return commandRegEx.matcher(
            content.substring(0, Math.min(maxCommandTriggerSize, content.length()))
        ).matches();
    }
}
