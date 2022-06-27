// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import caeruleusTait.WorldGen.worker.WGGenerator;
import caeruleusTait.WorldGen.worker.WGMain;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public class WGUnloadingScreen extends Screen {
    private final WGGenerator generator;
    private final int initial;

    public WGUnloadingScreen(WGMain main) {
        super(new TranslatableComponent("world-gen.loading.unloading"));
        generator = main.getWGLevel().generator();
        initial = generator.unloadedChunks();
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        int curr = generator.unloadedChunks();
        int diff = Math.max(0, initial-curr);
        float proc = ((float) diff / (float) initial) * 100.f;

        this.renderDirtBackground(0);
        drawCenteredString(poseStack, font, title, width / 2, 15, 0xFFFFFF);
        drawCenteredString(poseStack, font, String.format("%.1f %%   [%d/%d]", proc, diff, initial), width / 2, 70, 0xFFFFFF);
        super.render(poseStack, i, j, f);
    }
}
