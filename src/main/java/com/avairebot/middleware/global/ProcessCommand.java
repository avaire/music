package com.avairebot.middleware.global;

import com.avairebot.AvaIre;
import com.avairebot.commands.AliasCommandContainer;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.factories.MessageFactory;
import com.avairebot.middleware.MiddlewareStack;
import com.avairebot.shared.SentryConstants;
import com.avairebot.utilities.ArrayUtil;
import com.avairebot.utilities.CheckPermissionUtil;
import com.avairebot.utilities.RestActionUtil;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ProcessCommand extends Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCommand.class);

    private final static String COMMAND_OUTPUT = "Executing Command \"%command%\" in \"%category%\" category in shard %shard%:"
        + "\n\t\tUser:\t %author%"
        + "\n\t\tServer:\t %server%"
        + "\n\t\tChannel: %channel%"
        + "\n\t\tMessage: %message%";

    public ProcessCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        CheckPermissionUtil.PermissionCheckType permissionType = CheckPermissionUtil.canSendMessages(message.getChannel());
        if (!stack.getCommandContainer().getCategory().getName().equals("System") && !permissionType.canSendEmbed()) {
            if (!permissionType.canSendMessage()) {
                return false;
            }

            message.getTextChannel().sendMessage("I don't have the `Embed Links` permission, the permission is required for all of my commands to work.\nhttps://avairebot.com/missing-embed-permissions.png\nThis error can sometimes occur when the `everyone` role has it disabled and no other roles enables it.\nThis message will be automatically deleted in 30 seconds.")
                .queue(newMessage -> newMessage.delete().queueAfter(30, TimeUnit.SECONDS, null, RestActionUtil.IGNORE));

            return false;
        }

        String[] arguments = ArrayUtil.toArguments(message.getContentRaw());

        AvaIre.getLogger().info(COMMAND_OUTPUT
            .replace("%command%", stack.getCommand().getName())
            .replace("%category%", stack.getCommandContainer().getCategory().getName())
            .replace("%author%", generateUsername(message))
            .replace("%server%", generateServer(message))
            .replace("%channel%", generateChannel(message))
            .replace("%message%", message.getContentRaw())
            .replace("%shard%", message.getJDA().getShardInfo().getShardString())
        );

        try {
            String[] commandArguments = Arrays.copyOfRange(arguments, stack.isMentionableCommand() ? 2 : 1, arguments.length);
            if (stack.getCommandContainer() instanceof AliasCommandContainer) {
                AliasCommandContainer container = (AliasCommandContainer) stack.getCommandContainer();

                return runCommand(stack,
                    new CommandMessage(
                        stack.getCommandContainer(),
                        stack.getDatabaseEventHolder(),
                        message,
                        stack.isMentionableCommand(),
                        container.getAliasArguments()
                    ),
                    combineArguments(container.getAliasArguments(), commandArguments)
                );
            }

            return runCommand(stack, new CommandMessage(
                    stack.getCommandContainer(),
                    stack.getDatabaseEventHolder(),
                    message,
                    stack.isMentionableCommand(),
                    new String[0]
                ),
                commandArguments
            );
        } catch (Exception ex) {
            if (ex instanceof InsufficientPermissionException) {
                MessageFactory.makeError(message, "Error: " + ex.getMessage())
                    .queue(newMessage -> newMessage.delete().queueAfter(30, TimeUnit.SECONDS, null, RestActionUtil.IGNORE));

                return false;
            } else if (ex instanceof FriendlyException) {
                MessageFactory.makeError(message, "Error: " + ex.getMessage())
                    .queue(newMessage -> newMessage.delete().queueAfter(30, TimeUnit.SECONDS, null, RestActionUtil.IGNORE));
            }

            MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_GUILD, message.getGuild() != null ? message.getGuild().getId() : "PRIVATE");
            MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_SHARD, message.getJDA().getShardInfo().getShardString());
            MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_CHANNEL, message.getChannel().getId());
            MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_AUTHOR, message.getAuthor().getId());
            MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_MESSAGE, message.getContentRaw());
            LOGGER.error("An error occurred while running the " + stack.getCommand().getName(), ex);
            return false;
        }
    }

    private boolean runCommand(MiddlewareStack stack, CommandMessage message, String[] args) {
        return stack.getCommand().onCommand(message, args);
    }

    private String generateUsername(Message message) {
        return String.format("%s#%s [%s]",
            message.getAuthor().getName(),
            message.getAuthor().getDiscriminator(),
            message.getAuthor().getId()
        );
    }

    private String generateServer(Message message) {
        if (!message.getChannelType().isGuild()) {
            return "PRIVATE";
        }

        return String.format("%s [%s]",
            message.getGuild().getName(),
            message.getGuild().getId()
        );
    }

    private CharSequence generateChannel(Message message) {
        if (!message.getChannelType().isGuild()) {
            return "PRIVATE";
        }

        return String.format("%s [%s]",
            message.getChannel().getName(),
            message.getChannel().getId()
        );
    }

    private String[] combineArguments(String[] aliasArguments, String[] userArguments) {
        int length = aliasArguments.length + userArguments.length;

        String[] result = new String[length];

        System.arraycopy(aliasArguments, 0, result, 0, aliasArguments.length);
        System.arraycopy(userArguments, 0, result, aliasArguments.length, userArguments.length);

        return result;
    }
}
