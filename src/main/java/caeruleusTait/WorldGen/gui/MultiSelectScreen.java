// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MultiSelectScreen<T> extends Screen {
    private final Component subtitle;
    private final Screen parent;
    private final List<T> choices;
    private final ToStringConverter<T> toStringConverter;
    private final Callback<T> callback;
    private final T current;

    private TheList theList;

    private Button btnDone;
    private Button btnCancel;

    public interface ToStringConverter<T> {
        String el2str(T el);
    }

    public interface Callback<T> {
        void selected(@Nullable T el);
    }

    public MultiSelectScreen(Component title, Component subtitle, Screen parent, List<T> choices, T current, ToStringConverter<T> toStringConverter, Callback<T> callback) {
        super(title);

        this.subtitle = subtitle;
        this.parent = parent;
        this.choices = choices;
        this.toStringConverter = toStringConverter;
        this.callback = callback;
        this.current = current;
    }

    @Override
    protected void init() {
        // TODO: Is this still required?
        // minecraft.keyboardHandler.setSendRepeatsToGui(true);

        final GUIFactory gf = new GUIFactory(this);

        // The buttons
        btnDone = gf.button(gf.peekFromBottom(), CommonComponents.GUI_DONE, this::onDone, 0, 2);
        btnCancel = gf.button(gf.peekFromBottom(), CommonComponents.GUI_CANCEL, this::onCancel, 1, 2);

        addRenderableWidget(btnDone);
        addRenderableWidget(btnCancel);

        // The main list
        final List<TheEntry> entryList = choices.stream().map(x -> new TheEntry(toStringConverter.el2str(x), x)).toList();
        theList = new TheList(entryList);
        if (current != null && choices.contains(current)) {
            theList.setSelected(entryList.stream().filter(x -> Objects.equals(x.entry, current)).findFirst().orElse(null));
        }
        addWidget(theList);
    }

    private void onDone(Button btn) {
        if (theList.getSelected() != null) {
            callback.selected(theList.getSelected().entry);
        }
        minecraft.setScreen(parent);
    }

    private void onCancel(Button btn) {
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        theList.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(font, title, width / 2, 8, 16777215);
        guiGraphics.drawCenteredString(font, subtitle, width / 2, 28, 10526880);
        super.render(guiGraphics, i, j, f);
    }

    private void updateButtonValidity() {
        btnDone.active = theList.getSelected() != null;
    }

    private class TheList extends ObjectSelectionList<TheEntry> {
        public TheList(List<TheEntry> entryList) {
            super(MultiSelectScreen.this.minecraft, MultiSelectScreen.this.width, MultiSelectScreen.this.height, 40, MultiSelectScreen.this.height - 37, 16);

            entryList.forEach(this::addEntry);
        }

        public void setSelected(@Nullable TheEntry entry) {
            super.setSelected(entry);
            updateButtonValidity();
        }
    }

    private class TheEntry extends ObjectSelectionList.Entry<TheEntry> {
        private final String name;
        private final T entry;

        public TheEntry(String name, T entry) {
            this.name = name;
            this.entry = entry;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            guiGraphics.drawString(font, name, k + 5, j + 2, 16777215);
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (i == 0) {
                MultiSelectScreen.this.theList.setSelected(this);
                return true;
            } else {
                return false;
            }
        }
    }
}
