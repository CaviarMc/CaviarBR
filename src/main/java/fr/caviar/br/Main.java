package fr.caviar.br;

import org.bukkit.plugin.java.JavaPlugin;

import fr.caviar.br.game.GameManager;

public class Main extends JavaPlugin {

	private GameManager game;
	
	@Override
	public void onLoad() {
		game = new GameManager();
		
		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
		super.onLoad();
	}
	
	@Override
	public void onEnable() {
		game.enable();
		
		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), getDescription().getVersion());
		super.onEnable();
	}

	@Override
	public void onDisable() {
		game.disable();
		
		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), getDescription().getVersion());
		super.onDisable();
	}
	
	public GameManager getGame() {
		return game;
	}

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}
}
