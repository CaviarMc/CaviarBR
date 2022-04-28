package fr.caviar.br.nametag;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.api.INametagApi;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import fr.caviar.br.CaviarBR;
import fr.caviar.br.permission.Perm;

public class Nametag implements Listener {

	private final CommandAPICommand command;
	private final CaviarBR plugin;
	private NametagEdit nametagEditPlugin;
	private INametagApi api;
	private boolean isEnabled = false;

	public Nametag(CaviarBR plugin) {
		this.plugin = plugin;
		command = new CommandAPICommand("nametag").withPermission(Perm.DEV_COMMAND_NAMETAG.get());
		command.withSubcommand(new CommandAPICommand("reload").withArguments(new PlayerArgument("playerToReload")).executes((CommandExecutor) (sender, args) -> {
			Player playerToReload = args.length == 1 ? (Player) args[0] : null;
			if (playerToReload == null)
				return ;
			updatePlayer(playerToReload);
			if (sender instanceof Player p)
				api.reloadNametag(p);
			// TODO add msg
		}));
		plugin.getCommands().registerCommand(command);
	}

	public void enable() {
		PluginManager pluginManager = plugin.getServer().getPluginManager();
		isEnabled = pluginManager.isPluginEnabled("NametagEdit");
		if (!isEnabled)
			return;
		pluginManager.registerEvents(this, plugin);
		plugin.getConfig().addLoadTask("nametag_update", config -> plugin.getServer().getOnlinePlayers().forEach(p -> updatePlayer(p)));
		api = NametagEdit.getApi();
		if (pluginManager.getPlugin("NametagEdit") instanceof NametagEdit ntePl) {
			nametagEditPlugin = ntePl;
		} else {
			plugin.getLogger().log(Level.SEVERE, "Can't get Main class of plugin NametagEdit");
			nametagEditPlugin = null;
		}
	}

	public void disable() {
		if (!isEnabled)
			return;
		HandlerList.unregisterAll(this);
		plugin.getServer().getOnlinePlayers().forEach(p -> delete(p));
		isEnabled = false;
	}

	public void refreshAllTeams(Player player) {
		//nametagEditPlugin.getManager().reset(null);
	}

	public void updatePlayer(Player p) {
		if (!isEnabled && api != null)
			return;
		/*if (player.isOp()) {
			CaviarBR.getInstance().getPlayerHandler().get(player.getUniqueId(), (cPlayer, e) -> {
				if (e != null) {
					e.printStackTrace();
				} else if (cPlayer.getGroup() != null)
					api.setPrefix(player, "&dGroup: " + cPlayer.getGroup().toUpperCase() + " ");
				CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("Update nameTag : name = %s uuid = %s group = %s", cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
			});
		} else */

		;
		if (Perm.ADMIN_PREFIX.has(p))
			api.setPrefix(p, "&cADMIN ");
		else if (Perm.MODERATOR_PREFIX.has(p))
			api.setPrefix(p, "&cMOD ");
		else if (Perm.DEV_PREFIX.has(p))
			api.setPrefix(p, "&2DEV ");
		else if (Perm.STAFF_PREFIX.has(p))
			api.setPrefix(p, "&2STAFF ");
		else
			api.setPrefix(p, "&7");
	}

	public void setSpectator(Player player) {
		if (!isEnabled)
			return;
		api.setPrefix(player, new StringBuilder("&7[SPECTATOR] ").append(api.getNametag(player).getPrefix()).toString());
	}

	public void delete(Player player) {
		if (!isEnabled)
			return;
		api.clearNametag(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		this.updatePlayer(player);
	}
}
