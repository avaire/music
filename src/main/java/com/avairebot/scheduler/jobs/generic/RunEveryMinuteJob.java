package com.avairebot.scheduler.jobs.generic;

import com.avairebot.AvaIre;
import com.avairebot.contracts.scheduler.Job;
import com.avairebot.scheduler.tasks.ChangeGameTask;
import com.avairebot.scheduler.tasks.GarbageCollectorTask;

import java.util.concurrent.TimeUnit;

public class RunEveryMinuteJob extends Job {

    private final ChangeGameTask changeGameTask = new ChangeGameTask();
    private final GarbageCollectorTask garbageCollectorTask = new GarbageCollectorTask();

    public RunEveryMinuteJob(AvaIre avaire) {
        super(avaire, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        handleTask(
            changeGameTask,
            garbageCollectorTask
        );
    }
}
