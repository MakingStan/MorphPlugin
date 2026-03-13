package com.makingstan;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
		name = "Morph Plugin"
)
public class MorphPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MorphPluginConfig config;

	private int[] animIds;
	private String[] textReplacements;
	private final Set<Integer> loadedGroups = new HashSet<>();


	private boolean questListOpen = false;
	private boolean skillGuideOpen = false;
	private boolean groupOpen = false;


	@Override
	protected void startUp() throws Exception
	{
		String rawAnimIds = config.animIds().trim();
		if (!rawAnimIds.isEmpty())
		{
			String[] parts = rawAnimIds.split(",");
			animIds = new int[parts.length];
			for (int i = 0; i < parts.length; i++)
			{
				animIds[i] = Integer.parseInt(parts[i].trim());
			}
		}
		else
		{
			animIds = new int[0];
		}

		String rawText = config.objectTextOverride().trim();
		if (!rawText.isEmpty())
		{
			String[] parts = rawText.split(";");
			textReplacements = new String[parts.length];
			for (int i = 0; i < parts.length; i++)
			{
				textReplacements[i] = parts[i].trim();
			}
		}
		else
		{
			textReplacements = new String[0];
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (config.showAnimId())
		{
			client.getLocalPlayer().setOverheadText("");
		}
		loadedGroups.clear();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (animIds == null || animIds.length < 2)
		{
			return;
		}

		Actor actor = event.getActor();
		if (actor != client.getLocalPlayer())
		{
			return;
		}

		int currentAnim = actor.getAnimation();

		for (int i = 0; i + 1 < animIds.length; i += 2)
		{
			if (currentAnim == animIds[i])
			{
				actor.setAnimation(animIds[i + 1]);
				actor.setAnimationFrame(0);
				break;
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged changed)
	{
		if (changed.getKey().equals("showAnimId") && changed.getGroup().equals("objectoverride") && changed.getNewValue().equals("false"))
		{
			client.getLocalPlayer().setOverheadText("");
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (config.showAnimId())
		{
			client.getLocalPlayer().setOverheadText("<col=00ffff>Animation ID: " + client.getLocalPlayer().getAnimation() + "</col>");
		}




	}

	@Subscribe
	public void onBeforeRender(BeforeRender beforeRender)
	{

			if(questListOpen) {
				Widget questWidget = client.getWidget(InterfaceID.Questlist.LIST);
				if (questWidget == null) return;
				for (Widget child : questWidget.getChildren()) {
					if (child == null) break;
					traverseWidget(child);
				}
				questListOpen = false;
			}

			if(skillGuideOpen) {
				Widget skillGuideWidget = client.getWidget(InterfaceID.SkillGuide.WINDOW);
				Widget skillGuideCategories = client.getWidget(InterfaceID.SkillGuide.CATEGORIES);

				if (skillGuideWidget == null) return;
				if( skillGuideCategories == null) return;
				for (Widget child : skillGuideWidget.getStaticChildren()) {
					if (child == null) break;
					traverseWidgetRecursive(child);
				}

				for (Widget child : skillGuideCategories.getStaticChildren()) {
					if (child == null) break;
					traverseWidgetRecursive(child);
				}
				skillGuideOpen = false;
			}
			if(groupOpen) {
				Widget groupWidget = client.getWidget(InterfaceID.Grouping.DROPDOWN_CONTENTS);
				Widget currentGameWidget = client.getWidget(InterfaceID.Grouping.CURRENTGAME);
				currentGameWidget.setOnClickListener();

				if (currentGameWidget != null) {
					traverseWidget(currentGameWidget);
				}
				if (groupWidget != null) {
					try {
						for (Widget child : groupWidget.getChildren()) {
							if (child == null) break;
							traverseWidget(child);
						}
					} catch (NullPointerException e) {
						log.debug("No children of group widget");
					}
				}
			}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked clicked)
	{
		String option = clicked.getMenuOption().toLowerCase();
		switch (option) {
			case "view": skillGuideOpen = true; break;
			case "quest list":  questListOpen = true; break;
			case "select":
			case "grouping":
				groupOpen = true; break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (textReplacements == null || textReplacements.length < 2)
		{
			return;
		}

		MenuEntry entry = event.getMenuEntry();
		entry.setTarget(applyReplacements(entry.getTarget()));
		entry.setOption(applyReplacements(entry.getOption()));
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded loaded) {
		int groupId = loaded.getGroupId();

		switch (groupId) {
			case InterfaceID.QUESTLIST: questListOpen = true; break;
			case InterfaceID.SKILL_GUIDE: skillGuideOpen = true; break;
			case InterfaceID.GROUPING: groupOpen = true; break;
			default:
				questListOpen = false;
				skillGuideOpen = false;
				groupOpen = false;
				break;
		}

	}



	private void traverseWidgetRecursive(Widget widget) {
		traverseWidget(widget);
		Widget[] children = widget.getChildren();
		if (children == null) return;
		for (Widget child : children) {
			if (child == null) continue;

			traverseWidgetRecursive(child);
		}
	}
	private void traverseWidget(Widget widget)
	{
		String text = widget.getText();
		String name = widget.getName();

		if (text != null && !text.isEmpty())
		{
			widget.setText(applyReplacements(text));
			widget.revalidate();
		}
		if (name != null && !name.isEmpty())
		{
			widget.setName(applyReplacements(name));
			widget.revalidate();
		}
	}

	private String applyReplacements(String text)
	{
		for (int i = 0; i + 1 < textReplacements.length; i += 2)
		{
			String find = textReplacements[i];
			String replace = textReplacements[i + 1];
			String stripped = text.replaceAll("<[^>]*>", "");
			String pattern = "(?i)(?<=[\\-() ]|^)" + Pattern.quote(find) + "(?=[\\-() .\"',!?;:]|$)";
			if (stripped.matches(".*" + pattern + ".*"))
			{
				text = text.replaceAll("(?i)((?:<[^>]*>)*)" + Pattern.quote(find) + "((?:<[^>]*>)*)", "$1" + replace + "$2");
			}
		}
		return text;
	}

	@Provides
	MorphPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MorphPluginConfig.class);
	}
}
