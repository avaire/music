package com.avairebot.middleware;

import com.avairebot.AvaIre;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.DJGuildLevel;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.factories.MessageFactory;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

public class RequireDJLevelMiddleware extends Middleware {

    public RequireDJLevelMiddleware(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        if (args.length > 0) {
            DJGuildLevel level = DJGuildLevel.fromName(args[0]);

            if (level != null && AudioHandler.getDefaultAudioHandler().canRunDJAction(avaire, message, level)) {
                return stack.next();
            }

            return sendErrorMessage(message, stack);
        }

        if (AudioHandler.getDefaultAudioHandler().canRunDJAction(avaire, message, DJGuildLevel.NORMAL)) {
            return stack.next();
        }

        return sendErrorMessage(message, stack);
    }

    @SuppressWarnings("ConstantConditions")
    private boolean sendErrorMessage(Message message, MiddlewareStack stack) {
        MessageFactory.makeError(message, "The `DJ` Discord role is required to run this command!").queue();

        return false;
    }
}
