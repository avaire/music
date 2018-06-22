package com.avairebot.database.migrate.migrations;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CreateGuildTypeTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Wed, Sep 20, 2017 5:23 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        if (!createTable(schema)) {
            return false;
        }

        return schema.getDbm().newQueryBuilder(Constants.GUILD_TYPES_TABLE_NAME).insert(
            createRecord("VIP", 10, 50, 50),
            createRecord("VIP+", 25, 100, 150),
            createRecord("No Limit", 9999, 9999, 9999)
        ).size() == 2;
    }

    private Map<String, Object> createRecord(String name, int playlistLists, int playlistSongs, int aliases) {
        Map<String, Integer> playlist = new HashMap<>();
        playlist.put("lists", playlistLists);
        playlist.put("songs", playlistSongs);

        Map<String, Object> limits = new HashMap<>();
        Map<String, Object> items = new HashMap<>();

        limits.put("playlist", playlist);
        limits.put("aliases", aliases);

        items.put("name", name);
        items.put("limits", AvaIre.GSON.toJson(limits));

        return items;
    }

    private boolean createTable(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.GUILD_TYPES_TABLE_NAME, table -> {
            table.Increments("id");
            table.String("name");
            table.Text("limits");
            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.GUILD_TYPES_TABLE_NAME);
    }
}
