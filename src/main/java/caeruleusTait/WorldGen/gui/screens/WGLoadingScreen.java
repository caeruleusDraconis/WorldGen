// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui.screens;

import caeruleusTait.WorldGen.config.WGConfigState;
import caeruleusTait.WorldGen.gui.GUIFactory;
import caeruleusTait.WorldGen.worker.WGChunkProgressListener;
import caeruleusTait.WorldGen.worker.WGMain;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
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

    private NativeImage progressImg;
    private DynamicTexture progressTexture;

    private long frameCounter = 0;

    // Original minecraft colors
    private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> {
        object2IntOpenHashMap.defaultReturnValue(nativeColor(0x000000));
        object2IntOpenHashMap.put(ChunkStatus.EMPTY, nativeColor(0x545454));
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_STARTS, nativeColor(0x999999));
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_REFERENCES, nativeColor(0x5F6191));
        object2IntOpenHashMap.put(ChunkStatus.BIOMES, nativeColor(0x80B252));
        object2IntOpenHashMap.put(ChunkStatus.NOISE, nativeColor(0xD1D1D1));
        object2IntOpenHashMap.put(ChunkStatus.SURFACE, nativeColor(0x726809));
        object2IntOpenHashMap.put(ChunkStatus.CARVERS, nativeColor(0x6D665C));
        object2IntOpenHashMap.put(ChunkStatus.LIQUID_CARVERS, nativeColor(0x303572));
        object2IntOpenHashMap.put(ChunkStatus.FEATURES, nativeColor(0x21C600));
        object2IntOpenHashMap.put(ChunkStatus.LIGHT, nativeColor(0xCCCCCC));
        object2IntOpenHashMap.put(ChunkStatus.SPAWN, nativeColor(0xF26060));
        object2IntOpenHashMap.put(ChunkStatus.HEIGHTMAPS, nativeColor(0xEEEEEE));
        object2IntOpenHashMap.put(ChunkStatus.FULL, nativeColor(0xFFFFFF));
    });

    //Slighty faded colors
    private static final Object2IntMap<ChunkStatus> COLORS_UNLOADED = Util.make(new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> {
        object2IntOpenHashMap.defaultReturnValue(nativeColor(0x000000));
        object2IntOpenHashMap.put(ChunkStatus.EMPTY, nativeColor(0x343434));
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_STARTS, nativeColor(0x696969));
        object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_REFERENCES, nativeColor(0x3F4161));
        object2IntOpenHashMap.put(ChunkStatus.BIOMES, nativeColor(0x809252));
        object2IntOpenHashMap.put(ChunkStatus.NOISE, nativeColor(0xA1A1A1));
        object2IntOpenHashMap.put(ChunkStatus.SURFACE, nativeColor(0x423409));
        object2IntOpenHashMap.put(ChunkStatus.CARVERS, nativeColor(0x3D362C));
        object2IntOpenHashMap.put(ChunkStatus.LIQUID_CARVERS, nativeColor(0x101542));
        object2IntOpenHashMap.put(ChunkStatus.FEATURES, nativeColor(0x215600));
        object2IntOpenHashMap.put(ChunkStatus.LIGHT, nativeColor(0x8C8C8C));
        object2IntOpenHashMap.put(ChunkStatus.SPAWN, nativeColor(0xB26060));
        object2IntOpenHashMap.put(ChunkStatus.HEIGHTMAPS, nativeColor(0xAEAEAE));
        object2IntOpenHashMap.put(ChunkStatus.FULL, nativeColor(0xBFBFBF));
    });

    private static int nativeColor(int orig) {
        final int R = (orig >> 16) & 0xFF;
        final int G = (orig >> 8) & 0xFF;
        final int B = (orig >> 0) & 0xFF;
        return (R << 0) | (G << 8) | (B << 16) | (0xFF << 24);
    }

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
        this.progressImg = new NativeImage(NativeImage.Format.RGBA, progressListener.getDiameter(), progressListener.getDiameter(), true);
        this.progressTexture = new DynamicTexture(progressImg);
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

        WGUnloadingScreen unload;
        Screen nextScreen;

        if(this.main.isAborted()) {
            nextScreen = new AlertScreen(
                    () -> { minecraft.setScreen(lastScreen); },
                    Component.literal("Error"),
                    Component.literal("An error occurred during WorldGen!\n\nPlease check the Minecraft logs and submit the stack trace of the error on GitHub."));
        }
        else if (aborted)
        {
            nextScreen = lastScreen;
        }
        else
        {
            nextScreen = selectWorldScreen;
        }

        unload = new WGUnloadingScreen(main, nextScreen);

        this.minecraft.setScreen(unload);
        CompletableFuture.runAsync(
                () -> {
                    try {
                        main.close();
                    } catch (Throwable cause) {
                        cause.printStackTrace();
                    }

                    unload.signalComplete();
                }
        );

        progressImg.close();
        progressTexture.close();
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
        } else if(this.getProgress() >= 100 || this.main.isAborted()) {
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

        int verticalSpace = this.btnReturn.getY() - 10 - top;
        int vRadius = verticalSpace / 2;
        int boxVCenter = top + vRadius;
        int radius = Math.min(vRadius, xCenter);

        if (cfg.enableProgress) {
            renderChunks(
                    poseStack,
                    this.progressListener,
                    xCenter - radius,
                    xCenter + radius,
                    boxVCenter - radius,
                    boxVCenter + radius
            );
        }

        super.render(poseStack, i, j, f);
    }

    public void renderChunks(PoseStack poseStack, WGChunkProgressListener chunkProgressListener, int xMin, int xMax, int yMin, int yMax) {
        int diameter = chunkProgressListener.getDiameter();
        int colorBorder = 0xFF666666;

        // Display the chunks
        switch ((int) (frameCounter++ % 5)) {
            case 0 -> progressImg.fillRect(0, 0, diameter, diameter, 0xFF000000);
            case 1 -> updateTexture(diameter, chunkProgressListener);
            case 2 -> progressTexture.upload();
        }

        // Render the texture
        renderTexture(xMin, yMin, xMax, yMax, diameter);

        // Create a border
        WGLoadingScreen.fill(poseStack, xMin-1, yMin-1, xMax+1, yMin, colorBorder); // Right
        WGLoadingScreen.fill(poseStack, xMax, yMin, xMax+1, yMax, colorBorder); // Down
        WGLoadingScreen.fill(poseStack, xMin-1, yMax, xMax+1, yMax+1, colorBorder); // Left
        WGLoadingScreen.fill(poseStack, xMin-1, yMin, xMin, yMax, colorBorder); // Up
    }

    public void updateTexture(int diameter, WGChunkProgressListener chunkProgressListener) {
        // Get synchronized versions for this update in bulk so it does not have to be done for each chunk,
        // this may make the visualizer lag behind but its unnoticeable and instead improves performance
        Long2ObjectMap<ChunkStatus> statuses = chunkProgressListener.getStatuses();
        LongSet unloaded = chunkProgressListener.getUnloaded();

        ChunkPos spawnPos = chunkProgressListener.getSpawnPos();
        int r = chunkProgressListener.getRadius();
        for (int v = 0; v < diameter; ++v) {
            for (int w = 0; w < diameter; ++w) {
                final ChunkStatus chunkStatus = getStatusLocal(statuses, spawnPos, r, v, w);
                final boolean isUnloaded = isUnloadedLocal(unloaded, spawnPos, r, v, w);
                final int color = (isUnloaded ? COLORS_UNLOADED.getInt(chunkStatus) : COLORS.getInt(chunkStatus));
                progressImg.setPixelRGBA(v, w, color);
            }
        }
    }

    public void renderTexture(double xMin, double yMin, double xMax, double yMax, float diameter) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, progressTexture.getId());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(xMin, yMax, 0.0).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(xMax, yMax, 0.0).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(xMax, yMin, 0.0).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(xMin, yMin, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        tesselator.end();
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
