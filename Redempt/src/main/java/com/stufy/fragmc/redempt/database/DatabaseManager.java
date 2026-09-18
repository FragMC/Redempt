package com.stufy.fragmc.redempt.database;

import com.stufy.fragmc.redempt.Redempt;
import com.stufy.fragmc.redempt.models.PromoCode;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final Redempt plugin;
    private Connection connection;

    public DatabaseManager(Redempt plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/promocodes.db";
            connection = DriverManager.getConnection(url);

            createTables();
            plugin.getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createPromoCodesTable = "CREATE TABLE IF NOT EXISTS promo_codes (" +
                "code TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "money REAL NOT NULL," +
                "expiry_time INTEGER NOT NULL," +
                "max_uses INTEGER NOT NULL," +
                "current_uses INTEGER NOT NULL," +
                "created_time INTEGER NOT NULL" +
                ")";

        String createRedemptionsTable = "CREATE TABLE IF NOT EXISTS redemptions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "redeemed_time INTEGER NOT NULL," +
                "FOREIGN KEY (code) REFERENCES promo_codes(code)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPromoCodesTable);
            stmt.execute(createRedemptionsTable);
        }
    }

    public void savePromoCode(PromoCode promoCode) throws SQLException {
        String sql = "INSERT OR REPLACE INTO promo_codes (code, name, money, expiry_time, max_uses, current_uses, created_time) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, promoCode.getCode());
            pstmt.setString(2, promoCode.getName());
            pstmt.setDouble(3, promoCode.getMoney());
            pstmt.setLong(4, promoCode.getExpiryTime());
            pstmt.setInt(5, promoCode.getMaxUses());
            pstmt.setInt(6, promoCode.getCurrentUses());
            pstmt.setLong(7, promoCode.getCreatedTime());
            pstmt.executeUpdate();
        }
    }

    public PromoCode getPromoCode(String code) throws SQLException {
        String sql = "SELECT * FROM promo_codes WHERE code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new PromoCode(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getDouble("money"),
                        rs.getLong("expiry_time"),
                        rs.getInt("max_uses"),
                        rs.getInt("current_uses"),
                        rs.getLong("created_time")
                );
            }
        }

        return null;
    }

    public boolean hasPlayerRedeemed(String code, UUID playerUUID) throws SQLException {
        String sql = "SELECT COUNT(*) FROM redemptions WHERE code = ? AND player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    public void recordRedemption(String code, UUID playerUUID) throws SQLException {
        String sql = "INSERT INTO redemptions (code, player_uuid, redeemed_time) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, playerUUID.toString());
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }

    public void updatePromoCodeUses(String code, int uses) throws SQLException {
        String sql = "UPDATE promo_codes SET current_uses = ? WHERE code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, uses);
            pstmt.setString(2, code);
            pstmt.executeUpdate();
        }
    }

    public void updatePromoCodeExpiry(String code, long expiryTime) throws SQLException {
        String sql = "UPDATE promo_codes SET expiry_time = ? WHERE code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, expiryTime);
            pstmt.setString(2, code);
            pstmt.executeUpdate();
        }
    }

    public List<PromoCode> getAllPromoCodes() throws SQLException {
        List<PromoCode> codes = new ArrayList<>();
        String sql = "SELECT * FROM promo_codes";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                codes.add(new PromoCode(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getDouble("money"),
                        rs.getLong("expiry_time"),
                        rs.getInt("max_uses"),
                        rs.getInt("current_uses"),
                        rs.getLong("created_time")
                ));
            }
        }

        return codes;
    }

    public void deletePromoCode(String code) throws SQLException {
        String deleteRedemptions = "DELETE FROM redemptions WHERE code = ?";
        String deletePromoCode = "DELETE FROM promo_codes WHERE code = ?";

        try (PreparedStatement pstmt1 = connection.prepareStatement(deleteRedemptions);
             PreparedStatement pstmt2 = connection.prepareStatement(deletePromoCode)) {

            pstmt1.setString(1, code);
            pstmt1.executeUpdate();

            pstmt2.setString(1, code);
            pstmt2.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
}