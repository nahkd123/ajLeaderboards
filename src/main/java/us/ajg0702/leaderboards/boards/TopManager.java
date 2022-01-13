package us.ajg0702.leaderboards.boards;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TopManager {


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl) {
        plugin = pl;
    }

    private final HashMap<String, HashMap<TimedType, HashMap<Integer, Long>>> lastGet = new HashMap<>();
    private final HashMap<String, HashMap<TimedType, HashMap<Integer, StatEntry>>> cache = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param position The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStat(int position, String board, TimedType type) {
        if(!cache.containsKey(board)) {
            cache.put(board, new HashMap<>());
        }
        if(!lastGet.containsKey(board)) {
            lastGet.put(board, new HashMap<>());
        }

        if(!cache.get(board).containsKey(type)) {
            cache.get(board).put(type, new HashMap<>());
        }
        if(!lastGet.get(board).containsKey(type)) {
            lastGet.get(board).put(type, new HashMap<>());
        }


        if(cache.get(board).get(type).containsKey(position)) {
            if(System.currentTimeMillis() - lastGet.get(board).get(type).get(position) > 5000) {
                lastGet.get(board).get(type).put(position, System.currentTimeMillis());
                fetchPositionAsync(position, board, type);
            }
            return cache.get(board).get(type).get(position);
        }

        lastGet.get(board).get(type).put(position, System.currentTimeMillis());
        return fetchPosition(position, board, type);
    }

    private void fetchPositionAsync(int position, String board, TimedType type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchPosition(position, board, type));
    }
    private StatEntry fetchPosition(int position, String board, TimedType type) {
        StatEntry te = plugin.getCache().getStat(position, board, type);
        cache.get(board).get(type).put(position, te);
        return te;
    }


    private final HashMap<String, HashMap<TimedType, HashMap<OfflinePlayer, Long>>> lastGetSE = new HashMap<>();
    private final HashMap<String, HashMap<TimedType, HashMap<OfflinePlayer, StatEntry>>> cacheSE = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param player The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
        if(!cacheSE.containsKey(board)) {
            cacheSE.put(board, new HashMap<>());
        }
        if(!lastGetSE.containsKey(board)) {
            lastGetSE.put(board, new HashMap<>());
        }

        if(!cacheSE.get(board).containsKey(type)) {
            cacheSE.get(board).put(type, new HashMap<>());
        }
        if(!lastGetSE.get(board).containsKey(type)) {
            lastGetSE.get(board).put(type, new HashMap<>());
        }

        if(cacheSE.get(board).get(type).containsKey(player)) {
            if(System.currentTimeMillis() - lastGetSE.get(board).get(type).get(player) > 5000) {
                lastGetSE.get(board).get(type).put(player, System.currentTimeMillis());
                fetchStatEntryAsync(player, board, type);
            }
            return cacheSE.get(board).get(type).get(player);
        }

        lastGetSE.get(board).get(type).put(player, System.currentTimeMillis());
        return fetchStatEntry(player, board, type);
    }

    private void fetchStatEntryAsync(OfflinePlayer player, String board, TimedType type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchStatEntry(player, board, type));
    }
    private StatEntry fetchStatEntry(OfflinePlayer player, String board, TimedType type) {
        StatEntry te = plugin.getCache().getStatEntry(player, board, type);
        cacheSE.get(board).get(type).put(player, te);
        return te;
    }

    List<String> boardCache;
    long lastGetBoard = 0;
    public List<String> getBoards() {
        if(boardCache == null) return fetchBoards();

        if(System.currentTimeMillis() - lastGetBoard > 1000) {
            lastGetBoard = System.currentTimeMillis();
            fetchBoardsAsync();
        }
        return boardCache;
    }

    private void fetchBoardsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::fetchBoards);
    }
    private List<String> fetchBoards() {
        boardCache = plugin.getCache().getBoards();
        return boardCache;
    }

}
