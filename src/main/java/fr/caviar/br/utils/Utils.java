package fr.caviar.br.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import fr.caviar.br.api.regex.MatcherPattern;

public class Utils {

	private Utils() {}


	private static final Collector<?, ?, ?> SHUFFLER = Collectors.collectingAndThen(Collectors.toList(), list -> {
		Collections.shuffle(list);
		return list;
	});

	public static String locToStringH(Location loc) {
		StringJoiner sj = new StringJoiner(" ", "(", ")");
		sj.add(String.valueOf(loc.getX()));
		sj.add(String.valueOf(loc.getY()));
		sj.add(String.valueOf(loc.getZ()));
		if (loc.getPitch() != 0 && loc.getYaw() != 0) {
			sj.add(String.valueOf(loc.getPitch()));
			sj.add(String.valueOf(loc.getYaw()));
		}
		return loc.getWorld().getName() + sj.toString();
	}

	public static String getPluginVersion(Plugin plugin) {
		String pluginVersion = plugin.getDescription().getVersion();
		String[] split = pluginVersion.split("-");
		if (split.length < 4)
			return pluginVersion;
		String buildTime;
		try {
			buildTime = Utils.timestampToDateAndHour(new SimpleDateFormat("yyyyMMddHHmmss").parse(split[2]).getTime() / 1000L);
		} catch (java.text.ParseException e) {
			buildTime = split[2];
			e.printStackTrace();
		}
		return "v" + split[0] + " " + split[1] + " " + split[3] + " " + buildTime;
	}

	public static String hrFormatDuration(long timestampSecond) {
		long now = Utils.getCurrentTimeInSeconds(), distance;
		if (timestampSecond > now) {
			distance = timestampSecond - now;
		} else {
			distance = now - timestampSecond;
		}
		return durationToString(distance * 1000);
	}

	public static String hrDuration(long seconds) {
		return DurationFormatUtils.formatDurationWords(seconds * 1000, true, false);
	}

	public static String nanoSecondesToHumain(long nanoseconds) {
		double number;
		String unit;
		if (nanoseconds > 1000000000) {
			number = nanoseconds / 1000000000d;
			unit = "sec";
		} else if (nanoseconds > 1000000) {
			number = nanoseconds / 1000000d;
			unit = "ms";
		} else {
			number = nanoseconds;
			unit = "ns";
		}
		return new DecimalFormat("0.#").format(number) + unit;
	}

	public static boolean isAllUpperCase(String s) {
		for (int i = 0; i < s.length(); i++)
			if (!Character.isUpperCase(s.charAt(i)))
				return false;
		return true;
	}

	public static boolean isAllLowerCase(String s) {
		for (int i = 0; i < s.length(); i++)
			if (!Character.isLowerCase(s.charAt(i)))
				return false;
		return true;
	}

	public static Map<String, String> jsonToHumainReadable(String json) {
		Matcher matcher = MatcherPattern.of("\"?([^\":]+)\"?:\"?([^\",]+)\"?,?").getPattern().matcher(json);
		Map<String, String> map = new HashMap<>();
		while (matcher.find()) {
			String gr1 = matcher.group(1);
			if (gr1.charAt(0) == '{')
				gr1 = gr1.substring(1);
			if (gr1.charAt(gr1.length() - 1) == '}')
				gr1 = gr1.substring(0, gr1.length() - 1);
			String gr2 = matcher.group(2);
			if (gr2.charAt(0) == '{')
				gr2 = gr2.substring(1);
			if (gr2.charAt(gr2.length() - 1) == '}')
				gr2 = gr2.substring(0, gr2.length() - 1);
			map.put(gr1, gr2);
		}
		return map;
	}

	public static void getArray(JsonObject jsonObject) throws ParseException {
		jsonObject.isJsonObject();
		new JSONParser().parse("your string");
	}

	public static String durationToString(long time) {
		return durationToString(new DecimalFormat("0.###"), time);
	}

	public static String durationToString(NumberFormat numberFormat, long time) {
		StringBuilder sb = new StringBuilder();
		long days = time / 86_400_000;
		if (days != 0)
			sb.append(numberFormat.format(days)).append('d');
		time -= days * 86_400_000;
		long hours = time / 3_600_000;
		if (hours != 0) {
			if (sb.length() != 0)
				sb.append(' ');
			sb.append(numberFormat.format(hours)).append("h");
		}
		time -= hours * 3_600_000;
		long minutes = time / 60_000;
		if (sb.length() != 0)
			sb.append(' ');
		sb.append(numberFormat.format(minutes)).append("m");
		time -= minutes * 60_000;
		long seconds = time / 1_000;
		sb.append(numberFormat.format(seconds)).append("s");
		return sb.toString();
	}

