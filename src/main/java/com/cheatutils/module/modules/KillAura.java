package com.cheatutils.module.modules;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class KillAura extends Module {
    private final Setting<Double> range = register(Setting.range("Zasieg", 4.0, 2.0, 8.0));
    private final Setting<Double> cps = register(Setting.range("Ataki/s", 8.0, 1.0, 20.0));
    private final Setting<Boolean> players = register(Setting.bool("Atakuj graczy", false));
    private final Setting<Boolean> mobs = register(Setting.bool("Atakuj moby", true));
    private final Setting<Boolean> animals = register(Setting.bool("Atakuj zwierzeta", false));

    private int cooldownTicks = 0;

    public KillAura() {
        super("KillAura", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        double r = range.asDouble();
        AABB area = player.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, area, x -> isValidTarget(x, player))) {
            double d = e.distanceToSqr(player);
            if (d <= r * r && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }

        if (best != null) {
            mc.gameMode.attack(player, best);
            player.swing(InteractionHand.MAIN_HAND);
            cooldownTicks = (int) Math.round(20.0 / Math.max(1.0, cps.asDouble()));
        }
    }

    private boolean isValidTarget(LivingEntity e, LocalPlayer self) {
        if (e == self || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof Player) return players.asBool();
        MobCategory cat = e.getType().getCategory();
        if (cat == MobCategory.MONSTER) return mobs.asBool();
        if (cat == MobCategory.MISC) return false;
        return animals.asBool();
    }
}
