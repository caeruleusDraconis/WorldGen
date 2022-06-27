// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.gui.GUIFactory;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import caeruleusTait.WorldGen.config.WGConfigState;
import caeruleusTait.WorldGen.gui.GUIFactory;
import caeruleusTait.WorldGen.worker.WGChunkProgressListener;
import caeruleusTait.WorldGen.worker.WGMain;
import caeruleusTait.WorldGen.config.WGConfigState;
import caeruleusTait.WorldGen.worker.WGChunkProgressListener;
import caeruleusTait.WorldGen.worker.WGMain;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class WGLoadingScreen extends Screen {
    private final Screen lastScreen;
    private final Screen selectWorldScreen;
    private final WGMain main;

    private Button btnReturn;

    private boolean done;
    private boolean aborted;

    private final int chunkTotal;

    private int lastChunkCount;
    private long lastChunkTime;
    private double cps;

    private final WGConfigState cfg;

    private static final long NARRATION_DELAY_MS = 2000L;
    private final WGChunkProgressListener progressListener;
    private long lastNarration = -1L;

    // Original minecraft colors
    private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> {
        object2IntOpenHashMap.defaultReturnValue(0);
        object2IntOpenHashMap.put(ChunkStatus.EMPTY, 0x545454);
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_STARTS, 0x999999);
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_REFERENCES, 0x5F6191);
        object2IntOpenHashMap.put(ChunkStatus.BIOMES, 0x80B252);
        object2IntOpenHashMap.put(ChunkStatus.NOISE, 0xD1D1D1);
        object2IntOpenHashMap.put(ChunkStatus.SURFACE, 0x726809);
        object2IntOpenHashMap.put(ChunkStatus.CARVERS, 0x6D665C);
        object2IntOpenHashMap.put(ChunkStatus.LIQUID_CARVERS, 0x303572);
        object2IntOpenHashMap.put(ChunkStatus.FEATURES, 0x21C600);
        object2IntOpenHashMap.put(ChunkStatus.LIGHT, 0xCCCCCC);
        object2IntOpenHashMap.put(ChunkStatus.SPAWN, 0xF26060);
        object2IntOpenHashMap.put(ChunkStatus.HEIGHTMAPS, 0xEEEEEE);
        object2IntOpenHashMap.put(ChunkStatus.FULL, 0xFFFFFF);
    });

    //Slighty faded colors
    private static final Object2IntMap<ChunkStatus> COLORS_UNLOADED = Util.make(new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> {
        object2IntOpenHashMap.defaultReturnValue(0);
        object2IntOpenHashMap.put(ChunkStatus.EMPTY, 0x343434);
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_STARTS, 0x696969);
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_REFERENCES, 0x3F4161);
        object2IntOpenHashMap.put(ChunkStatus.BIOMES, 0x809252);
        object2IntOpenHashMap.put(ChunkStatus.NOISE, 0xA1A1A1);
        object2IntOpenHashMap.put(ChunkStatus.SURFACE, 0x423409);
        object2IntOpenHashMap.put(ChunkStatus.CARVERS, 0x3D362C);
        object2IntOpenHashMap.put(ChunkStatus.LIQUID_CARVERS, 0x101542);
        object2IntOpenHashMap.put(ChunkStatus.FEATURES, 0x215600);
        object2IntOpenHashMap.put(ChunkStatus.LIGHT, 0x8C8C8C);
        object2IntOpenHashMap.put(ChunkStatus.SPAWN, 0xB26060);
        object2IntOpenHashMap.put(ChunkStatus.HEIGHTMAPS, 0xAEAEAE);
        object2IntOpenHashMap.put(ChunkStatus.FULL, 0xBFBFBF);
    });

    public WGLoadingScreen(Screen _lastScreen, Screen selectWorldScreen, WGChunkProgressListener progressListener, WGMain main, int chunkTotal) {
        super(Component.translatable("world-gen.loading.title"));
        this.lastScreen = _lastScreen;
        this.selectWorldScreen = selectWorldScreen;
        this.progressListener = progressListener;
        this.main = main;
        this.chunkTotal = chunkTotal;
        this.done = false;
        this.aborted = false;
        this.lastChunkTime = -1;
        this.lastChunkCount = 0;
        this.cfg = WGConfigState.get();
    }

    @Override
    protected void init() {
        final GUIFactory gf = new GUIFactory(this);

        final CycleButton.Builder<Component> viewToggleBuilder =
                CycleButton.builder((Component value) -> Component.literal(value.getString()))
                        .withValues(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
                        .withInitialValue(CommonComponents.optionStatus(cfg.enableProgress));

        CycleButton<Component> btnViewToggle = gf.cycleButton(gf.peekFromBottom(),
                Component.translatable("world-gen.loading.toggleView"),
                viewToggleBuilder, this::onToggleView, 0, 2);
        btnReturn = gf.button(gf.pushFromBottom(),
                Component.translatable("world-gen.loading.cancel"),
                this::onClose, 1, 2);

        addRenderableWidget(btnViewToggle);
        addRenderableWidget(btnReturn);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }


    private void onToggleView(CycleButton<Component> btn, Component newValue) {
        cfg.enableProgress = (newValue == CommonComponents.OPTION_ON);
    }

    private void onClose(Button btn) {
        aborted = true;
        onClose();
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;

        this.minecraft.setScreen(new WGUnloadingScreen(main));
        CompletableFuture.runAsync(
                () -> {
                    try {
                        main.close();
                    } catch (Throwable cause) {
                        cause.printStackTrace();
                    }

                    if(aborted) {
                        // We aborted so we want to go back to our screen, init first
                        lastScreen.init(this.minecraft, this.width, this.height);
                        minecraft.setScreen(lastScreen);
                    } else {
                        // We successfully did it! Time to go back to the select world screen, init first
                        selectWorldScreen.init(this.minecraft, this.width, this.height);
                        minecraft.setScreen(selectWorldScreen);
                    }
                }
        );
    }

    @Override
    protected void updateNarratedWidget(@NotNull NarrationElementOutput narrationElementOutput) {
        if (this.done) {
            narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narrator.loading.done"));
        } else {
            String string = this.getFormattedProgress();
            narrationElementOutput.add(NarratedElementType.TITLE, string);
        }
    }

    private int getProgress() {
        return Mth.clamp(Mth.floor(((double) this.main.getGenCount() / chunkTotal) * 100), 0, 100);
    }

    private String getFormattedProgress() {
        return getProgress() + "%";
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int i, int j, float f) {
        if(this.done) {
            this.triggerImmediateNarration(true);
            this.onClose();
            return;
        } else if(this.getProgress() >= 100) {
            // We are done, but render one extra time
            this.done = true;
        }

        if(lastChunkTime == -1) {
            // Initialize the counter
            lastChunkCount = 0;
            lastChunkTime = Util.getMillis();
            cps = 0.0;
        } else {
            long newChunkTime = Util.getMillis();
            long timeDiff = newChunkTime - lastChunkTime;

            if(timeDiff > 1000L) {
                // Enough time has elapsed, now calculate the Chunks Per Second (CPS)
                cps = ((double) (this.main.getGenCount() - lastChunkCount)) / ((double) timeDiff / 1000);
                lastChunkCount = this.main.getGenCount();
                lastChunkTime = newChunkTime;
            }
        }

        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 16777215);

        // Narration function of minecraft
        long l = Util.getMillis();
        if (l - this.lastNarration > NARRATION_DELAY_MS) {
            this.lastNarration = l;
            this.triggerImmediateNarration(true);
        }


        int xCenter = this.width / 2;
        int top = 20 + this.font.lineHeight;

        WGLoadingScreen.drawCenteredString(poseStack, this.font, this.getFormattedProgress(), xCenter, top, 0xFFFFFF);
        top += 5 + this.font.lineHeight;
        WGLoadingScreen.drawCenteredString(poseStack, this.font, String.format("%.1f CPS   [%d/%d]", cps, this.main.getGenCount(), chunkTotal), xCenter, top, 0xFFFFFF);
        top += 5 + this.font.lineHeight;

        int verticalSpace = this.btnReturn.y - 10 - top;
        int vRadius = verticalSpace / 2;
        int boxVCenter = top + vRadius;
        int radius = Math.min(vRadius, xCenter);

        if (cfg.enableProgress) {
            WGLoadingScreen.renderChunks(
                    poseStack,
                    this.progressListener,
                    xCenter,
                    boxVCenter,
                    xCenter - radius,
                    xCenter + radius,
                    boxVCenter - radius,
                    boxVCenter + radius,
                    1,
                    0
            );
        }

        super.render(poseStack, i, j, f);
    }

    public static void renderChunks(PoseStack poseStack, WGChunkProgressListener storingChunkProgressListener, int xCenter, int yCenter, int xMin, int xMax, int yMin, int yMax, int chunkDrawSize, int spacing) {
        int blockSize = chunkDrawSize + spacing;
        int fullDia = storingChunkProgressListener.getFullDiameter();
        int fullChunkWidth = fullDia * blockSize - spacing;
        int diameter = storingChunkProgressListener.getDiameter();
        int fullDisplayWidth = diameter * blockSize - spacing;
        int left = xCenter - fullDisplayWidth / 2;
        int top = yCenter - fullDisplayWidth / 2;
        int chunkRadius = fullChunkWidth / 2 + 1;
        int colorBackground = 0xFF222222;
        int colorBorder = 0xFF666666;
        int colorOutline = 0xFF0011FF;

        // Get synchronized versions for this update in bulk so it does not have to be done for each chunk,
        // this may make the visualizer lag behind but its unnoticeable and instead improves performance
        Long2ObjectMap<ChunkStatus> statuses = storingChunkProgressListener.getStatuses();
        LongSet unloaded = storingChunkProgressListener.getUnloaded();

        ChunkPos spawnPos = storingChunkProgressListener.getSpawnPos();
        int r = storingChunkProgressListener.getRadius();

        if (spacing != 0) {
            WGLoadingScreen.fill(poseStack, xCenter - chunkRadius, yCenter - chunkRadius, xCenter - chunkRadius + 1, yCenter + chunkRadius, colorOutline);
            WGLoadingScreen.fill(poseStack, xCenter + chunkRadius - 1, yCenter - chunkRadius, xCenter + chunkRadius, yCenter + chunkRadius, colorOutline);
            WGLoadingScreen.fill(poseStack, xCenter - chunkRadius, yCenter - chunkRadius, xCenter + chunkRadius, yCenter - chunkRadius + 1, colorOutline);
            WGLoadingScreen.fill(poseStack, xCenter - chunkRadius, yCenter + chunkRadius - 1, xCenter + chunkRadius, yCenter + chunkRadius, colorOutline);
        }

        // Fill the background
        WGLoadingScreen.fill(poseStack, xMin, yMin, xMax, yMax, colorBackground);

        // Display the chunks
        for (int v = 0; v < diameter; ++v) {
            for (int w = 0; w < diameter; ++w) {
                ChunkStatus chunkStatus = getStatusLocal(statuses, spawnPos, r, v, w);
                int x = left + v * blockSize;
                int xEnd = x + chunkDrawSize;
                int y = top + w * blockSize;
                int yEnd = y + chunkDrawSize;

                if ((xMin <= x && xEnd <= xMax) && (yMin <= y && yEnd <= yMax)) { // Don't allow drawing outside the border
                    final boolean isUnloaded = isUnloadedLocal(unloaded, spawnPos, r, v, w);
                    final int color = (isUnloaded ? COLORS_UNLOADED.getInt(chunkStatus) : COLORS.getInt(chunkStatus)) | 0xFF000000;
                    WGLoadingScreen.fill(poseStack, x, y, xEnd, yEnd, color);
                }
            }
        }

        // Create a border
        WGLoadingScreen.fill(poseStack, xMin-1, yMin-1, xMax+1, yMin, colorBorder); // Right
        WGLoadingScreen.fill(poseStack, xMax, yMin, xMax+1, yMax, colorBorder); // Down
        WGLoadingScreen.fill(poseStack, xMin-1, yMax, xMax+1, yMax+1, colorBorder); // Left
        WGLoadingScreen.fill(poseStack, xMin-1, yMin, xMin, yMax, colorBorder); // Up
    }

    /**
     * Function to work on a cached 'unloaded' set.
     *
     * @param unloaded The cached 'unloaded' set
     * @param spawnPos The spawn position
     * @param radius The radius given to the chunk progress listener
     * @param x The x coordinate of the chunk
     * @param z The z coordinate of the chunk
     * @return Whether the given chunk is marked as unloaded in the cached set
     */
    private static boolean isUnloadedLocal(LongSet unloaded, ChunkPos spawnPos, int radius, int x, int z) {
        return unloaded.contains(ChunkPos.asLong(x + spawnPos.x - radius, z + spawnPos.z - radius));
    }

    /**
     * Function to work on a cached 'statuses' set.
     *
     * @param statuses The cached 'statuses' set
     * @param spawnPos The spawn position
     * @param radius The radius given to the chunk progress listener
     * @param x The x coordinate of the chunk
     * @param z The z coordinate of the chunk
     * @return The chunk status of the chunk in the cached map
     */
    private static ChunkStatus getStatusLocal(Long2ObjectMap<ChunkStatus> statuses, ChunkPos spawnPos, int radius, int x, int z) {
        return statuses.get(ChunkPos.asLong(x + spawnPos.x - radius, z + spawnPos.z - radius));
    }

}
