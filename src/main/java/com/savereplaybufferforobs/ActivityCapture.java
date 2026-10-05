package com.savereplaybufferforobs;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/** Records an activity from entering its area until leaving it, then asks Replay Buffer Pro to save a clip covering it. */
@Slf4j
final class ActivityCapture
{
    enum Activity
    {
        COX("Chambers of Xeric", SaveReplayBufferForObsConfig::captureCox),
        TOB("Theatre of Blood", SaveReplayBufferForObsConfig::captureTob),
        TOA("Tombs of Amascut", SaveReplayBufferForObsConfig::captureToa),
        INFERNO("Inferno", SaveReplayBufferForObsConfig::captureInferno),
        COLOSSEUM("Fortis Colosseum", SaveReplayBufferForObsConfig::captureColosseum),
        DOOM("Doom of Mokhaiotl", SaveReplayBufferForObsConfig::captureDoom);

        final String label;
        final Predicate<SaveReplayBufferForObsConfig> enabled;

        Activity(String label, Predicate<SaveReplayBufferForObsConfig> enabled)
        {
            this.label = label;
            this.enabled = enabled;
        }
    }

    private final ScheduledExecutorService scheduler;
    private final LongSupplier clock;
    private final IntConsumer save;
    private final Consumer<String> chat;
    private final Set<ScheduledFuture<?>> scheduledSaves = new HashSet<>();
    private Activity location;
    private boolean recording;
    private long started;

    /** {@code chat} receives the few messages players see; details go to the debug log. */
    ActivityCapture(ScheduledExecutorService scheduler, LongSupplier clock, IntConsumer save, Consumer<String> chat)
    {
        this.scheduler = scheduler;
        this.clock = clock;
        this.save = save;
        this.chat = chat;
    }

    static Activity activityAt(WorldPoint point)
    {
        switch (point.getRegionID())
        {
            case 12869: case 12613: case 13125: case 13122: case 13123:
            case 13379: case 12612: case 12611: case 12867:
                return Activity.TOB;
            case 14160: case 14672: case 15698: case 15700: case 14162: case 14164:
            case 15186: case 15188: case 14674: case 14676: case 15184: case 15696:
                return Activity.TOA;
            case 9043:
                return Activity.INFERNO;
        }
        if (point.getPlane() == 0 && point.getX() >= 1806 && point.getX() < 1844
            && point.getY() >= 3088 && point.getY() < 3126) { return Activity.COLOSSEUM; }
        if (point.getRegionID() == 13668 || point.getRegionID() == 14180
            || (point.getPlane() == 0 && point.getX() >= 1299 && point.getX() < 1323
                && point.getY() >= 9559 && point.getY() < 9585))
        {
            return Activity.DOOM;
        }
        return null;
    }

    synchronized void locationChanged(WorldPoint point, boolean inCox, SaveReplayBufferForObsConfig config)
    {
        Activity next = inCox ? Activity.COX : activityAt(point);
        if (location == Activity.COLOSSEUM && point.getRegionID() == 7216)
        {
            next = Activity.COLOSSEUM; // Keep the capture through the nearby reward chest.
        }
        if (location == next)
        {
            return;
        }
        String where = inCox ? "the CoX raid" : "region " + point.getRegionID();
        end("left for " + where, config);
        location = next;
        recording = next != null && next.enabled.test(config);
        if (recording)
        {
            started = clock.getAsLong();
            log.debug("{} capture started: entered {}", next.label, where);
            chat.accept("Recording " + next.label + " for a replay clip.");
        }
    }

    /** Logging out or hopping leaves the activity; logging back in inside it starts a new capture. */
    synchronized void exited(String reason, SaveReplayBufferForObsConfig config)
    {
        end(reason, config);
        location = null;
    }

    /** True inside an enabled activity, where the activity capture replaces the regular saves. */
    synchronized boolean inEnabledActivity(SaveReplayBufferForObsConfig config)
    {
        return location != null && location.enabled.test(config);
    }

    private void end(String reason, SaveReplayBufferForObsConfig config)
    {
        if (!recording)
        {
            return;
        }
        recording = false;
        if (!location.enabled.test(config))
        {
            log.debug("{} capture discarded ({}): capture was turned off mid-activity", location.label, reason);
            return;
        }
        long activityStarted = started;
        double activitySeconds = (clock.getAsLong() - activityStarted) / 1_000_000_000.0;
        double pre = activitySeconds * config.activityPrePercent() / 100.0;
        double post = activitySeconds * config.activityPostPercent() / 100.0;
        long delayMillis = (long) Math.ceil(post * 1000);
        log.debug("{} capture ended: {}. Activity lasted {}s; saving it plus {}s pre-padding after {}s post-padding",
            location.label, reason, Math.round(activitySeconds), Math.round(pre), Math.round(post));
        chat.accept("Saving your " + location.label + " replay clip (" + duration(activitySeconds) + ") in "
            + (long) Math.ceil(delayMillis / 1000.0) + " seconds.");
        scheduledSaves.removeIf(Future::isDone);
        scheduledSaves.add(scheduler.schedule(
            () -> save.accept(Math.max(1, (int) Math.ceil((clock.getAsLong() - activityStarted) / 1_000_000_000.0 + pre))),
            delayMillis, TimeUnit.MILLISECONDS));
    }

    /** Formats seconds as m:ss, or h:mm:ss from an hour. */
    static String duration(double seconds)
    {
        long total = Math.round(seconds);
        return total >= 3600
            ? String.format("%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60)
            : String.format("%d:%02d", total / 60, total % 60);
    }

    synchronized void cancel()
    {
        scheduledSaves.forEach(future -> future.cancel(false));
        scheduledSaves.clear();
        location = null;
        recording = false;
    }
}
