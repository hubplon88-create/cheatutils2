package com.cheatutils.module.modules;

import com.cheatutils.module.Module;
import net.minecraft.client.Minecraft;

/** Fullbright (gamma). */
public class Visuals extends Module {
    private double savedGamma = 1.0;

    public Visuals() {
        super("Fullbright", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) savedGamma = mc.options.gamma().get();
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) mc.options.gamma().set(savedGamma);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) mc.options.gamma().set(15.0);
    }
}
