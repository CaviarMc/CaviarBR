package fr.caviar.br.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import fr.caviar.br.api.regex.MatcherPattern;
import fr.caviar.br.api.regex.RegexMatcher;
import net.md_5.bungee.api.ChatColor;

public class ColorUtils {

	public static ChatColor ORANGE = ChatColor.of("#FF4500");
	private static final Pattern HEX_COLOR_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "x(?>" + ChatColor.COLOR_CHAR + "[0-9a-f]){6}", Pattern.CASE_INSENSITIVE);

	private static final Random RANDOM = new Random();
	private static final float MIN_BRIGHTNESS = 0.8f;

	public static ChatColor randomColor() {
		int rand_num = RANDOM.nextInt(0xffffff + 1);
		return ChatColor.of(String.format("#%06x", rand_num));
	}

	public static ChatColor randomBrightColor() {
		float h = RANDOM.nextFloat();
		float s = 1f;
		float b = MIN_BRIGHTNESS + (1f - MIN_BRIGHTNESS) * RANDOM.nextFloat();
		return ChatColor.of(Color.getHSBColor(h, s, b));
	}

	/**
	 * Method copied from Paper {@link org.bukkit.ChatColor#getLastColors(String)}
	 */
	@Nullable
	public static ChatColor getLastColor(String input) {
		Validate.notNull(input, "Cannot get last colors from null text");

		ChatColor result = null;
		int length = input.length();

		// Search backwards from the end as it is faster
		for (int index = length - 1; index > -1; index--) {
			char section = input.charAt(index);
			if (section == ChatColor.COLOR_CHAR && index < length - 1) {
				// Support hex colors
				if (index > 11 && input.charAt(index - 12) == ChatColor.COLOR_CHAR && (input.charAt(index - 11) == 'x' || input.charAt(index - 11) == 'X')) {
					String color = input.substring(index - 12, index + 2);
					if (HEX_COLOR_PATTERN.matcher(color).matches()) {
						result = ChatColor.of(color.substring(2).replace(ChatColor.COLOR_CHAR, Character.MIN_VALUE));
						break;
					}
				}
				char c = input.charAt(index + 1);
				ChatColor color = ChatColor.getByChar(c);

				if (color != null)
					// Once we find a color or reset we can stop searching
					if (!color.equals(ChatColor.RESET) && !color.equals(ChatColor.BOLD) && !color.equals(ChatColor.UNDERLINE) && !color.equals(ChatColor.STRIKETHROUGH) && !color.equals(ChatColor.MAGIC)) {
						result = color;
						break;
					}
			}
		}
		return result;
	}

	/**
	 * Permet de colorier chaque lettre une à une dans un mot pour faire une
	 * animation Pour BungeeCord
	 */
	public static List<String> colorString(String string, ChatColor color1, ChatColor color2) {
		List<String> dyn = new ArrayList<>();
		for (int i = 0; i < string.length(); i++)
			dyn.add(color1 + string.substring(0, i) + color2 + string.substring(i, i + 1) + color1 + string.substring(i + 1, string.length()));
		dyn.add(color1 + string);
		return dyn;
	}

	public static String format(String format, Object... args) {
		return color(String.format(format, args));
	}

	public static String translateHexColorCodes(String message) {
		MatcherPattern<String> regex = RegexMatcher.HEX_COLOR_CHAT;
		if (!regex.contains(message))
			return message;
		Matcher matcher = regex.getPattern().matcher(message);
		StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
		while (matcher.find()) {
			String group = matcher.group(1);
			matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
					+ ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
					+ ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
					+ ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5));
		}
		return matcher.appendTail(buffer).toString();
	}

	public static String translateAlternateColorCodes(String textToTranslate) {
		return translateAlternateColorCodes("0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx", textToTranslate);
	}

	public static String translateAlternateColorCodes(String allowedColors, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		char altColorChar = '&';
		for (int i = 0; i < b.length - 1; ++i)
			if (b[i] == altColorChar && allowedColors.indexOf(b[i + 1]) > -1) {
				b[i] = 167;
				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}
		return new String(b);
	}

	public static String colorSoft(String string) {
		return string != null ? translateAlternateColorCodes("012356789AaBbDdEeFfKkMmNnOoRrXx", translateHexColorCodes(string)) : null;
	}

	public static String color(String string) {
		return string != null ? translateAlternateColorCodes(translateHexColorCodes(string)) : null;
	}

	public static String joinGold(CharSequence... elements) {
		return join('e', '6', elements);
	}

	public static String joinGoldEt(Iterable<? extends CharSequence> elements) {
		return join('e', '6', elements.iterator(), " et ");
	}

	public static String joinGreenEt(Iterable<? extends CharSequence> elements) {
		return join('a', '2', elements.iterator(), " et ");
	}

	public static String joinGold(Iterable<? extends CharSequence> elements) {
		return join('e', '6', elements);
	}

	public static String joinGreen(CharSequence... elements) {
		return join('a', '2', elements);
	}

	public static String joinGreen(Iterable<? extends CharSequence> elements) {
		return join('a', '2', elements);
	}

	public static String joinRed(CharSequence... elements) {
		return join('c', '4', elements);
	}

	public static String joinRed(Iterable<? extends CharSequence> elements) {
		return join('c', '4', elements);
	}

	public static String joinRedEt(Iterable<? extends CharSequence> elements) {
		return join('c', '4', elements.iterator(), " et ");
	}

	public static String joinRedOu(Iterable<? extends CharSequence> elements) {
		return join('c', '4', elements.iterator(), " ou ");
	}

	public static String join(Character color1, Character color2, Iterable<? extends CharSequence> elements) {
		return join(color1, color2, elements.iterator());
	}

	public static String join(Iterable<? extends CharSequence> elements) {
		return join(null, null, elements.iterator());
	}

	public static String joinEt(Iterable<? extends CharSequence> elements) {
		return join(null, null, elements.iterator(), " et ");
	}

	public static String join(Character color1, Character color2, CharSequence... elements) {
		return join(color1, color2, Arrays.stream(elements).iterator());
	}

	public static String join(Character color1, Character color2, Iterator<? extends CharSequence> it) {
		return join(color1, color2, it, " ou ");
	}

	public static String join(Iterator<? extends CharSequence> it, String ouOrEt) {
		return join(null, null, it, ouOrEt);
	}

	public static String joinPlayer(Character color1, Character color2, Collection<? extends Player> elements) {
		return join(color1, color2, elements.stream().map(Player::getName).iterator(), " et ");
	}

	public static String join(Character c1, Character c2, Iterator<? extends CharSequence> it, String ouOrEt) {
		String color1 = c1 != null ? String.valueOf(ChatColor.COLOR_CHAR) + c1 : "";
		String color2 = c2 != null ? String.valueOf(ChatColor.COLOR_CHAR) + c2 : "";
		StringBuilder sb = new StringBuilder();
		Boolean hasNext = null;
		while (hasNext == null || hasNext)
			if (hasNext == null) {
				if (it.hasNext()) {
					sb.append(color2 + it.next());
					hasNext = it.hasNext();
				} else
					return "";
			} else if (hasNext != null && hasNext) {
				CharSequence next = it.next();
				sb.append(color1);
				if (!(hasNext = it.hasNext()))
					sb.append(ouOrEt);
				else
					sb.append(", ");
				sb.append(color2 + next);
			}
		sb.append(color1);
		return sb.toString();
	}

	public static List<String> color(List<String> l) {
		return l.stream().map(s -> color(s)).collect(Collectors.toList());
	}

	public static String join(CharSequence delimiter, CharSequence... elements) {
		return color(String.join(delimiter, elements));
	}

	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
		return color(String.join(delimiter, elements));
	}

	@SuppressWarnings("unchecked")
	public static String joinTry(Object delimiter, Object elements) {
		if (elements instanceof Iterable<?>)
			return join((CharSequence) delimiter, (Iterable<? extends CharSequence>) elements);
		else if (elements instanceof CharSequence[] chars)
			return join((CharSequence) delimiter, chars);
		else if (elements instanceof Object[] objs)
			return join((CharSequence) delimiter, Arrays.stream(objs).map(Object::toString).collect(Collectors.toList()));
		else if (elements instanceof Object)
			return join((CharSequence) delimiter, (CharSequence) elements.toString());
		throw new IllegalAccessError("Unknown Type for String.join() in ColorUtils.joinTry().");
	}

	public static ChatColor colorOf(String color) {
		if (color.length() == 1)
			return ChatColor.getByChar(color.charAt(0));
		else if (color.length() == 2 && (color.charAt(0) == '&' || color.charAt(0) == ChatColor.COLOR_CHAR))
			return ChatColor.getByChar(color.charAt(1));
		else if (RegexMatcher.HEX_COLOR_CHAT.is(color))
			return ChatColor.of(RegexMatcher.HEX_COLOR_CHAT.parse(color));
		throw new IllegalAccessError(color + " is not a color in format #FFFFFF or &f or §f or f.");
	}

	public static String stripColor(String string) {
		return RegexMatcher.ALL_CHAT_INVISIBLE_CHARS.replace(string, "");
	}

	private static Map<ChatColor, ColorSet<Integer, Integer, Integer>> colorMap = new HashMap<>();

	static {
		colorMap.put(ChatColor.BLACK, new ColorSet<>(0, 0, 0));
		colorMap.put(ChatColor.DARK_BLUE, new ColorSet<>(0, 0, 170));
		colorMap.put(ChatColor.DARK_GREEN, new ColorSet<>(0, 170, 0));
		colorMap.put(ChatColor.DARK_AQUA, new ColorSet<>(0, 170, 170));
		colorMap.put(ChatColor.DARK_RED, new ColorSet<>(170, 0, 0));
		colorMap.put(ChatColor.DARK_PURPLE, new ColorSet<>(170, 0, 170));
		colorMap.put(ChatColor.GOLD, new ColorSet<>(255, 170, 0));
		colorMap.put(ChatColor.GRAY, new ColorSet<>(170, 170, 170));
		colorMap.put(ChatColor.DARK_GRAY, new ColorSet<>(85, 85, 85));
		colorMap.put(ChatColor.BLUE, new ColorSet<>(85, 85, 255));
		colorMap.put(ChatColor.GREEN, new ColorSet<>(85, 255, 85));
		colorMap.put(ChatColor.AQUA, new ColorSet<>(85, 255, 255));
		colorMap.put(ChatColor.RED, new ColorSet<>(255, 85, 85));
		colorMap.put(ChatColor.LIGHT_PURPLE, new ColorSet<>(255, 85, 255));
		colorMap.put(ChatColor.YELLOW, new ColorSet<>(255, 255, 85));
		colorMap.put(ChatColor.WHITE, new ColorSet<>(255, 255, 255));
	}

	private static class ColorSet<R, G, B> {
		R red = null;
		G green = null;
		B blue = null;

		ColorSet(R red, G green, B blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		public R getRed() {
			return red;
		}

		public G getGreen() {
			return green;
		}

		public B getBlue() {
			return blue;
		}

	}

	public static ChatColor fromRGB(int r, int g, int b) {
		TreeMap<Integer, ChatColor> closest = new TreeMap<>();
		colorMap.forEach((color, set) -> {
			int red = Math.abs(r - set.getRed());
			int green = Math.abs(g - set.getGreen());
			int blue = Math.abs(b - set.getBlue());
			closest.put(red + green + blue, color);
		});
		return closest.firstEntry().getValue();
	}

	public static ChatColor getNearestForLegacyColor(Color color) {
		return fromRGB(color.getRed(), color.getGreen(), color.getBlue());
	}

	public static ChatColor getNearestForLegacyColor(ChatColor color) {
		return getNearestForLegacyColor(color.getColor());
	}

	// TODO (already call for Nametag)
	public static String replaceRGBByNearest(String s) {
		//		StringBuilder newString = new StringBuilder();
		//		ChatColor lastColor;
		//		for (int i = 0; s.length() > i; i++)
		//			if (s.charAt(i) == ChatColor.COLOR_CHAR && s.charAt(i) == 'x') {
		//
		//			}

		return s;
	}
}
