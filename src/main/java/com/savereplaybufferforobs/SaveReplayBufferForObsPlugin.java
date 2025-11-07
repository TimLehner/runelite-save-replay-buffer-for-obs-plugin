/*
 * Copyright (c) 2018, Tim Lehner <https://github.com/TimLehner>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.savereplaybufferforobs;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ScreenshotTaken;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.savereplaybufferforobs.Constants.ALLOWED_OBS_API_METHODS;
import static com.savereplaybufferforobs.Constants.PLUGIN_IDENTIFIER;

@PluginDescriptor(
        name = "Save Replay Buffer for OBS",
        description = "Enable the automatic saving of the OBS Replay Buffer when taking screenshots",
        tags = {"external", "videos", "integration", "OBS"}
)
@Slf4j
public class SaveReplayBufferForObsPlugin extends Plugin
{
    @Inject
    private SaveReplayBufferForObsConfig config;

    private WebSocketClientForObs obsClient;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Gson gson;

    @Provides
    SaveReplayBufferForObsConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SaveReplayBufferForObsConfig.class);
    }

    protected enum EventType
    {
        DEATH,
        SCREENSHOT
    }

    private int getDelayTime(EventType eventType)
    {
        switch (eventType) {
            case DEATH:
                return config.deathDelay();
            case SCREENSHOT:
                return config.screenshotDelay();
            default:
                return 0;
        }
    }

    private void saveReplayBuffer(EventType eventType)
    {
        log.debug("Attempting to save OBS Replay Buffer");
        scheduledExecutorService.schedule(obsClient::saveReplayBuffer, getDelayTime(eventType), TimeUnit.SECONDS);
    }

    @Subscribe
    private void onScreenshotTaken(ScreenshotTaken event)
    {
        if (config.saveOnScreenshot()) {
            saveReplayBuffer(EventType.SCREENSHOT);
        }
    }

    private void reconnect()
    {
        if (obsClient != null) {
            obsClient.disconnect();
        }

        obsClient = new WebSocketClientForObs(okHttpClient, gson, config.websocketServerHost(), config.websocketPort(), config.websocketPassword());
        obsClient.connect();
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event)
    {
        if (!Objects.equals(event.getGroup(), PLUGIN_IDENTIFIER)) {
            return;
        }

        reconnect();
    }

    @Subscribe
    private void onPluginMessage(PluginMessage event)
    {
        if (!config.saveOnPluginMessage() || !Objects.equals(event.getNamespace(), PLUGIN_IDENTIFIER)) {
            return;
        }

        String requestMethod = event.getName();
        if (ALLOWED_OBS_API_METHODS.contains(requestMethod)) {
            // temporarily require whitelist of commands. Perhaps we should expand the plugin scope to generic OBS integration?
            obsClient.makeOBSRequest(requestMethod, "runelite-clip-req", event.getData());
        }
    }

    @Override
    protected void startUp()
    {
        if (config.saveOnScreenshot()) {
            log.debug("Startup OBS Connection");
            reconnect();
        }
    }

    @Override
    protected void shutDown()
    {
        if (this.obsClient != null) {
            log.debug("Shutdown OBS Connection");
            this.obsClient.disconnect();
        }
    }

    /**
     * This plugin aims to follow the built-in Screenshots plugin in terms of functionality as much as possible
     * however since these plugins have different maintainers, and there should not be any expectation that the built-in
     * plugin is aware of this plugin in future iterations.
     *
     * Code below this comment was largely copied from https://github.com/runelite/runelite/blob/f448dc9d0d0be8553500c2e992afabe643b57b2f/runelite-client/src/main/java/net/runelite/client/plugins/screenshot/ScreenshotPlugin.java
     * =====
     * Copyright (c) 2018, Lotto <https://github.com/devLotto>
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions are met:
     *
     * 1. Redistributions of source code must retain the above copyright notice, this
     *    list of conditions and the following disclaimer.
     * 2. Redistributions in binary form must reproduce the above copyright notice,
     *    this list of conditions and the following disclaimer in the documentation
     *    and/or other materials provided with the distribution.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
     * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
     * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
     * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
     * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
     * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
     * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
     * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
     * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
     * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     *
     */

    @Inject
    private Client client;

    @Subscribe
    public void onActorDeath(ActorDeath actorDeath)
    {
        // original source https://github.com/runelite/runelite/blob/f448dc9d0d0be8553500c2e992afabe643b57b2f/runelite-client/src/main/java/net/runelite/client/plugins/screenshot/ScreenshotPlugin.java#L291
        Actor actor = actorDeath.getActor();
        if (actor instanceof Player)
        {
            Player player = (Player) actor;
            if (player == client.getLocalPlayer() && config.savePlayerDeath())
            {
                saveReplayBuffer(EventType.DEATH);
            }
            else if (player != client.getLocalPlayer()
                    && player.getCanvasTilePoly() != null
                    && (((player.isFriendsChatMember() || player.isFriend()) && config.saveFriendDeath())
                    || (player.isClanMember() && config.saveClanDeath())))
            {
                saveReplayBuffer(EventType.DEATH);
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged animationChanged)
    {
        // original source https://github.com/runelite/runelite/blob/f448dc9d0d0be8553500c2e992afabe643b57b2f/runelite-client/src/main/java/net/runelite/client/plugins/screenshot/ScreenshotPlugin.java#L312
        Actor actor = animationChanged.getActor();
        if (actor == client.getLocalPlayer()
                && actor.getAnimation() == AnimationID.HUMAN_DOOM_SCORPION_01_PLAYER_DEATH_01
                && config.savePlayerDeath())
        {
            saveReplayBuffer(EventType.DEATH);
        }
    }
}
