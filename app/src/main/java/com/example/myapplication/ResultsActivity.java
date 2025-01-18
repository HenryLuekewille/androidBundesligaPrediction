package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "ResultsActivity";
    private Spinner seasonSpinner;
    private Spinner gamedaySpinner;
    private TableLayout resultsTable;
    private List<MatchData> allMatches;
    private static final String HISTORICAL_DATA_FILE = "2015-2024_Bundesligadata.csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Initialize list
        allMatches = new ArrayList<>();

        // Initialize views
        seasonSpinner = findViewById(R.id.seasonSpinner);
        gamedaySpinner = findViewById(R.id.gamedaySpinner);
        resultsTable = findViewById(R.id.resultsTable);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Get file from internal storage
        File csvFile = new File(getFilesDir(), HISTORICAL_DATA_FILE);

        if (csvFile.exists()) {
            loadHistoricalData(csvFile);
            if (!allMatches.isEmpty()) {
                setupSpinners();
            } else {
                Toast.makeText(this, "No match data found", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "CSV file not found at: " + csvFile.getAbsolutePath());
            Toast.makeText(this, "Historical data file not found", Toast.LENGTH_LONG).show();
        }
    }

    private void loadHistoricalData(File csvFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                try {
                    String[] columns = line.split(",");
                    if (columns.length >= 8) {
                        MatchData match = new MatchData(
                                columns[0].trim(),                    // season
                                Integer.parseInt(columns[1].trim()),  // gameday
                                columns[4].trim(),                    // homeTeam
                                columns[5].trim(),                    // awayTeam
                                Integer.parseInt(columns[6].trim()),  // homeGoals
                                Integer.parseInt(columns[7].trim())   // awayGoals
                        );
                        allMatches.add(match);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing line: " + line, e);
                }
            }
            Log.d(TAG, "Loaded " + allMatches.size() + " matches");
        } catch (IOException e) {
            Log.e(TAG, "Error reading historical data", e);
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinners() {
        // Setup season spinner
        List<String> seasons = new ArrayList<>();
        for (MatchData match : allMatches) {
            if (!seasons.contains(match.season)) {
                seasons.add(match.season);
            }
        }
        Collections.sort(seasons, Collections.reverseOrder());

        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                seasons
        );
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(seasonAdapter);

        // Setup listeners
        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateGamedaySpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                gamedaySpinner.setAdapter(null);
                resultsTable.removeAllViews();
            }
        });

        gamedaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateResultsTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                resultsTable.removeAllViews();
            }
        });

        // Trigger initial selection
        if (seasonAdapter.getCount() > 0) {
            seasonSpinner.setSelection(0);
        }
    }

    private void updateGamedaySpinner() {
        String selectedSeason = (String) seasonSpinner.getSelectedItem();
        if (selectedSeason == null) return;

        List<Integer> gamedays = new ArrayList<>();
        for (MatchData match : allMatches) {
            if (match.season.equals(selectedSeason) && !gamedays.contains(match.gameday)) {
                gamedays.add(match.gameday);
            }
        }
        Collections.sort(gamedays);

        ArrayAdapter<Integer> gamedayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                gamedays
        );
        gamedayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamedaySpinner.setAdapter(gamedayAdapter);

        if (!gamedays.isEmpty()) {
            gamedaySpinner.setSelection(0);
        }
    }

    private void updateResultsTable() {
        resultsTable.removeAllViews();

        String selectedSeason = (String) seasonSpinner.getSelectedItem();
        Integer selectedGameday = (Integer) gamedaySpinner.getSelectedItem();

        if (selectedSeason == null || selectedGameday == null) return;

        // Add header row
        TableRow headerRow = new TableRow(this);
        String[] headers = {"Home Team", "Away Team", "Result"};
        for (String header : headers) {
            TextView tv = new TextView(this);
            tv.setText(header);
            tv.setPadding(16, 8, 16, 8);
            tv.setTypeface(null, Typeface.BOLD);
            headerRow.addView(tv);
        }
        resultsTable.addView(headerRow);

        // Add match rows
        for (MatchData match : allMatches) {
            if (match.season.equals(selectedSeason) && match.gameday == selectedGameday) {
                TableRow row = new TableRow(this);

                TextView homeTeam = new TextView(this);
                homeTeam.setText(match.homeTeam);
                homeTeam.setPadding(16, 8, 16, 8);

                TextView awayTeam = new TextView(this);
                awayTeam.setText(match.awayTeam);
                awayTeam.setPadding(16, 8, 16, 8);

                TextView result = new TextView(this);
                result.setText(match.homeGoals + " - " + match.awayGoals);
                result.setPadding(16, 8, 16, 8);

                row.addView(homeTeam);
                row.addView(awayTeam);
                row.addView(result);

                resultsTable.addView(row);
            }
        }
    }

    public static class MatchData {
        String season;
        int gameday, homeGoals, awayGoals;
        String homeTeam, awayTeam;

        MatchData(String season, int gameday, String homeTeam, String awayTeam, int homeGoals, int awayGoals) {
            this.season = season;
            this.gameday = gameday;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
        }
    }
}