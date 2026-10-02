package com.hoverlag;

import java.util.ArrayList;
import java.util.List;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

public class HoverLagTest
{
	public static void main(String[] args) throws Exception
	{
		List<Class<? extends Plugin>> plugins = new ArrayList<>();
		plugins.add(HoverLagPlugin.class);
		// Comma-separated plugin classes compiled in alongside, e.g. by another project's dev launch.
		for (String name : System.getProperty("extraPlugins", "").split(","))
		{
			if (!name.isBlank())
			{
				plugins.add(Class.forName(name.trim()).asSubclass(Plugin.class));
			}
		}
		// loadBuiltin replaces its list on every call, so everything goes in one call.
		ExternalPluginManager.loadBuiltin(plugins.toArray(new Class[0]));
		RuneLite.main(args);
	}
}
