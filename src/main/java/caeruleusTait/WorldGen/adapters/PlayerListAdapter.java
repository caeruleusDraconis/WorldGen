// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.adapters;

import caeruleusTait.WorldGen.worker.WGMain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

public class PlayerListAdapter extends PlayerList {
    public PlayerListAdapter(
            MinecraftServer minecraftServer,
            WGMain main
    ) {
        super(minecraftServer, main.worldStem().registries(), null, 0);
    }

    @Override
    public int getViewDistance() {
        return 12;
    }

    @Override
    public int getSimulationDistance() {
        return 12;
    }
}
