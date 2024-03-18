package us.ajg0702.leaderboards.rewards;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.clip.placeholderapi.PlaceholderAPI;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;

/**
* @author nahkd123
*/
public class RewardsPoolsManager {
    private LeaderboardPlugin plugin;
    private Map<String, RewardsPool> pools = new HashMap<>();
    private RewardsQueue queue;

    public RewardsPoolsManager(LeaderboardPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.queue = new RewardsQueue(this, new File(plugin.getDataFolder(), "pending_rewards_data.yml"));

        plugin.getLogger().info("Loading all rewards pools...");

        for (String poolId : config.getKeys(false)) {
            ConfigurationSection poolConfig = config.getConfigurationSection(poolId);
            String board = poolConfig.getString("board");
            TimedType timedType = TimedType.valueOf(poolConfig.getString("type", "HOURLY").toUpperCase());
            RewardsPool pool = new RewardsPool(this, poolId, board, timedType);

            for (String rankStr : poolConfig.getConfigurationSection("rewards").getKeys(false)) {
                int rank = Integer.parseInt(rankStr) - 1;
                for (String command : poolConfig.getStringList("rewards." + rankStr)) pool.addReward(rank, command);
            }

            pools.put(poolId, pool);
            plugin.getLogger().info(String.format("  Loaded %s pool that targets %s board with %s reset type", poolId, board, timedType));
        }
    }

    public LeaderboardPlugin getPlugin() {
        return plugin;
    }

    public RewardsQueue getQueue() {
        return queue;
    }

    public Map<String, RewardsPool> getPools() {
        return pools;
    }

    public void handleBoardReset(String boardName, TimedType timedType) {
        for (RewardsPool pool : pools.values()) {
            if (pool.matches(boardName, timedType)) {
                plugin.getLogger().info(String.format("Giving rewards in %s pool...", pool.getId()));
                pool.handleRewards();
            }
        }
    }

    public void handlePlayerJoin(Player player) {
        List<String> commands = queue.popAllRewards(player);
        if (commands.isEmpty()) return;
        giveRewards(player, commands);
    }

    protected void giveRewards(Player player, List<String> commands) {
        plugin.getLogger().info(String.format("  Giving rewards to %s (UUID = %s)", player.getName(), player.getUniqueId()));

        for (String command : commands) {
            String processed = PlaceholderAPI.setPlaceholders(player, command);

            plugin.getLogger().info(String.format("    Running command as console: %s", processed));
            boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processed);

            if (!success) {
                plugin.getLogger().warning(String.format("      Failed to execute command: %s", processed));
            }
        }
    }

    protected void giveRewardsSync(Player player, List<String> commands) {
        if (Bukkit.isPrimaryThread()) giveRewards(player, commands);
        else new BukkitRunnable() {
            @Override
            public void run() {
                giveRewards(player, commands);
            }
        }.runTask(plugin);
    }
}
