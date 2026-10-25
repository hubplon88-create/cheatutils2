package com.cheatutils.module;

import com.cheatutils.module.modules.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    public static KillAura killAura;
    public static Esp esp;
    public static Xray xray;
    public static AutoClicker autoClicker;
    public static AutoFarm autoFarm;
    public static Visuals visuals;

    public static void init() {
        register(killAura = new KillAura());
        register(esp = new Esp());
        register(xray = new Xray());
        register(autoClicker = new AutoClicker());
        register(autoFarm = new AutoFarm());
        register(visuals = new Visuals());
    }

    private static void register(Module m) {
        MODULES.add(m);
    }

    public static List<Module> getModules() {
        return MODULES;
    }

    public static void tickAll() {
        for (Module m : MODULES) {
            if (m.isEnabled()) {
                m.onTick();
            }
        }
    }
}
