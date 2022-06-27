package caeruleusTait.WorldGen.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * An abstract class to encapsulate work unit behavior.
 * A work unit has dependencies that need to be fulfilled before it is deemed as ready.
 */
public abstract class WGWorkUnit {

    private static final AtomicInteger globalID = new AtomicInteger(0);

    private final int id;

    private boolean inProgress = false;
    private boolean completed = false;

    protected abstract boolean work();

    protected List<WGWorkUnit> dependencies;

    public WGWorkUnit() {
        this.id = globalID.getAndIncrement();
        this.dependencies = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void addDependencies(List<WGWorkUnit> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    public void addDependency(WGWorkUnit dep) {
        this.dependencies.add(dep);
    }

    public List<WGWorkUnit> getDependencies() {
        return dependencies;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public boolean isReady() {
        boolean ready = true;

        for (WGWorkUnit dependency : dependencies) {
            if (!dependency.isCompleted()) {
                ready = false;
                break;
            }
        }

        return ready;
    }


    public <T> boolean startWork(Function<T, Boolean> onSuccess, T input) {
        return startWork(onSuccess, (x -> false), input);
    }

    public <T> boolean startWork(Function<T, Boolean> onSuccess, Function<T, Boolean> onFail, T input) {
        // Be careful around here, since we don't want to have multiple threads start to work on this unit
        synchronized (this) {
            if (isCompleted())
                return true;
            if (!isReady() || inProgress)
                return false;

            inProgress = true;
        }

        boolean result = work();
        inProgress = false;


        if (result) {
            completed = true;
            return onSuccess.apply(input);
        } else {
            return onFail.apply(input);
        }
    }

}
