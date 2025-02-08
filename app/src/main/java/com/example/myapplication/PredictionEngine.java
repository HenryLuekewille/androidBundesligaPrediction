package com.example.myapplication;

// Import necessary libraries for Android and Java functionality
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

// Main class for predicting football match outcomes
public class PredictionEngine {
    // TAG for logging purposes
    private static final String TAG = "PredictionEngine";

    // AssetManager to access files stored in the app's assets folder
    private AssetManager assetManager;

    // Current season of the league (e.g., "2024/2025")
    private String currentSeason;

    // Lists to store historical matches, future matches, and matches from the current season
    private List<HistoricalMatch> historicalMatches;
    private List<FutureMatch> futureMatches;
    private List<HistoricalMatch> currentSeasonMatches;

    // File names for historical data and future match fixtures
    private static final String HISTORICAL_DATA_FILE = "2015-2024_Bundesligadata.csv";
    private static final String GAMEPLAN_FILE = "gameplan_24_25.csv";

    // Constructor to initialize the PredictionEngine with an AssetManager
    public PredictionEngine(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.historicalMatches = new ArrayList<>();
        this.currentSeasonMatches = new ArrayList<>();
        this.futureMatches = new ArrayList<>();
    }

    // Method to load the current season from the game plan file
    public void loadCurrentSeason() {
        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // Skip the header line and read the first data line
            String line = reader.readLine(); // Skip header
            if ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                currentSeason = columns[0].trim(); // Extract the current season
            }
        } catch (IOException e) {
            // Log an error if the file cannot be read
            Log.e(TAG, "Error reading current season", e);
            currentSeason = "2024/2025"; // Fallback value if reading fails
        }
    }

    // Method to load historical match data from a file in internal storage
    public void loadHistoricalData(Context context) {
        // Clear existing data
        historicalMatches.clear();
        currentSeasonMatches.clear();

        // Create a file object pointing to the historical data file
        File file = new File(context.getFilesDir(), HISTORICAL_DATA_FILE);

        // Check if the file exists
        if (!file.exists()) {
            Log.e(TAG, "Historical data file does not exist: " + HISTORICAL_DATA_FILE);
            return;
        }

        // Read the file line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.readLine(); // Skip header

            // Process each line in the file
            while ((line = reader.readLine()) != null) {
                try {
                    // Parse the line into a HistoricalMatch object
                    HistoricalMatch match = parseHistoricalMatch(line);

                    // Add the match to the appropriate list based on the season
                    if (match.season.equals(currentSeason)) {
                        currentSeasonMatches.add(match);
                    } else {
                        historicalMatches.add(match);
                    }
                } catch (Exception e) {
                    // Log an error if parsing fails
                    Log.e(TAG, "Error parsing line: " + line, e);
                }
            }
        } catch (IOException e) {
            // Log an error if the file cannot be read
            Log.e(TAG, "Error reading historical data from internal storage", e);
        }
    }

    // Helper method to parse a line of historical match data into a HistoricalMatch object
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

    // Method to get a list of available future gamedays
    public List<String> getAvailableGamedays() {
        Set<Integer> gamedays = new TreeSet<>();
        int currentGameday = getCurrentGameday();

        // Read the game plan file to find future gamedays
        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine(); // Skip header

            // Process each line in the file
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns[0].trim().equals(currentSeason)) {
                    int gameday = Integer.parseInt(columns[1].trim());
                    if (gameday > currentGameday) {  // Only include future gamedays
                        gamedays.add(gameday);
                    }
                }
            }
        } catch (IOException e) {
            // Log an error if the file cannot be read
            Log.e(TAG, "Error reading fixtures data", e);
        }

        // Convert the set of gamedays to a sorted list of strings
        return gamedays.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    // Helper method to determine the current gameday based on played matches
    private int getCurrentGameday() {
        int maxPlayedGameday = 0;
        for (HistoricalMatch match : currentSeasonMatches) {
            if (match.gameday > maxPlayedGameday) {
                maxPlayedGameday = match.gameday;
            }
        }
        return maxPlayedGameday;
    }

    // Method to calculate predictions for a selected gameday
    public List<FutureMatch> calculatePredictions(int selectedGameday) {
        // Load future matches for the selected gameday
        loadFutureMatches(selectedGameday);

        // Calculate predictions for each future match
        for (FutureMatch match : futureMatches) {
            // Calculate team statistics
            TeamStats stats = calculateTeamStatistics(match.homeTeam, match.awayTeam, selectedGameday);

            // Calculate win probabilities, average goals, and over/under probabilities
            double[] probabilities = calculateWinProbabilities(stats);
            double[] avgGoals = calculateAverageGoals(match.homeTeam, match.awayTeam);
            double[] overUnderProbs = calculateOverUnderProbabilities(avgGoals[2]);

            // Update the match object with the calculated predictions
            updateMatchPredictions(match, probabilities, avgGoals, overUnderProbs);
        }

        // Return the list of future matches with predictions
        return futureMatches;
    }

    // Helper method to load future matches for a selected gameday
    private void loadFutureMatches(int selectedGameday) {
        futureMatches.clear();

        // Read the game plan file to find matches for the selected gameday
        try (InputStream inputStream = assetManager.open(GAMEPLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine(); // Skip header

            // Process each line in the file
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (isMatchForSelectedGameday(columns, selectedGameday)) {
                    // Add the match to the futureMatches list
                    futureMatches.add(createFutureMatch(columns));
                }
            }
        } catch (IOException e) {
            // Log an error if the file cannot be read
            Log.e(TAG, "Error reading fixtures data", e);
        }
    }

    // Helper method to check if a match is for the selected gameday
    private boolean isMatchForSelectedGameday(String[] columns, int selectedGameday) {
        return columns[0].trim().equals(currentSeason) &&
                Integer.parseInt(columns[1].trim()) == selectedGameday;
    }

    // Helper method to create a FutureMatch object from a line of data
    private FutureMatch createFutureMatch(String[] columns) {
        return new FutureMatch(
                columns[2].trim(),  // date
                columns[4].trim(),  // homeTeam
                columns[5].trim()   // awayTeam
        );
    }

    // Method to calculate team statistics for prediction
    private TeamStats calculateTeamStatistics(String homeTeam, String awayTeam, int selectedGameday) {
        // Calculate weights based on the selected gameday
        double[] weights = calculateWeights(selectedGameday);
        double weightPast = weights[0];
        double weightCurrent = weights[1];

        // If the selected gameday is after 6, only use current season data
        if (selectedGameday > 6) {
            // Calculate current season statistics up to the selected gameday
            double currentHomeGoals = 0, currentAwayGoals = 0;
            double currentHomeWins = 0, currentAwayWins = 0;
            int currentHomeGames = 0, currentAwayGames = 0;

            for (HistoricalMatch match : currentSeasonMatches) {
                if (match.gameday < selectedGameday) {  // Only include matches before the selected gameday
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

            // Calculate team strengths and wins
            double homeStrength = currentHomeGames > 0 ? currentHomeGoals / currentHomeGames : 0;
            double awayStrength = currentAwayGames > 0 ? currentAwayGoals / currentAwayGames : 0;
            double homeWins = currentHomeWins;
            double awayWins = currentAwayWins;

            return new TeamStats(homeStrength, awayStrength, homeWins, awayWins);
        }

        // For gamedays 1-6, use a combination of past and current season data
        Set<String> homeTeamSeasons = new HashSet<>();
        Set<String> awayTeamSeasons = new HashSet<>();

        // Find common seasons for both teams
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

    // Helper method to calculate weights for past and current season data
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

    // Method to calculate win probabilities for a match
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

    // Method to calculate average goals for a match
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

        // Calculate average goals
        double homeAvgGoals = homeGames > 0 ? (double) homeGoals / homeGames : 0;
        double awayAvgGoals = awayGames > 0 ? (double) awayGoals / awayGames : 0;
        double totalAvgGoals = (homeAvgGoals + awayAvgGoals);

        return new double[] {homeAvgGoals, awayAvgGoals, totalAvgGoals};
    }

    // Method to calculate over/under probabilities for a match based on total average goals
    private double[] calculateOverUnderProbabilities(double totalAvgGoals) {
        double over15Prob;

        // Determine the probability of over 1.5 goals based on the total average goals
        if (totalAvgGoals <= 1) over15Prob = 0.10; // Low probability if average goals are <= 1
        else if (totalAvgGoals <= 2) over15Prob = 0.20; // Slightly higher probability if <= 2
        else if (totalAvgGoals <= 3) over15Prob = 0.50; // Moderate probability if <= 3
        else if (totalAvgGoals <= 4) over15Prob = 0.60; // Higher probability if <= 4
        else if (totalAvgGoals <= 5) over15Prob = 0.70; // Even higher probability if <= 5
        else over15Prob = 0.95; // Very high probability if average goals are > 5

        // Calculate the probability of over 2.5 goals as 10% less than over 1.5 goals
        double over25Prob = Math.max(0, over15Prob - 0.10);

        // Return the probabilities for over 1.5 and over 2.5 goals
        return new double[] {over15Prob, over25Prob};
    }

    // Method to update a FutureMatch object with calculated predictions
    private void updateMatchPredictions(FutureMatch match, double[] probabilities,
                                        double[] avgGoals, double[] overUnderProbs) {
        // Set the home, draw, and away probabilities
        match.homeProbability = probabilities[0];
        match.drawProbability = probabilities[1];
        match.awayProbability = probabilities[2];

        // Set the total average goals for the match
        match.totalAvgGoals = avgGoals[2];

        // Set the probabilities for over 1.5 and over 2.5 goals
        match.over15Probability = overUnderProbs[0];
        match.over25Probability = overUnderProbs[1];
    }
}

// Class to represent a historical match with its details
class HistoricalMatch {
    public String season; // The season in which the match was played
    public int gameday;   // The gameday of the match
    public String homeTeam; // The home team
    public String awayTeam; // The away team
    public int homeGoals;   // Goals scored by the home team
    public int awayGoals;   // Goals scored by the away team

    // Constructor to initialize a HistoricalMatch object
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

// Class to store statistics for a team
class TeamStats {
    public double homeStrength; // Strength of the home team (average goals scored)
    public double awayStrength; // Strength of the away team (average goals scored)
    public double homeWins;     // Number of wins for the home team
    public double awayWins;     // Number of wins for the away team

    // Constructor to initialize a TeamStats object
    public TeamStats(double homeStrength, double awayStrength, double homeWins, double awayWins) {
        this.homeStrength = homeStrength;
        this.awayStrength = awayStrength;
        this.homeWins = homeWins;
        this.awayWins = awayWins;
    }
}

// Class to represent a future match with its details and predictions
class FutureMatch {
    public String date; // The date of the match
    public String homeTeam; // The home team
    public String awayTeam; // The away team
    public double homeProbability; // Probability of the home team winning
    public double drawProbability; // Probability of a draw
    public double awayProbability; // Probability of the away team winning
    public double totalAvgGoals;   // Total average goals for the match
    public double over15Probability; // Probability of over 1.5 goals
    public double over25Probability; // Probability of over 2.5 goals

    // Constructor to initialize a FutureMatch object
    public FutureMatch(String date, String homeTeam, String awayTeam) {
        this.date = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeProbability = 0.0; // Initialize probabilities to 0
        this.drawProbability = 0.0;
        this.awayProbability = 0.0;
        this.totalAvgGoals = 0.0; // Initialize average goals to 0
        this.over15Probability = 0.0; // Initialize over/under probabilities to 0
        this.over25Probability = 0.0;
    }
}