package fr.caviar.br.nametag;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.api.INametagApi;

public class Nametag implements Listener {
	
	private final Plugin plugin;
	private boolean isEnabled = false;

	public Nametag(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public void enable() {
		isEnabled = plugin.getServer().getPluginManager().isPluginEnabled("NametagEdit");
		if (!isEnabled)
			return;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getOnlinePlayers().forEach(p -> updatePlayer(p));
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
		if (player.hasPermission("caviar.admin.prefix"))
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
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		this.updatePlayer(player);
	}
}
