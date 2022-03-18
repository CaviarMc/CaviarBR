package fr.caviar.br;

import java.util.Objects;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum CaviarStrings {
	
	PREFIX(
			"<bold><blue>Caviar<aqua>BR</bold> <gray>>> "),
	PREFIX_WARNING(
			"<bold><yellow>Caviar<gold>BR</bold> ⚠ ", Style.style(NamedTextColor.GRAY)),
	PREFIX_ERROR(
			"<bold><dark_red>Caviar<red>BR</bold> <dark_red>✖ ", Style.style(NamedTextColor.RED)),
	PREFIX_NEUTRAL(
			"",
			Style.style(NamedTextColor.GRAY),
			PREFIX),
	PREFIX_GOOD(
			"",
			Style.style(NamedTextColor.GREEN),
			PREFIX),
	PREFIX_BAD(
			"",
			Style.style(NamedTextColor.RED),
			PREFIX),
	
	STATE_WAIT_COUNTDOWN(
			"Game is starting in <green>{0}</green> seconds.",
			PREFIX_NEUTRAL),
	STATE_WAIT_COUNTDOWN_START(
			"There is enough players to start the game!",
			PREFIX_GOOD),
	STATE_WAIT_CANCEL(
			"There is not enough players to start the game.",
			PREFIX_BAD),
	STATE_PREPARING_PREPARE(
			"We are preparing your game. This will only take a few seconds.",
			PREFIX_GOOD),
	STATE_PREPARING_TITLE(
			"<yellow>Preparing..."),
	STATE_PREPARING_SUBTITLE_TREASURE(
			"<gray>Preparing treasure"),
	STATE_PREPARING_SUBTITLE_SPAWNPOINTS(
			"<aqua>Finding spawnpoints"),
	STATE_PREPARING_SUBTITLE_STARTING(
			"<green>Starting game"),
	
	STATE_PLAYING_START(
			"The game has started! Good luck, and be the first one to find the treasure!",
			PREFIX_GOOD),
	STATE_PLAYING_BREAK_TREASURE(
			"You cannot break the treasure... click on it!",
			PREFIX_BAD),
	
	STATE_WIN(
			"<light_purple>{0}</light_purple> won the game! Congratulations!"),
	
	ITEM_COMPASS_NAME(
			"<yello>Treasure Compass"),
	
	COMMAND_SETTING_SET(
			"Setting {0} has changed from <dark_green>{1}</dark_green> to <dark_green>{2}</dark_green>.",
			PREFIX_GOOD),
	COMMAND_SETTING_SAME(
			"Setting {0} already had value <dark_red>%s</dark_red>.",
			PREFIX_BAD),
	
	COMMAND_GAMEADMIN_CONFIRM(
			"Are you sure you want to change the game state from <gold>{0}</gold> to <gold>{1}</gold>? Put \"confirm\" at the end of your command.",
			PREFIX_WARNING),
	COMMAND_GAMEADMIN_NOTSTATE(
			"The game is currently in the state <dark_red>{0}</dark_red> while it has to be in <dark_red>{0}</dark_red> to do that.",
			PREFIX_BAD),
	COMMAND_GAMEADMIN_RESET(
			"The game has been reset.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_STARTED(
			"You have launched the preparation state to start the game.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_FORCESTARTED(
			"You have forced the start of the game.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_TREASURE_EDITED(
			"Edited treasure location.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_TREASURE_TELEPORTED(
			"You have been teleported to the treasure.",
			PREFIX_GOOD),
	
	;
	
	private static final Pattern ARGUMENT_MATCH = Pattern.compile("\\{(\\d+)\\}");
	
	private String value;
	private Style style;
	private CaviarStrings[] prefixes;
	
	private Component component;
	private Boolean hasFormatting;
	
	private CaviarStrings(String value, CaviarStrings... prefixes) {
		this(value, null, prefixes);
	}
	
	private CaviarStrings(String value, Style style, CaviarStrings... prefixes) {
		this.value = value;
		this.style = style;
		this.prefixes = prefixes;
	}
	
	public String getValue() {
		return value;
	}
	
	public Component toComponent() {
		if (component == null) {
			component = createComponent();
		}
		return component;
	}
	
	protected Component createComponent() {
		Component compo = MiniMessage.miniMessage().deserialize(value);
		for (CaviarStrings prefix : prefixes) {
			compo = prefix.toComponent().append(compo);
		}
		if (style != null) compo = compo.style(style);
		return compo;
	}
	
	public boolean hasFormatting() {
		if (hasFormatting != null) return hasFormatting.booleanValue();
		
		if (ARGUMENT_MATCH.matcher(value).find()) {
			hasFormatting = true;
			return true;
		}
		for (CaviarStrings prefix : prefixes) {
			if (prefix.hasFormatting()) {
				hasFormatting = true;
				return true;
			}
		}
		hasFormatting = false;
		return false;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	public Component format(Object... args) {
		Component runningComponent = toComponent();
		if (hasFormatting()) {
			runningComponent = runningComponent.replaceText(builder -> builder
					.match(ARGUMENT_MATCH)
					.replacement((match, builder2) -> {
						int argNumber = Integer.parseInt(match.group(1));
						if (argNumber < args.length) {
							Object arg = args[argNumber];
							if (arg instanceof Component argCompo) return argCompo;
							return Component.text(Objects.toString(arg));
						}
						return Component.text("{" + match.group(1) + "}");
					}));
		}
		return runningComponent;
	}
	
	public void broadcast(Object... args) {
		Bukkit.broadcast(format(args));
	}
	
	public void send(CommandSender sender, Object... args) {
		sender.sendMessage(format(args));
	}
	
	public void sendWith(CommandSender sender, Component next, Object... args) {
		sender.sendMessage(format(args).append(next));
	}
	
}
