package fr.caviar.br.game.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameState;
import fr.caviar.br.game.StatePlaying;
import fr.caviar.br.game.StatePreparing;
import fr.caviar.br.game.StateWait;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.executors.CommandExecutor;

public class GameAdminCommand {
	
	private GameManager game;
	private CommandAPICommand command;
	
	public GameAdminCommand(GameManager game) {
		this.game = game;
		
		command = new CommandAPICommand("gameadmin")
				.withAliases("game")
				.withPermission("caviarbr.command.gameadmin")
				.withSubcommand(stateCommand("reset", StateWait.class, this::reset))
				.withSubcommand(stateCommand("start", StatePreparing.class, this::start))
				.withSubcommand(new CommandAPICommand("forceStart")
						.withArguments(new LocationArgument("treasure", LocationType.BLOCK_POSITION))
						.executes((CommandExecutor) (sender, args) -> askConfimration(StatePlaying.class, sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::forceStart)))
				;
		
		game.getPlugin().getCommands().registerCommand(command);
	}
	
	private CommandAPICommand stateCommand(String name, Class<? extends GameState> targetState, CommandExecutor executor) {
		return new CommandAPICommand(name)
				.executes((CommandExecutor) (sender, args) -> askConfimration(targetState, sender))
				.withSubcommand(new CommandAPICommand("confirm")
						.executes(executor));
	}

	private void askConfimration(Class<? extends GameState> targetState, CommandSender sender) {
		CaviarStrings.COMMAND_GAMEADMIN_CONFIRM.send(
				sender,
				game.getState().getClass().getSimpleName(),
				targetState.getClass().getSimpleName());
	}
	
	private void reset(CommandSender sender, Object[] args) {
		game.setState(new StateWait(game));
		CaviarStrings.COMMAND_GAMEADMIN_RESET.broadcast();
	}
	
	private void start(CommandSender sender, Object[] args) {
		game.setState(new StatePreparing(game));
		CaviarStrings.COMMAND_GAMEADMIN_STARTED.send(sender);
	}
	
	private void forceStart(CommandSender sender, Object[] args) {
		game.setState(new StatePlaying(game, (Location) args[0]));
		CaviarStrings.COMMAND_GAMEADMIN_FORCESTARTED.send(sender);
	}
	
}
