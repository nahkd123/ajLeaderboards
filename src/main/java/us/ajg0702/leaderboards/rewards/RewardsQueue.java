package us.ajg0702.leaderboards.rewards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * <p>Handles the rewards queue. This includes saving to and loading from file.</p>
 * @author nahkd123
 */
public class RewardsQueue {
    private File file;
    private YamlConfiguration configRoot;
    private RewardsPoolsManager poolsManager;

    public RewardsQueue(RewardsPoolsManager poolsManager, File file) {
        // Could have been a database here
        this.poolsManager = poolsManager;
        this.file = file;
        this.configRoot = YamlConfiguration.loadConfiguration(file);
    }

    public File getFile() {
        return file;
    }

    public RewardsPoolsManager getPoolsManager() {
        return poolsManager;
    }

    public void enqueue(UUID uuid, RewardsPool pool, int rank) {
        List<String> pendingPoolNames = new ArrayList<>();
        if (configRoot.contains(uuid.toString())) pendingPoolNames.addAll(configRoot.getStringList(uuid.toString()));
        pendingPoolNames.add(pack(pool.getId(), rank));
        configRoot.set(uuid.toString(), pendingPoolNames);
    }

    public void enqueueSync(UUID uuid, RewardsPool pool, int rank) {
        if (Bukkit.isPrimaryThread()) enqueue(uuid, pool, rank);
        else new BukkitRunnable() {
            @Override
            public void run() {
                enqueue(uuid, pool, rank);
            }
        }.runTask(poolsManager.getPlugin());
    }

    public List<String> popAllRewards(Player player) {
        if (!configRoot.contains(player.getUniqueId().toString())) return Collections.emptyList();
        List<String> allCommands = configRoot.getStringList(player.getUniqueId().toString()).stream()
            .map(packed -> unpack(packed))
            .flatMap(pair -> poolsManager.getPools().get(pair.poolId).getRewards().get(pair.rank).stream())
            .map(cmd -> PlaceholderAPI.setPlaceholders(player, cmd))
            .collect(Collectors.toList());
        configRoot.set(player.getUniqueId().toString(), null);
        return allCommands;
    }

    public void save() {
        try {
            configRoot.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            poolsManager.getPlugin().getLogger().warning("Unable to save rewards queue file");
        }
    }

    private static String pack(String poolId, int rank) {
        return poolId + "::" + rank;
    }

    private static Unpacked unpack(String s) {
        String[] ss = s.split("::", 2);
        return new Unpacked(ss[0], Integer.parseInt(ss[1]));
    }

    private static class Unpacked {
        public String poolId;
        public int rank;

        public Unpacked(String poolId, int rank) {
            this.poolId = poolId;
            this.rank = rank;
        }
    }
}
