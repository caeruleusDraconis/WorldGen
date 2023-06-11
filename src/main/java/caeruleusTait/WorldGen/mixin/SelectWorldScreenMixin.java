// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import caeruleusTait.WorldGen.gui.screens.WGConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
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

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/SelectWorldScreen;updateButtonStatus(ZZ)V"))
    private void postInit(CallbackInfo info) {
        SelectWorldScreen screen = (SelectWorldScreen) (Object) this;


        worldGenButton = Button.builder(Component.translatable("world-gen.world-gen"), button -> {
                    getList().getSelectedOpt().ifPresent(worldListEntry -> {
                        ((ScreenAccessor) this).getMinecraft().forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                        final LevelSummary summary = ((WorldListEntryAccessor)(Object) worldListEntry).getSummary();
                        final WGConfigScreen wgConfigScreen = new WGConfigScreen(screen, summary.getLevelId());
                        ((ScreenAccessor) this).getMinecraft().setScreen(wgConfigScreen);
                    });
                })
                .pos(screen.width / 2 - 154 - 72 - 6, screen.height - 28)
                .size(72, 20)
                .build();

        ((ScreenAccessor) this).getChildren().add(worldGenButton);
        ((ScreenAccessor) this).getNarratables().add(worldGenButton);
        ((ScreenAccessor) this).getRenderables().add(worldGenButton);
    }

    @Inject(method = "updateButtonStatus", at = @At("TAIL"))
    private void updateWorldGenButtonStatus(boolean isActive, boolean isDeleteActive, CallbackInfo info) {
        worldGenButton.active = isActive;
    }


    // Accessor

    @Accessor
    public abstract WorldSelectionList getList();
}
