/*
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
 */
package com.savereplaybufferforobs;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import static com.savereplaybufferforobs.Constants.PLUGIN_IDENTIFIER;

@ConfigGroup(PLUGIN_IDENTIFIER)
public interface SaveReplayBufferForObsConfig extends Config
{
    @ConfigSection(
            name = "OBS WebSocket Options",
            description = "Options for the OBS WebSocket connection",
            position = 99
    )
    String wsSection = "websocket";

    @ConfigItem(
            keyName = "saveOnScreenshot",
            name = "Save on screenshot",
            description = "Attempt to save the OBS replay buffer whenever saving screenshots.",
            position = 0
    )
    default boolean saveOnScreenshot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "saveAfterDelay",
            name = "Save after delay",
            description = "Delay the attempt to save the OBS replay buffer by a number of seconds.",
            position = 1
    )
    default int saveAfterDelay()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "saveOnPluginMessage",
            name = "Save on Plugin Messages (advanced)",
            description = "Allow other plugins to send messages to immediately save the OBS replay buffer with custom delay.",
            position = 2
    )
    default boolean saveOnPluginMessage()
    {
        return false;
    }

    @ConfigItem(
            keyName = "websocketPassword",
            name = "WebSocket Server Password",
            description = "The WebSocket Server Password displayed in OBS Tools -> WebSocket Server Settings",
            position = 0,
            section = wsSection,
            secret = true
    )
    default String websocketPassword()
    {
        return "";
    }

    @ConfigItem(
            keyName = "websocketPort",
            name = "WebSocket Server Port",
            description = "The WebSocket Server Port displayed in OBS Tools -> WebSocket Server Settings",
            position = 1,
            section = wsSection
    )
    default int websocketPort()
    {
        return 4455;
    }

    @ConfigItem(
            keyName = "websocketServerHost",
            name = "WebSocket Server Host",
            description = "The WebSocket Server Host displayed in OBS Tools -> WebSocket Server Settings",
            position = 2,
            section = wsSection
    )
    default String websocketServerHost()
    {
        return "localhost";
    }
}
