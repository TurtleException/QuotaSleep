package de.eldritch.spigot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        QuotaSleep.singleton.refresh(event.getBed().getWorld());
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        QuotaSleep.singleton.refresh(event.getBed().getWorld());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        QuotaSleep.singleton.refresh(event.getPlayer().getWorld());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        QuotaSleep.singleton.refresh(event.getPlayer().getWorld());
    }
}
