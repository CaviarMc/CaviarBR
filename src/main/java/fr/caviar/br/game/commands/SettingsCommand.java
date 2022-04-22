package fr.caviar.br.game.commands;

import java.util.Objects;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;
import fr.caviar.br.game.GameSettings.GameSetting;
import fr.caviar.br.permission.Perm;
import dev.jorel.commandapi.CommandAPICommand;

public class SettingsCommand {
	
	private GameSettings settings;
	
	private CommandAPICommand command;
	
	public SettingsCommand(GameManager game) {
		settings = game.getSettings();
		
		command = new CommandAPICommand("settings")
			.withPermission(Perm.MODERATOR_COMMAND_SETTINGS.get());
		CommandAPICommand cmd;
		for (GameSetting<?> setting : settings.getSettings()) {
			cmd = settingCommand(setting);
			cmd.setRequirements(s -> true);
			command.withSubcommand(cmd);
		}
		
		game.getPlugin().getCommands().registerCommand(command);
	}
	
	private <T> CommandAPICommand settingCommand(GameSetting<T> setting) {
		return new CommandAPICommand(setting.getKey())
				.withArguments(setting.getArguments())
				.executes((sender, args) -> {
					T oldValue = setting.get();
					if (args == null || args.length == 0) {
						CaviarStrings.COMMAND_SETTING_SHOW.send(sender, setting.getKey(), Objects.toString(oldValue));
						return;
					}
					T newValue = setting.getValueFromArguments(args);
					if (oldValue != newValue) {
						setting.set(newValue);
						CaviarStrings.COMMAND_SETTING_SET.send(sender, setting.getKey(), Objects.toString(oldValue), Objects.toString(newValue));
					} else {
						CaviarStrings.COMMAND_SETTING_SAME.send(sender, setting.getKey(), Objects.toString(oldValue));
					}
				});
	}
	
}
