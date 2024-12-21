package com.example.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class PredictionEngine {
    private static final String TAG = "PredictionEngine";
    private AssetManager assetManager;
    private String currentSeason;
    private List<HistoricalMatch> historicalMatches;
    private List<FutureMatch> futureMatches;
    private List<HistoricalMatch> currentSeasonMatches;
    private static final String HISTORICAL_DATA_FILE = "2015-2024_Bundesligadata.csv";
    private static final String GAMEPLAN_FILE = "gameplan_24_25.csv";

    public PredictionEngine(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.historicalMatches = new ArrayList<>();
        this.currentSeasonMatches = new ArrayList<>();
        this.futureMatches = new ArrayList<>();
    }

    public void loadCurrentSeason() {
        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line = reader.readLine(); // Skip header
            if ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                currentSeason = columns[0].trim();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading current season", e);
            currentSeason = "2024/2025"; // Fallback value
        }
    }

    public void loadHistoricalData(Context context) {
        historicalMatches.clear();
        currentSeasonMatches.clear();

        File file = new File(context.getFilesDir(), HISTORICAL_DATA_FILE);

        if (!file.exists()) {
            Log.e(TAG, "Historical data file does not exist: " + HISTORICAL_DATA_FILE);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                try {
                    HistoricalMatch match = parseHistoricalMatch(line);
                    if (match.season.equals(currentSeason)) {
                        currentSeasonMatches.add(match);
                    } else {
                        historicalMatches.add(match);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing line: " + line, e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading historical data from internal storage", e);
        }
    }


    private HistoricalMatch parseHistoricalMatch(String line) {
        String[] columns = line.split(",");
        return new HistoricalMatch(
                columns[0].trim(),                    // season
                Integer.parseInt(columns[1].trim()),  // gameday
                columns[4].trim(),                    // homeTeam
                columns[5].trim(),                    // awayTeam
                Integer.parseInt(columns[6].trim()),  // homeGoals
                Integer.parseInt(columns[7].trim())   // awayGoals
        );
    }

    public List<String> getAvailableGamedays() {
        Set<Integer> gamedays = new TreeSet<>();
        int currentGameday = getCurrentGameday();

        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns[0].trim().equals(currentSeason)) {
                    int gameday = Integer.parseInt(columns[1].trim());
                    if (gameday > currentGameday) {  // Nur zukünftige Spieltage
                        gamedays.add(gameday);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading fixtures data", e);
        }

        return gamedays.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }
    private int getCurrentGameday() {
        int maxPlayedGameday = 0;
        for (HistoricalMatch match : currentSeasonMatches) {
            if (match.gameday > maxPlayedGameday) {
                maxPlayedGameday = match.gameday;
            }
        }
        return maxPlayedGameday;
    }

    public List<FutureMatch> calculatePredictions(int selectedGameday) {
        loadFutureMatches(selectedGameday);

        for (FutureMatch match : futureMatches) {
            TeamStats stats = calculateTeamStatistics(match.homeTeam, match.awayTeam, selectedGameday);
            double[] probabilities = calculateWinProbabilities(stats);
            double[] avgGoals = calculateAverageGoals(match.homeTeam, match.awayTeam);
            double[] overUnderProbs = calculateOverUnderProbabilities(avgGoals[2]);

            updateMatchPredictions(match, probabilities, avgGoals, overUnderProbs);
        }

        return futureMatches;
    }

    private void loadFutureMatches(int selectedGameday) {
        futureMatches.clear();

        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (isMatchForSelectedGameday(columns, selectedGameday)) {
                    futureMatches.add(createFutureMatch(columns));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading fixtures data", e);
        }
    }

    private boolean isMatchForSelectedGameday(String[] columns, int selectedGameday) {
        return columns[0].trim().equals(currentSeason) &&
                Integer.parseInt(columns[1].trim()) == selectedGameday;
    }

    private FutureMatch createFutureMatch(String[] columns) {
        return new FutureMatch(
                columns[2].trim(),  // date
                columns[4].trim(),  // homeTeam
                columns[5].trim()   // awayTeam
        );
    }

    private TeamStats calculateTeamStatistics(String homeTeam, String awayTeam, int selectedGameday) {
        // Calculate weights based on selected gameday
        double[] weights = calculateWeights(selectedGameday);
        double weightPast = weights[0];
        double weightCurrent = weights[1];

        // Wenn wir nach Spieltag 6 sind, brauchen wir keine historischen Daten mehr
        if (selectedGameday > 6) {
            // Calculate current season statistics up to selected gameday
            double currentHomeGoals = 0, currentAwayGoals = 0;
            double currentHomeWins = 0, currentAwayWins = 0;
            int currentHomeGames = 0, currentAwayGames = 0;

            for (HistoricalMatch match : currentSeasonMatches) {
                if (match.gameday < selectedGameday) {  // Nur Spiele vor dem ausgewählten Spieltag
                    if (match.homeTeam.equals(homeTeam)) {
                        currentHomeGoals += match.homeGoals;
                        if (match.homeGoals > match.awayGoals) currentHomeWins++;
                        currentHomeGames++;
                    }
                    if (match.awayTeam.equals(awayTeam)) {
                        currentAwayGoals += match.awayGoals;
                        if (match.awayGoals > match.homeGoals) currentAwayWins++;
                        currentAwayGames++;
                    }
                }
            }

            double homeStrength = currentHomeGames > 0 ? currentHomeGoals / currentHomeGames : 0;
            double awayStrength = currentAwayGames > 0 ? currentAwayGoals / currentAwayGames : 0;
            double homeWins = currentHomeWins;
            double awayWins = currentAwayWins;

            return new TeamStats(homeStrength, awayStrength, homeWins, awayWins);
        }

        // Für Spieltage 1-6 verwenden wir die ursprüngliche Logik mit Gewichtung
        Set<String> homeTeamSeasons = new HashSet<>();
        Set<String> awayTeamSeasons = new HashSet<>();

        for (HistoricalMatch match : historicalMatches) {
            if (match.homeTeam.equals(homeTeam) || match.awayTeam.equals(homeTeam)) {
                homeTeamSeasons.add(match.season);
            }
            if (match.homeTeam.equals(awayTeam) || match.awayTeam.equals(awayTeam)) {
                awayTeamSeasons.add(match.season);
            }
        }

        homeTeamSeasons.retainAll(awayTeamSeasons);
        String lastCommonSeason = null;
        if (!homeTeamSeasons.isEmpty()) {
            lastCommonSeason = Collections.max(homeTeamSeasons);
        }

        // Calculate past season statistics
        double pastHomeGoals = 0, pastAwayGoals = 0;
        double pastHomeWins = 0, pastAwayWins = 0;
        int pastHomeGames = 0, pastAwayGames = 0;

        if (lastCommonSeason != null) {
            for (HistoricalMatch match : historicalMatches) {
                if (match.season.equals(lastCommonSeason)) {
                    if (match.homeTeam.equals(homeTeam)) {
                        pastHomeGoals += match.homeGoals;
                        if (match.homeGoals > match.awayGoals) pastHomeWins++;
                        pastHomeGames++;
                    }
                    if (match.awayTeam.equals(awayTeam)) {
                        pastAwayGoals += match.awayGoals;
                        if (match.awayGoals > match.homeGoals) pastAwayWins++;
                        pastAwayGames++;
                    }
                }
            }
        }

        // Calculate current season statistics
        double currentHomeGoals = 0, currentAwayGoals = 0;
        double currentHomeWins = 0, currentAwayWins = 0;
        int currentHomeGames = 0, currentAwayGames = 0;

        for (HistoricalMatch match : currentSeasonMatches) {
            if (match.gameday < selectedGameday) {
                if (match.homeTeam.equals(homeTeam)) {
                    currentHomeGoals += match.homeGoals;
                    if (match.homeGoals > match.awayGoals) currentHomeWins++;
                    currentHomeGames++;
                }
                if (match.awayTeam.equals(awayTeam)) {
                    currentAwayGoals += match.awayGoals;
                    if (match.awayGoals > match.homeGoals) currentAwayWins++;
                    currentAwayGames++;
                }
            }
        }

        // Combine weighted statistics
        double homeStrength = weightPast * (pastHomeGames > 0 ? pastHomeGoals / pastHomeGames : 0) +
                weightCurrent * (currentHomeGames > 0 ? currentHomeGoals / currentHomeGames : 0);
        double awayStrength = weightPast * (pastAwayGames > 0 ? pastAwayGoals / pastAwayGames : 0) +
                weightCurrent * (currentAwayGames > 0 ? currentAwayGoals / currentAwayGames : 0);
        double homeWins = weightPast * (pastHomeGames > 0 ? pastHomeWins : 0) +
                weightCurrent * (currentHomeGames > 0 ? currentHomeWins : 0);
        double awayWins = weightPast * (pastAwayGames > 0 ? pastAwayWins : 0) +
                weightCurrent * (currentAwayGames > 0 ? currentAwayWins : 0);

        return new TeamStats(homeStrength, awayStrength, homeWins, awayWins);
    }


    private double[] calculateWeights(int gameday) {
        double weightPast, weightCurrent;

        if (gameday == 1) {
            weightPast = 1.0;
            weightCurrent = 0.0;
        } else if (gameday <= 6) {
            weightPast = Math.max(0, 1 - (gameday - 1) * 0.2);
            weightCurrent = 1 - weightPast;
        } else {
            weightPast = 0.0;
            weightCurrent = 1.0;
        }

        return new double[] {weightPast, weightCurrent};
    }

    private double[] calculateWinProbabilities(TeamStats stats) {
        double totalStrengthWins = (0.3 * stats.homeStrength + 0.7 * stats.homeWins) +
                (0.3 * stats.awayStrength + 0.7 * stats.awayWins);

        // Calculate win ratio
        double winRatio = (stats.homeWins + stats.awayWins) > 0 ?
                stats.homeWins / (stats.homeWins + stats.awayWins) : 0.5;

        // Calculate strength ratio
        double strengthRatio = totalStrengthWins > 0 ?
                (0.3 * stats.homeStrength + 0.7 * stats.homeWins) / totalStrengthWins : 0.5;

        // Calculate draw probability
        double drawFactor = Math.max(0, 1 - Math.abs(1 - strengthRatio)) *
                (1 - Math.abs(1 - winRatio));
        double drawAdjustment = drawFactor * 20;
        double drawProbability = Math.min(0.4, Math.max(0.1, (drawAdjustment + 25) / 100.0));

        // Calculate home and away probabilities
        double remainingProbability = 1.0 - drawProbability;
        double homeProbability, awayProbability;

        if (totalStrengthWins > 0) {
            homeProbability = remainingProbability *
                    ((0.3 * stats.homeStrength + 0.7 * stats.homeWins) / totalStrengthWins);
            awayProbability = remainingProbability *
                    ((0.3 * stats.awayStrength + 0.7 * stats.awayWins) / totalStrengthWins);
        } else {
            homeProbability = awayProbability = remainingProbability / 2;
        }

        // Normalize probabilities
        double total = homeProbability + drawProbability + awayProbability;
        return new double[] {
                homeProbability / total,
                drawProbability / total,
                awayProbability / total
        };
    }

    private double[] calculateAverageGoals(String homeTeam, String awayTeam) {
        int homeGoals = 0, awayGoals = 0;
        int homeGames = 0, awayGames = 0;

        // Calculate from all historical matches including current season
        for (HistoricalMatch match : historicalMatches) {
            if (match.homeTeam.equals(homeTeam)) {
                homeGoals += match.homeGoals;
                homeGames++;
            }
            if (match.awayTeam.equals(awayTeam)) {
                awayGoals += match.awayGoals;
                awayGames++;
            }
        }

        for (HistoricalMatch match : currentSeasonMatches) {
            if (match.homeTeam.equals(homeTeam)) {
                homeGoals += match.homeGoals;
                homeGames++;
            }
            if (match.awayTeam.equals(awayTeam)) {
                awayGoals += match.awayGoals;
                awayGames++;
            }
        }

        double homeAvgGoals = homeGames > 0 ? (double) homeGoals / homeGames : 0;
        double awayAvgGoals = awayGames > 0 ? (double) awayGoals / awayGames : 0;
        double totalAvgGoals = (homeAvgGoals + awayAvgGoals);

        return new double[] {homeAvgGoals, awayAvgGoals, totalAvgGoals};
    }

    private double[] calculateOverUnderProbabilities(double totalAvgGoals) {
        double over15Prob;

        if (totalAvgGoals <= 1) over15Prob = 0.10;
        else if (totalAvgGoals <= 2) over15Prob = 0.20;
        else if (totalAvgGoals <= 3) over15Prob = 0.50;
        else if (totalAvgGoals <= 4) over15Prob = 0.60;
        else if (totalAvgGoals <= 5) over15Prob = 0.70;
        else over15Prob = 0.95;

        // Over 2.5 is 10% less than over 1.5
        double over25Prob = Math.max(0, over15Prob - 0.10);

        return new double[] {over15Prob, over25Prob};
    }

    private void updateMatchPredictions(FutureMatch match, double[] probabilities,
                                        double[] avgGoals, double[] overUnderProbs) {
        match.homeProbability = probabilities[0];
        match.drawProbability = probabilities[1];
        match.awayProbability = probabilities[2];
        match.totalAvgGoals = avgGoals[2];
        match.over15Probability = overUnderProbs[0];
        match.over25Probability = overUnderProbs[1];
    }
}

