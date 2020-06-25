package me.nowaha.tribeshout;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.*;

public final class TribeShout extends JavaPlugin implements Listener {

    HashMap<UUID, BossBar> bars = new HashMap<>();

    List<UUID> toggledPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        saveResource("config.yml", false);

        System.out.println("§aTribeShout successfully started!");

        for (World world : Bukkit.getWorlds()) {
            List<ArmorStand> armorStands = new ArrayList<>(world.getEntitiesByClass(ArmorStand.class));

            for (ArmorStand as : armorStands) {
                if (!as.isVisible() && as.isCustomNameVisible() && as.isSmall() && as.getCustomName() != null) {
                    as.remove();
                }
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (shouts.size() == 0) return;
            if (armorStandList.size() == 0) return;

            for (UUID owner : shouts.keySet()) {
                if (shouts.get(owner) - System.currentTimeMillis() <= 0) {
                    armorStandList.get(owner).remove();
                    armorStandList.remove(owner);
                    shouts.remove(owner);
                    bars.get(owner).removeAll();
                    bars.get(owner).setVisible(false);
                    bars.remove(owner);
                } else {
                    Player player = Bukkit.getPlayer(owner);

                    player.addPassenger(armorStandList.get(owner));

                    if (bars.get(player.getUniqueId()) == null) {
                        BossBar bar = Bukkit.createBossBar(armorStandList.get(owner).getCustomName(), BarColor.RED, BarStyle.SOLID);
                        bar.addPlayer(player);
                        bar.setVisible(true);
                        bars.put(player.getUniqueId(), bar);
                    }

                    BossBar bar = bars.get(player.getUniqueId());

                    bar.setProgress((double)(shouts.get(player.getUniqueId()) - new Date().getTime()) / (double)(getConfig().getInt("shoutduration") * 1000));
                }
            }
        }, 0, 1);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (UUID toggled : toggledPlayers) {
                Player player = Bukkit.getPlayer(toggled);
                if (player != null) {
                    player.sendActionBar("§eShouting is ON.");
                }
            }
        }, 0, 40);
    }

    @Override
    public Logger getSLF4JLogger() {
        return null;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (ArmorStand as : armorStandList.values()) {
            as.remove();
        }

        for (World world : Bukkit.getWorlds()) {
            List<ArmorStand> armorStands = new ArrayList<>(world.getEntitiesByClass(ArmorStand.class));

            for (ArmorStand as : armorStands) {
                if (!as.isVisible() && as.isCustomNameVisible() && as.isSmall() && as.getCustomName() != null) {
                    as.remove();
                }
            }
        }

        System.out.println("§cTribeShout successfully shut down!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        if (p.hasPermission("essentials.kits.hero")) {
            if (command.getLabel().equalsIgnoreCase("shout")) {
                if (shouts.containsKey(p.getUniqueId())) {
                    p.sendMessage("§cPlease wait before shouting again.");
                    return true;
                }
                shout(p, String.join(" ", args));
            } else if (command.getLabel().equalsIgnoreCase("shouttoggle")) {
                if (toggledPlayers.contains(p.getUniqueId())) {
                    toggledPlayers.remove(p.getUniqueId());
                    p.sendMessage("§eToggled OFF shouting.");
                } else {
                    toggledPlayers.add(p.getUniqueId());
                    p.sendMessage("§eToggled ON shouting. All your messages will now be shouts.");
                }
            }
        } else {
            p.sendMessage("§cYou cannot use this command.");
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(PlayerChatEvent e) {
        Player p = e.getPlayer();
        if (toggledPlayers.contains(p.getUniqueId())) {
            if (p.hasPermission("essentials.kits.hero")) {
                e.setCancelled(true);

                if (armorStandList.containsKey(e.getPlayer().getUniqueId())) {
                    try {
                        armorStandList.get(e.getPlayer().getUniqueId()).remove();
                    } catch (Exception ignored) {}
                    armorStandList.remove(e.getPlayer().getUniqueId());
                    shouts.remove(e.getPlayer().getUniqueId());
                    try {
                        bars.get(e.getPlayer().getUniqueId()).removeAll();
                        bars.get(e.getPlayer().getUniqueId()).setVisible(false);
                    } catch (Exception ignored) {}

                    bars.remove(e.getPlayer().getUniqueId());
                }

                shout(p, e.getMessage());
            } else {
                toggledPlayers.remove(p.getUniqueId());
            }
        }
    }

    HashMap<UUID, Long> shouts = new HashMap<>();
    Map<UUID, ArmorStand> armorStandList = new HashMap();

    public void shout(Player player, String message) {
        if (message.length() >= getConfig().getInt("shoutlimit")) {
            player.sendMessage("§cThat message is too long!");
            return;
        }

        if (shouts.containsKey(player.getUniqueId())) {
            shouts.remove(player.getUniqueId());
        }

        ArmorStand as = (ArmorStand) player.getLocation().getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        as.setCustomNameVisible(true);
        as.setCustomName(message);
        as.setGravity(false);
        as.setVisible(false);
        as.setSmall(true);

        player.addPassenger(as);
        armorStandList.put(player.getUniqueId(), as);
        shouts.put(player.getUniqueId(), new Date().getTime() + (1000 * getConfig().getInt("shoutduration", 10)));

        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.getWorld().equals(player.getWorld())) {
                if (nearby.getLocation().distance(player.getLocation()) < getConfig().getInt("shoutrange", 100)) {
                    nearby.playSound(nearby.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 10, 2);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!e.getRightClicked().isVisible() && e.getRightClicked().isCustomNameVisible() && e.getRightClicked().isSmall() && e.getRightClicked().getCustomName() != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (armorStandList.containsKey(e.getPlayer().getUniqueId())) {
            try {
                armorStandList.get(e.getPlayer().getUniqueId()).remove();
            } catch (Exception ignored) {}
            armorStandList.remove(e.getPlayer().getUniqueId());
            shouts.remove(e.getPlayer().getUniqueId());
            try {
                bars.get(e.getPlayer().getUniqueId()).removeAll();
                bars.get(e.getPlayer().getUniqueId()).setVisible(false);
            } catch (Exception ignored) {}

            bars.remove(e.getPlayer().getUniqueId());
        }
    }
}
