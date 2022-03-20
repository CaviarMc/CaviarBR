package fr.caviar.br.game.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameState;
import fr.caviar.br.game.StatePlaying;
import fr.caviar.br.game.StatePreparing;
import fr.caviar.br.game.StateWait;
import fr.caviar.br.game.StateWin;

import net.kyori.adventure.text.Component;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.PlayerArgument;
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
						.executes((CommandExecutor) (sender, args) -> askConfirmation(StatePlaying.class, sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::forceStart)))
				
				.withSubcommand(new CommandAPICommand("finish")
						.withArguments(new PlayerArgument("winner"))
						.executes((CommandExecutor) (sender, args) -> askConfirmation(StateWin.class, sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::finish)))
				.withSubcommand(new CommandAPICommand("finish")
						.executes((CommandExecutor) (sender, args) -> askConfirmation(StateWin.class, sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::finish)))
				
				.withSubcommand(new CommandAPICommand("treasure")
						.withSubcommand(new CommandAPICommand("set")
								.withArguments(new LocationArgument("treasure", LocationType.BLOCK_POSITION))
								.executes((CommandExecutor) this::setTreasure))
						.withSubcommand(new CommandAPICommand("teleport")
								.executesPlayer(this::teleportTreasure))
						.withSubcommand(new CommandAPICommand("giveCompass")
								.executes(this::giveCompass))
						)
				
				.withSubcommand(new CommandAPICommand("shutdown")
						.executes((CommandExecutor) (sender, args) -> CaviarStrings.COMMAND_GAMEADMIN_SHUTDOWN_CONFIRM.send(sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::shutdown))
						)
				
				;
		
		game.getPlugin().getCommands().registerCommand(command);
	}
	
	private CommandAPICommand stateCommand(String name, Class<? extends GameState> targetState, CommandExecutor executor) {
		return new CommandAPICommand(name)
				.executes((CommandExecutor) (sender, args) -> askConfirmation(targetState, sender))
				.withSubcommand(new CommandAPICommand("confirm")
						.executes(executor));
	}

	private void askConfirmation(Class<? extends GameState> targetState, CommandSender sender) {
		CaviarStrings.COMMAND_GAMEADMIN_CONFIRM.send(
				sender,
				game.getState().getClass().getSimpleName(),
				targetState.getClass().getSimpleName());
	}
	
	private <T extends GameState> T testGameState(Class<T> targetState, CommandSender sender) {
		if (targetState.isInstance(game.getState())) return targetState.cast(game.getState());
		CaviarStrings.COMMAND_GAMEADMIN_NOTSTATE.send(
				sender,
				game.getState().getClass().getSimpleName(),
				targetState.getSimpleName());
		return null;
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
	
	private void finish(CommandSender sender, Object[] args) {
		Player winnerPlayer = args.length == 1 ? (Player) args[0] : null;
		game.setState(new StateWin(game, winnerPlayer == null ? null : game.getPlayers().get(winnerPlayer.getUniqueId())));
		CaviarStrings.COMMAND_GAMEADMIN_FINISHED.send(sender, winnerPlayer == null ? "x" : winnerPlayer.getName());
	}
	
	private void setTreasure(CommandSender sender, Object[] args) {
		setTreasure(sender, (Location) args[0]);
	}
	
	private void setTreasure(CommandSender sender, Location location) {
		if (location == null) {
			CaviarStrings.PREFIX_ERROR.sendWith(sender, Component.text("no location"));
			return;
		}
		
		StatePlaying state = testGameState(StatePlaying.class, sender);
		if (state == null) return;
		
		state.setTreasure(location);
		CaviarStrings.COMMAND_GAMEADMIN_TREASURE_EDITED.send(sender);
	}
	
	private void teleportTreasure(Player player, Object[] args) {
		StatePlaying state = testGameState(StatePlaying.class, player);
		if (state == null) return;
		
		player.teleport(state.getTreasure());
		CaviarStrings.COMMAND_GAMEADMIN_TREASURE_TELEPORTED.send(player);
	}
	
	private void giveCompass(CommandSender sender, Object[] args) {
		StatePlaying state = testGameState(StatePlaying.class, sender);
		if (state == null) return;
		
		Player target = (Player) args[0];
		target.getInventory().addItem(state.getCompass());
		CaviarStrings.COMMAND_GAMEADMIN_COMPASS_GIVEN.send(sender, target.getName());
	}
	
	private void shutdown(CommandSender sender, Object[] args) {
		game.shutdown();
		CaviarStrings.COMMAND_GAMEADMIN_SHUTDOWN_DONE.send(sender);
	}
	
}
