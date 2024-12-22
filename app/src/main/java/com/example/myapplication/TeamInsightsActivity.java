package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TeamInsightsActivity extends AppCompatActivity {

    private Spinner teamSpinner;
    private TextView teamStatsTextView;

    private JSONArray bundesligaTeams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_insights);

        teamSpinner = findViewById(R.id.teamSpinner);
        teamStatsTextView = findViewById(R.id.teamStatsTextView);

        String jsonData = loadJSONFromAssets(); // Load the JSON data from assets

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            bundesligaTeams = jsonObject.getJSONArray("BundesligaTeams");

            // Extract team names for the Spinner
            List<String> teamNames = new ArrayList<>();
            for (int i = 0; i < bundesligaTeams.length(); i++) {
                teamNames.add(bundesligaTeams.getJSONObject(i).getString("name"));
            }

            // Set up the Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            teamSpinner.setAdapter(adapter);

            // Handle team selection
            teamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    displayTeamStats(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    teamStatsTextView.setText("");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Display the selected team's stats
    private void displayTeamStats(int position) {
        try {
            JSONObject selectedTeam = bundesligaTeams.getJSONObject(position);
            String stats = "Coach: " + selectedTeam.getString("coach") + "\n" +
                    "Bundesliga Titles: " + selectedTeam.getInt("bundesliga_titles") + "\n" +
                    "DFB-Pokal Titles: " + selectedTeam.getInt("dfb_pokal_titles") + "\n" +
                    "Champions League Titles: " + selectedTeam.getInt("champions_league_titles");

            teamStatsTextView.setText(stats);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load JSON data from the assets folder
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
}
