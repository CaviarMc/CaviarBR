package fr.caviar.br.nametag;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.api.INametagApi;

//import net.dev.eazynick.api.NickManager;

public class Nametag {
	
	private final Plugin plugin;
	private boolean isEnabled = false;

	public Nametag(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public void enable() {
//		isEnabled = plugin.getServer().getPluginManager().isPluginEnabled("EazyNick");
		isEnabled = plugin.getServer().getPluginManager().isPluginEnabled("NametagEdit");
		plugin.getServer().getOnlinePlayers().forEach(p -> updatePlayer(p));
	}

	public void updatePlayer(Player player) {
		if (!isEnabled)
			return;

//		updatePlayer(player, "§4ADMIN ", "", "ADMIN", 1);

		INametagApi api = NametagEdit.getApi();
		api.setPrefix(player, "&cADMIN ");
	}

//	private void updatePlayer(Player player, String prefix, String suffix, String groupName, int sortId) {
//		NickManager nickManager = new NickManager(player);
//		nickManager.updatePrefixSuffix(nickManager.getRealName(), nickManager.getRealName(), prefix, suffix, prefix, suffix, prefix, suffix, sortId, groupName);
//	}
}
