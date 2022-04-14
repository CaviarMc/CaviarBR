package fr.caviar.br;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.api.regex.MatcherPattern;
import fr.caviar.br.api.regex.RegexMatcher;
import fr.caviar.br.scoreboard.Scoreboard;
import fr.caviar.br.utils.Utils;

public class MainTest {

	private ServerMock server;
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
	public void testPluginVersion() {
		System.out.println(Utils.getPluginVersion(plugin));
	}
	
	@Test
	public void testTimestamp() {
		String time1 = Utils.hrFormatDuration(Utils.getCurrentTimeInSeconds() + 60);
		String time2 = Utils.hrFormatDuration(Utils.getCurrentTimeInSeconds() - 60);
		String time3 = Utils.hrFormatDuration(Utils.getCurrentTimeInSeconds());
		
		if (!time1.equals(time2))
			throw new RuntimeException("Time to duration past time is not equels to futur time");
		
		if (time1.equals(time3) || time2.equals(time3))
			throw new RuntimeException("Time to duration now is not to futur time or past time");
	}
	
	@Test
	public void testDevideList() {
		List<String> list = List.of("Ceci", "Début", "est la", "de", "1ère", "la 2ème", "ligne", "ligne");
		Utils.DevideList<String> devideList = new Utils.DevideList<>(list, 2);
		List<List<String>> newLists = devideList.nbList();
		if (newLists.size() != 2) {
			throw new RuntimeException("Utils.DevideList not good size test");
		}
		
		newLists.get(0).forEach(s -> System.out.println(s));
		System.out.println("------------");
		newLists.get(1).forEach(s -> System.out.println(s));
		

		if (!newLists.get(1).get(0).equals("Début"))
			throw new RuntimeException("Utils.DevideList not good result 1");
		
		if (!newLists.get(0).get(2).equals("1ère"))
			throw new RuntimeException("Utils.DevideList not good result 2");
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
