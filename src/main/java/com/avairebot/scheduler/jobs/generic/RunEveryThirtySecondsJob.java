package com.avairebot.scheduler.jobs.generic;

import com.avairebot.AvaIre;
import com.avairebot.contracts.scheduler.Job;
import com.avairebot.scheduler.tasks.MusicActivityTask;

import java.util.concurrent.TimeUnit;

public class RunEveryThirtySecondsJob extends Job {

    private final MusicActivityTask musicActivityTask = new MusicActivityTask();

    public RunEveryThirtySecondsJob(AvaIre avaire) {
        super(avaire, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        handleTask(
            musicActivityTask
        );
    }
}
