package com.avairebot.chat;

import com.avairebot.contracts.chat.Restable;
import com.avairebot.utilities.StringReplacementUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderMessage extends Restable {

    private final Map<String, String> placeholders = new HashMap<>();

    private EmbedBuilder builder;
    private String message;

    private PlaceholderType globalPlaceholder;
    private Object globalObject;

    public PlaceholderMessage(MessageChannel channel, EmbedBuilder builder, String message) {
        super(channel);

        this.builder = builder;
        this.message = message;
    }

    public PlaceholderMessage(EmbedBuilder builder, String message) {
        super(null);

        this.builder = builder;
        this.message = message;
    }

    public PlaceholderMessage set(String placeholder, String value) {
        placeholders.put(placeholder, value);
        return this;
    }

    public PlaceholderMessage set(String placeholder, Object value) {
        return set(placeholder, value.toString());
    }

    public PlaceholderMessage setTitle(String title, String url) {
        builder.setTitle(trimString(title, MessageEmbed.TITLE_MAX_LENGTH), url);
        return this;
    }

    public PlaceholderMessage setTitle(String title) {
        return setTitle(title, null);
    }

    public PlaceholderMessage setFooter(String text, String iconUrl) {
        builder.setFooter(trimString(text, MessageEmbed.TITLE_MAX_LENGTH), iconUrl);
        return this;
    }

    public PlaceholderMessage setFooter(String text) {
        return setFooter(text, null);
    }

    public PlaceholderMessage setThumbnail(String thumbnail) {
        builder.setThumbnail(thumbnail);
        return this;
    }

    public PlaceholderMessage setImage(String image) {
        builder.setImage(image);
        return this;
    }

    public PlaceholderMessage setTimestamp(Instant timestamp) {
        builder.setTimestamp(timestamp);
        return this;
    }

    public PlaceholderMessage setAuthor(String name, String url, String iconUrl) {
        builder.setAuthor(name, url, iconUrl);
        return this;
    }

    public PlaceholderMessage setAuthor(String name, String url) {
        builder.setAuthor(name, url);
        return this;
    }

    public PlaceholderMessage setAuthor(String name) {
        builder.setAuthor(name);
        return this;
    }

    public PlaceholderMessage setDescription(String description) {
        message = description;
        return this;
    }

    public PlaceholderMessage setColor(Color color) {
        builder.setColor(color);
        return this;
    }

    public PlaceholderMessage addField(String name, String value, boolean inline) {
        builder.addField(
            trimString(name, MessageEmbed.TITLE_MAX_LENGTH),
            trimString(value, MessageEmbed.VALUE_MAX_LENGTH),
            inline
        );

        return this;
    }

    public PlaceholderMessage addField(MessageEmbed.Field field) {
        builder.addField(field);
        return this;
    }

    public PlaceholderMessage setGlobalPlaceholderType(PlaceholderType type, Object object) {
        globalPlaceholder = type;
        globalObject = object;
        return this;
    }

    public EmbedBuilder build() {
        return builder.setDescription(formatMessage());
    }

    @Override
    public MessageEmbed buildEmbed() {
        return builder.setDescription(formatMessage()).build();
    }

    @Override
    public String toString() {
        return formatMessage();
    }

    private String formatMessage() {
        if (placeholders.isEmpty()) {
            return trimString(
                formatGlobalMessage(message),
                MessageEmbed.TEXT_MAX_LENGTH
            );
        }

        List<String> keys = new ArrayList<>(placeholders.keySet());
        keys.sort((o1, o2) -> o2.length() - o1.length());
        keys.forEach(key -> {
            message = StringReplacementUtil.replaceAll(
                message, ":" + key, placeholders.get(key)
            );
        });

        return trimString(
            formatGlobalMessage(message),
            MessageEmbed.TEXT_MAX_LENGTH
        );
    }

    private String formatGlobalMessage(String message) {
        if (globalPlaceholder == null) {
            return message;
        }
        return globalPlaceholder.parse(globalObject, message);
    }

    private String trimString(String string, int length) {
        if (string == null) {
            return null;
        }

        if (string.length() < length) {
            return string;
        }

        return string.substring(0, length);
    }
}
