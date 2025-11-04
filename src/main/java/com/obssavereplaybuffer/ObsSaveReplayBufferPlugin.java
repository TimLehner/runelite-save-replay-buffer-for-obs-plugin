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
package com.obssavereplaybuffer;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ScreenshotTaken;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Save OBS Replay Buffer",
        description = "Enable the automatic saving of the OBS Replay Buffer when taking screenshots",
        tags = {"external", "videos", "integration", "OBS"}
)
@Slf4j
public class ObsSaveReplayBufferPlugin extends Plugin
{
    @Inject
    private ObsSaveReplayBufferConfig config;

    private ObsWebSocketClient obsClient;

    private final OkHttpClient client;
    private final Gson gson;


    public ObsSaveReplayBufferPlugin(OkHttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    @Provides
    ObsSaveReplayBufferConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ObsSaveReplayBufferConfig.class);
    }

    private void setTimeout(Runnable runnable, int delaySeconds) {
        new Thread(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                runnable.run();
            } catch (InterruptedException e) {
                runnable.run();
            }
        }).start();
    }

    @Subscribe
    private void onScreenshotTaken(ScreenshotTaken event) {
        if (config.saveObsReplayBuffer()) {
            log.debug("Attempting to save OBS Replay Buffer");
            this.setTimeout(this.obsClient::saveReplayBuffer, config.saveAfterDelay());
        }
    }

    @Override
    protected void startUp()
    {
        if (config.saveObsReplayBuffer()) {
            log.debug("Startup OBS Connection");
            this.obsClient = new ObsWebSocketClient(client, gson, config.websocketServerHost(), config.websocketPort(), config.websocketPassword());
            this.obsClient.connect();
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
