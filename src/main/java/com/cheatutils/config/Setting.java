package com.cheatutils.config;

import java.util.function.Consumer;

/**
 * Pojedyncze ustawienie modułu (slider liczbowy, checkbox, lub enum/mode).
 */
public class Setting<T> {
    public enum Type { BOOLEAN, DOUBLE, MODE }

    private final String name;
    private final Type type;
    private T value;
    private final T min;
    private final T max;
    private String[] modes;
    private Consumer<T> onChange;

    private Setting(String name, Type type, T value, T min, T max) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public static Setting<Boolean> bool(String name, boolean def) {
        return new Setting<>(name, Type.BOOLEAN, def, null, null);
    }

    public static Setting<Double> range(String name, double def, double min, double max) {
        return new Setting<>(name, Type.DOUBLE, def, min, max);
    }

    public static Setting<String> mode(String name, String def, String... options) {
        Setting<String> s = new Setting<>(name, Type.MODE, def, null, null);
        s.modes = options;
        return s;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public T getValue() { return value; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public String[] getModes() { return modes; }

    public void setValue(T value) {
        this.value = value;
        if (onChange != null) onChange.accept(value);
    }

    public Setting<T> onChange(Consumer<T> cb) {
        this.onChange = cb;
        return this;
    }

    public double asDouble() { return ((Number) value).doubleValue(); }
    public boolean asBool() { return (Boolean) value; }
    public String asString() { return (String) value; }
}
