// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.adapters;

import caeruleusTait.WorldGen.mixin.SectionStorageAccessor;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SynchronizedPoiManager extends PoiManager {
    private final PoiManager real;

    public SynchronizedPoiManager(PoiManager _real, Path _path) {
        super(_path.resolve("poi_tmp"), null, false, null, null);
        real = _real;
    }

    public synchronized void flushAndRemove(ChunkPos chunkPos) {
        real.flush(chunkPos);
        ((SectionStorageAccessor<PoiSection>) real).getStorage().remove(chunkPos.toLong());
    }

    @Override
    public synchronized void add(BlockPos blockPos, Holder<PoiType> holder) {
        real.add(blockPos, holder);
    }

    @Override
    public synchronized void remove(BlockPos blockPos) {
        real.remove(blockPos);
    }

    @Override
    public synchronized long getCountInRange(Predicate<Holder<PoiType>> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getCountInRange(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized boolean existsAtPosition(ResourceKey<PoiType> resourceKey, BlockPos blockPos) {
        return real.existsAtPosition(resourceKey, blockPos);
    }

    @Override
    public synchronized Stream<PoiRecord> getInSquare(Predicate<Holder<PoiType>> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getInSquare(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<PoiRecord> getInRange(Predicate<Holder<PoiType>> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.getInRange(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<PoiRecord> getInChunk(Predicate<Holder<PoiType>> predicate, ChunkPos chunkPos, Occupancy occupancy) {
        return real.getInChunk(predicate, chunkPos, occupancy);
    }

    @Override
    public synchronized Stream<BlockPos> findAll(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findAll(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<Pair<Holder<PoiType>, BlockPos>> findAllWithType(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findAllWithType(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findAllClosestFirstWithType(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> find(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.find(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> findClosest(Predicate<Holder<PoiType>> predicate, BlockPos blockPos, int i, Occupancy occupancy) {
        return real.findClosest(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<Pair<Holder<PoiType>, BlockPos>> findClosestWithType(
            Predicate<Holder<PoiType>> predicate,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findClosestWithType(predicate, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> findClosest(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            BlockPos blockPos,
            int i,
            Occupancy occupancy
    ) {
        return real.findClosest(predicate, predicate2, blockPos, i, occupancy);
    }

    @Override
    public synchronized Optional<BlockPos> take(
            Predicate<Holder<PoiType>> predicate,
            BiPredicate<Holder<PoiType>, BlockPos> biPredicate,
            BlockPos blockPos,
            int i
    ) {
        return real.take(predicate, biPredicate, blockPos, i);
    }

    @Override
    public synchronized Optional<BlockPos> getRandom(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> predicate2,
            Occupancy occupancy,
            BlockPos blockPos,
            int i,
            RandomSource randomSource
    ) {
        return real.getRandom(predicate, predicate2, occupancy, blockPos, i, randomSource);
    }

    @Override
    public synchronized boolean release(BlockPos blockPos) {
        return real.release(blockPos);
    }

    @Override
    public synchronized boolean exists(BlockPos blockPos, Predicate<Holder<PoiType>> predicate) {
        return real.exists(blockPos, predicate);
    }

    @Override
    public synchronized Optional<Holder<PoiType>> getType(BlockPos blockPos) {
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
    protected synchronized void setDirty(long l) {
        throw new RuntimeException("NyI");
    }

    @Override
    protected synchronized void onSectionLoad(long l) {
        throw new RuntimeException("NyI");
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

    @Nullable
    @Override
    protected synchronized Optional<PoiSection> get(long l) {
        throw new RuntimeException("NyI");
    }

    @Override
    protected synchronized Optional<PoiSection> getOrLoad(long l) {
        throw new RuntimeException("NyI");
    }

    @Override
    protected synchronized boolean outsideStoredRange(long l) {
        throw new RuntimeException("NyI");
    }

    @Override
    protected synchronized PoiSection getOrCreate(long l) {
        throw new RuntimeException("NyI");
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
