// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class WGLabel implements Renderable, GuiEventListener, NarratableEntry {
    private Font font;
    private Component component;
    private int color;
    private int x;
    private int y;
    private int length;
    private int height;

    private TextAlignment alignment;
    private int textWidth;
    private int startX;
    private int startY;

    boolean visible = true;
    boolean focused = false;
    
    public WGLabel(Font _font, int _x, int _y, int _length, int _height, TextAlignment _alignment, Component _component, int _color) {
        font = _font;
        component = _component;
        color = _color;
        alignment = _alignment;
        x = _x;
        y = _y;
        length = _length;
        height = _height;

        update();
    }

    public void update() {
        textWidth = font.width(component.getVisualOrderText());

        startY = y + (height / 2) - (font.lineHeight / 2);
        startX = switch (alignment) {
            case LEFT -> x;
            case CENTER -> (x + (length / 2)) - (textWidth / 2);
            case RIGHT -> (x + length) - textWidth;
        };
    }

    public void setPos(int _x, int _y) {
        x = _x;
        y = _y;
        update();
    }

    public void setText(Component _component) {
        component = _component;
        update();
    }

    public void setColor(int _color) {
        color = _color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        if (visible) {
            guiGraphics.drawString(font, component, startX, startY, color);
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    public enum TextAlignment {
        LEFT, CENTER, RIGHT
    }
}
