package fr.caviar.br;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.api.regex.MatcherPattern;
import fr.caviar.br.api.regex.RegexMatcher;

public class MainTest {

	private ServerMock server;
	@SuppressWarnings("unused")
	private CaviarPlugin plugin;

	@BeforeEach
	public void setUp() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(CaviarPlugin.class);

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
	
	@Test
	public void regexTest() throws CaviarTestException {
		MatcherPattern<UUID> uuid = RegexMatcher.UUID;
		for (int i = 0; i < 10; ++i) {
			String randomUUID = UUID.randomUUID().toString();
			if (!uuid.is(randomUUID)) {
				throw new CaviarTestException(String.format("Regex UUID does not work : It did not recognize %s as a UUID", randomUUID));
			}
		}
		for (int i = 0; i < 10; ++i) {
			String randomUUID = UUID.randomUUID().toString().replace("-", "");
			if (!uuid.is(randomUUID)) {
				throw new CaviarTestException(String.format("Regex UUID does not work : It did not recognize %s as a UUID", randomUUID));
			}
		}
	}
}
