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

    public PromoCode getPromoCode(String code) throws SQLException {
        return databaseManager.getPromoCode(code.toUpperCase());
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
        return databaseManager.hasPlayerRedeemed(code.toUpperCase(), playerUUID);
    }

    public void deletePromoCode(String code) throws SQLException {
        databaseManager.deletePromoCode(code.toUpperCase());
    }

    public void renewPromoCode(String code, long newExpiryTime) throws SQLException {
        databaseManager.updatePromoCodeExpiry(code.toUpperCase(), newExpiryTime);
    }
}