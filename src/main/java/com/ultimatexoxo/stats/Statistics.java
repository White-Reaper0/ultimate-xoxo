package com.ultimatexox.stats;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Very simple local statistics for Human vs AI games.
 *
 * Stored as a properties file in the user's home directory:
 *   ~/.ultimate-xox/stats.properties
 *
 * Only four counters are tracked — exactly what Version 1 needs:
 * games played, human wins, AI wins and draws. A win rate is derived.
 */
public final class Statistics {

    /** Outcome of one finished Human vs AI game. */
    public enum Result { HUMAN_WIN, AI_WIN, DRAW }

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".ultimate-xox", "stats.properties");

    private int gamesPlayed;
    private int humanWins;
    private int aiWins;
    private int draws;

    public Statistics() {
        load();
    }

    public synchronized void recordResult(Result result) {
        gamesPlayed++;
        switch (result) {
            case HUMAN_WIN -> humanWins++;
            case AI_WIN -> aiWins++;
            case DRAW -> draws++;
        }
        save();
    }

    public synchronized void reset() {
        gamesPlayed = 0;
        humanWins = 0;
        aiWins = 0;
        draws = 0;
        save();
    }

    public synchronized int getGamesPlayed() { return gamesPlayed; }
    public synchronized int getHumanWins()   { return humanWins; }
    public synchronized int getAiWins()      { return aiWins; }
    public synchronized int getDraws()       { return draws; }

    /** Percentage of decided games won by the human, or -1 when nothing played yet. */
    public synchronized double getHumanWinRatePercent() {
        int decided = humanWins + aiWins;
        return decided == 0 ? -1 : 100.0 * humanWins / decided;
    }

    // ── Persistence ─────────────────────────────────────────────────────

    private void load() {
        if (!Files.exists(FILE)) return;
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            gamesPlayed = Integer.parseInt(p.getProperty("gamesPlayed", "0"));
            humanWins   = Integer.parseInt(p.getProperty("humanWins", "0"));
            aiWins      = Integer.parseInt(p.getProperty("aiWins", "0"));
            draws       = Integer.parseInt(p.getProperty("draws", "0"));
        } catch (IOException | NumberFormatException e) {
            // Corrupt or unreadable file: start from zero rather than crash the
            // game over saved statistics. The message is printed so the cause is visible.
            System.err.println("[Statistics] Could not read " + FILE + ": " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("gamesPlayed", String.valueOf(gamesPlayed));
            p.setProperty("humanWins", String.valueOf(humanWins));
            p.setProperty("aiWins", String.valueOf(aiWins));
            p.setProperty("draws", String.valueOf(draws));
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Ultimate XOX statistics (Human vs AI)");
            }
        } catch (IOException e) {
            System.err.println("[Statistics] Could not write " + FILE + ": " + e.getMessage());
        }
    }
}
