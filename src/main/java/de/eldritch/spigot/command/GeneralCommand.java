package de.eldritch.spigot.command;

import de.eldritch.spigot.QuotaSleep;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GeneralCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(args.length == 1 && args[0].equalsIgnoreCase("reload"))) return false;

        QuotaSleep.singleton.reload();

        sender.spigot().sendMessage(TextComponent.fromLegacyText("§7Plugin config reloaded."));
        sender.spigot().sendMessage(TextComponent.fromLegacyText("§7All worlds updated."));

        return true;
    }
}
