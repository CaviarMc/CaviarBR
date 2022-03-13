package fr.caviar.br.game.commands;

import org.bukkit.command.CommandSender;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class SettingsCommand {
	
	private GameSettings settings;
	
	public SettingsCommand(GameManager manager) {
		settings = manager.getSettings();
		
		new CommandAPICommand("settings")
			.withPermission("caviarbr.command.settings")
			.withSubcommand(new CommandAPICommand("minPlayers")
					.withArguments(new IntegerArgument("min", 1))
					.executes(this::minPlayers))
			.withSubcommand(new CommandAPICommand("maxPlayers")
					.withArguments(new IntegerArgument("max", 1))
					.executes(this::maxPlayers))
			.register();
	}
	
	public void minPlayers(CommandSender sender, Object[] args) {
		int min = (int) args[0];
		int oldMin = settings.getMinPlayers().get();
		if (oldMin != min) {
			settings.getMinPlayers().set(min);
			CaviarStrings.PREFIX_GOOD.sendWith(sender, "Setting minPlayers has changed from " + oldMin + " to " + min);
		}else {
			CaviarStrings.PREFIX_BAD.sendWith(sender, "Setting minPlayers was already set to " + min);
		}
	}
	
	public void maxPlayers(CommandSender sender, Object[] args) {
		int max = (int) args[0];
		int oldMax = settings.getMaxPlayers().get();
		if (oldMax != max) {
			settings.getMaxPlayers().set(max);
			CaviarStrings.PREFIX_GOOD.sendWith(sender, "Setting maxPlayers has changed from " + oldMax + " to " + max);
		}else {
			CaviarStrings.PREFIX_BAD.sendWith(sender, "Setting maxPlayers was already set to " + max);
		}
	}
	
}
