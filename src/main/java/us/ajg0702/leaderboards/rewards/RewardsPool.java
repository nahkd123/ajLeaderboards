package us.ajg0702.leaderboards.rewards;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;

public class RewardsPool {
    private RewardsPoolsManager poolsManager;
    private String id, board;
    private TimedType timedType;
    private List<List<String>> rewards = new ArrayList<>();

    public RewardsPool(RewardsPoolsManager poolsManager, String id, String board, TimedType timedType) {
        this.poolsManager = poolsManager;
        this.id = id;
        this.board = board;
        this.timedType = timedType;
    }

    public RewardsPoolsManager getPoolsManager() {
        return poolsManager;
    }

    public String getId() {
        return id;
    }

    public String getBoard() {
        return board;
    }

    public TimedType getTimedType() {
        return timedType;
    }

    public boolean matches(String boardName, TimedType timedType) {
        return this.board.equals(boardName) && this.timedType == timedType;
    }

    public void addReward(int rank, String command) {
        while (this.rewards.size() < rank + 1) this.rewards.add(null);
        List<String> rewards = this.rewards.get(rank);
        if (rewards == null) this.rewards.set(rank, rewards = new ArrayList<>());
        rewards.add(command);
    }

    public void handleRewards() {
        for (int rank = 0; rank < this.rewards.size(); rank++) {
            List<String> rankRewards = this.rewards.get(rank);
            if (rankRewards == null) continue;

            // IMPORTANT!
            // You must add 0-indexed rank by 1
            StatEntry entry = poolsManager.getPlugin().getCache().getStat(rank + 1, board, timedType);

            if (entry == null || entry.getScore() <= -1d || entry.getPlayerID() == null) {
                poolsManager.getPlugin().getLogger().warning(String.format("  Board %s at position #%s does not have a player!", board, rank + 1));
                continue;
            }

            Player player = poolsManager.getPlugin().getServer().getPlayer(entry.getPlayerID());

            if (player == null || !player.isOnline()) {
                poolsManager.getPlugin().getLogger().info(String.format("  Player with UUID = %s is not online; rewards for rank #%s will be given when they went online", entry.getPlayerID(), rank + 1));
                poolsManager.getQueue().enqueue(entry.getPlayerID(), this, rank);
            } else {
                poolsManager.giveRewards(player, rankRewards);
            }
        }
    }

    public List<List<String>> getRewards() {
        return rewards;
    }
}
