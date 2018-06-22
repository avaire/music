package com.avairebot.database.transformers;

import com.avairebot.audio.DJGuildLevel;
import com.avairebot.contracts.database.transformers.Transformer;
import com.avairebot.database.collection.DataRow;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.core.entities.Guild;

public class GuildTransformer extends Transformer {

    private final GuildTypeTransformer guildType;

    private String id;
    private String name;
    private String nameRaw;
    private String locale;

    private String musicChannelText = null;
    private String musicChannelVoice = null;
    private int defaultVolume = 50;
    private DJGuildLevel djGuildLevel = null;

    public GuildTransformer(Guild guild) {
        super(null);

        locale = null;
        id = guild.getId();
        name = guild.getName();
        nameRaw = guild.getName();
        guildType = new GuildTypeTransformer(null);
    }

    public GuildTransformer(DataRow data) {
        super(data);

        guildType = new GuildTypeTransformer(data);

        if (hasData()) {
            id = data.getString("id");
            name = data.getString("name");
            nameRaw = data.get("name").toString();
            locale = data.getString("local");

            musicChannelText = data.getString("music_channel_text");
            musicChannelVoice = data.getString("music_channel_voice");
            djGuildLevel = DJGuildLevel.fromId(data.getInt("dj_level", DJGuildLevel.getNormal().getId()));
            defaultVolume = data.getInt("default_volume", 50);

            // Sets the default volume to a value between 10 and 100.
            defaultVolume = NumberUtil.getBetween(defaultVolume, 10, 100);
        }

        reset();
    }

    public String getId() {
        return id;
    }

    public GuildTypeTransformer getType() {
        return guildType;
    }

    public String getName() {
        return name;
    }

    public String getNameRaw() {
        return nameRaw;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String code) {
        this.locale = code;
    }

    public String getMusicChannelText() {
        return musicChannelText;
    }

    public void setMusicChannelText(String musicChannelText) {
        this.musicChannelText = musicChannelText;
    }

    public String getMusicChannelVoice() {
        return musicChannelVoice;
    }

    public void setMusicChannelVoice(String musicChannelVoice) {
        this.musicChannelVoice = musicChannelVoice;
    }

    public DJGuildLevel getDJLevel() {
        if (djGuildLevel == null) {
            djGuildLevel = DJGuildLevel.getNormal();
        }
        return djGuildLevel;
    }

    public void setDJLevel(DJGuildLevel djGuildLevel) {
        this.djGuildLevel = djGuildLevel;
    }

    public int getDefaultVolume() {
        return defaultVolume;
    }

    public void setDefaultVolume(int defaultVolume) {
        this.defaultVolume = defaultVolume;
    }
}
