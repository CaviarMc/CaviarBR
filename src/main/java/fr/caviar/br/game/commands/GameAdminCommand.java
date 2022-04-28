package fr.caviar.br.game.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GamePlayer;
import fr.caviar.br.game.GameState;
import fr.caviar.br.game.StatePlaying;
import fr.caviar.br.game.StatePreparing;
import fr.caviar.br.game.StateWait;
import fr.caviar.br.game.StateWin;
import fr.caviar.br.generate.WorldLoader;
import fr.caviar.br.permission.Perm;
import fr.caviar.br.player.CaviarPlayerSpigot;
import io.reactivex.functions.Consumer;
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
				.withPermission(Perm.MODERATOR_COMMAND_GAMEADMIN.get())
			
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
								.executes(this::giveCompass)))
			
				.withSubcommand(new CommandAPICommand("generate")
						.withSubcommand(new CommandAPICommand("stop")
								.executes(this::disableGenerate))
						.withSubcommand(new CommandAPICommand("start")
								.executes(this::startGenerate)))
			
				.withSubcommand(new CommandAPICommand("shutdown")
						.executes((CommandExecutor) (sender, args) -> CaviarStrings.COMMAND_GAMEADMIN_SHUTDOWN_CONFIRM.send(sender))
						.withSubcommand(new CommandAPICommand("confirm")
								.executes(this::shutdown)))

				.withSubcommand(new CommandAPICommand("addPlayer").executes(this::spectatorToPlayer))
			
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
				targetState.getSimpleName());
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
		game.getState().end();
		game.setState(new StateWait(game));
		CaviarStrings.COMMAND_GAMEADMIN_RESET.broadcast();
	}

	private void start(CommandSender sender, Object[] args) {
		game.setState(new StatePreparing(game));
		CaviarStrings.COMMAND_GAMEADMIN_STARTED.send(sender);
	}

	private void disableGenerate(CommandSender sender, Object[] args) {
		game.getWorldLoader().stop(false);
		CaviarStrings.COMMAND_GAMEADMIN_DISABLE_GENERATE.send(sender);
	}

	private void startGenerate(CommandSender sender, Object[] args) {
		WorldLoader worldLoader = game.getWorldLoader();
		worldLoader.start(true);
		CaviarStrings.COMMAND_GAMEADMIN_ENABLE_GENERATE.send(sender, worldLoader.getRealMapMinX(), worldLoader.getRealMapMinZ(),
				worldLoader.getRealMapMaxX(), worldLoader.getRealMapMaxZ(), worldLoader.getTotalChunks());
	}

	private void forceStart(CommandSender s, Object[] args) {
		tryCommand(s, sender -> {
			game.setTreasure((Location) args[0]);
			game.setState(new StatePlaying(game));
			CaviarStrings.COMMAND_GAMEADMIN_FORCESTARTED.send(sender);
		});
	}

	private void finish(CommandSender sender, Object[] args) {
		Player winnerPlayer = args.length == 1 ? (Player) args[0] : null;
		game.setState(new StateWin(game, winnerPlayer == null ? null : game.getPlayers().get(winnerPlayer.getUniqueId())));
		CaviarStrings.COMMAND_GAMEADMIN_FINISHED.send(sender, winnerPlayer == null ? "x" : winnerPlayer.getName());
	}

	private void setTreasure(CommandSender sender, Object[] args) {
		game.getPlugin().getTaskManager().removeTaskByName("treasure");
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
	
		if (game.getTreasure() == null) {
			CaviarStrings.COMMAND_GAMEADMIN_TREASURE_TELEPORTED_NOT_EXIST.send(player);
			return;
		}
		player.teleport(game.getTreasure());
		CaviarStrings.COMMAND_GAMEADMIN_TREASURE_TELEPORTED.send(player);
	}

	private void giveCompass(CommandSender sender, Object[] args) {
		StatePlaying state = testGameState(StatePlaying.class, sender);
		if (state == null) return;
	
		Player target = (Player) args[0];
		// state.giveCompass(); // Is this the correct functionality of this cmd?
		target.getInventory().addItem(state.getCompass());
		CaviarStrings.COMMAND_GAMEADMIN_COMPASS_GIVEN.send(sender, target.getName());
		CaviarStrings.STATE_PLAYING_COMPASS.send(target);
	}

	private void spectatorToPlayer(CommandSender sender, Object[] args) {
		StatePlaying state = testGameState(StatePlaying.class, sender);
		if (state == null) return;
	
		Player target = (Player) args[0];
		CaviarPlayerSpigot caviarPlayer = game.getPlugin().getPlayerHandler().getObjectCached(target.getUniqueId());
		GamePlayer gamePlayer;
		if (caviarPlayer == null) {
			return;
		}
		gamePlayer = new GamePlayer(caviarPlayer);
		game.getPlayers().put(target.getUniqueId(), gamePlayer);
		CaviarStrings.COMMAND_GAMEADMIN_PLAYER_ADD.send(sender, target.getName());
	}

	private void shutdown(CommandSender sender, Object[] args) {
		game.shutdown();
		CaviarStrings.COMMAND_GAMEADMIN_SHUTDOWN_DONE.send(sender);
	}

	private void tryCommand(CommandSender sender, Consumer<CommandSender> consumer) {
		try {
			consumer.accept(sender);
		} catch (Exception e) {
			CaviarStrings.COMMAND_GAMEADMIN_ERROR.send(sender, e.getMessage());
		}
	}
}
