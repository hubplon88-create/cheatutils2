package com.cheatutils;

import com.cheatutils.esp.EspRenderer;
import com.cheatutils.gui.ClickGuiScreen;
import com.cheatutils.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CheatUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("cheatutils", "main"));

        KeyMapping openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cheatutils.openmenu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            if (client.player != null) {
                ModuleManager.tickAll();
                EspRenderer.tickScan();
            }
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render);
    }
}
