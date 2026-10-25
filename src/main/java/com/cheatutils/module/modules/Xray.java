package com.cheatutils.module.modules;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/** Xray jako "ore ESP": podswietla wybrane rudy przez sciany. */
public class Xray extends Module {
    public final Setting<Double> scanRadius = register(Setting.range("Promien skanu", 24.0, 8.0, 48.0));
    private final Map<Block, Setting<Boolean>> oreToggles = new LinkedHashMap<>();

    public Xray() {
        super("Xray", Category.WORLD);
        addOre("Diamond", Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        addOre("Emerald", Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        addOre("Gold", Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        addOre("Iron", Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        addOre("Redstone", Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        addOre("Lapis", Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        addOre("Copper", Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        addOre("Ancient Debris", Blocks.ANCIENT_DEBRIS);
    }

    private void addOre(String label, Block... blocks) {
        Setting<Boolean> s = register(Setting.bool(label, true));
        for (Block b : blocks) oreToggles.put(b, s);
    }

    public boolean isTracked(Block block) {
        Setting<Boolean> s = oreToggles.get(block);
        return s != null && s.asBool();
    }
}
