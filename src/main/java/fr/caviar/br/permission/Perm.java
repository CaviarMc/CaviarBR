package fr.caviar.br.permission;

import org.bukkit.entity.Player;

public enum Perm {

	ADMIN_PREFIX,
	MODERATOR,
	MODERATOR_PREFIX,
	MODERATOR_VANISH_SHOW,
	MODERATOR_COMMAND_VANISH,
	MODERATOR_COMMAND_GAMEADMIN,
	MODERATOR_COMMAND_SETTINGS,
	DEV_PREFIX,
	DEV_COMMAND_CONFIG,
	DEV_COMMAND_NAMETAG,
	STAFF_PREFIX,
	STAFF_JOIN_FULL,
	STAFF_INFO,
	VIP_SPECTATOR,
	;

	String perm;

	private Perm() {
		this.perm = name().toLowerCase().replace("_", ".");
	}

	public boolean has(Player p) {
		return p.hasPermission(perm);
	}

	public String get() {
		return perm;
	}
}
