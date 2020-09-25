package bestem0r.villagermarket;

import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class AsyncThreads {

    public void beginSaveThread(DataManager dataManager) {
        long interval = 20 * 60 * VMPlugin.getInstance().getConfig().getLong("auto_save_interval");
        Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (String entityUUID : dataManager.getVillagers().keySet()) {
                dataManager.getVillagers().get(entityUUID).save();
            }
        }, 20L, interval);
    }

    public void beginExpireThread(DataManager dataManager, FileConfiguration config) {
        long interval = 20 * VMPlugin.getInstance().getConfig().getLong("expire_check_interval");
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (String entityUUID : dataManager.getVillagers().keySet()) {
                VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);
                if (villagerShop.getExpireDate().getTime() == 0L) continue;

                if (villagerShop.getExpireDate().before(new Date())) {
                    Player player = Bukkit.getPlayer(UUID.fromString(villagerShop.getOwnerUUID()));
                    if (player != null) {
                        player.sendMessage(new Color.Builder().path("messages.expired").addPrefix().build());
                        player.playSound(player.getLocation(), Sound.valueOf(config.getString("sounds.expired")), 1, 1);
                    }
                    villagerShop.abandon();
                }
            }
        }, 20L, interval);
    }
}
