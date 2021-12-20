package de.eldritch.spigot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GeneralTabCompleter implements TabCompleter {
    private final String[] completions = {"reload"};

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        try {
            return StringUtil.copyPartialMatches(args[0], List.of(completions), new ArrayList<>());
        } catch (Exception ignored) {
            return null;
        }
    }
}
