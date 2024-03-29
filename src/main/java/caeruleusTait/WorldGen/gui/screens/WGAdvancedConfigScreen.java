// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.config.WGConfigState;
import caeruleusTait.WorldGen.gui.GUIFactory;
import caeruleusTait.WorldGen.gui.MultiSelectScreen;
import caeruleusTait.WorldGen.gui.widgets.WGCheckbox;
import caeruleusTait.WorldGen.gui.widgets.WGLabel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class WGAdvancedConfigScreen extends Screen {
    private final Screen parent;

    private Button btnBack;

    // Threading
    private WGCheckbox chEnableThreading;
    private WGLabel lMaxThreads;
    private EditBox boxMaxThreads;

    // Etc.
    private WGCheckbox chEnableProgress;
    private Button btnMaxStatus;

    // Danger
    private WGCheckbox chIWantToBreakMyWorld;
    private WGCheckbox chOverrideChunks;
    private Button btnFakeStatus;
    private WGLabel lWarning;

    private final WGConfigState cfg;

    public static final Predicate<String> numericMin2StringFilter = ((val) -> val.matches("(|[1-9][0-9]?[0-9]?)"));

    private static final Component textEnableThreads = Component.translatable("world-gen.advanced.mt.enable");
    private static final Component textMaxThreads = Component.translatable("world-gen.advanced.mt.num");
    private static final Component textEnableProgress = Component.translatable("world-gen.advanced.progress");
    private static final Component textMaxCsTitle = Component.translatable("world-gen.advanced.max.title");
    private static final Component textMaxCsSubtitle = Component.translatable("world-gen.advanced.max.subtitle");
    private static final Component textWorldBreaker = Component.translatable("world-gen.advanced.danger.wold-breaker");
    private static final Component textOverrideChunks = Component.translatable("world-gen.advanced.danger.override");
    private static final Component textFakeStatusTitle = Component.translatable("world-gen.advanced.danger.fake.title");
    private static final Component textFakeStatusSubtitle = Component.translatable("world-gen.advanced.danger.fake.subtitle");
    private static final Component textWarning = Component.translatable("world-gen.advanced.danger.enabled");

    protected WGAdvancedConfigScreen(Screen parent) {
        super(Component.translatable("world-gen.advanced.title"));
        this.parent = parent;
        this.cfg = WGConfigState.get();
    }

    @Override
    protected void init() {
        GUIFactory gf = new GUIFactory(this);

        // Threading
        chEnableThreading = gf.checkbox(gf.peekFromTop(), textEnableThreads, this::onCheckboxToggle, 0, 2);
        chEnableThreading.setSelected(cfg.enableThreads);
        lMaxThreads = gf.label(gf.peekFromTop(), font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textMaxThreads, 2, 4);
        boxMaxThreads = gf.editBox(gf.pushFromTop(), this.font, textMaxThreads, 3, 4);
        boxMaxThreads.setFilter(numericMin2StringFilter);
        boxMaxThreads.setValue(Integer.toString(cfg.maxThreads));

        // Enable progress visualizer
        chEnableProgress = gf.checkbox(gf.pushFromTop(), textEnableProgress, null);
        chEnableProgress.setSelected(cfg.enableProgress);

        //
        Component compMax = Component.translatable("world-gen.advanced.max.btn", chunkStatusToStr(cfg.maxStatus));
        btnMaxStatus = gf.button(gf.pushFromTop(), compMax, this::onMaxStatusSelector);

        // Danger
        Component compFake = Component.translatable("world-gen.advanced.danger.fake.btn", chunkStatusToStr(cfg.maxStatus));
        chIWantToBreakMyWorld = gf.checkbox(gf.pushFromTop(), textWorldBreaker, this::onCheckboxToggle);
        chOverrideChunks = gf.checkbox(gf.pushFromTop(), textOverrideChunks, this::onCheckboxToggle);
        btnFakeStatus = gf.button(gf.pushFromTop(), compFake, this::onFakeStatusSelector);
        chIWantToBreakMyWorld.setSelected(cfg.enableWorldBreakingOptions);
        chOverrideChunks.setSelected(cfg.overrideExistingChunks);

        // Back
        btnBack = gf.button(gf.pushFromBottom(), CommonComponents.GUI_DONE, this::onClose);
        lWarning = gf.label(gf.pushFromBottom(), font, 0xFF9900, WGLabel.TextAlignment.CENTER, textWarning);

        addRenderableWidget(chEnableThreading);
        addRenderableWidget(lMaxThreads);
        addRenderableWidget(boxMaxThreads);
        addRenderableWidget(btnBack);
        addRenderableWidget(chEnableProgress);
        addRenderableWidget(btnMaxStatus);
        addRenderableWidget(chIWantToBreakMyWorld);
        addRenderableWidget(chOverrideChunks);
        addRenderableWidget(btnFakeStatus);
        addRenderableWidget(lWarning);

        update();
    }

    private static String cs2str(ChunkStatus cs) {
        return BuiltInRegistries.CHUNK_STATUS.getKey(cs).toString();
    }

    private void onCheckboxToggle(WGCheckbox cb) {
        update();
    }

    private static List<ChunkStatus> buildStatusList() {
        List<ChunkStatus> csList = new ArrayList<>();

        ChunkStatus last = null;
        for (ChunkStatus cs = ChunkStatus.FULL; cs != last; cs = cs.getParent()) {
            last = cs;
            csList.add(cs);
        }

        Collections.reverse(csList);
        return csList;
    }

    private static String chunkStatusToStr(ChunkStatus cs) {
        if (cs == null) {
            return "<NO CHANGE>";
        }
        return BuiltInRegistries.CHUNK_STATUS.getKey(cs).getPath();
    }

    private void onMaxStatusSelector(Button btn) {
        minecraft.setScreen(
                new MultiSelectScreen<>(
                        textMaxCsTitle,
                        textMaxCsSubtitle,
                        this,
                        buildStatusList(),
                        cfg.maxStatus,
                        WGAdvancedConfigScreen::chunkStatusToStr,
                        this::onMaxStatusChanged
                )
        );
    }

    private void onMaxStatusChanged(ChunkStatus newCS) {
        cfg.maxStatus = newCS;
        update();
    }

    private void onFakeStatusSelector(Button btn) {
        List<ChunkStatus> statusList = buildStatusList();
        statusList.add(0, null);

        minecraft.setScreen(
                new MultiSelectScreen<>(
                        textFakeStatusTitle,
                        textFakeStatusSubtitle,
                        this,
                        statusList,
                        cfg.fakeStatus,
                        WGAdvancedConfigScreen::chunkStatusToStr,
                        this::onFakeStatusChanged
                )
        );
    }

    private void onFakeStatusChanged(ChunkStatus newCS) {
        cfg.fakeStatus = newCS;
        update();
    }

    private void update() {
        validateConfig();

        if (chEnableThreading.selected()) {
            boxMaxThreads.setFocused(true);
            boxMaxThreads.setEditable(true);
            lMaxThreads.setColor(0xFFFFFF);
        } else {
            boxMaxThreads.setFocused(false);
            boxMaxThreads.setEditable(false);
            lMaxThreads.setColor(0x707070);
        }

        if (cfg.enableWorldBreakingOptions) {
            btnFakeStatus.visible = true;
            chOverrideChunks.visible = true;
            lWarning.setVisible(true);
        } else {
            btnFakeStatus.visible = false;
            chOverrideChunks.visible = false;
            lWarning.setVisible(false);
        }

        btnMaxStatus.setMessage(Component.translatable("world-gen.advanced.max.btn", chunkStatusToStr(cfg.maxStatus)));
        btnFakeStatus.setMessage(Component.translatable("world-gen.advanced.danger.fake.btn", chunkStatusToStr(cfg.fakeStatus)));
    }

    private boolean validateConfig() {
        cfg.enableThreads = chEnableThreading.selected();
        cfg.maxThreads = WGConfigScreen.getValueSafe(boxMaxThreads, 1);
        cfg.enableProgress = chEnableProgress.selected();
        cfg.enableWorldBreakingOptions = chIWantToBreakMyWorld.selected();
        cfg.overrideExistingChunks = chOverrideChunks.selected();

        return true;
    }

    private void onClose(Button btn) {
        onClose();
    }

    @Override
    public void onClose() {
        validateConfig();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, i, j, f);
    }
}
