package com.illit.levels;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TopService {

    public record TopEntry(UUID uuid, String name, int level, long exp) {}

    private final PlayerDataStore store;
    private final int cacheSeconds;

    private volatile List<TopEntry> cached = List.of();
    private final AtomicLong lastRefreshMs = new AtomicLong(0);

    public TopService(IllitLevelsPlugin plugin, PlayerDataStore store) {
        this.store = store;
        this.cacheSeconds = Math.max(5, plugin.getConfig().getInt("top.cache-seconds", 15));
    }

    public List<TopEntry> top10() {
        refreshIfNeeded();
        return cached;
    }

    public TopEntry getRank(int rank1based) {
        if (rank1based < 1 || rank1based > 10) return null;
        List<TopEntry> t = top10();
        return (rank1based <= t.size()) ? t.get(rank1based - 1) : null;
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastRefreshMs.get();
        if (now - last < cacheSeconds * 1000L) return;
        if (!lastRefreshMs.compareAndSet(last, now)) return;

        Map<UUID, PlayerProgress> all = store.snapshotAll();
        List<TopEntry> list = new ArrayList<>(all.size());

        for (var e : all.entrySet()) {
            UUID uuid = e.getKey();
            PlayerProgress p = e.getValue();
            String name = p.name();
            if (name == null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                name = op.getName();
                if (name != null) store.setName(uuid, name);
            }
            list.add(new TopEntry(uuid, name != null ? name : "unknown", p.level(), p.exp()));
        }

        list.sort((a, b) -> {
            int cmp = Integer.compare(b.level(), a.level());
            if (cmp != 0) return cmp;
            cmp = Long.compare(b.exp(), a.exp());
            if (cmp != 0) return cmp;
            return a.uuid().toString().compareTo(b.uuid().toString());
        });

        if (list.size() > 10) list = list.subList(0, 10);
        cached = List.copyOf(list);
    }
}