	public static long addTimeToCurrentTime(int field, int amount) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(field, amount);
		return calendar.getTimeInMillis() / 1000;
	}

	public static int booleanToBinary(Boolean b) {
		return b.booleanValue() ? 1 : 0;
	}

	public static String capitalize(String name) {
		if (name.isBlank())
			return name;
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}

	/**
	 * Permet de savoir si un String ignore case est dans une List<String>
	 *
	 * @param s String
	 * @param l List<String>
	 * @return Boolean true/false
	 */

	public static boolean containsIgnoreCase(String s, List<String> l) {
		return l.stream().filter(Ls -> Ls.equalsIgnoreCase(s)).findFirst().isPresent();
	}

	public static void copyFile(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0)
				os.write(buffer, 0, length);
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
		}
	}

	public static boolean equalsIgnoreAccents(String a, String b) {
		Collator insenstiveStringComparator = Collator.getInstance();
		insenstiveStringComparator.setStrength(Collator.PRIMARY);
		return insenstiveStringComparator.compare(a, b) == 0;
	}

	public static boolean equalsIgnoreCase(String text, String text2) {
		return text != null && text2 != null && removeAccents(text.toLowerCase()).equalsIgnoreCase(removeAccents(text2.toLowerCase()));
	}

	public static String formatDouble(double value, int digits) {
		return String.format("%." + digits + "f", value);
	}

	public static String generateUUID(String... noThiss) {
		String noThis = noThiss.length != 0 ? noThiss[0] : new String();
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 5; i++)
			builder.append(chars.charAt((int) (Math.random() * chars.length())));
		if (!noThis.isEmpty() && builder.toString().equalsIgnoreCase(noThis))
			return generateUUID(noThis);
		return builder.toString();
	}

	/**
	 * Récupère le String après un caratère dans un String
	 *
	 * @param s     String
	 * @param after Caratère contenu dans s
	 */
	public static String getAfterFirst(String s, String after) {
		return s.substring(s.indexOf(after) + 1).trim();
	}

	public static long getCurrentTimeInSeconds() {
		return System.currentTimeMillis() / 1000L;
	}

	/**
	 * Récupère un String ignore case dans une List<String>
	 *
	 * @param s String
	 * @param l List<String>
	 * @return List<String>
	 */
	public static List<String> getIgnoreCase(String s, List<String> l) {
		return l.stream().filter(Ls -> Ls.equalsIgnoreCase(s)).collect(Collectors.toList());
	}

	public static JsonElement getJsonObject(String url) {
		try {
			JsonElement obj;
			URL url2 = new URL(url);
			InputStreamReader reader = new InputStreamReader(url2.openStream());
			obj = new JsonParser().parse(reader);
			return obj;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Permet de récupérer la lettre correspondant au nombre
	 *
	 * @param i 0 < i < 27
	 */
	public static String getLetterOfNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char) (i + 64 + 32)) : null;
	}

	/**
	 * Récupère le nombre de miliseconde avant la prochaine minute
	 *
	 * @return temps en milliseconds
	 */
	public static long getMillisBeforeNextMin() {
		Calendar calendar = Calendar.getInstance();
		long startTime = calendar.getTimeInMillis();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis() - startTime;
	}

	/**
	 * Permet de récupérer la lettre opposé correspondant au nombre
	 *
	 * @param i 0 < i < 27
	 */
	public static String getOppositeCharForNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char) ('Z' - (Character.toUpperCase((char) (i + 64)) - 'A'))) : null;
	}

	public static String getUUIDString(UUID uuid) {
		return uuid == null ? null : uuid.toString().replaceAll("-", "");
	}

	public static String insertChar(String string, String insert) {
		StringBuilder builder = new StringBuilder(string);
		int index = 0;
		for (int i = 0; i < string.length() + 1; i++) {
			builder.insert(index, insert);
			index += 2;
		}
		return builder.toString();
	}

	public static String intToSymbole(int i) {
		return String.valueOf(i)
				.replace("10", "➓")
				.replace("0", "")
				.replace("1", "➊")
				.replace("2", "➋")
				.replace("3", "➌")
				.replace("4", "➍")
				.replace("5", "➎")
				.replace("6", "➏")
				.replace("7", "➐")
				.replace("8", "➑")
				.replace("9", "➒");
	}

	public static boolean isJSONValid(String jsonInString) {
		try {
			new Gson().fromJson(jsonInString, Object.class);
			return true;
		} catch (JsonSyntaxException ex) {
			return false;
		}
	}

	public static float nextFloat(float min, float max) {
		return nextFloat(new Random(), min, max);
	}

	public static float nextFloat(Random random, float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	public static String removeAccents(String text) {
		return text == null ? null : Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static List<String> replaceInList(List<String> list, HashMap<String, String> replace) {
		for (Entry<String, String> Ereplace : replace.entrySet())
			list = replaceInList(list, Ereplace.getKey(), Ereplace.getValue());
		return list;
	}

	public static List<String> replaceInList(List<String> list, String toReplace, String replaced) {
		List<String> list2 = new ArrayList<>(list);
		for (int i = 0; i < list2.size(); i++)
			list2.set(i, list2.get(i).replaceAll(toReplace, replaced));
		return list2;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] arrayAdd(T[] array, T... add) {
		T[] array2 = (T[]) new Object[array.length + add.length];
		System.arraycopy(array, 0, array2, 0, array.length);
		for (int i = 0; i < add.length; i++)
			array2[array.length + i] = add[i];
		return array2;
	}

	/**
	 * Permet d'arrondir un double avec x nombre après la virgule
	 */
	public static double round(double value, int x) {
		if (x < 0)
			throw new IllegalArgumentException();
		long factor = (long) Math.pow(10, x);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	public static String secondsToCalendar(int s) {
		return LocalTime.ofSecondOfDay(s).toString();
	}

	public static List<String> startWords(String word, Iterable<String> allWords) {
		TreeSet<String> startWordList = new TreeSet<>(Collator.getInstance());
		for (String currentWord : allWords)
			if (currentWord != null && removeAccents(currentWord).toLowerCase().startsWith(removeAccents(word.toLowerCase())) && !startWordList.contains(currentWord))
				startWordList.add(currentWord);
		return new ArrayList<>(startWordList);
	}

	public static String timestampToDate(long timestamp) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		return format.format(new Date(timestamp * 1000));
	}

	public static String timestampToDateAndHour(long timestamp) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		return format.format(new Date(timestamp * 1000));
	}

/*
	public static String timeToDuration(long time) {
		return timestampToDuration(time + Utils.getCurrentTimeInSeconds());
	}

	public static String timestampToDuration(long timestamp) {
		return timestampToDuration(timestamp, 2);
	}
	public static String tsToShortDur(long timestamp) {
		long now = Utils.getCurrentTimeInSeconds();
		LocalDateTime timestamp2 = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), TimeZone.getDefault().toZoneId());
		LocalDateTime now2 = LocalDateTime.ofInstant(Instant.ofEpochSecond(now), TimeZone.getDefault().toZoneId());
		LocalDateTime start;
		LocalDateTime end;
		if (timestamp > now) {
			start = now2;
			end = timestamp2;
		} else {
			start = timestamp2;
			end = now2;
		}
		Duration dur = Duration.between(start.toLocalTime(), end.toLocalTime());
		LocalDate e = end.toLocalDate();
		if (dur.isNegative()) {
			dur = dur.plusDays(1);
			e = e.minusDays(1);
		}
		Period per = Period.between(start.toLocalDate(), e);
		long year = per.getYears();
		long month = per.getMonths();
		long day = per.getDays();
		long hour = dur.toHours();
		long minute = dur.toMinutes() - 60 * dur.toHours();
		long second = dur.getSeconds() - 60 * dur.toMinutes();
		StringJoiner sj = new StringJoiner(" ");
		if (year > 1)
			sj.add(year + " ans");
		else if (year == 1)
			sj.add(year + " an");
		if (month != 0 && (sj.length() == 0 || month > 5))
			sj.add(month + " mo");
		if (day != 0 && (sj.length() == 0 || sj.length() < 2 && day > 16))
			sj.add(day + "j");
		if (hour != 0 && (sj.length() == 0 || sj.length() < 2 && day < 2 && day != 0 && hour > 12))
			sj.add(hour + "h");
		if (minute != 0 && (sj.length() == 0 || sj.length() < 2 && hour == 1))
			sj.add(minute + "min");
		if (sj.length() == 0 && second != 0)
			sj.add(second + "s");
		return sj.toString();
	}

	public static String timestampToDuration(long timestamp, int precision, long now) {
		LocalDateTime timestamp2 = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), TimeZone.getDefault().toZoneId());
		LocalDateTime now2 = LocalDateTime.ofInstant(Instant.ofEpochSecond(now), TimeZone.getDefault().toZoneId());
		LocalDateTime start;
		LocalDateTime end;
		if (timestamp > now) {
			start = now2;
			end = timestamp2;
		} else {
			start = timestamp2;
			end = now2;
		}
		Duration dur = Duration.between(start.toLocalTime(), end.toLocalTime());
		LocalDate e = end.toLocalDate();
		if (dur.isNegative()) {
			dur = dur.plusDays(1);
			e = e.minusDays(1);
		}
		Period per = Period.between(start.toLocalDate(), e);
		long year = per.getYears();
		long month = per.getMonths();
		long day = per.getDays();
		long hour = dur.toHours();
		long minute = dur.toMinutes() - 60 * dur.toHours();
		long second = dur.getSeconds() - 60 * dur.toMinutes();
		List<String> msg = new ArrayList<>();
		if (year > 1)
			msg.add(year + " ans");
		else if (year == 1)
			msg.add(year + " an");
		if (month != 0)
			msg.add(month + " mois");
		if (day > 1)
			msg.add(day + " jours");
		else if (day == 1)
			msg.add(day + " jour");
		if (hour > 1)
			msg.add(hour + " heures");
		else if (hour == 1)
			msg.add(hour + " heure");
		if (minute > 1)
			msg.add(minute + " minutes");
		else if (minute == 1)
			msg.add(minute + " minute");
		if (second > 1)
			msg.add(second + " secondes");
		else if (second == 1)
			msg.add(second + " seconde");
		List<String> msgs = new ArrayList<>();
		for (String message : msg) {
			if (message != null)
				msgs.add(message);
			if (msgs.size() >= precision)
				break;
		}
		return String.join(", ", msgs);
	}

	public static String timestampToDuration(long timestamp, int precision) {
		return timestampToDuration(timestamp, precision, Utils.getCurrentTimeInSeconds());
	}*/

	public static int toField(String field) {
		switch (field) {

		case "year":
			return Calendar.YEAR;
		case "month", "mois":
			return Calendar.MONTH;
		case "day":
			return Calendar.DAY_OF_MONTH;
		case "hour":
			return Calendar.HOUR_OF_DAY;
		case "minute":
			return Calendar.MINUTE;
		case "second":
			return Calendar.SECOND;
		default:
			return 0;
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> Collector<T, ?, List<T>> toShuffledList() {
		return (Collector<T, ?, List<T>>) SHUFFLER;
	}

	/**
	 * Permet d'accroder ou nom un mot.
	 *
	 * @param i Nombre d'object
	 * @return Un s si le nombre n'est pas 1
	 */
	public static String withOrWithoutS(int i) {
		return i == 1 ? "" : "s";
	}

	public static String withOrWithoutS(long i, String string) {
		return Long.toString(i) + " " + (i < 2 ? string : string + "s");
	}

	public static boolean isEmptyFile(File file) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			return br.readLine() == null;
		}
	}

	public static int getRandomAmount(Random random, int min, int max) {
		if (min == max)
			return min;
		return random.nextInt(max - min + 1) + min;
	}

	public static <T> T getRandomFrom(Random random, List<T> list) {
		return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
	}

	public static <T> T getRandomFrom(Random random, Collection<T> collection) {
		return collection.isEmpty() ? null : collection.stream().skip(random.nextInt(collection.size())).findFirst().orElse(null);
	}


	public static <T> T getRandomFrom(Random random, T[] array) {
		return array.length == 0 ? null : array[random.nextInt(array.length)];
	}

	public static String[] fillArray(String string, int size) {
		List<String> list = new ArrayList<>();
		while (size-- > 0)
			list.add(string);
		return list.toArray(String[]::new);
	}

	public static class DevideList<T> {
		private Collection<T> list;
		private int nb;

		public DevideList(Collection<T> list, int nb) {
			this.list = list;
			this.nb = nb;
		}

		public List<List<T>> maxSizeList() {
			int itemPerList = nb;
			int listSize = list.size() / itemPerList;
			List<List<T>> lists = new ArrayList<>(listSize);
			for (int i2 = 0; i2 < listSize; ++i2) {
				lists.add(new ArrayList<>(itemPerList));
			}
			int i = 0;
			for (Iterator<T> it = list.iterator(); it.hasNext(); ++i) {
				lists.get(i).add(it.next());
				if (i + 1 == listSize)
					i = -1;
			}
			return lists;
		}

		public List<List<T>> nbList() {
			int listSize = nb;
			int itemPerList = (list.size() / listSize) + 1;
			List<List<T>> lists = new ArrayList<>(listSize);
			for (int i2 = 0; i2 < listSize; ++i2) {
				lists.add(new ArrayList<>(itemPerList));
			}
			int i = 0;
			for (Iterator<T> it = list.iterator(); it.hasNext(); ++i) {
				lists.get(i).add(it.next());
				if (i + 1 == listSize)
					i = -1;
			}
			return lists;
		}
	}
}
