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
            name = "Events to capture",
            description = "Options for what triggers saving the Replay Buffer",
            position = 0
    )
    String whatSection = "what";

    @ConfigItem(
            keyName = "saveOnScreenshot",
            name = "All screenshots",
            description = "Attempt to save the OBS replay buffer whenever saving screenshots.",
            position = 0,
            section = whatSection
    )
    default boolean saveOnScreenshot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "saveOnPluginMessage",
            name = "Plugin Messages (advanced)",
            description = "Allow other plugins to send messages to immediately save the OBS replay buffer.",
            position = 99,
            section = whatSection
    )
    default boolean saveOnPluginMessage()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveRewards",
            name = "Rewards",
            description = "Attempt to save the OBS replay buffer for chests, clues, barrows and quest completion.",
            position = 3,
            section = whatSection
    )
    default boolean saveRewards()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveLevels",
            name = "Levels",
            description = "Attempt to save the OBS replay buffer for level ups.",
            position = 4,
            section = whatSection
    )
    default boolean saveLevels()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveKingdom",
            name = "Kingdom rewards",
            description = "Attempt to save the OBS replay buffer for kingdom reward.",
            position = 5,
            section = whatSection
    )
    default boolean saveKingdom()
    {
        return false;
    }

    @ConfigItem(
            keyName = "savePet",
            name = "Pets",
            description = "Attempt to save the OBS replay buffer when receiving pets.",
            position = 6,
            section = whatSection
    )
    default boolean savePet()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveKills",
            name = "PvP kills",
            description = "Attempt to save the OBS replay buffer for PvP kills.",
            position = 8,
            section = whatSection
    )
    default boolean saveKills()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveBossKills",
            name = "Boss kills",
            description = "Attempt to save the OBS replay buffer for boss kills.",
            position = 9,
            section = whatSection
    )
    default boolean saveBossKills()
    {
        return false;
    }

    @ConfigItem(
            keyName = "savePlayerDeath",
            name = "Player Deaths",
            description = "Attempt to save the OBS replay buffer whenever you die.",
            position = 10,
            section = whatSection
    )
    default boolean savePlayerDeath()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveFriendDeath",
            name = "Friend deaths",
            description = "Attempt to save the OBS replay buffer whenever friends or friends chat members die.",
            position = 11,
            section = whatSection
    )
    default boolean saveFriendDeath()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveClanDeath",
            name = "Clan deaths",
            description = "Attempt to save the OBS replay buffer whenever clan members die.",
            position = 12,
            section = whatSection
    )
    default boolean saveClanDeath()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveDuels",
            name = "Duels",
            description = "Attempt to save the OBS replay buffer after duels.",
            position = 13,
            section = whatSection
    )
    default boolean saveDuels()
    {
        return false;
    }

    @ConfigItem(
            keyName = "saveValuableDrop",
            name = "Valuable drops",
            description = "Attempt to save the OBS replay buffer when you receive a valuable drop.<br>"
                    + "Requires 'Loot drop notifications' to be enabled in the RuneScape settings.",
            position = 14,
            section = whatSection
    )
    default boolean saveValuableDrop()
    {
        return false;
    }

    @ConfigItem(
            keyName = "valuableDropThreshold",
            name = "Valuable threshold",
            description = "The minimum value to save screenshots of valuable drops.<br>"
                    + "Requires 'Minimum item value needed for loot notification' to be set to a lesser or equal value in the RuneScape settings.",
            position = 15,
            section = whatSection
    )
    default int valuableDropThreshold()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "saveUntradeableDrop",
            name = "Untradeable drops",
            description = "Attempt to save the OBS replay buffer when you receive an untradeable drop.<br>"
                    + "Requires 'Untradeable loot notifications' to be enabled in the RuneScape settings.",
            position = 16,
            section = whatSection
    )
    default boolean saveUntradeableDrop()
    {
        return false;
    }

    @ConfigSection(
            name = "Delay before capture (s)",
            description = "Delay before saving the replay buffer after the event occurs, to capture live reactions to the event",
            position = 1
    )
    String delaySection = "delay";

    @ConfigItem(
            keyName = "saveAfterDelay",
            name = "All screenshots",
            description = "Delay the attempt to save the OBS replay buffer after screenshots by a number of seconds.",
            position = 0,
            section = delaySection
    )
    default int screenshotDelay() { return 0; }

    @ConfigItem(
            keyName = "rewardsDelay",
            name = "Rewards",
            description = "Delay the attempt to save the OBS replay buffer after any rewards by a number of seconds.",
            position = 1,
            section = delaySection
    )
    default int rewardsDelay() { return 0; }

    @ConfigItem(
            keyName = "levelsDelay",
            name = "Levels",
            description = "Delay the attempt to save the OBS replay buffer after any gained levels.",
            position = 2,
            section = delaySection
    )
    default int levelsDelay() { return 0; }

    @ConfigItem(
            keyName = "kingdomDelay",
            name = "Kingdom",
            description = "Delay the attempt to save the OBS replay buffer after collection Kingdom rewards in Miscellania.",
            position = 3,
            section = delaySection
    )
    default int kingdomDelay() { return 0; }

    @ConfigItem(
            keyName = "petDelay",
            name = "Pets",
            description = "Delay the attempt to save the OBS replay buffer after receiving pets.",
            position = 4,
            section = delaySection
    )
    default int petDelay() { return 0; }

    @ConfigItem(
            keyName = "pvpKillDelay",
            name = "PvP kill",
            description = "Delay the attempt to save the OBS replay buffer after PvP kills.",
            position = 5,
            section = delaySection
    )
    default int pvpKillDelay() { return 0; }

    @ConfigItem(
            keyName = "bossKillDelay",
            name = "Boss kill",
            description = "Delay the attempt to save the OBS replay buffer after boss kills.",
            position = 6,
            section = delaySection
    )
    default int bossKillDelay() { return 0; }

    @ConfigItem(
            keyName = "deathDelay",
            name = "All deaths",
            description = "Delay the attempt to save the OBS replay buffer after any deaths by a number of seconds.",
            position = 10,
            section = delaySection
    )
    default int deathDelay() { return 0; }

    @ConfigItem(
            keyName = "duelsDelay",
            name = "Duels",
            description = "Delay the attempt to save the OBS replay buffer after duels.",
            position = 13,
            section = delaySection
    )
    default int duelsDelay() { return 0; }

    @ConfigItem(
            keyName = "valuableDropDelay",
            name = "Valuable drops",
            description = "Delay the attempt to save the OBS replay buffer after valuable drops over threshold.",
            position = 14,
            section = delaySection
    )
    default int valuableDropDelay() { return 0; }

    @ConfigItem(
            keyName = "untradeableDropDelay",
            name = "Untradeable drops",
            description = "Delay the attempt to save the OBS replay buffer after untradeable drops.",
            position = 14,
            section = delaySection
    )
    default int untradeableDropDelay() { return 0; }

    @ConfigSection(
            name = "OBS WebSocket Options",
            description = "Options for the OBS WebSocket connection",
            position = 99
    )
    String wsSection = "websocket";

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
    default String websocketServerHost() { return "localhost"; }
}
