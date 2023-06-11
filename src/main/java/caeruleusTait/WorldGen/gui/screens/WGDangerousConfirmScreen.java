// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.gui.GUIFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class WGDangerousConfirmScreen extends Screen {
    private final Runnable onConfirm;
    private final Screen parent;

    private MultiLineLabel multiLineLabel;

    public WGDangerousConfirmScreen(Screen _parent, Runnable _onConfirm) {
        super(Component.translatable("world-gen.advanced.danger.enabled"));
        parent = _parent;
        onConfirm = _onConfirm;
    }

    @Override
    protected void init() {
        GUIFactory gf = new GUIFactory(this);

        multiLineLabel = MultiLineLabel.create(font, Component.translatable("world-gen.confirm.description"), width - 50);
        int btnVPos = 100 + multiLineLabel.getLineCount() * font.lineHeight;

        final Button btnYES = gf.button(btnVPos, CommonComponents.GUI_PROCEED, this::onAccept, 0, 2);
        final Button btnNO = gf.button(btnVPos, CommonComponents.GUI_CANCEL, this::onClose, 1, 2);

        addRenderableWidget(btnYES);
        addRenderableWidget(btnNO);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 25, 0xFF9900);
        multiLineLabel.renderCentered(guiGraphics, width / 2, 70);
        super.render(guiGraphics, i, j, f);
    }

    private void onAccept(Button btn) {
        onConfirm.run();
    }

    public void onClose(Button btn) {
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
