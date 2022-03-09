package fr.caviar.br.player;

import java.util.UUID;

public class UniversalPlayer {

	private String name;
	private UUID uuid;

	protected UniversalPlayer(String name, UUID uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public UUID getUuid() {
		return uuid;
	}
}
