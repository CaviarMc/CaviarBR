package fr.caviar.br;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;

public class CaviarCommands {

	private List<CommandAPICommand> commands = new ArrayList<>();
	private Plugin plugin;

	public CaviarCommands(Plugin plugin) {
		this.plugin = plugin;
		CommandAPI.onLoad(new CommandAPIConfig()
				.missingExecutorImplementationMessage("This command cannot be ran by %S."));
	}

	public void registerCommand(CommandAPICommand command) {
		command.register();
		commands.add(command);
	}

	public void enable() {
		CommandAPI.onEnable(plugin);
	}

	public void disable() {
		commands.stream().map(CommandAPICommand::getName).forEach(CommandAPI::unregister);
		commands.clear();
	}

}
