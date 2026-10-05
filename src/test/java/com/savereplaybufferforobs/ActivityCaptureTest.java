package com.savereplaybufferforobs;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivityCaptureTest
{
    private final SaveReplayBufferForObsConfig config = new SaveReplayBufferForObsConfig()
    {
        public boolean captureCox() { return true; }
        public boolean captureTob() { return true; }
        public boolean captureToa() { return true; }
        public boolean captureInferno() { return true; }
        public boolean captureColosseum() { return true; }
        public boolean captureDoom() { return true; }
    };
    private final SaveReplayBufferForObsConfig disabled = new SaveReplayBufferForObsConfig() { };
    private static final WorldPoint OUTSIDE = new WorldPoint(3200, 3200, 0);
    private long now;
    private final List<Runnable> callbacks = new ArrayList<>();
    private final List<Long> delays = new ArrayList<>();
    private final List<Long> dueTimes = new ArrayList<>();
    private final List<Integer> requests = new ArrayList<>();
    private final List<ScheduledFuture<?>> futures = new ArrayList<>();
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1)
    {
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
        {
            callbacks.add(command);
            long delayMillis = unit.toMillis(delay);
            delays.add(delayMillis);
            dueTimes.add(now + TimeUnit.MILLISECONDS.toNanos(delayMillis));
            ScheduledFuture<?> future = super.schedule(command, 1, TimeUnit.DAYS);
            futures.add(future);
            return future;
        }
    };
    private final ActivityCapture capture = new ActivityCapture(scheduler, () -> now, requests::add, message -> { });

    @After
    public void close()
    {
        scheduler.shutdownNow();
    }

    @Test
    public void eachActivitySavesFromEntryUntilLeaving()
    {
        for (ActivityCapture.Activity activity : ActivityCapture.Activity.values())
        {
            capture.cancel();
            callbacks.clear();
            dueTimes.clear();
            now = 0;
            enter(activity);
            now = TimeUnit.SECONDS.toNanos(60);
            capture.locationChanged(OUTSIDE, false, config);
            assertEquals(activity.toString(), 1, callbacks.size());
            run(0);
            assertEquals(activity.toString(), Integer.valueOf(62), requests.get(requests.size() - 1));
        }
    }

    @Test
    public void roomChangesDelvesAndTheColosseumChestKeepOneRecording()
    {
        capture.locationChanged(region(15698), false, config);
        capture.locationChanged(region(15700), false, config); // ToA room change.
        capture.locationChanged(region(15184), false, config);
        capture.locationChanged(new WorldPoint(1300, 9560, 0), false, config); // Straight into Doom ends ToA.
        capture.locationChanged(new WorldPoint(3400, 6400, 0), false, config); // Next delve.
        capture.locationChanged(new WorldPoint(3520, 6400, 0), false, config);
        assertEquals(1, callbacks.size());

        capture.locationChanged(new WorldPoint(1820, 3100, 0), false, config); // Colosseum arena ends Doom.
        capture.locationChanged(new WorldPoint(1800, 3080, 0), false, config); // Reward chest, same region.
        assertEquals(2, callbacks.size());
        capture.locationChanged(OUTSIDE, false, config);
        assertEquals(3, callbacks.size());
    }

    @Test
    public void paddingAndSchedulingLagAreIncluded()
    {
        capture.locationChanged(region(15698), false, config);
        now = TimeUnit.SECONDS.toNanos(100);
        capture.locationChanged(OUTSIDE, false, config);
        assertEquals(Long.valueOf(1000), delays.get(0)); // 1% post-padding.
        assertTrue(requests.isEmpty());
        now = TimeUnit.MILLISECONDS.toNanos(101_500); // 500 ms scheduler lag.
        run(0);
        assertEquals(Integer.valueOf(103), requests.get(0)); // 101.5s elapsed plus 1% pre-padding.
    }

    @Test
    public void disabledActivitiesNeverRecordAndTurningOneOffDiscardsTheRecording()
    {
        capture.locationChanged(region(15698), false, disabled);
        assertFalse(capture.inEnabledActivity(disabled));
        capture.locationChanged(OUTSIDE, false, disabled);
        assertTrue(callbacks.isEmpty());

        capture.locationChanged(region(15698), false, config);
        assertTrue(capture.inEnabledActivity(config));
        capture.locationChanged(OUTSIDE, false, disabled);
        assertTrue(callbacks.isEmpty());
        assertFalse(capture.inEnabledActivity(config));
    }

    @Test
    public void logoutAndHopEndTheRecordingButALostConnectionKeepsIt() throws Exception
    {
        SaveReplayBufferForObsPlugin plugin = new SaveReplayBufferForObsPlugin();
        setField(plugin, "activityCapture", capture);
        setField(plugin, "config", config);
        GameStateChanged state = new GameStateChanged();
        enter(ActivityCapture.Activity.TOB);
        now = TimeUnit.SECONDS.toNanos(30);
        state.setGameState(GameState.CONNECTION_LOST);
        plugin.onGameStateChanged(state);
        enter(ActivityCapture.Activity.TOB); // Reconnected into the same raid.
        assertTrue(callbacks.isEmpty());

        now = TimeUnit.SECONDS.toNanos(60);
        state.setGameState(GameState.HOPPING);
        plugin.onGameStateChanged(state);
        assertEquals(1, callbacks.size());
        run(0);
        assertEquals(Integer.valueOf(62), requests.get(0));

        enter(ActivityCapture.Activity.INFERNO); // Logged back in inside the Inferno after pausing.
        now = TimeUnit.SECONDS.toNanos(1860);
        state.setGameState(GameState.LOGIN_SCREEN);
        plugin.onGameStateChanged(state);
        assertEquals(2, callbacks.size());
        run(1);
        assertEquals(Integer.valueOf(1836), requests.get(1)); // Only the time since logging back in.
    }

    @Test
    public void aSaveSurvivesEnteringTheNextActivityButShutdownCancelsIt() throws Exception
    {
        enter(ActivityCapture.Activity.TOA);
        now = TimeUnit.SECONDS.toNanos(100);
        enter(ActivityCapture.Activity.DOOM);
        run(0);
        assertEquals(1, requests.size());

        SaveReplayBufferForObsPlugin plugin = new SaveReplayBufferForObsPlugin()
        {
            @Override
            public void clearObsException() { }
        };
        setField(plugin, "activityCapture", capture);
        now = TimeUnit.SECONDS.toNanos(200);
        capture.locationChanged(OUTSIDE, false, config);
        plugin.shutDown();
        assertTrue(futures.get(1).isCancelled()); // The pending Doom save.
    }

    private void enter(ActivityCapture.Activity activity)
    {
        switch (activity)
        {
            case COX:
                capture.locationChanged(OUTSIDE, true, config);
                break;
            case TOB:
                capture.locationChanged(region(12869), false, config);
                break;
            case TOA:
                capture.locationChanged(region(15698), false, config);
                break;
            case INFERNO:
                capture.locationChanged(region(9043), false, config);
                break;
            case COLOSSEUM:
                capture.locationChanged(new WorldPoint(1820, 3100, 0), false, config);
                break;
            case DOOM:
                capture.locationChanged(new WorldPoint(1300, 9560, 0), false, config);
                break;
            default:
                throw new AssertionError(activity);
        }
    }

    private void run(int index)
    {
        now = Math.max(now, dueTimes.get(index));
        callbacks.get(index).run();
    }

    private static void setField(Object object, String name, Object value) throws Exception
    {
        Field field = SaveReplayBufferForObsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static WorldPoint region(int id)
    {
        return new WorldPoint((id >> 8) * 64, (id & 255) * 64, 0);
    }
}
