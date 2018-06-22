package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.DatabaseEngine;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateGuildTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Wed, Sep 20, 2017 5:11 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.GUILD_TABLE_NAME, table -> {
            table.String("id", 32);
            table.Integer("type", 2).defaultValue(0);
            table.String("owner", 32);
            table.Text("name");

            table.String("icon").nullable();
            table.String("local", 12).nullable();
            table.String("level_channel").nullable();
            table.String("music_channel_text").nullable();
            table.String("music_channel_voice").nullable();
            table.String("autorole").nullable();

            table.Integer("dj_level").defaultValue(0);
            table.Integer("default_volume").defaultValue(50);

            table.Timestamps();

            table.setEngine(DatabaseEngine.InnoDB);
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.GUILD_TABLE_NAME);
    }
}
