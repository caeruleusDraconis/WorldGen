// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.util;

import caeruleusTait.WorldGen.worker.WGChunkGenWorkHost;
import caeruleusTait.WorldGen.worker.WGChunkWorkUnit;
import caeruleusTait.WorldGen.worker.WGWorkUnit;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class WGChunkListGenerator {

    private WGChunkListGenerator() {
    }


    /**
     * Returns the maximum number of possible threads given the rectangle configuration.
     *
     * @param rowHeight The height of a row
     * @param start The top left coordinate of the rectangle
     * @param end The bottom right coordinate of the rectangle
     * @param maxThreads The maximum amount of threads available
     * @return The amount of threads that can be used for the giiven configuration
     */
    public static int maxPossibleThreads(int rowHeight, ChunkPos start, ChunkPos end, int maxThreads) {
        int height = end.z - start.z + 1;
        int multiplier = 1;
        int width = end.x - start.x + 1;
        int taskRowCount = Mth.ceil((double) height / (24 + rowHeight));

        if((taskRowCount < maxThreads / 2) && (width > (24 + 2*rowHeight))) {
            multiplier = 2;
        }

        return multiplier * taskRowCount;
    }

    /**
     * Generates a list of fully functional chunk work units, with filled dependencies.
     *
     * @param workHost The work host that will manage the work units
     * @param rowHeight The height of a singular row
     * @param maxThreads The maximum amount of threads that are available
     * @param start The top left coordinate of the rectangle
     * @param end The bottom right coordinate of the rectangle
     * @param chunkPosFilter The filter to apply on the chunk positions, for special shapes
     * @return A list of fully assembled chunk work units to generate the given rectangle
     */
    public static List<WGChunkWorkUnit> generateChunkWorkList(WGChunkGenWorkHost workHost, int rowHeight, int maxThreads, ChunkPos start, ChunkPos end, Predicate<? super ChunkPos> chunkPosFilter) {
        int height = end.z - start.z + 1;
        int width = end.x - start.x + 1;

        int gap = Mth.ceil((double) 24 / rowHeight) + 1;
        int rowCount = Mth.ceil((double) height / rowHeight);
        int taskRowCount = Mth.ceil((double) height / (24 + rowHeight));
        int colCount = 1;

        List<WGChunkWorkUnit> result;

        int totalCount = rowCount;

        // If we only have 1 row, just do one full block
        if(rowCount == 1) {
            result = new ArrayList<>(1);
            if(width == height && width % 2 == 1) {
                // If we have a square with an odd diameter, use the snail method because it looks fancy
                int radius = width/2;
                result.add(new WGChunkWorkUnit(workHost, snailTraversal(new ChunkPos(start.x + radius, start.z + radius), radius).stream().filter(chunkPosFilter).toList()));
            } else {
                // Otherwise, feed it all to the snake
                result.add(new WGChunkWorkUnit(workHost, snakeTraversal(start.x, end.x, start.z, end.z, TraversalDirection.RIGHT).stream().filter(chunkPosFilter).toList()));
            }

            return result;
        }

        boolean multiColumn = false;

        // Check if we can split it in the x-axis as well, if we have enough threads and the field is wide enough
        if((taskRowCount < maxThreads) && (width > (24 + 2*rowHeight))) {
            totalCount = totalCount * 2 + 1;
            colCount = 2;
            multiColumn = true;
        }

        result = new ArrayList<>(totalCount);

        // Create the work units and map them onto task block IDs
        Map<Integer, List<WGChunkWorkUnit>> mappedUnits = createMappedWorkUnits(
                                                                            workHost,
                                                                            start, end,
                                                                            taskRowCount, colCount,
                                                                            gap, rowHeight,
                                                                            chunkPosFilter);

        // Create the corresponding task-internal and inter-task dependencies
        createDependencies(mappedUnits, colCount);

        // Accumulate all work unit lists into the final result
        for(List<WGChunkWorkUnit> workUnitList : mappedUnits.values()) {
            result.addAll(workUnitList);
        }


        // Add the stripes between columns to finish everything off
        if(multiColumn) {
            int xStride = (width - (colCount-1)*24) / colCount;
            int xBegin = start.x + xStride + 1;

            // Let's go through each filler column
            for(int col = 0; col < colCount-1; ++col) {
                List<WGWorkUnit> preWorkBlock = new ArrayList<>(taskRowCount*colCount);

                // The column stripes need to wait until all touching tasks are done,
                // so let's find them
                for(int taskID = 0; taskID < taskRowCount; ++taskID) {
                    // The offset is to find the left and right side of the column
                    for(int offset = 0; offset < 2; ++offset) {
                        // Now we assemble everything and use the column index to get us the right value
                        List<WGChunkWorkUnit> mappedList = mappedUnits.get((taskID*colCount)+col+offset);

                        // And from the list we just want the last row of each task as our dependency
                        preWorkBlock.add(mappedList.get(mappedList.size() - 1));
                    }
                }


                // Put it into our row format as well
                Map<Integer, List<WGChunkWorkUnit>> stripeMapped = createMappedWorkUnits(
                        workHost,
                        new ChunkPos(xBegin, start.z), new ChunkPos(xBegin + 22, end.z),
                        taskRowCount, 1,
                        gap, rowHeight,
                        chunkPosFilter);

                // Create the corresponding task-internal and inter-task dependencies of the stripe
                createDependencies(stripeMapped, 1);


                for(List<WGChunkWorkUnit> workUnitList : stripeMapped.values()) {
                    // First make sure that each thread of this column waits for the prior work to be done
                    workUnitList.get(0).addDependencies(preWorkBlock);

                    result.addAll(workUnitList);
                }

                xBegin += xStride + 24;
            }
        }

        return result;
    }

    /**
     * Creates the internal dependency structure between and within the task blocks in the given map.
     *
     * @param mappedUnits The mapped work units to process
     * @param colCount The amount of columns to use.
     */
    private static void createDependencies(Map<Integer, List<WGChunkWorkUnit>> mappedUnits, int colCount) {
        for(int taskID = 0; taskID < mappedUnits.keySet().size(); ++taskID) {
            List<WGChunkWorkUnit> taskWork = mappedUnits.get(taskID);

            // Each row of a task depends on the row directly before it (internal dependencies)
            for(int i = 1; i < taskWork.size(); ++i) {
                taskWork.get(i).addDependency(taskWork.get(i - 1));
            }


            // Add a dependency to the next task to ensure sufficient distance (24 chunks)
            for(int i = 1; i < taskWork.size(); ++i) {

                // Check if there is another task to depend on
                if(taskID + colCount >= mappedUnits.size()) {
                    break;
                }

                List<WGChunkWorkUnit> nextTaskWork = mappedUnits.get(taskID + colCount);

                // Check if the next task even has a row that we need to depend upon
                if(i-1 >= nextTaskWork.size()) {
                    break;
                }

                taskWork.get(i).addDependency(nextTaskWork.get(i-1));
            }
        }
    }

    /**
     * Internal function for creating a structured relation between task blocks to corresponding work units.
     * This function takes the rectangle given to it and splits it into the given count of rows & columns.
     * A given filter is applied on the computed chunks to get special shapes such as the circle.
     *
     * @param workHost The work host that will manage the work units
     * @param start The top left corner of the rectangle
     * @param end The bottom right corner of the rectangle
     * @param taskRowCount The amount of task rows (= number of task blocks)
     * @param taskColCount The amount of columns
     * @param rowsPerTask The amount of rows per task (necessary to ensure thread-safety)
     * @param rowHeight The height of a row
     * @param chunkPosFilter The filter to apply on the chunk positions
     * @return A mapping between task block IDs and corresponding work units
     */
    private static Map<Integer, List<WGChunkWorkUnit>> createMappedWorkUnits(
                                                                    WGChunkGenWorkHost workHost,
                                                                    ChunkPos start, ChunkPos end,
                                                                    int taskRowCount, int taskColCount,
                                                                    int rowsPerTask, int rowHeight,
                                                                    Predicate<? super ChunkPos> chunkPosFilter) {
        Map<Integer, List<WGChunkWorkUnit>> mappedUnits = new Int2ObjectOpenHashMap<>(taskRowCount*taskColCount);

        int xStride = end.x - start.x;

        if(taskColCount > 1) {
            // Calculate how far we need to walk each time to get to the start of a new column
            xStride = (xStride - (taskColCount-1)*24) / taskColCount;
        }

        int zStartPos = start.z;


        // The entire chunk rectangle is split into task blocks that need 24 chunks of distance to the next task block
        // to be worked on safely in a multi-threaded scenario.
        // A task block consists of 1 row of rowHeight + the rows for processing the 24 chunks to the next task block.
        for(int taskRow = 0; taskRow < taskRowCount; ++taskRow) {
            for (int stage = 0; stage < rowsPerTask; ++stage) {
                if (zStartPos > end.z) {
                    break;
                }

                int taskID = taskRow * taskColCount;
                int xStartPos = start.x;

                TraversalDirection currentDirection = TraversalDirection.RIGHT;

                // For each column, generate the snake
                for(int col = 0; col < taskColCount; ++col) {
                    if(xStartPos > end.x)
                        break;

                    // Get a snake to fill the current row
                    WGChunkWorkUnit workUnit =
                            new WGChunkWorkUnit(workHost,
                                    snakeTraversal(
                                            xStartPos,
                                            Math.min(end.x, xStartPos + xStride),
                                            zStartPos,
                                            Math.min(end.z, zStartPos + rowHeight - 1),
                                            currentDirection
                                    ).stream().filter(chunkPosFilter).toList()
                            );


                    if(!mappedUnits.containsKey(taskID)) {
                        // The task block was not seen yet, initialize
                        List<WGChunkWorkUnit> unitList = new ArrayList<>(rowsPerTask);
                        unitList.add(workUnit);
                        mappedUnits.put(taskID, unitList);
                    } else {
                        // The task block already exists, add to it
                        mappedUnits.get(taskID).add(workUnit);
                    }

                    // Increase the task ID and move over horizontally
                    ++taskID;
                    xStartPos += xStride + 24;
                    currentDirection = (currentDirection == TraversalDirection.RIGHT ?
                                        TraversalDirection.LEFT : TraversalDirection.RIGHT); // Switch direction
                }

                zStartPos += rowHeight;
            }
        }

        return mappedUnits;
    }

    /**
     * Generates a directional rectangle of chunk positions, by bouncing between two points at each step on a walk.
     *
     * @param walkStart The starting coordinate of the walk
     * @param walkEnd The ending coordinate of the walk (has to be smaller than walkStart)
     * @param bounceStart The starting coordinate of the bounce
     * @param bounceEnd The ending coordinate of the bounce (has to be smaller than bounceStart)
     * @param direction The direction to walk in
     * @return An ordered list of chunk positions
     */
    @NotNull
    public static List<ChunkPos> snakeTraversal(int walkStart, int walkEnd, int bounceStart, int bounceEnd, TraversalDirection direction) {
        // Prevent impossible work
        if(walkStart > walkEnd || bounceStart > bounceEnd)
            return new ArrayList<>(0);

        List<ChunkPos> chunks = new ArrayList<>(Mth.abs(walkEnd-walkStart * bounceEnd-bounceStart));

        boolean flipXZ; // Do we flip x and z coordinates?
        boolean invert; // Do we invert the list at the end?

        switch(direction) {
            case UP -> {
                flipXZ = true;
                invert = true;
            }
            case DOWN -> {
                flipXZ = true;
                invert = false;
            }
            case LEFT -> {
                flipXZ = false;
                invert = true;
            }
            case RIGHT -> {
                flipXZ = false;
                invert = false;
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        }

        int x = walkStart;
        int z = (invert ? bounceEnd : bounceStart); // Also invert where the bounce starts for **AESTHETICS**


        // We do a walk
        while(x <= walkEnd) {

            // And at each step, we bounce up and down
            if(z <= bounceStart) {
                for(z = bounceStart; z <= bounceEnd; ++z) {
                    chunks.add((flipXZ ? new ChunkPos(z, x) : new ChunkPos(x, z)));
                }
            } else {
                for(z = bounceEnd; z >= bounceStart; --z) {
                    chunks.add((flipXZ ? new ChunkPos(z, x) : new ChunkPos(x, z)));
                }
            }

            ++x;
        }

        if(invert) {
            Collections.reverse(chunks);
        }

        return chunks;
    }

    /**
     * Generates a square of chunk positions, by forming a snails house, growing from the center.
     *
     * @param center The center of the snail house
     * @param r The radius of the snail house
     * @return An ordered list of chunk positions
     */
    public static List<ChunkPos> snailTraversal(ChunkPos center, int r) {
        int diameter = 2 * r + 1;
        List<ChunkPos> chunks = new ArrayList<>(diameter * diameter);

        chunks.add(center);

        for (int distance = 1; distance <= r; ++distance) {
            int x = distance;
            int z = -(distance - 1);

            for (; z < distance; ++z) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }

            for (; x > -distance; --x) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }

            for (; z > -distance; --z) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }

            for (; x <= distance; ++x) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }
        }


        return chunks;
    }
}
