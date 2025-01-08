package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {

    private Spinner seasonSpinner;
    private Spinner gamedaySpinner;
    private TableLayout resultsTable;

    private List<MatchData> allMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        seasonSpinner = findViewById(R.id.seasonSpinner);
        gamedaySpinner = findViewById(R.id.gamedaySpinner);
        resultsTable = findViewById(R.id.resultsTable);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Finish this activity and return to the previous one
            finish();
        });

        // Load matches using CSVReader2
        CSVReader2 csvReader = new CSVReader2(this);
        allMatches = csvReader.readMatchData2();

        // Populate season spinner
        populateSeasonSpinner();

        // Populate gameday spinner with default values
        populateGamedaySpinner();

        // Add listeners for spinners
        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateGamedaySpinner();
                updateResultsTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
    }

    private void populateSeasonSpinner() {
        List<String> seasons = new ArrayList<>();
        for (MatchData match : allMatches) {
            if (!seasons.contains(match.season)) {
                seasons.add(match.season);
            }
        }
        Collections.sort(seasons);

        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(seasonAdapter);
    }

    private void populateGamedaySpinner() {
        List<Integer> gamedays = new ArrayList<>();
        for (int i = 1; i <= 34; i++) {
            gamedays.add(i);
        }

        ArrayAdapter<Integer> gamedayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gamedays);
        gamedayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamedaySpinner.setAdapter(gamedayAdapter);
    }

    private void updateGamedaySpinner() {
        String selectedSeason = (String) seasonSpinner.getSelectedItem();
        List<Integer> gamedays = new ArrayList<>();

        for (MatchData match : allMatches) {
            if (match.season.equals(selectedSeason) && !gamedays.contains(match.gameday)) {
                gamedays.add(match.gameday);
            }
        }
        Collections.sort(gamedays);

        ArrayAdapter<Integer> gamedayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gamedays);
        gamedayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamedaySpinner.setAdapter(gamedayAdapter);
    }

    private void updateResultsTable() {
        String selectedSeason = (String) seasonSpinner.getSelectedItem();
        int selectedGameday = (int) gamedaySpinner.getSelectedItem();

        List<MatchData> filteredMatches = new ArrayList<>();
        for (MatchData match : allMatches) {
            if (match.season.equals(selectedSeason) && match.gameday == selectedGameday) {
                filteredMatches.add(match);
            }
        }

        resultsTable.removeAllViews();

        // Add header row
        TableRow headerRow = new TableRow(this);
        String[] headers = {"Home Team", "Away Team", "Result"};
        for (String header : headers) {
            headerRow.addView(createTextView(header));
        }
        resultsTable.addView(headerRow);

        // Add match rows
        for (MatchData match : filteredMatches) {
            TableRow row = new TableRow(this);
            String result = match.homeGoals + " - " + match.awayGoals;

            row.addView(createTextView(match.homeTeam));
            row.addView(createTextView(match.awayTeam));
            row.addView(createTextView(result));

            resultsTable.addView(row);
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        return textView;
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
