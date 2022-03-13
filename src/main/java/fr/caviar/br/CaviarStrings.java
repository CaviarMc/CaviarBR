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
			"We are preparing your spawnpoints.",
			PREFIX_GOOD),
	STATE_PREPARING_TITLE(
			"§ePreparing..."),
	STATE_PREPARING_SUBTITLE_1(
			"§7Finding spawnpoints"),
	STATE_PREPARING_SUBTITLE_2(
			"§aStarting game"),
	
	STATE_PLAYING_START(
			"The game has started! Good luck, and be the first one to find the treasure!",
			PREFIX_GOOD),
	
	ITEM_COMPASS_NAME(
			"§eTreasure Compass")
	
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
