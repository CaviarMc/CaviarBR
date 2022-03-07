package fr.caviar.br;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	@Override
	public void onLoad() {
		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
		super.onLoad();
	}
	
	@Override
	public void onEnable() {
		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), getDescription().getVersion());
		super.onEnable();
	}

	@Override
	public void onDisable() {
		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), getDescription().getVersion());
		super.onDisable();
	}

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}
}
