package com.stufy.fragmc.redempt.managers;

import com.stufy.fragmc.redempt.Redempt;
import com.stufy.fragmc.redempt.database.DatabaseManager;
import com.stufy.fragmc.redempt.models.PromoCode;
import com.stufy.fragmc.redempt.utils.PromoCodeGenerator;

import java.sql.SQLException;
import java.util.UUID;

public class PromoCodeManager {

    private final Redempt plugin;
    private final DatabaseManager databaseManager;

    public PromoCodeManager(Redempt plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public PromoCode createPromoCode(String name, double money, long expiryTime, int maxUses) throws SQLException {
        String code;
        PromoCode existingCode;

        // Generate unique code
        do {
            code = PromoCodeGenerator.generateCode();
            existingCode = databaseManager.getPromoCode(code);
        } while (existingCode != null);

        PromoCode promoCode = new PromoCode(
                code,
                name,
                money,
                expiryTime,
                maxUses,
                0,
                System.currentTimeMillis()
        );

        databaseManager.savePromoCode(promoCode);
        return promoCode;
    }

    private String normalizeCode(String code) {
        String normalized = code.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (normalized.length() == 16) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8, 12) + "-" + normalized.substring(12, 16);
        } else if (normalized.length() == 12) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8, 12);
        }
        return code.toUpperCase();
    }

    public PromoCode getPromoCode(String code) throws SQLException {
        return databaseManager.getPromoCode(normalizeCode(code));
    }

    public boolean redeemCode(String code, UUID playerUUID) throws SQLException {
        PromoCode promoCode = getPromoCode(code);

        if (promoCode == null) {
            return false;
        }

        if (!promoCode.isValid()) {
            return false;
        }

        if (databaseManager.hasPlayerRedeemed(promoCode.getCode(), playerUUID)) {
            return false;
        }

        // Record redemption
        databaseManager.recordRedemption(promoCode.getCode(), playerUUID);

        // Update uses
        promoCode.incrementUses();
        databaseManager.updatePromoCodeUses(promoCode.getCode(), promoCode.getCurrentUses());

        return true;
    }

    public boolean hasPlayerRedeemed(String code, UUID playerUUID) throws SQLException {
        return databaseManager.hasPlayerRedeemed(normalizeCode(code), playerUUID);
    }

    public void deletePromoCode(String code) throws SQLException {
        databaseManager.deletePromoCode(normalizeCode(code));
    }

    public void renewPromoCode(String code, long newExpiryTime) throws SQLException {
        databaseManager.updatePromoCodeExpiry(normalizeCode(code), newExpiryTime);
    }
}