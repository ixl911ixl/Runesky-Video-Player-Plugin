package com.runesky;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RuneskyVideoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneskyVideoPlugin.class);
		RuneLite.main(args);
	}
}