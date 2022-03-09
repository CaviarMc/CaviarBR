package fr.caviar.br.player.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.player.CaviarPlayerSpigot;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerLoginListener implements Listener {

	@EventHandler
	public void on1PlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.get(uuid, (uPlayer, exception) -> {
//			System.out.println("Uuid : " + uPlayer.getUuid());
//			System.out.println("Group : " + uPlayer.getGroup());
		}); // TODO remove runTaskAsync
	}
	
	@EventHandler
	public void on2PlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
		if (uPlayer == null)
			uPlayer = playerHandler.createPlayer(player);
	}

	@EventHandler
	public void on3PlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
		if (uPlayer == null)
			return ;
		TextComponent textComponent = Component.text("You're a ")
				  .color(TextColor.color(0x443344))
				  .append(Component.text("Bunny", NamedTextColor.LIGHT_PURPLE))
				  .append(Component.text("! Press "))
				  .append(Component.keybind("key.jump").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true))
				  .append(Component.text(" to jump!"));
		player.sendMessage(textComponent);
		event.setJoinMessage(ColorUtils.format("&7[&a+&7] %s", player.getName()));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuitHigh(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		event.setQuitMessage(ColorUtils.format("&7[&c-&7] %s", player.getName()));
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.get(player.getUniqueId(), (uPlayer, exception) -> {
			if (uPlayer == null)
				return;
			playerHandler.savePlayer(uPlayer);
			playerHandler.removePlayer(uPlayer);
		});
	}
}
