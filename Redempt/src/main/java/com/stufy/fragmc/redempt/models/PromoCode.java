package com.stufy.fragmc.redempt.models;

import java.util.UUID;

public class PromoCode {

    private final String code;
    private final String name;
    private final double money;
    private final long expiryTime; // -1 for infinite
    private final int maxUses; // -1 for infinite
    private int currentUses;
    private final long createdTime;

    public PromoCode(String code, String name, double money, long expiryTime, int maxUses, int currentUses, long createdTime) {
        this.code = code;
        this.name = name;
        this.money = money;
        this.expiryTime = expiryTime;
        this.maxUses = maxUses;
        this.currentUses = currentUses;
        this.createdTime = createdTime;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getMoney() {
        return money;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void incrementUses() {
        currentUses++;
    }

    public boolean isExpired() {
        if (expiryTime == -1) {
            return false;
        }
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean hasReachedMaxUses() {
        if (maxUses == -1) {
            return false;
        }
        return currentUses >= maxUses;
    }

    public boolean isValid() {
        return !isExpired() && !hasReachedMaxUses();
    }
}