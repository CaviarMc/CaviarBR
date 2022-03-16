package fr.caviar.br.game.commands;

import java.util.Objects;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;
import fr.caviar.br.game.GameSettings.GameSetting;

import dev.jorel.commandapi.CommandAPICommand;

public class SettingsCommand {
	
	private GameSettings settings;
	
	private CommandAPICommand command;
	
	public SettingsCommand(GameManager game) {
		settings = game.getSettings();
		
		command = new CommandAPICommand("settings")
			.withPermission("caviarbr.command.settings");
		
		for (GameSetting<?> setting : settings.getSettings()) {
			command.withSubcommand(settingCommand(setting));
		}
		
		game.getPlugin().getCommands().registerCommand(command);
	}
	
	private <T> CommandAPICommand settingCommand(GameSetting<T> setting) {
		return new CommandAPICommand(setting.getKey())
				.withArguments(setting.getArguments())
				.executes((sender, args) -> {
					T newValue = setting.getValueFromArguments(args);
					T oldValue = setting.get();
					if (oldValue != newValue) {
						setting.set(newValue);
						CaviarStrings.COMMAND_SETTING_SET.send(sender, setting.getKey(), Objects.toString(oldValue), Objects.toString(newValue));
					}else {
						CaviarStrings.COMMAND_SETTING_SAME.send(sender, setting.getKey(), Objects.toString(oldValue));
					}
				});
	}
	
}
