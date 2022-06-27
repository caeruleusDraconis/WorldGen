// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.adapters;

import caeruleusTait.WorldGen.mixin.SectionStorageAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SynchronizedPoiManager extends PoiManager {
    private final PoiManager real;

    public SynchronizedPoiManager(PoiManager _real, Path _path) {
        super(_path.resolve("poi_tmp"), null, false, null);
        real = _real;
    }

    public synchronized void flushAndRemove(ChunkPos chunkPos) {
        real.flush(chunkPos);
        ((SectionStorageAccessor<PoiSection>) real).getStorage().remove(chunkPos.toLong());
    }

    @Override
    public synchronized void add(BlockPos blockPos, PoiType poiType) {
        real.add(blockPos, poiType);
    }

    @Override
    public synchronized void remove(BlockPos blockPos) {
        real.remove(blockPos);
    }

    @Override
    public synchronized long getCountInRange(Predicate<PoiType> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getCountInRange(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized boolean existsAtPosition(PoiType poiType, BlockPos blockPos) {
        return real.existsAtPosition(poiType, blockPos);
    }

    @Override
    public synchronized Stream<PoiRecord> getInSquare(Predicate<PoiType> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getInSquare(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<PoiRecord> getInRange(Predicate<PoiType> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getInRange(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<PoiRecord> getInChunk(Predicate<PoiType> predicate, ChunkPos chunkPos, Occupancy occupancy) {
        return real.getInChunk(predicate, chunkPos, occupancy);
    }

    @Override
    public synchronized Stream<BlockPos> findAll(
            Predicate<PoiType> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findAll(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<BlockPos> findAllClosestFirst(
            Predicate<PoiType> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findAllClosestFirst(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> find(
            Predicate<PoiType> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.find(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> findClosest(Predicate<PoiType> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.findClosest(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> findClosest(
            Predicate<PoiType> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findClosest(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> take(Predicate<PoiType> predicate, Predicate<BlockPos> predicate2, BlockPos blockPos, int i) {
        return real.take(predicate, predicate2, blockPos, i);
    }

    @Override
    public synchronized Optional<BlockPos> getRandom(
            Predicate<PoiType> predicate,
            Predicate<BlockPos> predicate2,
            Occupancy occupancy,
            BlockPos blockPos,
            int i,
            Random random
    ) {
        return real.getRandom(predicate, predicate2, occupancy, blockPos, i, random);
    }

    @Override
    public synchronized boolean release(BlockPos blockPos) {
        return real.release(blockPos);
    }

    @Override
    public synchronized boolean exists(BlockPos blockPos, Predicate<PoiType> predicate) {
        return real.exists(blockPos, predicate);
    }

    @Override
    public synchronized Optional<PoiType> getType(BlockPos blockPos) {
        return real.getType(blockPos);
    }

    @Override
    public synchronized int getFreeTickets(BlockPos blockPos) {
        return real.getFreeTickets(blockPos);
    }

    @Override
    public synchronized int sectionsToVillage(SectionPos sectionPos) {
        return real.sectionsToVillage(sectionPos);
    }

    @Override
    public synchronized void tick(BooleanSupplier booleanSupplier) {
        real.tick(booleanSupplier);
    }

    @Override
    public synchronized void checkConsistencyWithBlocks(ChunkPos chunkPos, LevelChunkSection levelChunkSection) {
        real.checkConsistencyWithBlocks(chunkPos, levelChunkSection);
    }

    @Override
    public synchronized void ensureLoadedAndValid(LevelReader levelReader, BlockPos blockPos, int i) {
        real.ensureLoadedAndValid(levelReader, blockPos, i);
    }

    @Override
    public synchronized boolean hasWork() {
        return real.hasWork();
    }

    @Override
    public synchronized void flush(ChunkPos chunkPos) {
        real.flush(chunkPos);
    }

    @Override
    public synchronized void close() throws IOException {
        real.close();
    }
}
