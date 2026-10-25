package com.cheatutils.module.modules;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;

public class Esp extends Module {
    public final Setting<Boolean> players = register(Setting.bool("Gracze", true));
    public final Setting<Boolean> mobs = register(Setting.bool("Moby", true));
    public final Setting<Boolean> animals = register(Setting.bool("Zwierzeta", false));
    public final Setting<Boolean> drops = register(Setting.bool("Itemy (drops)", true));
    public final Setting<Boolean> storage = register(Setting.bool("Skrzynie/beczki/piece", true));
    public final Setting<Boolean> boxes = register(Setting.bool("Boxy", true));
    public final Setting<Boolean> tracers = register(Setting.bool("Tracery", true));
    public final Setting<Double> maxDistance = register(Setting.range("Max dystans", 64.0, 8.0, 128.0));

    public Esp() {
        super("ESP", Category.RENDER);
    }
}
