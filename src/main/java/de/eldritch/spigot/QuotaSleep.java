package de.eldritch.spigot;

import de.eldritch.spigot.command.GeneralCommand;
import de.eldritch.spigot.command.GeneralTabCompleter;
import de.eldritch.spigot.util.Runnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Objects;
import java.util.logging.Level;

public class QuotaSleep extends JavaPlugin {
    public static QuotaSleep singleton;

    private final File configFile;

    public QuotaSleep() {
        singleton = this;

        // get config file
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configFile = new File(getDataFolder(), "config.yml");
    }

    @Override
    public void onEnable() {
        reload();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        try {
            Objects.requireNonNull(getCommand("quotasleep")).setExecutor(new GeneralCommand());
            Objects.requireNonNull(getCommand("quotasleep")).setTabCompleter(new GeneralTabCompleter());
        } catch (NullPointerException e) {
            getLogger().log(Level.WARNING, "Unable to register command.", e);
        }

        refreshAll();
    }

    /**
     * Refreshes a world and checks whether the night should be skipped.
     * @param world World to refresh.
     */
    public void refresh(World world) {
        if (!world.isNatural()) return;

        if (gameRuleOverride(world)) return;

        int sleeping = 0;
        int required = 0;
        for (Player player : world.getPlayers()) {
            if (player.isSleeping()) {
                sleeping++;
            }
        }

        if (sleeping > 0) {
            required = (int) (getPercentageRequired() / 100 * world.getPlayers().size());

            String message;
            if (required - sleeping > 1) {
                message = "§6" + (required - sleeping) + " §7more players required to skip sleeping. §8(" + getPercentageRequired() + "%)";
            } else if (required - sleeping == 1) {
                message = "§6One §7more player required to skip sleeping. §8(" + getPercentageRequired() + "%)";
            } else {
                message = "§7Skipping sleep...";
            }
            getServer().getScheduler().runTaskLater(this, () -> {
                for (Player player : world.getPlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                }
            }, 20L);
        }

        if (sleeping >= required && required != 0) {
            int duration = getDuration(24000L - world.getTime());
            LinkedList<Long> ticks = getTickValues(world.getTime() + 21L, duration);

            getLogger().info("Skipped sleeping in world '" + world.getName() + "' from tick, because " + sleeping
                    + " of " + world.getPlayers().size() + " players were sleeping (" + getPercentageRequired()
                    + "% required).");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (canSleep(world) && !ticks.isEmpty()) {
                        world.setTime(ticks.removeFirst());
                    } else {
                        world.setThundering(false);
                        world.setStorm(false);
                        cancel();
                    }
                }
            };
            runnable.setId(getServer().getScheduler().scheduleSyncRepeatingTask(this, runnable, 20L, 1L));
        }
    }

    /**
     * Checks whether sleeping should be ignored in a {@link World} because of a {@link GameRule}.
     * @param world World to check.
     * @return true if the world should be ignored.
     */
    private boolean gameRuleOverride(World world) {
        return Boolean.FALSE.equals(world.getGameRuleValue(world.isThundering() ? GameRule.DO_WEATHER_CYCLE : GameRule.DO_DAYLIGHT_CYCLE));
    }

    /**
     * Refreshes all worlds.
     * @see QuotaSleep#refresh(World)
     */
    public void refreshAll() {
        this.getServer().getWorlds().forEach(this::refresh);
    }

    /**
     * Reloads the config with defaults and saves it to the file.
     */
    public void reload() {
        // define config defaults
        try {
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.load(new InputStreamReader(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("config.yml"))));
            getConfig().setDefaults(defaults);
            getConfig().options().copyDefaults(true);
        } catch (IOException | InvalidConfigurationException | NullPointerException e) {
            getLogger().log(Level.WARNING, "Unable to load config defaults.", e);
        }

        // load config file
        try {
            configFile.createNewFile();
            getConfig().load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().log(Level.WARNING, "Unable to load config from file.", e);
        }

        // save to file
        try {
            getConfig().save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Unable to save config to file", e);
        }
    }

    /**
     * Get the required percentage of players sleeping to skip.
     * @return Required percentage of sleeping players.
     */
    private double getPercentageRequired() {
        return getConfig().getDouble("percentage", 50.0);
    }

    /**
     * Retrieves the amount of iterations for the transition.
     * <p>This method used to retrieve the amount of ticks until dawn, which didn't account for thunderstorms.
     * @param time Ticks until the night / thunderstorm is over.
     * @return Amount of iterations for the transition.
     */
    private int getDuration(long time) {
        if (time > 8000L)
            return getConfig().getInt("duration.midnight", 75);
        if (time > 4000L)
            return getConfig().getInt("duration.evening", 100);
        return getConfig().getInt("duration.morning", 50);
    }

    /**
     * Provides a {@link LinkedList} of tick values for a smooth transition until the next morning.
     *
     * @param from The time from wich the transition should start.
     * @param ticks The amount of ticks for the transition (will be greater by 1).
     * @return List of time values.
     */
    private @NotNull LinkedList<Long> getTickValues(long from, int ticks) {
        long diff = (long) 24000 - from;
        LinkedList<Long> val = new LinkedList<>();
        for (int i = 0; i < ticks; i++)
            val.add((long) (((Math.sin(3D * ((double) i / (double) ticks) - 0.5D * Math.PI) + 1D) / 2D) * (double) diff + (double) from));
        val.add(24000L);
        return val;
    }

    /**
     * Checks whether a world is currently in a state that allows sleeping (night or thunderstorm).
     * <p>Values taken from <code>https://minecraft.fandom.com/wiki/Bed</code>.
     *
     * @param world {@link World} that should be checked.
     * @return true if it is currently possible to sleep.
     */
    private boolean canSleep(@NotNull World world) {
        if (world.isClearWeather()) {
            // clear weather
            return 12542 < world.getTime();
        } else if (world.isThundering()) {
            // thunder
            return true;
        } else {
            // rain
            return 12010 < world.getTime();
        }
    }
}
