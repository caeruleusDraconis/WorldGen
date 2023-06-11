// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.worker.WGGenerator;
import caeruleusTait.WorldGen.worker.WGMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class WGUnloadingScreen extends Screen {
    private final WGGenerator generator;
    private final int initial;
    private final Screen nextScreen;
    private boolean isComplete;

    public WGUnloadingScreen(WGMain main, Screen nextScreen) {
        super(Component.translatable("world-gen.loading.unloading"));
        this.generator = main.getWGLevel().generator();
        this.initial = generator.unloadedChunks();
        this.nextScreen = nextScreen;
        this.isComplete = false;
    }

    public void signalComplete() {
        this.isComplete = true;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        if(this.isComplete)
        {
            this.minecraft.setScreen(nextScreen);
            return;
        }

        int curr = generator.unloadedChunks();
        int diff = Math.max(0, initial-curr);
        float proc = ((float) diff / (float) initial) * 100.f;

        this.renderDirtBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, String.format("%.1f %%   [%d/%d]", proc, diff, initial), width / 2, 70, 0xFFFFFF);
        super.render(guiGraphics, i, j, f);
    }
}
