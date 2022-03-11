package fr.caviar.br;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

public class Main {

	private ServerMock server;
	private CaviarBR plugin;

	@BeforeAll
	public void setUp() {
		server = MockBukkit.mock();
		plugin = (CaviarBR) MockBukkit.load(CaviarBR.class);

	}

	@AfterAll
	public void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	public void test() {
		server.setPlayers(128);
	}
}
