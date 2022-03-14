package fr.caviar.br;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public enum CaviarStrings {
	
	PREFIX(
			"§9§lCaviar§b§lBR §7>> "),
	PREFIX_GOOD(
			"§a",
			PREFIX),
	PREFIX_BAD(
			"§c",
			PREFIX),
	
	STATE_WAIT_COUNTDOWN(
			"Game is launching in §a%d§7 seconds.",
			PREFIX),
	STATE_WAIT_COUNTDOWN_START(
			"There is enough players to start the game!",
			PREFIX_GOOD),
	STATE_WAIT_CANCEL(
			"Not enough players to start the game.",
			PREFIX_BAD),
	STATE_PREPARING_PREPARE(
			"We are preparing your game. This will only take a few seconds.",
			PREFIX_GOOD),
	STATE_PREPARING_TITLE(
			"§ePreparing..."),
	STATE_PREPARING_SUBTITLE_TREASURE(
			"§7Preparing treasure"),
	STATE_PREPARING_SUBTITLE_SPAWNPOINTS(
			"§bFinding spawnpoints"),
	STATE_PREPARING_SUBTITLE_STARTING(
			"§aStarting game"),
	
	STATE_PLAYING_START(
			"The game has started! Good luck, and be the first one to find the treasure!",
			PREFIX_GOOD),
	STATE_PLAYING_BREAK_TREASURE(
			"You cannot break the treasure... click on it!",
			PREFIX_BAD),
	
	STATE_WIN(
			"§d%s§7 won the game! Congratulations!"),
	
	ITEM_COMPASS_NAME(
			"§eTreasure Compass"),
	
	COMMAND_SETTING_SET(
			"Setting %s has changed from §2%s§r to §2%s§r.",
			PREFIX_GOOD),
	COMMAND_SETTING_SAME(
			"Setting %s already had value §4%s§r.",
			PREFIX_BAD),
	
	;
	
	private String value;
	private CaviarStrings[] prefixes;
	
	private String toString;
	
	private CaviarStrings(String value, CaviarStrings... prefixes) {
		this.value = value;
		this.prefixes = prefixes;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		if (toString == null) {
			StringBuilder builder = new StringBuilder(value.length());
			for (CaviarStrings prefix : prefixes) builder.append(prefix.toString());
			builder.append(value);
			toString = builder.toString();
		}
		return toString;
	}
	
	public String format(Object... args) {
		return toString().formatted(args);
	}
	
	public void broadcast(Object... args) {
		Bukkit.broadcastMessage(format(args));
	}
	
	public void send(CommandSender sender, Object... args) {
		sender.sendMessage(format(args));
	}
	
	public void sendWith(CommandSender sender, String next, Object... args) {
		sender.sendMessage(format(args) + next);
	}
	
}
