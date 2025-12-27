package com.illit.levels;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class LevelService {

    private final PlayerDataStore store;

    private final int minLevel;
    private final int maxLevel;
    private final int baseExp;

    private final double growthBaseMult;
    private final double growthSqrtCoef;
    private final double growthLinearCoef;

    private final int endgameThreshold;
    private final double endgameHardMult;
    private final double endgameQuadCoef;
    private final double endgameExpCoef;

    public LevelService(IllitLevelsPlugin plugin, PlayerDataStore store) {
        this.store = store;

        FileConfiguration c = plugin.getConfig();
        this.minLevel = Math.max(1, c.getInt("min-level", 1));
        this.maxLevel = Math.max(this.minLevel, c.getInt("max-level", 100));
        this.baseExp = Math.max(1, c.getInt("base-exp", 100));

        this.growthBaseMult = c.getDouble("growth.base-mult", 1.085);
        this.growthSqrtCoef = c.getDouble("growth.sqrt-coef", 0.0025);
        this.growthLinearCoef = c.getDouble("growth.linear-coef", 0.0009);

        this.endgameThreshold = c.getInt("endgame.threshold", 90);
        this.endgameHardMult = c.getDouble("endgame.hard-mult", 1.45);
        this.endgameQuadCoef = c.getDouble("endgame.quad-coef", 0.010);
        this.endgameExpCoef = c.getDouble("endgame.exp-coef", 0.020);
    }

    public void setName(UUID uuid, String name) { store.setName(uuid, name); }

    public int getLevel(UUID uuid) { return store.get(uuid).level(); }
    public long getExp(UUID uuid) { return store.get(uuid).exp(); }

    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }

    public void reset(UUID uuid) {
        PlayerProgress p = store.get(uuid);
        store.put(uuid, new PlayerProgress(minLevel, 0, p.name()));
        store.markDirty(uuid);
    }

    public void setLevel(UUID uuid, int level) {
        PlayerProgress p = store.get(uuid);
        int clamped = clamp(level, minLevel, maxLevel);
        store.put(uuid, new PlayerProgress(clamped, 0, p.name()));
        store.markDirty(uuid);
    }

    public void setExp(UUID uuid, long exp) {
        PlayerProgress p = store.get(uuid);
        int level = clamp(p.level(), minLevel, maxLevel);
        long v = Math.max(0, exp);
        if (level >= maxLevel) v = 0;
        store.put(uuid, new PlayerProgress(level, v, p.name()));
        store.markDirty(uuid);
    }

    public LevelUpResult addExp(UUID uuid, long amount) {
        if (amount <= 0) return new LevelUpResult(0, false);

        PlayerProgress p = store.get(uuid);
        int level = clamp(p.level(), minLevel, maxLevel);
        long exp = Math.max(0, p.exp());
        String name = p.name();

        if (level >= maxLevel) {
            store.put(uuid, new PlayerProgress(maxLevel, 0, name));
            store.markDirty(uuid);
            return new LevelUpResult(0, false);
        }

        exp += amount;

        int gained = 0;
        boolean reachedMax = false;

        while (level < maxLevel) {
            long need = getRequiredExpForNextLevel(level);
            if (exp >= need) {
                exp -= need;
                level++;
                gained++;
                if (level >= maxLevel) {
                    exp = 0;
                    reachedMax = true;
                    break;
                }
            } else break;
        }

        store.put(uuid, new PlayerProgress(level, exp, name));
        store.markDirty(uuid);
        return new LevelUpResult(gained, reachedMax);
    }

    public LevelDownResult removeExp(UUID uuid, long amount) {
        if (amount <= 0) return new LevelDownResult(0, false);

        PlayerProgress p = store.get(uuid);
        int level = clamp(p.level(), minLevel, maxLevel);
        long exp = Math.max(0, p.exp());
        String name = p.name();

        int lost = 0;
        boolean hitMin = false;

        long remaining = amount;

        while (remaining > 0) {
            if (remaining <= exp) {
                exp -= remaining;
                remaining = 0;
                break;
            }

            remaining -= exp;
            exp = 0;

            if (level <= minLevel) {
                hitMin = true;
                remaining = 0;
                break;
            }

            level--;
            lost++;

            long bar = getRequiredExpForNextLevel(level);
            exp = Math.max(0, bar);
        }

        if (level >= maxLevel) exp = 0;

        store.put(uuid, new PlayerProgress(level, exp, name));
        store.markDirty(uuid);

        return new LevelDownResult(lost, hitMin);
    }

    public LevelChangeResult addLevels(UUID uuid, int amount) {
        if (amount <= 0) return new LevelChangeResult(0, false);
        PlayerProgress p = store.get(uuid);
        int before = clamp(p.level(), minLevel, maxLevel);
        int after = clamp(before + amount, minLevel, maxLevel);
        store.put(uuid, new PlayerProgress(after, 0, p.name()));
        store.markDirty(uuid);
        return new LevelChangeResult(after - before, after >= maxLevel);
    }

    public LevelChangeResult removeLevels(UUID uuid, int amount) {
        if (amount <= 0) return new LevelChangeResult(0, false);
        PlayerProgress p = store.get(uuid);
        int before = clamp(p.level(), minLevel, maxLevel);
        int after = clamp(before - amount, minLevel, maxLevel);
        store.put(uuid, new PlayerProgress(after, 0, p.name()));
        store.markDirty(uuid);
        return new LevelChangeResult(before - after, after <= minLevel);
    }

    public long getRequiredExpForNextLevel(int currentLevel) {
        if (currentLevel >= maxLevel) return 0;

        int nextLevel = currentLevel + 1;

        double x = (double) (nextLevel - minLevel);
        if (x < 1.0) x = 1.0;

        double expFactor = Math.pow(growthBaseMult, Math.max(0.0, x - 1.0));
        double bump = 1.0 + growthSqrtCoef * Math.sqrt(x) + growthLinearCoef * x;
        double factor = expFactor * bump;

        if (nextLevel >= endgameThreshold) {
            double k = (nextLevel - endgameThreshold + 1.0);
            double quad = 1.0 + endgameQuadCoef * (k * k);
            double extraExp = Math.exp(endgameExpCoef * k);
            factor = factor * endgameHardMult * quad * extraExp;
        }

        long required = Math.max(1L, Math.round(baseExp * factor));
        if (required < 0) required = Long.MAX_VALUE / 4;
        return required;
    }

    public int getNextLevel(UUID uuid) {
        int level = getLevel(uuid);
        return Math.min(level + 1, maxLevel);
    }

    public long getExpToNext(UUID uuid) {
        int level = getLevel(uuid);
        if (level >= maxLevel) return 0;
        long need = getRequiredExpForNextLevel(level);
        long cur = getExp(uuid);
        return Math.max(0, need - cur);
    }

    public int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public int getLevel(OfflinePlayer player) {
        if (player == null) return minLevel;
        return getLevel(player.getUniqueId());
    }

    public long getExp(OfflinePlayer player) {
        if (player == null) return 0;
        return getExp(player.getUniqueId());
    }

    public int getNextLevel(OfflinePlayer player) {
        if (player == null) return minLevel + 1;
        return getNextLevel(player.getUniqueId());
    }

    public long getExpToNext(OfflinePlayer player) {
        if (player == null) return getRequiredExpForNextLevel(minLevel);
        return getExpToNext(player.getUniqueId());
    }
}
