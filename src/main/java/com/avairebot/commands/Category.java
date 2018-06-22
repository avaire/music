package com.avairebot.commands;

public class Category {

    private final String name;

    private boolean isGlobal = false;

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    Category setGlobal(boolean value) {
        isGlobal = value;
        return this;
    }
}
