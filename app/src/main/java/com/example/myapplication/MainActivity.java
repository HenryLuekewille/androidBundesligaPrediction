package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // Firebase authentication instance
    private Button predictionButton; // Button for predictions
    private Button teamInsightsButton; // Button for team insights
    private Button resultsButton; // Button for results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        TableLayout tableLayout = findViewById(R.id.tableLayout);
        Spinner gamedaySpinner = findViewById(R.id.gamedaySpinner);

        // File containing historical data
        File csvFile = new File(getFilesDir(), "2015-2024_Bundesligadata.csv");

        // Create an instance of DataSetUpdater
        DataSetUpdater dataSetUpdater = new DataSetUpdater();

        // Update dataset and display Bundesliga table if successful
        dataSetUpdater.updateDataset(this, success -> {
            if (success && csvFile.exists()) {
                runOnUiThread(() -> {
                    TableManager tableManager = new TableManager(this, tableLayout, gamedaySpinner);
                    tableManager.displayBundesligaTable(csvFile);
                });
            } else {
                Log.e("MainActivity", "Dataset update failed or file does not exist.");
            }
        });

        // Initialize and set click listener for prediction button
        predictionButton = findViewById(R.id.predictionButton);
        predictionButton.setOnClickListener(v -> {
            Intent intentPredict = new Intent(MainActivity.this, PredictionActivity.class);
            startActivity(intentPredict);
        });

        // Initialize and set click listener for team insights button
        teamInsightsButton = findViewById(R.id.teamInsightsButton);
        teamInsightsButton.setOnClickListener(v -> {
            Intent intentTeamInsights = new Intent(MainActivity.this, TeamInsightsActivity.class);
            startActivity(intentTeamInsights);
        });

        // Initialize and set click listener for results button
        resultsButton = findViewById(R.id.resultsButton);
        resultsButton.setOnClickListener(v -> {
            Intent intentResults = new Intent(MainActivity.this, ResultsActivity.class);
            startActivity(intentResults);
        });
    }
}