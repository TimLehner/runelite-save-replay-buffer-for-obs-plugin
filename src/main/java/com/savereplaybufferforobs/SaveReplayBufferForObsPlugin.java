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

    @Subscribe
    private void onScreenshotTaken(ScreenshotTaken event) {
        if (config.saveOnScreenshot()) {
            log.debug("Attempting to save OBS Replay Buffer");
            scheduledExecutorService.schedule(obsClient::saveReplayBuffer, config.saveAfterDelay(), TimeUnit.SECONDS);
        }
    }

    private void reconnect() {
        if (obsClient != null) {
            obsClient.disconnect();
        }

        obsClient = new WebSocketClientForObs(okHttpClient, gson, config.websocketServerHost(), config.websocketPort(), config.websocketPassword());
        obsClient.connect();
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!Objects.equals(event.getGroup(), PLUGIN_IDENTIFIER)) {
            return;
        }

        reconnect();
    }

    @Subscribe
    private void onPluginMessage(PluginMessage event) {
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

}
