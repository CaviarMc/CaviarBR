package fr.caviar.br;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

public class MainTest {

	private ServerMock server;
	private CaviarBRTest plugin;

	@BeforeEach
	public void setUp() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(CaviarBRTest.class);

	}

	@AfterEach
	public void tearDown() {
		if (MockBukkit.isMocked())
			MockBukkit.unmock();
	}

	@Test
	public void test() {
		server.setPlayers(10);
	}
}
