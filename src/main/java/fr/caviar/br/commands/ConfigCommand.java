package fr.caviar.br.commands;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.function.BiConsumer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandExecutor;
import fr.caviar.br.CaviarBR;
import fr.caviar.br.api.config.ConfigSpigot;
import fr.caviar.br.utils.Prefix;

public class ConfigCommand {
	
	private CommandAPICommand command;
	
	public ConfigCommand(CaviarBR plugin) {
		command = new CommandAPICommand("config").withPermission("caviarbr.command.config");
		CommandAPICommand reloadCommand = new CommandAPICommand("reload");
		CommandAPICommand saveCommand = new CommandAPICommand("save");
		ConfigSpigot.getConfigs().forEach(config -> {
			reloadCommand.withSubcommand(createConfigCmd(config, this::configReload));
			saveCommand.withSubcommand(createConfigCmd(config, this::configSave));
		});
		command.withSubcommand(reloadCommand);
		command.withSubcommand(saveCommand);
		plugin.getCommands().registerCommand(command);
	}
	
	private CommandAPICommand createConfigCmd(ConfigSpigot config, BiConsumer<ConfigSpigot, CommandSender> f) {
		CommandAPICommand configCmd = new CommandAPICommand(config.getFileName()).executes((CommandExecutor) (sender, args) -> {
			f.accept(config, sender);
		});
		
		return configCmd;
	}
	
	private void configReload(ConfigSpigot config, CommandSender sender) {
		long time = System.nanoTime();
		try {
			config.reload();
			time = System.nanoTime() - time;
			Prefix.DEFAULT_GOOD.sendMessage(sender, "Config &2%s&a load in &2%s ms", config.getName(), new DecimalFormat("0.#").format(time / 10000000d));
		} catch (IOException | InvalidConfigurationException e) {
			Prefix.ERROR.sendMessage(sender, "Unable to load config &4%s&c : &4%s&c.", config.getName(), e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void configSave(ConfigSpigot config, CommandSender sender) {
		long time = System.nanoTime();
		try {
			config.saveUnSafe();
			time = System.nanoTime() - time;
			Prefix.DEFAULT_GOOD.sendMessage(sender, "Config &2%s&a saved en &2%s ms", config.getName(), new DecimalFormat("0.#").format(time / 10000000d));
		} catch (IOException e) {
			Prefix.ERROR.sendMessage(sender, "Unable to save config &4%s&c on disk : &4%s&c.", config.getName(), e.getMessage());
			e.printStackTrace();
		}
	}
}
