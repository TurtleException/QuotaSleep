package de.eldritch.spigot.util;

import de.eldritch.spigot.QuotaSleep;

public abstract class Runnable implements java.lang.Runnable {
    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public void cancel() {
        QuotaSleep.singleton.getServer().getScheduler().cancelTask(id);
    }
}