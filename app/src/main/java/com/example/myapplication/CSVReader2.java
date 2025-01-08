package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CSVReader2 {

    private static final String FILE_NAME = "2015-2024_Bundesligadata.csv";
    private final Context context;

    public CSVReader2(Context context) {
        this.context = context;
    }

    /**
     * Reads the CSV file and returns a list of MatchData objects.
     *
     * @return List of MatchData parsed from the CSV file.
     */
    public List<ResultsActivity.MatchData> readMatchData2() {
        List<ResultsActivity.MatchData> matches = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            // Read the file line by line
            while ((line = br.readLine()) != null) {
                // Skip the header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Split the line into columns
                String[] columns = line.split(",");
                if (columns.length < 8) continue;

                try {
                    // Parse the data
                    String season = columns[1].trim(); // Season
                    int gameday = Integer.parseInt(columns[2].trim()); // Gameday
                    String homeTeam = columns[4].trim(); // Home Team
                    String awayTeam = columns[5].trim(); // Away Team
                    int homeGoals = Integer.parseInt(columns[6].trim()); // Home Goals
                    int awayGoals = Integer.parseInt(columns[7].trim()); // Away Goals

                    // Create MatchData object
                    matches.add(new ResultsActivity.MatchData(season, gameday, homeTeam, awayTeam, homeGoals, awayGoals));
                } catch (NumberFormatException e) {
                    Log.e("CSVReader", "Error parsing number: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            Log.e("CSVReader", "Error reading file: ", e);
        }

        return matches;
    }

    /**
     * Print the last few lines of the CSV file (debugging helper method).
     *
     * @param numberOfLines Number of lines to print from the end.
     */
    public void printLastLines(int numberOfLines) {
        File file = new File(context.getFilesDir(), FILE_NAME);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            LinkedList<String> lastLines = new LinkedList<>();

            while ((line = br.readLine()) != null) {
                if (lastLines.size() == numberOfLines) {
                    lastLines.poll();
                }
                lastLines.add(line);
            }

            for (String lastLine : lastLines) {
                Log.i("CSVReader", lastLine);
                System.out.println(lastLine);
            }

        } catch (IOException e) {
            Log.e("CSVReader", "Error reading file: ", e);
        }
    }
}
