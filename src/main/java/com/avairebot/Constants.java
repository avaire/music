package com.avairebot;

import java.io.File;

public class Constants {

    public static final File STORAGE_PATH = new File("storage");

    // Database Tables
    public static final String GUILD_TABLE_NAME = "guilds";
    public static final String GUILD_TYPES_TABLE_NAME = "guild_types";
    public static final String MUSIC_PLAYLIST_TABLE_NAME = "playlists";

    // Package Specific Information
    public static final String PACKAGE_COMMAND_PATH = "com.avairebot.commands";
    public static final String PACKAGE_JOB_PATH = "com.avairebot.scheduler";

    public static final String SOURCE_URI = "https://github.com/avaire/avaire/tree/master/src/main/java/com/avairebot/commands/%s/%s.java";
}
