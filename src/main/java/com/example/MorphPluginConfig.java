package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("morphplugin")
public interface MorphPluginConfig extends Config
{
	@ConfigItem(
			keyName = "showAnimId",
			name = "Show Animation ID",
			description = "Toggle showing current animation ID"
	)
	default boolean showAnimId()
	{
		return false;
	}


	@ConfigItem(
		keyName = "animIds",
		name = "Animation IDS",
		description = "A list of comma seperated animation IDs that you'd like to be replaced. (ex. 1, 2, 3, 4 = replace animation id 1 with 2, and 3 with 4)"
	)
	default String animIds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "objectTexts",
			name = "Menu Text Replace",
			description = "A list of ; seperated menu texts that you'd like to be replaced. (ex. god; king; man; cool guy; = replace any menu text of 'god' with 'king' and any text of 'cool' with 'cool guy')"
	)
	default String objectTextOverride()
	{
		return "";
	}
}
