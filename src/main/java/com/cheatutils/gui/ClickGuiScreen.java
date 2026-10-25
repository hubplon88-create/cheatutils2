package com.cheatutils.gui;

import com.cheatutils.config.Setting;
import com.cheatutils.module.Module;
import com.cheatutils.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClickGUI. Klikniecia wykrywane przez polling stanu myszy w render() (bez nadpisywania
 * metod mouse*, ktorych sygnatury zmienily sie w 1.21.9+).
 * LPM na module = wlacz/wylacz, PPM = rozwin ustawienia.
 */
public class ClickGuiScreen extends Screen {

    private static final int W = 150;
    private static final int H = 16;

    private record Row(int x, int y, Module module, Setting<?> setting) {}

    private final List<Row> rows = new ArrayList<>();
    private Module expanded = null;
    private Setting<?> dragging = null;
    private int dragX = 0;
    private boolean prevLeft = false;
    private boolean prevRight = false;

    public ClickGuiScreen() {
        super(Component.literal("CheatUtils"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        Minecraft mc = Minecraft.getInstance();
        boolean left = mc.mouseHandler.isLeftPressed();
        boolean right = mc.mouseHandler.isRightPressed();

        if (left && !prevLeft) click(mouseX, mouseY, 0);
        if (right && !prevRight) click(mouseX, mouseY, 1);
        if (!left) dragging = null;
        else if (dragging != null) updateSlider(dragging, mouseX, dragX);
        prevLeft = left;
        prevRight = right;

        rows.clear();
        int colX = 20;
        for (Module.Category cat : Module.Category.values()) {
            int y = 20;
            g.fill(colX, y, colX + W, y + H, 0xDD202020);
            g.drawString(font, cat.name(), colX + 4, y + 4, 0xFFFFA500, true);
            y += H;

            for (Module m : ModuleManager.getModules()) {
                if (m.getCategory() != cat) continue;

                g.fill(colX, y, colX + W, y + H, m.isEnabled() ? 0xFF2F8F2F : 0xDD2A2A2A);
                g.drawString(font, m.getName(), colX + 4, y + 4, 0xFFFFFFFF, true);
                rows.add(new Row(colX, y, m, null));
                y += H;

                if (expanded == m) {
                    for (Setting<?> s : m.getSettings()) {
                        drawSetting(g, s, colX, y);
                        rows.add(new Row(colX, y, m, s));
                        y += H;
                    }
                }
            }
            colX += W + 10;
        }
    }

    private void drawSetting(GuiGraphics g, Setting<?> s, int x, int y) {
        g.fill(x, y, x + W, y + H, 0xAA101010);
        switch (s.getType()) {
            case BOOLEAN -> {
                boolean v = (Boolean) s.getValue();
                g.drawString(font, s.getName(), x + 4, y + 4, v ? 0xFF55FF55 : 0xFFAAAAAA, true);
            }
            case DOUBLE -> {
                double v = ((Number) s.getValue()).doubleValue();
                double min = ((Number) s.getMin()).doubleValue();
                double max = ((Number) s.getMax()).doubleValue();
                int filled = (int) (W * (v - min) / (max - min));
                g.fill(x, y, x + filled, y + H, 0x8800AEEF);
                g.drawString(font, s.getName() + ": " + String.format("%.1f", v), x + 4, y + 4, 0xFFFFFFFF, true);
            }
            case MODE -> g.drawString(font, s.getName() + ": " + s.getValue(), x + 4, y + 4, 0xFF66CCFF, true);
        }
    }

    @SuppressWarnings("unchecked")
    private void click(int mx, int my, int button) {
        for (Row r : rows) {
            if (mx < r.x() || mx > r.x() + W || my < r.y() || my > r.y() + H) continue;

            if (r.setting() == null) {
                if (button == 0) r.module().toggle();
                else expanded = (expanded == r.module()) ? null : r.module();
                return;
            }

            Setting<?> s = r.setting();
            switch (s.getType()) {
                case BOOLEAN -> {
                    Setting<Boolean> b = (Setting<Boolean>) s;
                    b.setValue(!b.getValue());
                }
                case DOUBLE -> {
                    dragging = s;
                    dragX = r.x();
                    updateSlider(s, mx, r.x());
                }
                case MODE -> {
                    Setting<String> ms = (Setting<String>) s;
                    String[] modes = ms.getModes();
                    int idx = (Arrays.asList(modes).indexOf(ms.getValue()) + 1) % modes.length;
                    ms.setValue(modes[idx]);
                }
            }
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private void updateSlider(Setting<?> s, int mx, int x) {
        Setting<Double> slider = (Setting<Double>) s;
        double min = slider.getMin();
        double max = slider.getMax();
        double frac = Math.max(0, Math.min(1, (mx - x) / (double) W));
        slider.setValue(min + frac * (max - min));
    }
}
