// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import caeruleusTait.WorldGen.adapters.PlayerListAdapter;
import com.mojang.authlib.GameProfile;
import net.minecraft.SystemReport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.PlayerList;

import java.io.IOException;
import java.util.concurrent.Executor;

public class WGMinecraftServer extends MinecraftServer {
    private final WGMain main;
    private final ChunkProgressListener chunkProgressListener;

    public WGMinecraftServer(
            WGMain main,
            ChunkProgressListener chunkProgressListener
    ) {
        super(
                main.mainThread(),
                main.levelStorageAccess(),
                main.packRepository(),
                main.worldStem(),
                main.minecraft().getProxy(),
                main.dataFixer(),
                new Services(null, null, null, null),
                i -> chunkProgressListener
        );

        this.main = main;
        this.chunkProgressListener = chunkProgressListener;
    }

    public ChunkProgressListener chunkProgressListener() {
        return chunkProgressListener;
    }

    @Override
    public PlayerList getPlayerList() {
        return new PlayerListAdapter(this, main);
    }

    @Override
    protected boolean initServer() throws IOException {
        return true;
    }

    @Override
    public void close() {
        // super.close();
    }

    public Executor executor() {
        //return ((MinecraftServerAccessor) this).getExecutor();
        return Runnable::run;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public boolean scheduleExecutables() {
        return false;
    }

    //  =================
    // === NyI Section ===
    //  =================


    @Override
    public int getOperatorUserPermissionLevel() {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getFunctionCompilationLevel() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean shouldRconBroadcast() {
        throw new RuntimeException("NyI");
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport systemReport) {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean isEpollEnabled() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean isCommandBlockEnabled() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean isPublished() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean shouldInformAdmins() {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile gameProfile) {
        throw new RuntimeException("NyI");
    }
}
