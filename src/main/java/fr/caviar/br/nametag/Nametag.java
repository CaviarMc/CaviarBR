package fr.caviar.br.nametag;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.api.INametagApi;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.api.CaviarPlugin;

public class Nametag implements Listener {

	private final CaviarPlugin plugin;
	private boolean isEnabled = false;

	public Nametag(CaviarPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void enable() {
		isEnabled = plugin.getServer().getPluginManager().isPluginEnabled("NametagEdit");
		if (!isEnabled)
			return;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getConfig().addLoadTask("nametag_update", config -> plugin.getServer().getOnlinePlayers().forEach(p -> updatePlayer(p)));
	}
	
	public void disable() {
		if (!isEnabled)
			return;
		HandlerList.unregisterAll(this);
		plugin.getServer().getOnlinePlayers().forEach(p -> delete(p));
		isEnabled = false;
	}
	
	public void updatePlayer(Player player) {
		if (!isEnabled)
			return;
		INametagApi api = NametagEdit.getApi();
		
		if (player.isOp()) {
			CaviarBR.getInstance().getPlayerHandler().get(player.getUniqueId(), (cPlayer, e) -> {
				if (e != null) {
					e.printStackTrace();
				} else if (cPlayer.getGroup() != null)
					api.setPrefix(player, "&dGroup: " + cPlayer.getGroup().toUpperCase() + " ");
				CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("Update nameTag : name = %s uuid = %s group = %s", cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
			});
		} else if (player.hasPermission("caviar.admin.prefix"))
			api.setPrefix(player, "&cADMIN ");
		else if (player.hasPermission("caviar.mod.prefix"))
			api.setPrefix(player, "&cMOD ");
		else
			api.setPrefix(player, "&7");
	}

	public void delete(Player player) {
		if (!isEnabled)
			return;
		INametagApi api = NametagEdit.getApi();
		api.clearNametag(player);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		this.updatePlayer(player);
	}
}
