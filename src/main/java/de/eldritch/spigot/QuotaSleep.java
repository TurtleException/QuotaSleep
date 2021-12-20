package de.eldritch.spigot;

import de.eldritch.spigot.util.Runnable;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
            Objects.requireNonNull(getCommand("quotasleep reload")).setExecutor(new ReloadCommand());
        } catch (NullPointerException e) {
            getLogger().log(Level.WARNING, "Unable to register command 'quotasleep reload'.", e);
        }

        refreshAll();
    }

    /**
     * Refreshes a world and checks whether the night should be skipped.
     * @param world World to refresh.
     */
    public void refresh(World world) {
        if (!world.isNatural()) return;

        int sleeping = 0;
        for (Player player : world.getPlayers()) {
            if (player.isSleeping()) {
                sleeping++;
            }
        }

        if (shouldSkip(sleeping, world.getPlayers().size())) {
            int duration = getDuration(sleepEnd(world) - world.getTime());
            LinkedList<Long> ticks = getTickValues(world.getTime(), sleepEnd(world), duration);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (canSleep(world) && !ticks.isEmpty()) {
                        world.setTime(ticks.removeFirst());
                    } else {
                        cancel();
                    }
                }
            };
            runnable.setId(getServer().getScheduler().scheduleSyncRepeatingTask(this, runnable, 0L, 1L));
        }
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
            defaults.load(new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream("config.yml"))));
            getConfig().setDefaults(defaults);
        } catch (IOException | InvalidConfigurationException | NullPointerException e) {
            getLogger().log(Level.WARNING, "Unable to load config defaults.", e);
        }

        // load config file
        try {
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
     * Checks whether the amount of players sleeping is enough to skip a night.
     * @param sleeping Amount of sleeping players.
     * @param players Total amount of players.
     * @return true if the night should be skipped.
     */
    private boolean shouldSkip(int sleeping, int players) {
        return ((double) (sleeping / players) >= getConfig().getDouble("percentage", 50.0));
    }

    /**
     * Retrieves the amount of iterations for the transition.
     * <p>This method used to retrieve the amount of ticks until dawn, which didn't account for thunderstorms.
     * @param time Ticks until the night / thunderstorm is over.
     * @return Amount of iterations for the transition.
     */
    private int getDuration(long time) {
        if (time < 4000)
            return getConfig().getInt("duration.evening", 100);
        if (time < 8000)
            return getConfig().getInt("duration.midnight", 75);
        return getConfig().getInt("duration.morning", 50);
    }

    /**
     * Provides a {@link LinkedList} of tick values for a smooth transition between two specified tick values.
     *
     * @param from The time from wich the list should start.
     * @param to The time at wich the list should stop
     * @param ticks The amount of ticks for the transition.
     * @return List of length ticks + 1 with time values between from and to.
     */
    private LinkedList<Long> getTickValues(long from, long to, int ticks) {
        long diff = to - from;
        LinkedList<Long> val = new LinkedList<>();
        for (int i = 0; i <= ticks; i++)
            val.add((long) (((Math.sin(3D * ((double) i / (double) ticks) - 0.5D * Math.PI) + 1D) / 2D) * diff + from));
        return val;
    }

    /**
     * Checks whether a world is currently in a state that allows sleeping (night or thunderstorm).
     * <p>Values taken from <code>https://minecraft.fandom.com/wiki/Bed</code>.
     *
     * @param world {@link World} that should be checked.
     * @return true if it is currently possible to sleep.
     */
    private boolean canSleep(World world) {
        if (world.isClearWeather()) {
            // clear weather
            return (12542 < world.getTime()) && (world.getTime() < 23459);
        } else if (world.isThundering()) {
            // thunder
            return true;
        } else {
            // rain
            return (12010 < world.getTime()) && (world.getTime() < 23991);
        }
    }

    /**
     * The time when the night / thunderstorm is over.
     * <p>Values taken from <code>https://minecraft.fandom.com/wiki/Bed</code>.
     *
     * @param world {@link World} that should be checked.
     * @return Time in ticks.
     */
    private long sleepEnd(World world) {
        if (world.isClearWeather()) {
            // clear weather
            return 23459L;
        } else if (world.isThundering()) {
            // thunder
            long ticks = world.getTime() + world.getWeatherDuration();
            return (ticks < 24000) ? ticks : (ticks - 24000L);
        } else {
            // rain
            return 23991L;
        }
    }
}
