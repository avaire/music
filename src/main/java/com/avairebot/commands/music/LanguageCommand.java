package com.avairebot.commands.music;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.chat.SimplePaginator;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.language.Language;
import com.avairebot.utilities.NumberUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LanguageCommand extends Command {

    public LanguageCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Language Command";
    }

    @Override
    public String getDescription() {
        return "Show a list of available languages or set a language that should be used for the server.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command [page]` - Displays a list of languages, 10 languages per page.",
            "`:command [code]` - Sets the language to the given language code."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command 2` - Displays the languages on page 2",
            "`:command english` - Changes the language of the bot to English"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("language", "lang");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "require:user,general.administrator",
            "throttle:guild,1,5"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendLanguageList(context, 1);
        }

        if (NumberUtil.isNumeric(args[0])) {
            return sendLanguageList(context, NumberUtil.parseInt(args[0], 1));
        }

        Language language = Language.parse(String.join(" ", args));
        if (language == null) {
            return sendErrorMessage(
                context,
                "Invalid language code given, `{0}` is not a valid language code, or it is not yet supported!",
                String.join(" ", args)
            );
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "errors.errorOccurredWhileLoading", "server settings");
        }

        try {
            avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .where("id", context.getGuild().getId())
                .update(statement -> statement.set("locale", language.getCode()));
            transformer.setLocale(language.getCode());

            context.makeSuccess("The servers language has been successfully been updated to use the :name language!")
                .set("name", language.getNativeName())
                .queue();
        } catch (SQLException e) {
            AvaIre.getLogger().error("Failed to update the language for a server({}), error: " + e.getMessage(),
                context.getGuild().getId()
            );
            return sendErrorMessage(context, "Failed to update the servers language settings, please try again, if this problem persists, please contact one of the bot developers about it.");
        }

        return true;
    }

    private boolean sendLanguageList(CommandMessage context, int pageNum) {
        List<String> items = new ArrayList<>();
        for (Language lang : Language.values()) {
            items.add(String.format("`%s` %s", lang.getCode(), lang.getNativeName()));
        }

        SimplePaginator paginator = new SimplePaginator(items, 10, pageNum);

        List<String> messages = new ArrayList<>();
        paginator.forEach((index, key, val) -> messages.add((String) val));

        String trigger = generateCommandTrigger();
        String note = "AvaIre supports several user-contributed languages that you can select with this command. Translations may not be 100% accurate or complete.\nTo select a language use `" + trigger + " <code>`";

        context.makeInfo(":note\n\n:languages\n\n:paginator")
            .set("note", note)
            .set("languages", String.join("\n", messages))
            .set("paginator", paginator.generateFooter(generateCommandTrigger()))
            .queue();

        return false;
    }
}
