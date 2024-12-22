package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TeamInsightsActivity extends AppCompatActivity {

    private Spinner teamSpinner;
    private TextView teamStatsTextView;
    private TableLayout lastGamesTable;

    private JSONArray bundesligaTeams;
    private List<MatchData> allMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_insights);

        // Back button functionality
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Finish the current activity and return to the main activity
            finish();
        });

        teamSpinner = findViewById(R.id.teamSpinner);
        teamStatsTextView = findViewById(R.id.teamStatsTextView);
        lastGamesTable = findViewById(R.id.lastGamesTable);

        // Load JSON data for team stats
        String jsonData = loadJSONFromAssets();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            bundesligaTeams = jsonObject.getJSONArray("BundesligaTeams");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Log JSON data for debugging
        System.out.println("Loaded JSON data: " + jsonData);

        // Load matches from CSV
        File csvFile = new File(getFilesDir(), "2015-2024_Bundesligadata.csv");
        allMatches = parseCSV(csvFile);

        // Populate spinner with team names
        List<String> teamNames = getTeamNames(allMatches);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSpinner.setAdapter(adapter);

        // Listener for team selection
        teamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTeam = (String) parent.getItemAtPosition(position);

                // Log selected team for debugging
                System.out.println("Selected team: " + selectedTeam);

                // Display stats and last games
                displayTeamStats(selectedTeam);
                displayLastFiveGames(selectedTeam);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                teamStatsTextView.setText("Select a team to view stats.");
                lastGamesTable.removeAllViews();
            }
        });
    }

    private String loadJSONFromAssets() {
        StringBuilder json = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("teams.json"))
            );
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private List<MatchData> parseCSV(File csvFile) {
        List<MatchData> matches = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header
                }

                String[] columns = line.split(",");
                if (columns.length < 15) continue;

                try {
                    String season = columns[0];
                    int gameday = Integer.parseInt(columns[1]);
                    String homeTeam = columns[4];
                    String awayTeam = columns[5];
                    int homeGoals = Integer.parseInt(columns[6].trim());
                    int awayGoals = Integer.parseInt(columns[7].trim());

                    MatchData match = new MatchData(season, gameday, homeTeam, awayTeam, homeGoals, awayGoals);
                    matches.add(match);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matches;
    }

    private List<String> getTeamNames(List<MatchData> matches) {
        List<String> teamNames = new ArrayList<>();
        for (MatchData match : matches) {
            if (!teamNames.contains(match.homeTeam)) {
                teamNames.add(match.homeTeam);
            }
            if (!teamNames.contains(match.awayTeam)) {
                teamNames.add(match.awayTeam);
            }
        }
        Collections.sort(teamNames);
        return teamNames;
    }

    private void displayTeamStats(String team) {
        try {
            for (int i = 0; i < bundesligaTeams.length(); i++) {
                JSONObject teamObject = bundesligaTeams.getJSONObject(i);

                // Log each team name for debugging
                System.out.println("Checking team: " + teamObject.getString("name"));

                if (teamObject.getString("name").equals(team)) {
                    String stats = "Coach: " + teamObject.getString("coach") + "\n" +
                            "Bundesliga Titles: " + teamObject.getInt("bundesliga_titles") + "\n" +
                            "DFB-Pokal Titles: " + teamObject.getInt("dfb_pokal_titles") + "\n" +
                            "Champions League Titles: " + teamObject.getInt("champions_league_titles");

                    // Update the TextView
                    teamStatsTextView.setText(stats);

                    // Log stats for debugging
                    System.out.println("Stats displayed: " + stats);
                    return;
                }
            }

            // If no matching team is found, display a message
            teamStatsTextView.setText("No stats available for the selected team.");
            System.out.println("No matching team found for: " + team);

        } catch (Exception e) {
            e.printStackTrace();
            teamStatsTextView.setText("Error loading team stats.");
        }
    }

    private void displayLastFiveGames(String team) {
        List<MatchData> teamMatches = new ArrayList<>();
        for (MatchData match : allMatches) {
            if (match.homeTeam.equals(team) || match.awayTeam.equals(team)) {
                teamMatches.add(match);
            }
        }

        // Sort by gameday in descending order
        teamMatches.sort(Comparator.comparingInt((MatchData m) -> m.gameday).reversed());

        // Get the last 5 games
        List<MatchData> lastFiveGames = teamMatches.subList(0, Math.min(5, teamMatches.size()));

        // Display in the table
        lastGamesTable.removeAllViews();

        // Add header row
        TableRow headerRow = new TableRow(this);
        String[] headers = {"Gameday", "Opponent", "Result"};
        for (String header : headers) {
            TextView textView = new TextView(this);
            textView.setText(header);
            headerRow.addView(textView);
        }
        lastGamesTable.addView(headerRow);

        // Add rows for each game
        for (MatchData match : lastFiveGames) {
            TableRow row = new TableRow(this);
            String opponent = match.homeTeam.equals(team) ? match.awayTeam : match.homeTeam;
            String result = match.homeTeam.equals(team)
                    ? match.homeGoals + " - " + match.awayGoals
                    : match.awayGoals + " - " + match.homeGoals;

            row.addView(createTextView(String.valueOf(match.gameday)));
            row.addView(createTextView(opponent));
            row.addView(createTextView(result));

            lastGamesTable.addView(row);
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        return textView;
    }

    private static class MatchData {
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
