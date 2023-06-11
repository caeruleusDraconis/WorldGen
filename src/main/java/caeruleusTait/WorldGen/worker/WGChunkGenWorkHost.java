package caeruleusTait.WorldGen.worker;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class WGChunkGenWorkHost {

    private List<WGChunkWorkUnit> activeWork;

    private final WGMain wgmain;
    private final int maxThreads;

    private List<Thread> activeThreads;
    private int threadID;

    private boolean aborted = false;

    public WGChunkGenWorkHost(WGMain wgmain, int maxThreads) {
        this.wgmain = wgmain;
        this.maxThreads = maxThreads;
    }

    public void abortWork() {
        aborted = true;
        joinThreads();
        activeThreads.clear();
    }

    public void joinThreads() {
        this.activeThreads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isAborted() {
        return aborted;
    }

    public void submitGenerate(ChunkPos pos) {
        wgmain.generateChunk(pos);
    }


    public void submitWork(List<WGChunkWorkUnit> workList) {
        this.activeWork = workList;
        this.activeThreads = new ArrayList<>(maxThreads);
        this.aborted = false;
        this.threadID = 0;

        // Distribute the work of any ready work units
        distributeWork(activeWork.stream().filter(WGWorkUnit::isReady).toList());
    }

    private synchronized void distributeWork(List<WGChunkWorkUnit> workList) {
        if(aborted) {
            // Don't distribute when we are aborted
            return;
        }

        // Start a new Thread for each task and start it
        for(WGChunkWorkUnit workUnit : workList) {
            if(this.activeThreads.size() >= maxThreads)
                break;

            Thread newWorker = new Thread(
                    () -> workUnit.startWork(this::onWorkUnitCompleted, this::onWorkFailed, workUnit),
                    "WG-Worker-" + threadID);

            newWorker.start();

            this.activeThreads.add(newWorker);
            ++threadID;
        }
    }

    private boolean onWorkFailed(WGChunkWorkUnit failedUnit) {
        this.aborted = true; // Set this flag first, otherwise WGMain will attempt to make the workers join themselves.
        wgmain.abortWork();
        return false;
    }

    public boolean onWorkUnitCompleted(WGChunkWorkUnit completedUnit) {
        // We are no longer running, so let's stop here
        if(aborted) {
            return false;
        }

        // Get all free work units
        List<WGChunkWorkUnit> readyWork =
                activeWork.stream().filter(
                    workUnit -> !workUnit.isInProgress()
                             && !workUnit.isCompleted()
                             &&  workUnit.isReady()).toList();

        if(readyWork.isEmpty()) {
            // We are done, let's stop
            synchronized (this) {
                this.activeThreads.remove(Thread.currentThread());
            }

            return true;
        } else if(readyWork.size() > 1) {
            // If more than 1 Work Unit is ready, distribute the others
            distributeWork(readyWork.subList(1, readyWork.size()));
        }

        // And then recycle the thread to do another task
        return readyWork.get(0).startWork(this::onWorkUnitCompleted,this::onWorkFailed, readyWork.get(0));
    }


}
