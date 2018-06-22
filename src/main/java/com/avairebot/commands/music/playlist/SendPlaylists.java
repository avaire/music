package com.avairebot.commands.music.playlist;

import com.avairebot.AvaIre;
import com.avairebot.chat.SimplePaginator;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.music.PlaylistCommand;
import com.avairebot.contracts.commands.playlist.PlaylistSubCommand;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.NumberUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SendPlaylists extends PlaylistSubCommand {

    public SendPlaylists(AvaIre avaire, PlaylistCommand command) {
        super(avaire, command);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args, GuildTransformer guild, Collection playlists) {
        SimplePaginator paginator = new SimplePaginator(playlists.sort(
            Comparator.comparing(dataRow -> dataRow.getString("name"))
        ).getItems(), 5);

        if (args.length > 0) {
            paginator.setCurrentPage(NumberUtil.parseInt(args[0], 1));
        }

        List<String> messages = new ArrayList<>();
        paginator.forEach((index, key, val) -> {
            DataRow row = (DataRow) val;

            messages.add(String.format(
                context.i18n("playlistLine"),
                row.getString("name"),
                row.getInt("amount")
            ));
        });

        String counter = String.format(context.i18n("playlistSize"),
            playlists.size(), guild.getType().getLimits().getPlaylist().getPlaylists()
        );

        context.makeInfo("\u2022 " +
            String.join("\n\u2022 ", messages) + "\n\n" +
            paginator.generateFooter(command.generateCommandTrigger())
        ).setTitle(String.format(
            context.i18n("playlistTitle"),
            counter
        )).queue();

        return true;
    }
}
