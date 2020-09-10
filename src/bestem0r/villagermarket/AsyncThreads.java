package bestem0r.villagermarket;

import org.bukkit.Bukkit;

public class AsyncThreads {

    public void beginSaveThread(DataManager dataManager) {
        long interval = 20 * 60 * VMPlugin.getInstance().getConfig().getLong("auto_save_interval");
        Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(VMPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                for (String entityUUID : dataManager.getVillagers().keySet()) {
                    dataManager.getVillagers().get(entityUUID).save();
                }
            }
        }, 20L, interval);
    }
}
