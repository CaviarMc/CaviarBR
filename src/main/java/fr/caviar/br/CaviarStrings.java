package fr.caviar.br;

import java.util.Collection;
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

	FORMAT_BARS(
			"""
				
				<dark_purple>==========-====-==========-====</dark_purple>
				   {0}
				<dark_purple>==========-====-==========-====</dark_purple>
				"""),

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
	STATE_PREPARING_TELEPORT(
			"Teleportation in progress for all players...",
			PREFIX_GOOD),
	STATE_PREPARING_TITLE(
			"<yellow>Preparing...</yellow>"),
	STATE_PREPARING_TITLE_STARTING_IN(
			"<green>STARTING IN</green>"),
	STATE_PREPARING_SUBTITLE_TREASURE(
			"<gray>Preparing treasure</gray>"),
	STATE_PREPARING_SUBTITLE_SPAWNPOINTS(
			"<aqua>Finding spawnpoints</aqua>"),
	STATE_PREPARING_SUBTITLE_STARTING(
			"<green>Starting game</green>"),

	STATE_PLAYING_START(
			"The game has started! Good luck, and be the first one to find the treasure!",
			PREFIX_GOOD),
	STATE_PLAYING_TREASURE_SPAWN(
			"<yellow><bold>A treasure has spawn on the map. Good luck to find it !</bold></yellow>",
			PREFIX_GOOD),
	STATE_PLAYING_COMPASS(
			"<bold>Here is a compass that points to the treasure. It will work for {0} secondes.</bold>",
			PREFIX_GOOD),
	STATE_PLAYING_COMPASS_GROUND(
			"Compass is on ground, your inventory is full.",
			PREFIX_BAD),
	STATE_PLAYING_COMPASS_STOP(
			"The bousole no longer points to the treasure. It will be back in {0} minutes.",
			PREFIX_BAD),
	STATE_PLAYING_BREAK_TREASURE(
			"<gold>You cannot break the treasure... click on it!</gold>",
			PREFIX_BAD),
	STATE_PLAYING_MORE_PLAYERS(
			"<red><bold>There must be a maximum of 3 players alive to trigger the treasure...</bold></red>",
			PREFIX_BAD),

	STATE_WIN(
			"<light_purple>{0}</light_purple> won the game! Congratulations!"),

	GAME_SHUTDOWN(
			"<bold>The game is now shutting down. Thank you for playing!</bold>",
			PREFIX_NEUTRAL),

	ITEM_COMPASS_NAME(
			"<yellow>Treasure Compass"),

	COMMAND_SETTING_SHOW(
			"Setting {0} is set to <dark_green>{1}</dark_green>.",
			PREFIX),
	COMMAND_SETTING_SET(
			"Setting {0} has changed from <dark_green>{1}</dark_green> to <dark_green>{2}</dark_green>.",
			PREFIX_GOOD),
	COMMAND_SETTING_SAME(
			"Setting {0} already had value <dark_red>{1}</dark_red>.",
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
	COMMAND_GAMEADMIN_ENABLE_GENERATE(
			"You have activated chunk generator for a map of %d %d to %d %d (%d chunks).",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_DISABLE_GENERATE(
			"You have disabled chunk generator.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_ERROR(
			"An error has occurred :",
			PREFIX_BAD),
	COMMAND_GAMEADMIN_FORCESTARTED(
			"You have forced the start of the game.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_FINISHED(
			"You have finished the game with <dark_green>{0}</dark_green> as a winner.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_TREASURE_EDITED(
			"Edited treasure location.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_TREASURE_TELEPORTED(
			"You have been teleported to the treasure.",
			PREFIX_BAD),
	COMMAND_GAMEADMIN_TREASURE_TELEPORTED_NOT_EXIST(
			"The treasure has not yet appeared.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_COMPASS_GIVEN(
			"You have gave a compass to <dark_green>{0}</dark_green>.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_PLAYER_ADD(
			"You add a player in the game : <dark_green>{0}</dark_green>.",
			PREFIX_GOOD),
	COMMAND_GAMEADMIN_SHUTDOWN_CONFIRM(
			"Are you sure you want to shutdown the server? All players will get disconnected, and the world will be reset. Put \"confirm\" at the end of your command.",
			PREFIX_WARNING),
	COMMAND_GAMEADMIN_SHUTDOWN_DONE(
			"Server shutdown initiated.",
			PREFIX_GOOD),

	LOGIN_SCREEN_PREFIX(
			"<bold><blue>Caviar<aqua>BR</bold>\n\n",
			Style.style(NamedTextColor.GRAY)),
	LOGIN_SCREEN_FINISHED(
			"The game has ended! Thanks for playing!",
			LOGIN_SCREEN_PREFIX),
	LOGIN_SCREEN_FINISHED_KICK(
			"The game is finished.",
			LOGIN_SCREEN_PREFIX),
	LOGIN_SCREEN_STARTED_KICK(
			"The game has already started.",
			LOGIN_SCREEN_PREFIX),
	LOGIN_SCREEN_ERROR_KICK(
			"An error has occurred, please try again.",
			LOGIN_SCREEN_PREFIX),
	LOGIN_SCREEN_KICK_SPEC(
			"The game no longer accepts spectators.\nIf you think this is a mistake, try logging in again.",
			LOGIN_SCREEN_PREFIX),
	VANISH_ON(
			"<green>Vanish ON",
			PREFIX_GOOD),
	VANISH_ON_OTHER(
			"<green>%s now has the Vanish mode turned on",
			PREFIX_GOOD),
	VANISH_OFF(
			"<red>Vanish OFF",
			PREFIX_BAD),
	VANISH_OFF_OTHER(
			"<red>%s now has the Vanish mode turned off",
			PREFIX_BAD),
	COMMAND_NO_CONSOLE(
			"<red>Can't execute this command with the console",
			PREFIX_BAD),
	ENTER_SPECTATOR_MODE(
			"<gray>You are now in specator mode. Use the wheel-click to change the player",
			PREFIX_GOOD),
	NOT_ENOUGH_SPAWNPOINTS(
			"They are not enough spawn points for %d players. They are only %d. Maybe some chunks are not load, or settings of game is impossible. Try <bold>/game start</bold> to retry it.",
			PREFIX_BAD),
	CANT_DO_THIS(
			"You can't do this.",
			PREFIX_BAD),

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

	public void send(Collection<? extends CommandSender> senders, Object... args) {
		Component msg = format(args);
		senders.forEach(sender -> sender.sendMessage(msg));
	}

	public void send(CommandSender sender, Object... args) {
		sender.sendMessage(format(args));
	}

	public void sendWith(CommandSender sender, Component next, Object... args) {
		sender.sendMessage(format(args).append(next));
	}

}
