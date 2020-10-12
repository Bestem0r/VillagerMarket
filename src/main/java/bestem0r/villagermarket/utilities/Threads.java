package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Threads {

    /** Thread runs save() method for all Villager Shops */
    public void beginSaveThread() {
        long interval = 20 * 60 * VMPlugin.getInstance().getConfig().getLong("auto_save_interval");
        Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                villagerShop.save();
            }
        }, 20L, interval);
    }

    /** Thread check if rent time has expired and runs abandon() method */
    public void beginExpireThread() {
        long interval = 20 * VMPlugin.getInstance().getConfig().getLong("expire_check_interval");
        FileConfiguration config = VMPlugin.getInstance().getConfig();

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                if (!(villagerShop instanceof PlayerShop)) continue;
                PlayerShop playerShop = (PlayerShop) villagerShop;

                if (playerShop.hasExpired()) {
                    Player player = Bukkit.getPlayer(UUID.fromString(villagerShop.getOwnerUUID()));
                    if (player != null) {
                        player.sendMessage(new Color.Builder().path("messages.expired").addPrefix().build());
                        player.playSound(player.getLocation(), Sound.valueOf(config.getString("sounds.expired")), 1, 1);
                    }
                    playerShop.abandon();
                }
            }
        }, 20L, interval);
    }
}