class HistoricalMatch {
    public String season;
    public int gameday;
    public String homeTeam;
    public String awayTeam;
    public int homeGoals;
    public int awayGoals;

    public HistoricalMatch(String season, int gameday, String homeTeam,
                           String awayTeam, int homeGoals, int awayGoals) {
        this.season = season;
        this.gameday = gameday;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }
}
class TeamStats {
    public double homeStrength;
    public double awayStrength;
    public double homeWins;
    public double awayWins;

    public TeamStats(double homeStrength, double awayStrength, double homeWins, double awayWins) {
        this.homeStrength = homeStrength;
        this.awayStrength = awayStrength;
        this.homeWins = homeWins;
        this.awayWins = awayWins;
    }
}

class FutureMatch {
    public String date;
    public String homeTeam;
    public String awayTeam;
    public double homeProbability;
    public double drawProbability;
    public double awayProbability;
    public double totalAvgGoals;
    public double over15Probability;
    public double over25Probability;

    public FutureMatch(String date, String homeTeam, String awayTeam) {
        this.date = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeProbability = 0.0;
        this.drawProbability = 0.0;
        this.awayProbability = 0.0;
        this.totalAvgGoals = 0.0;
        this.over15Probability = 0.0;
        this.over25Probability = 0.0;
    }
}