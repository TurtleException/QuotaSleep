package de.turtleboi.spigot.quotasleep;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class EventListener implements Listener {
    private final QuotaSleep plugin;

    EventListener(QuotaSleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        plugin.scheduleWorldUpdate(event.getBed().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        plugin.scheduleWorldUpdate(event.getBed().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.scheduleWorldUpdate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.scheduleWorldUpdate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        plugin.scheduleWorldUpdate(event.getFrom());
        plugin.scheduleWorldUpdate(event.getPlayer().getWorld());
    }
}
