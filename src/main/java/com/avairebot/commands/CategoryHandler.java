package com.avairebot.commands;

import com.avairebot.contracts.commands.Command;
import com.avairebot.utilities.RandomUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CategoryHandler {

    private static final List<Category> VALUES = new ArrayList<>();
    private static String PREFIX;

    static {
        VALUES.add(new Category("all").setGlobal(true));
    }

    public static boolean addCategory(@Nonnull String name) {
        for (Category category : VALUES) {
            if (category.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }

        VALUES.add(new Category(name));
        return true;
    }

    public static Category fromLazyName(@Nonnull String name) {
        return fromLazyName(name, false);
    }

    public static Category fromLazyName(@Nonnull String name, boolean includeGlobals) {
        name = name.toLowerCase();

        for (Category category : getValues()) {
            if (!includeGlobals && category.isGlobal()) {
                continue;
            }

            if (category.getName().toLowerCase().startsWith(name)) {
                return category;
            }
        }
        return null;
    }

    public static Category fromCommand(@Nonnull Command command) {
        Category category = command.getCategory();
        if (category != null) {
            return category;
        }

        String[] path = command.getClass().getName().split("\\.");
        String commandPackage = path[path.length - 2];

        for (Category cat : getValues()) {
            if (cat.getName().equalsIgnoreCase(commandPackage)) {
                return cat;
            }
        }
        return null;
    }

    public static Category random() {
        return VALUES.get(RandomUtil.getInteger(VALUES.size()));
    }

    public static Category random(boolean includeGlobals) {
        return VALUES.stream()
            .filter(category -> !category.isGlobal())
            .findAny().orElseGet(CategoryHandler::random);
    }

    public static List<Category> getValues() {
        return VALUES;
    }

    public static String getPrefix() {
        return PREFIX;
    }

    public static void setPrefix(String prefix) {
        CategoryHandler.PREFIX = prefix;
    }
}
