package fr.caviar.br.player;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class CaviarPlayerSpigot extends UniversalPlayer {

	private OfflinePlayer player;
	private String group;
	private boolean fakePlayer;

	public CaviarPlayerSpigot(OfflinePlayer player) {
		super(player.getName(), player.getUniqueId());
		this.player = player;
	}

	public CaviarPlayerSpigot(String name, UUID uuid) {
		super(name, uuid);
		player = Bukkit.getOfflinePlayer(uuid); // TODO Check the use against the Mojang API
	}

	public OfflinePlayer getPlayer() {
		return player;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public boolean isFakePlayer() {
		return fakePlayer;
	}

	public void setFakePlayer(boolean fakePlayer) {
		this.fakePlayer = fakePlayer;
	}

}