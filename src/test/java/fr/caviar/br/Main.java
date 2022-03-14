package fr.caviar.br;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

public class Main {

	private ServerMock server;
	private CaviarBR plugin;
	private CaviarBR plugin2;

	@BeforeAll
	public void setUp() {
		server = MockBukkit.mock();
		plugin = (CaviarBR) MockBukkit.load(CaviarBR.class);
		plugin2.sendMessage("Hello");

	}

	@AfterAll
	public void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	public void test() {
		plugin2.sendMessage("Hello");
		server.setPlayers(128);
	}
}
