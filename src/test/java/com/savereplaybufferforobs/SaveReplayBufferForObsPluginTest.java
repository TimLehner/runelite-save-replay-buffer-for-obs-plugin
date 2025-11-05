package com.savereplaybufferforobs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SaveReplayBufferForObsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SaveReplayBufferForObsPlugin.class);
		RuneLite.main(args);
	}
}