package caeruleusTait.WorldGen.worker;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * A work unit that works on processing a list of chunks.
 */
public class WGChunkWorkUnit extends WGWorkUnit {

    private final List<ChunkPos> chunkPositions;
    private final WGChunkGenWorkHost workHost;

    public WGChunkWorkUnit(WGChunkGenWorkHost workHost, List<ChunkPos> chunks) {
        this.chunkPositions = chunks;
        this.workHost = workHost;
    }

    public List<ChunkPos> getChunkPositions() {
        return new ArrayList<>(chunkPositions);
    }

    @Override
    protected boolean work() {
        for(ChunkPos chunkPos : chunkPositions) {
            if(workHost.isAborted()) {
                // We are no longer running, abort
                return false;
            } else {
                // Hand the work itself off to the generator
                workHost.submitGenerate(chunkPos);
            }
        }
        return true;
    }

}
