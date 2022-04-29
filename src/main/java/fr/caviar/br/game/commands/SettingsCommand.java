package fr.caviar.br.game.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;

import dev.jorel.commandapi.CommandAPICommand;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;
import fr.caviar.br.game.GameSettings.GameSetting;
import fr.caviar.br.game.GameSettings.GameSettingBoolean;
import fr.caviar.br.game.GameSettings.GameSettingInt;
import fr.caviar.br.game.GameSettings.GameSettingMinute;
import fr.caviar.br.game.GameSettings.GameSettingSecond;
import fr.caviar.br.permission.Perm;

public class SettingsCommand {

	private GameSettings settings;
	private CommandAPICommand command;
	private Set<HumanEntity> playerInGui = new HashSet<>();

	public SettingsCommand(GameManager game) {
		settings = game.getSettings();

		command = new CommandAPICommand("settings")
			.withPermission(Perm.MODERATOR_COMMAND_SETTINGS.get())
			.executesPlayer((player, args) -> {
				addGui(player);
			});
		CommandAPICommand subCmd;
		for (GameSetting<?> setting : settings.getSettings()) {
			subCmd = settingCommand(setting);
			command.withSubcommand(subCmd);
		}

		game.getPlugin().getCommands().registerCommand(command);
	}

	private <T> CommandAPICommand settingCommand(GameSetting<T> setting) {
		return new CommandAPICommand(setting.getKey())
				.withArguments(setting.getArguments())
				.executes((sender, args) -> {
					T oldValue = setting.get();
					if (args == null || args.length == 0) {
						CaviarStrings.COMMAND_SETTING_SHOW.send(sender, setting.getKey(), Objects.toString(oldValue));
						return;
					}
					T newValue = setting.getValueFromArguments(args);
					if (oldValue != newValue) {
						setting.set(newValue);
						CaviarStrings.COMMAND_SETTING_SET.send(sender, setting.getKey(), Objects.toString(oldValue), Objects.toString(newValue));
					} else {
						CaviarStrings.COMMAND_SETTING_SAME.send(sender, setting.getKey(), Objects.toString(oldValue));
					}
				});
	}


	private void applyItem(GameSetting<?> setting, ItemStack item) {
		ItemMeta itemMeta = item.getItemMeta();
		itemMeta.setDisplayName("§r§e" + setting.getKey());
		
		List<String> lores = new ArrayList<>();
		lores.add("§r§aValue " + setting.get());
		lores.add("");
		
		if (setting instanceof GameSettingInt gsi) {
			if (setting instanceof GameSettingSecond) {
				itemMeta.setDisplayName(itemMeta.getDisplayName() + "InSecond");
			} else if (setting instanceof GameSettingMinute) {
				itemMeta.setDisplayName(itemMeta.getDisplayName() + "InMinute");
			} 
			lores.add("§7Min " + gsi.getMin());
			lores.add("§7Max " + gsi.getMax());
			lores.add("");
			lores.add("§dLEFT Click +1");
			lores.add("§dRIGHT Click -1");
			lores.add("§dSHIFT LEFT Click +10");
			lores.add("§dSHIFT RIGHT Click -10");
			item.setAmount(gsi.get());
		} else if (setting instanceof GameSettingBoolean gsb) {
			lores.add("§7True or False");
			lores.add("");
			if (gsb.get())
				item.setAmount(64);
			else
				item.setAmount(1);
			lores.add("§dClick toggle");
		} else {
			lores.add("§dLEFT Click +1");
			lores.add("§dRIGHT Click -1");
		}
		itemMeta.setLore(lores);
		item.setItemMeta(itemMeta);
	}

	private void addGui(HumanEntity player) {
		ChestGui gui = new ChestGui(5, "Settings");
		OutlinePane pane = new OutlinePane(0, 0, 9, 5);

		ItemStack item;
		for (GameSetting<?> setting : settings.getSettings()) {
			item = new ItemStack(setting.getMaterial());
			item.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			applyItem(setting, item);
			GuiItem guiItem = new GuiItem(item, event -> {
				event.setCancelled(true);
				if (event.isShiftClick()) {
					if (event.isLeftClick()) {
						setting.bigIncrement();
					} else if (event.isRightClick())
						setting.bigDecrement();
					else
						return;
				} else {
					if (event.isLeftClick()) {
						setting.smallIncrement();
					} else if (event.isRightClick())
						setting.smallDecrement();
					else
						return;
				}
				addGui(player);
//				playerInGui.forEach(this::addGui);
			});
			pane.addItem(guiItem);
		}
		gui.addPane(pane);
		gui.setOnBottomClick(event -> {
			if (event.getCursor() != null && event.getInventory().contains(event.getCursor()) && InventoryAction.COLLECT_TO_CURSOR.equals(event.getAction())) {
				event.setCancelled(true);
				return;
			}
			if (event.isShiftClick())
				event.setCancelled(true);
		});
		gui.setOnTopClick(event -> {
			if (event.getCursor() != null)
				event.setCancelled(true);
		});
		gui.setOnTopDrag(event -> {
			if (event.getCursor() != null)
				event.setCancelled(true);
		});
//		gui.setOnClose(event -> playerInGui.remove(player));
		gui.show(player);
//		playerInGui.add(player);
	}
}
