// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.gui.screens.WGConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {

    private Button worldGenButton = null;

    // Main Work Mixins

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/SelectWorldScreen;updateButtonStatus(Z)V"))
    private void postInit(CallbackInfo info) {
        SelectWorldScreen screen = (SelectWorldScreen) (Object) this;


        worldGenButton = new Button(screen.width / 2 - 154 - 72 - 6, screen.height - 28, 72, 20, new TranslatableComponent("world-gen.world-gen"), button -> getList().getSelectedOpt().ifPresent(worldListEntry -> {
            ((ScreenAccessor) this).getMinecraft().forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
            final LevelSummary summary = ((WorldListEntryAccessor)(Object) worldListEntry).getSummary();
            final WGConfigScreen wgConfigScreen = new WGConfigScreen(screen, summary.getLevelId());
            ((ScreenAccessor) this).getMinecraft().setScreen(wgConfigScreen);
        }));

        ((ScreenAccessor) this).getChildren().add(worldGenButton);
        ((ScreenAccessor) this).getNarratables().add(worldGenButton);
        ((ScreenAccessor) this).getRenderables().add(worldGenButton);
    }

    @Inject(method = "updateButtonStatus", at = @At("TAIL"))
    private void updateWorldGenButtonStatus(boolean isActive, CallbackInfo info) {
        worldGenButton.active = isActive;
    }


    // Accessor

    @Accessor
    public abstract WorldSelectionList getList();
}
