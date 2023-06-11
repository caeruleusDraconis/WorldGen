// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.config.PregenSizeMode;
import caeruleusTait.WorldGen.config.WGConfigState;
import caeruleusTait.WorldGen.gui.GUIFactory;
import caeruleusTait.WorldGen.gui.MultiSelectScreen;
import caeruleusTait.WorldGen.gui.widgets.WGLabel;
import caeruleusTait.WorldGen.util.Utils;
import caeruleusTait.WorldGen.worker.WGChunkGenWorkHost;
import caeruleusTait.WorldGen.worker.WGMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class WGConfigScreen extends Screen {
    private final Screen lastScreen;
    private final String levelID;

    private Button btnDimension;

    // WGLabel lCachedChunks;
    // private EditBox boxCachedChunks;

    WGLabel lWarning;

    private static final int MIN_CACHED_CHUNKS = 128;

    private static final Component textRadius = Component.translatable("world-gen.main.size-radius");
    private static final Component textHSize = Component.translatable("world-gen.main.size-horizontal");
    private static final Component textVSize = Component.translatable("world-gen.main.size-vertical");
    private static final Component textXMin = Component.translatable("world-gen.main.size-xMin");
    private static final Component textXMax = Component.translatable("world-gen.main.size-xMax");
    private static final Component textZMin = Component.translatable("world-gen.main.size-yMin");
    private static final Component textZMax = Component.translatable("world-gen.main.size-yMax");
    private static final Component textCenterX = Component.translatable("world-gen.main.size.center-x");
    private static final Component textCenterZ = Component.translatable("world-gen.main.size.center-z");

    private static final Component textCachedChunks = Component.translatable("world-gen.main.cachedChunks");
    private static final Component textWarning = Component.translatable("world-gen.advanced.danger.enabled");

    private static final ResourceLocation ACCESSIBILITY_TEXTURE = new ResourceLocation("textures/gui/accessibility.png");


    // Regex to filter correct inputs
    public static final Predicate<String> numericPositiveLargeStringFilter = ((val) -> val.matches("(|[1-9][0-9]?[0-9]?[0-9]?[0-9]?)"));
    public static final Predicate<String> numericPositiveStringFilter = ((val) -> val.matches("(|[1-9][0-9]?[0-9]?[0-9]?)"));
    public static final Predicate<String> numericStringFilter = ((val) -> val.matches("(|0|-|-?[1-9][0-9]*)"));


    private final WGConfigState cfg;
    private static boolean started = false;

    private WGChunkGenWorkHost activeWorkHost;

    private static boolean confirmedBackupScreen = false;

    /**
     * Settings for {@link PregenSizeMode#SQUARE} and {@link PregenSizeMode#CIRCLE}
     */
    private WGLabel lRadius;
    private EditBox boxRadius;

    /**
     * Settings for {@link PregenSizeMode#SQUARE}, {@link PregenSizeMode#CIRCLE}, and {@link PregenSizeMode#RECTANGLE}
     */
    private WGLabel lCenterX;
    private EditBox boxCenterX;
    private WGLabel lCenterZ;
    private EditBox boxCenterZ;
    private ImageButton btnCenterPicker;

    /**
     * Settings for {@link PregenSizeMode#RECTANGLE}
     */
    private WGLabel lHSize;
    private EditBox boxHSize;
    private WGLabel lVSize;
    private EditBox boxVSize;

    /**
     * Settings for {@link PregenSizeMode#COORDINATES}
     */
    private WGLabel lXMin;
    private EditBox boxXMin;
    private WGLabel lXMax;
    private EditBox boxXMax;
    private WGLabel lZMin;
    private EditBox boxZMin;
    private WGLabel lZMax;
    private EditBox boxZMax;

    /**
     * The main worker
     */
    private WGMain wgMain;

    public WGConfigScreen(Screen _lastScreen, String _levelID) {
        super(Component.translatable("world-gen.main.title"));
        lastScreen = _lastScreen;
        levelID = _levelID;
        cfg = WGConfigState.get();
    }

    @Override
    protected void init() {
        if (started) {
            doStop();
        }

        if (wgMain == null) {
            try {
                wgMain = new WGMain(minecraft, levelID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        GUIFactory gf = new GUIFactory(this);
        activeWorkHost = null;


        final CycleButton.Builder<PregenSizeMode> cycleButtonBuilder =
                CycleButton.builder((PregenSizeMode value) -> Component.literal(value.name()))
                        .withValues(PregenSizeMode.SQUARE, PregenSizeMode.CIRCLE, PregenSizeMode.RECTANGLE, PregenSizeMode.COORDINATES)
                        .withInitialValue(cfg.pregenMode);



        btnDimension = gf.button(gf.pushFromTop(), Component.translatable("world-gen.main.dimension", cfg.dimension.location().toString()), this::openDimensionSelector);
        Button btnStart = gf.button(gf.peekFromBottom(), Component.translatable("world-gen.main.start"), this::startGen, 0, 2);
        Button btnCancel = gf.button(gf.pushFromBottom(), Component.translatable("world-gen.main.cancel"), this::onClose, 1, 2);
        Button btnAdvanced = gf.button(gf.pushFromBottom(), Component.translatable("world-gen.main.advanced"), this::onAdvanced);
        lWarning = gf.label(gf.pushFromBottom(), font, 0xFF9900, WGLabel.TextAlignment.CENTER, textWarning);
        CycleButton<PregenSizeMode> btnSizeMode = gf.cycleButton(gf.pushFromTop(), Component.translatable("world-gen.main.size-mode"), cycleButtonBuilder, this::pregenModeChanged);

        int vposI1 = gf.pushFromTop();
        int vposI2 = gf.pushFromTop();

        // The options CIRCLE and SQUARE have 1 parameter
        lRadius   = gf.label(vposI1, this.font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textRadius, 0, 2);
        boxRadius = gf.editBox(vposI1, this.font, Component.translatable("world-gen.main.size-radius"), 2, 4);
        boxRadius.setValue(Integer.toString(cfg.radius));
        boxRadius.setFilter(numericPositiveStringFilter);
        // ----------------------

        // The option RECTANGLE has 2 parameters, on same line
        lHSize   = gf.label(vposI1, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textHSize, 0, 4);
        boxHSize = gf.editBox(vposI1, font, textHSize, 1, 4);
        lVSize   = gf.label(vposI1, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textVSize, 2, 4);
        boxVSize = gf.editBox(vposI1, font, textVSize, 3, 4);
        boxHSize.setValue(Integer.toString(cfg.hSize));
        boxHSize.setFilter(numericPositiveStringFilter);
        boxVSize.setValue(Integer.toString(cfg.vSize));
        boxVSize.setFilter(numericPositiveStringFilter);
        // ----------------------

        // Common for CIRCLE, SQUARE, and RECTANGLE
        lCenterX   = gf.label(vposI2, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textCenterX, 0, 4);
        boxCenterX = gf.editBox(vposI2, font, textCenterX, 1, 4);
        lCenterZ   = gf.label(vposI2, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textCenterZ, 2, 4);
        boxCenterZ = gf.editBox(vposI2, font, textCenterZ, 3, 4);
        boxCenterX.setValue(Integer.toString(cfg.center.x));
        boxCenterX.setFilter(numericStringFilter);
        boxCenterZ.setValue(Integer.toString(cfg.center.z));
        boxCenterZ.setFilter(numericStringFilter);
        btnCenterPicker = new ImageButton(gf.hCenter() + GUIFactory.LINE_CENTER_HORIZONTAL + GUIFactory.LINE_HSPACE,
                                          vposI2, 20, 20, 0, 0, 20,
                                          ACCESSIBILITY_TEXTURE, 32, 64,
                                          this::openPlayerPicker
        );
        btnCenterPicker.active = wgMain.worldData().getLoadedPlayerTag() != null;

        // The option COORDINATES has 4 parameters, 2 on each line
        lXMin   = gf.label(vposI1, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textXMin, 0, 4);
        boxXMin = gf.editBox(vposI1, font, textXMin, 1, 4);
        lZMin = gf.label(vposI1, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textZMin, 2, 4);
        boxZMin = gf.editBox(vposI1, font, textZMin, 3, 4);
        lXMax   = gf.label(vposI2, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textXMax, 0, 4);
        boxXMax = gf.editBox(vposI2, font, textXMax, 1, 4);
        lZMax = gf.label(vposI2, font, 0xFFFFFF, WGLabel.TextAlignment.RIGHT, textZMax, 2, 4);
        boxZMax = gf.editBox(vposI2, font, textZMax, 3, 4);
        boxXMin.setValue(Integer.toString(cfg.upperLeft.x));
        boxXMin.setFilter(numericStringFilter);
        boxZMin.setValue(Integer.toString(cfg.upperLeft.z));
        boxZMin.setFilter(numericStringFilter);
        boxXMax.setValue(Integer.toString(cfg.lowerRight.x));
        boxXMax.setFilter(numericStringFilter);
        boxZMax.setValue(Integer.toString(cfg.lowerRight.z));
        boxZMax.setFilter(numericStringFilter);
        // ----------------------

        gf.pushFromTop();


        /*
        lCachedChunks = gf.label(gf.peekFromTop(), this.font, 0xFFFFFF, WGLabel.TextAlignment.CENTER, textCachedChunks, 0, 2);

        boxCachedChunks = gf.editBox(gf.pushFromTop(), this.font, textCachedChunks, 3, 6);
        boxCachedChunks.setValue(Integer.toString(cfg.cachedChunks));
        boxCachedChunks.setFilter(numericPositiveLargeStringFilter);
        boxCachedChunks.setResponder(this::cachedChunksChanged);
         */

        updateBoxStatus();

        addRenderableWidget(btnStart);
        addRenderableWidget(btnCancel);
        addRenderableWidget(btnAdvanced);
        addRenderableWidget(btnDimension);
        addRenderableWidget(btnSizeMode);
        addRenderableWidget(boxRadius);
        addRenderableWidget(lRadius);
        addRenderableWidget(boxHSize);
        addRenderableWidget(boxVSize);
        addRenderableWidget(lHSize);
        addRenderableWidget(lVSize);
        addRenderableWidget(boxCenterX);
        addRenderableWidget(boxCenterZ);
        addRenderableWidget(lCenterX);
        addRenderableWidget(lCenterZ);
        addRenderableWidget(boxXMin);
        addRenderableWidget(boxXMax);
        addRenderableWidget(boxZMin);
        addRenderableWidget(boxZMax);
        addRenderableWidget(lXMin);
        addRenderableWidget(lXMax);
        addRenderableWidget(lZMin);
        addRenderableWidget(lZMax);
        // addRenderableWidget(boxCachedChunks);
        // addRenderableWidget(lCachedChunks);
        addRenderableWidget(lWarning);
        addRenderableWidget(btnCenterPicker);
    }

    /**
     * Safely retrieve the value from the given EditBox
     * @param box The EditBox to read
     * @param safeValue A safe value to assume if things go wrong
     * @return The value entered if valid, or the safeValue otherwise
     */
    public static int getValueSafe(EditBox box, int safeValue) {
        try {
            return Integer.parseInt(box.getValue());
        } catch (NumberFormatException ex) {
            return safeValue;
        }
    }

    /*
    private void cachedChunksChanged(String newVal) {
        int number;
        try {
            number = Integer.parseInt(newVal);
            boxCachedChunks.setSuggestion("");
        } catch (NumberFormatException ex) {
            number = 0;
            boxCachedChunks.setSuggestion(Integer.toString(MIN_CACHED_CHUNKS));
        }

        if (number < MIN_CACHED_CHUNKS) {
            btnStart.active = false;
            boxCachedChunks.setTextColor(0xEE6666);
        } else {
            btnStart.active = true;
            boxCachedChunks.setTextColor(0xFFFFFF);
        }
    }
     */

    private void updateBoxStatus() {
        boxRadius.setVisible(false);
        boxRadius.setFocused(false);
        lRadius.setVisible(false);

        boxHSize.setVisible(false);
        boxVSize.setVisible(false);
        boxHSize.setFocused(false);
        boxVSize.setFocused(false);
        lHSize.setVisible(false);
        lVSize.setVisible(false);

        boxCenterX.setVisible(false);
        boxCenterZ.setVisible(false);
        boxCenterX.setFocused(false);
        boxCenterZ.setFocused(false);
        lCenterX.setVisible(false);
        lCenterZ.setVisible(false);
        btnCenterPicker.visible = false;

        boxXMin.setVisible(false);
        boxXMax.setVisible(false);
        boxZMin.setVisible(false);
        boxZMax.setVisible(false);
        boxXMin.setFocused(false);
        boxXMax.setFocused(false);
        boxZMin.setFocused(false);
        boxZMax.setFocused(false);
        lXMin.setVisible(false);
        lXMax.setVisible(false);
        lZMin.setVisible(false);
        lZMax.setVisible(false);

        lWarning.setVisible(cfg.enableWorldBreakingOptions);

        switch (cfg.pregenMode) {
            case CIRCLE, SQUARE -> {
                boxRadius.setVisible(true);
                boxCenterX.setVisible(true);
                boxCenterZ.setVisible(true);
                lRadius.setVisible(true);
                lCenterX.setVisible(true);
                lCenterZ.setVisible(true);
                btnCenterPicker.visible = true;
            }
            case RECTANGLE -> {
                boxHSize.setVisible(true);
                boxVSize.setVisible(true);
                boxCenterX.setVisible(true);
                boxCenterZ.setVisible(true);
                lHSize.setVisible(true);
                lVSize.setVisible(true);
                lCenterX.setVisible(true);
                lCenterZ.setVisible(true);
                btnCenterPicker.visible = true;
            }
            case COORDINATES -> {
                boxXMin.setVisible(true);
                boxXMax.setVisible(true);
                boxZMin.setVisible(true);
                boxZMax.setVisible(true);
                lXMin.setVisible(true);
                lXMax.setVisible(true);
                lZMin.setVisible(true);
                lZMax.setVisible(true);
            }
        }
    }

    private void pregenModeChanged(CycleButton<PregenSizeMode> btn, PregenSizeMode value) {
        cfg.pregenMode = value;
        updateBoxStatus();
    }


    private void startGen(Button btn) {
        assert this.minecraft != null;

        if (!validateSettings()) {
            return;
        }

        if (cfg.enableWorldBreakingOptions) {
            minecraft.setScreen(new WGDangerousConfirmScreen(this, this::doStart));
        } else {
            // Only show the confirm backup screen once per session
            if(confirmedBackupScreen) {
                doStart();
            } else {
                minecraft.setScreen(new WGBackupConfirmScreen(this, this::doStart));
            }
        }
    }

    private boolean validateSettings() {
        boolean valid = true;

        cfg.radius = getValueSafe(boxRadius, 16);
        cfg.hSize = getValueSafe(boxHSize, 32);
        cfg.vSize = getValueSafe(boxVSize, 32);
        cfg.center = new ChunkPos(getValueSafe(boxCenterX, 0), getValueSafe(boxCenterZ, 0));
        cfg.upperLeft = new ChunkPos(getValueSafe(boxXMin, -16), getValueSafe(boxZMin, -16));
        cfg.lowerRight = new ChunkPos(getValueSafe(boxXMax, 16), getValueSafe(boxZMax, 16));
        // cfg.cachedChunks = getValueSafe(boxCachedChunks, MIN_CACHED_CHUNKS);

        if (cfg.pregenMode == PregenSizeMode.COORDINATES) {
            // Check that the coordinates make sense
            if (cfg.upperLeft.x > cfg.lowerRight.x || cfg.upperLeft.z > cfg.lowerRight.z) {
                valid = false;
            }
        }
        return valid;
    }


    private void doStart() {
        // Remember that a confirm has happened
        if(!confirmedBackupScreen) {
            confirmedBackupScreen = true;
        }

        started = true;

        ChunkPos start;
        ChunkPos end;

        // The default chunk position filter just keeps all values
        Predicate<? super ChunkPos> filter = (val) -> true;

        switch (cfg.pregenMode) {
            case SQUARE -> {
                start = new ChunkPos( cfg.center.x - cfg.radius, cfg.center.z - cfg.radius);
                end   = new ChunkPos( cfg.center.x + cfg.radius, cfg.center.z + cfg.radius);
            }
            case CIRCLE -> {

                start = new ChunkPos( cfg.center.x - cfg.radius, cfg.center.z - cfg.radius);
                end   = new ChunkPos( cfg.center.x + cfg.radius, cfg.center.z + cfg.radius);

                final int rSquared = cfg.radius * cfg.radius;

                // Overwrite the chunk position filter, making sure to take the center offset into account
                filter = (val) -> ((val.x-cfg.center.x) * (val.x-cfg.center.x) + (val.z-cfg.center.z) * (val.z-cfg.center.z)) <= rSquared;
            }
            case RECTANGLE -> {
                int hCenter = Mth.floor((double) cfg.hSize / 2);
                int vCenter = Mth.floor((double) cfg.vSize / 2);

                // Place the rectangle around the given center, making sure to
                start = new ChunkPos( cfg.center.x - hCenter, cfg.center.z - vCenter);
                end   = new ChunkPos( cfg.center.x + hCenter, cfg.center.z + vCenter);
            }
            case COORDINATES -> {
                start = new ChunkPos( cfg.upperLeft.x,  cfg.upperLeft.z);
                end   = new ChunkPos(cfg.lowerRight.x, cfg.lowerRight.z);
            }
            default -> throw new RuntimeException("Pregen mode " + cfg.pregenMode + " is not yet implemented!");
        }


        wgMain.startWork(start, end, filter, this, lastScreen);
    }

    private void doStop() {
        started = false;

        if(activeWorkHost != null) {
            activeWorkHost.abortWork();
            activeWorkHost = null;
        }

        try {
            wgMain = new WGMain(minecraft, levelID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onClose(Button btn) {
        onClose();
    }

    private void onAdvanced(Button btn) {
        assert this.minecraft != null;

        validateSettings();
        minecraft.setScreen(new WGAdvancedConfigScreen(this));
    }

    private void openDimensionSelector(Button btn) {
        assert this.minecraft != null;

        List<ResourceKey<Level>> dimensionList = wgMain
                .worldStem()
                .registries()
                .getLayer(RegistryLayer.DIMENSIONS)
                .registryOrThrow(Registries.LEVEL_STEM)
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .map(Utils::levelStemToLevel)
                .toList();

        final MultiSelectScreen<ResourceKey<Level>> screen = new MultiSelectScreen<>(
                Component.translatable("world-gen.dim.title"),
                Component.translatable("world-gen.dim.subtitle"),
                this,
                dimensionList,
                cfg.dimension,
                el -> el.location().toString(),
                this::onDimensionChanged
        );

        // Save our values before switching
        validateSettings();
        minecraft.setScreen(screen);
    }

    private void onDimensionChanged(ResourceKey<Level> newDim) {
        cfg.dimension = newDim;
        btnDimension.setMessage(Component.translatable("world-gen.main.dimension", cfg.dimension.location().toString()));
    }

    private void openPlayerPicker(Button btn) {
        final CompoundTag playerTag = wgMain.worldData().getLoadedPlayerTag();

        if(playerTag == null) {
            WorldGen.LOGGER.warn("Could not find a player.");
            return;
        }

        final ListTag posList = (ListTag) playerTag.get("Pos");

        if(posList == null) {
            WorldGen.LOGGER.warn("Could not find the player position.");
            return;
        }

        final DoubleTag xTag = (DoubleTag) posList.get(0);
        final DoubleTag zTag = (DoubleTag) posList.get(2);

        if (btn == btnCenterPicker) {
            cfg.center = new ChunkPos(SectionPos.blockToSectionCoord(xTag.getAsInt()), SectionPos.blockToSectionCoord(zTag.getAsInt()));
            boxCenterX.setValue(Integer.toString(cfg.center.x));
            boxCenterZ.setValue(Integer.toString(cfg.center.z));
        }
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;

        // Save our values before they are lost forever
        validateSettings();

        if (wgMain != null) {
            wgMain.close();
            wgMain = null;
        }
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
        super.render(guiGraphics, i, j, f);
    }
}
