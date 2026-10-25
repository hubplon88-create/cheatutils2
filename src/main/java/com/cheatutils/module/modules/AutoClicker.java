package com.cheatutils.module.modules;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoClicker extends Module {
    private final Setting<String> button = register(Setting.mode("Przycisk", "LEWY", "LEWY", "PRAWY"));
    private final Setting<Double> cps = register(Setting.range("Klikniecia/s", 12.0, 1.0, 20.0));

    private int cooldown = 0;

    public AutoClicker() {
        super("AutoClicker", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;

        boolean left = button.asString().equals("LEWY");
        boolean held = left ? mc.mouseHandler.isLeftPressed() : mc.mouseHandler.isRightPressed();
        if (!held) {
            cooldown = 0;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult eh && left) {
            mc.gameMode.attack(mc.player, eh.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (hit instanceof BlockHitResult bh) {
            if (left) {
                mc.gameMode.startDestroyBlock(bh.getBlockPos(), bh.getDirection());
            } else {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bh);
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        cooldown = Math.max(1, (int) Math.round(20.0 / Math.max(1.0, cps.asDouble())));
    }
}
