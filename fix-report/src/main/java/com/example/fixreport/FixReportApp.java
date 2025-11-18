package com.example.fixreport;

import javax.sql.DataSource;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FixReportApp {

    private final DataSource dataSource;

    public FixReportApp(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // -------------------------------------------------------
    // PUBLIC API: Generate a full report
    // -------------------------------------------------------
    public void generateReport(String outputPath) throws Exception {

        try (Connection conn = dataSource.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

            writer.println("==============================");
            writer.println("FIX Message Analysis Report");
            writer.println("Generated: " + now());
            writer.println("==============================");
            writer.println();

            // TOTAL COUNTS
            long totalMessages = count(conn, "SELECT COUNT(*) FROM fix_messages");
            writer.println("Total FIX messages: " + totalMessages);
            writer.println("Unique Symbols: " + count(conn, "SELECT COUNT(DISTINCT symbol) FROM fix_messages"));
            writer.println();

            // TOP SYMBOLS BY VOLUME
            writeTopSymbols(writer, conn);

            // BUY/SELL BREAKDOWN
            writeSideBreakdown(writer, conn, totalMessages);

            // MESSAGE TYPES (tag 35)
            writer.println();
            writer.println("Message Types:");
            summarizeTag(writer, conn, "msg_type");
        }

        System.out.println("📄 Report written to: " + outputPath);
    }

    // -------------------------------------------------------
    // SECTION: Top Symbols
    // -------------------------------------------------------
    private void writeTopSymbols(PrintWriter writer, Connection conn) throws SQLException {

        writer.println("Top Symbols by Trade Volume:");

        String sql =
                "SELECT symbol, SUM(order_qty::int) AS volume, AVG(price::numeric) AS avg_price " +
                "FROM fix_messages " +
                "WHERE symbol IS NOT NULL AND order_qty IS NOT NULL AND price IS NOT NULL " +
                "GROUP BY symbol " +
                "HAVING SUM(order_qty::int) > 0 " +
                "ORDER BY volume DESC " +
                "LIMIT 5;";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int rank = 1;
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                long volume = rs.getLong("volume");
                double avgPrice = rs.getDouble("avg_price");

                writer.printf("%d. %s — %,d shares — Avg Price: %.2f%n",
                        rank++, symbol, volume, avgPrice);
            }
        }

        writer.println();
    }

    // -------------------------------------------------------
    // SECTION: Side Breakdown
    // -------------------------------------------------------
    private void writeSideBreakdown(PrintWriter writer, Connection conn, long total) throws SQLException {

        long buy = count(conn, "SELECT COUNT(*) FROM fix_messages WHERE side='1'");
        long sell = count(conn, "SELECT COUNT(*) FROM fix_messages WHERE side='2'");
        long missing = total - buy - sell;

        writer.println("Side Breakdown:");
        writer.printf("- BUY  : %.1f%%%n", percent(buy, total));
        writer.printf("- SELL : %.1f%%%n", percent(sell, total));
        writer.printf("- MISSING : %.1f%%%n", percent(missing, total));
        writer.println();
    }

    // -------------------------------------------------------
    // SUMMARY OF ANY TAG (e.g., MsgType = 35)
    // -------------------------------------------------------
    private void summarizeTag(PrintWriter writer, Connection conn, String tag) throws SQLException {

        String sql = String.format(
                "SELECT %s AS tag_value, COUNT(*) AS cnt " +
                "FROM fix_messages " +
                "WHERE %s IS NOT NULL " +
                "GROUP BY %s " +
                "ORDER BY cnt DESC;",
                tag, tag, tag
        );

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                writer.printf("- %s : %d%n",
                        rs.getString("tag_value"),
                        rs.getLong("cnt"));
            }
        }
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------
    private long count(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private double percent(long part, long total) {
        if (total == 0) return 0.0;
        return (part * 100.0) / total;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
