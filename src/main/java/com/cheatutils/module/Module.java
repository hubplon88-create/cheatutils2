package com.cheatutils.module;

import com.cheatutils.config.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category { COMBAT, RENDER, WORLD, PLAYER, MOVEMENT }

    private final String name;
    private final Category category;
    private boolean enabled = false;
    private int keyCode = -1; // GLFW keycode, -1 = brak bindu
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected <T> Setting<T> register(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public List<Setting<?>> getSettings() { return settings; }
    public int getKeyCode() { return keyCode; }
    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
    }

    /** Wywoływane co klatkę/tick gdy moduł jest aktywny. Nadpisać w podklasie. */
    public void onTick() {}

    protected void onEnable() {}
    protected void onDisable() {}
}
