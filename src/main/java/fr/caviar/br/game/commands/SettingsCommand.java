package fr.caviar.br.game.commands;

import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;
import fr.caviar.br.game.GameSettings.GameSetting;
import fr.caviar.br.game.GameSettings.GameSettingBoolean;
import fr.caviar.br.game.GameSettings.GameSettingInt;
import fr.caviar.br.permission.Perm;
import dev.jorel.commandapi.CommandAPICommand;

public class SettingsCommand {

	private GameSettings settings;

	private CommandAPICommand command;

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
			subCmd.setRequirements(s -> true);
			command.withSubcommand(subCmd);
		}

		game.getPlugin().getCommands().registerCommand(command);
	}

	private void addGui(HumanEntity player) {
		ChestGui gui = new ChestGui(5, "Settings");
		OutlinePane pane = new OutlinePane(0, 0, 9, 5);

		ItemStack item = new ItemStack(Material.ICE);
		GuiItem guiItem;
		for (GameSetting<?> setting : settings.getSettings()) {
			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.setDisplayName("§r§e" + setting.getKey());
			if (setting instanceof GameSettingInt gsi) {
				itemMeta.setLore(List.of("§r§aValue " + gsi.get(), "", "§7Min " + gsi.getMin(), "§7Max " + gsi.getMax()));
				item.setAmount(gsi.get());
			} else if (setting instanceof GameSettingBoolean gsb) {
				itemMeta.setLore(List.of("§r§aValue " + gsb.get(), "", "§7True or False"));
				if (gsb.get())
					item.setAmount(1);
				else
					item.setAmount(-1);
			} else
				itemMeta.setLore(List.of("§r§aValue " + setting.get(), ""));
			item.setItemMeta(itemMeta);
			guiItem = new GuiItem(item, event -> event.getWhoClicked().sendMessage("In dev"));
			pane.addItem(guiItem);
		}
		gui.addPane(pane);
		gui.show(player);
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

}
