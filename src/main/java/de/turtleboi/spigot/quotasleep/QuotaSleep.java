package de.turtleboi.spigot.quotasleep;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class QuotaSleep extends JavaPlugin {
    public static double fraction;
    public static int[] steps;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        fraction = getConfig().getDouble("percentage", 50.0) / 100;
        steps = new int[]{
                getConfig().getInt("duration.evening" , 100),
                getConfig().getInt("duration.midnight",  75),
                getConfig().getInt("duration.morning" ,  50)
        };

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
    }

    public void scheduleWorldUpdate(World world) {
        getServer().getScheduler().runTaskLater(this, () -> this.updateWorld(world), 1L);
    }

    public void updateWorld(World world) {
        if (!world.isNatural()) return;

        boolean grDaylightCycle = Optional.ofNullable(world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)).orElse(true);
        boolean grWeatherCycle  = Optional.ofNullable(world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)).orElse(true);

        // skip if neither daylight- nor weather-cycle are enabled (sleeping won't change anything)
        if (!grDaylightCycle && !grWeatherCycle) return;

        int    totalPlayers = world.getPlayers().size();
        int sleepingPlayers = (int) world.getPlayers().stream().filter(LivingEntity::isSleeping).count();
        int requiredPlayers = Math.max((int) (totalPlayers * QuotaSleep.fraction), 1);
        int  missingPlayers = requiredPlayers - sleepingPlayers;

        boolean changeWeather;
        boolean changeTime;

        if (world.isClearWeather()) {
            changeWeather = false;
            changeTime    = (world.getTime() > 12542);
        } else if (world.isThundering()) {
            changeWeather = true;
            changeTime    = true;
        } else {
            changeWeather = true;
            changeTime    = (world.getTime() > 12010);
        }

        // gamerule overrides
        if (!grDaylightCycle) changeTime    = false;
        if (!grWeatherCycle)  changeWeather = false;

        if (missingPlayers > 1) {
            broadcast(world, "§6" + missingPlayers + " §7more players required to skip sleeping. §8(" + (fraction * 100) + "%)");
            return;
        } else if (missingPlayers == 1) {
            broadcast(world, "§6One §7more player required to skip sleeping. §8(" + (fraction * 100) + "%)");
            return;
        } else {
            broadcast(world, "§7Skipping sleep...");
        }

        if (changeTime) {
            long[] ticks = getTickValues(world.getTime(), getSteps(world.getTime()));

            for (int i = 0; i < ticks.length; i++) {
                final long time = ticks[i];

                getServer().getScheduler().runTaskLater(this, () -> world.setTime(time), i + 1);
            }
        } else if (changeWeather) { // else because weather would be changed during the time transition
            world.setThundering(false);
            world.setStorm(false);
        }
    }

    private static long[] getTickValues(long start, int steps) {
        long diff = 24000 - start;
        long[] val = new long[steps + 1];
        for (int i = 0; i < steps; i++)
            val[i] = (long) ((((Math.sin(3D * (double) i / (double) steps) - 0.5D * Math.PI) + 1D) / 2D) * (double) diff + (double) start);
        val[steps] = 24000L;
        return val;
    }

    private static int getSteps(long time) {
        if (time > 8000L)
            return steps[1];
        if (time > 4000L)
            return steps[0];
        return steps[2];
    }

    private void broadcast(World world, String message) {
        getServer().getScheduler().runTaskLater(this, () -> {
            BaseComponent[] messageComponents = TextComponent.fromLegacyText(message);

            for (Player player : world.getPlayers())
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, messageComponents);
        }, 1L);
    }
}
