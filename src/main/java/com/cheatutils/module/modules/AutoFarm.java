package com.cheatutils.module.modules;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;
import net.minecraft.client.Minecraft;

/** Trzymasz wybrany przycisk myszy -> naprzemiennie A przez X s i D przez Y s. */
public class AutoFarm extends Module {
    private final Setting<String> trigger = register(Setting.mode("Przycisk aktywujacy", "PRAWY", "LEWY", "PRAWY"));
    private final Setting<Double> durationA = register(Setting.range("Czas A (s)", 2.0, 0.1, 30.0));
    private final Setting<Double> durationD = register(Setting.range("Czas D (s)", 2.0, 0.1, 30.0));

    private boolean phaseA = true;
    private int phaseTicks = 0;

    public AutoFarm() {
        super("AutoFarm", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        releaseKeys();
        phaseA = true;
        phaseTicks = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            releaseKeys();
            return;
        }

        boolean left = trigger.asString().equals("LEWY");
        boolean held = left ? mc.mouseHandler.isLeftPressed() : mc.mouseHandler.isRightPressed();
        if (!held) {
            releaseKeys();
            phaseA = true;
            phaseTicks = 0;
            return;
        }

        mc.options.keyLeft.setDown(phaseA);
        mc.options.keyRight.setDown(!phaseA);

        int limit = (int) Math.round(20.0 * (phaseA ? durationA.asDouble() : durationD.asDouble()));
        phaseTicks++;
        if (phaseTicks >= Math.max(1, limit)) {
            phaseTicks = 0;
            phaseA = !phaseA;
        }
    }

    private void releaseKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
        }
    }
}
