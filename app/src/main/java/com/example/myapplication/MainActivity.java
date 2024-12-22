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

    private FirebaseAuth mAuth;
    private Button predictionButton;
    private Button teamInsightsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TableLayout tableLayout = findViewById(R.id.tableLayout);
        Spinner gamedaySpinner = findViewById(R.id.gamedaySpinner);

        File csvFile = new File(getFilesDir(), "2015-2024_Bundesligadata.csv");

        DataSetUpdater dataSetUpdater = new DataSetUpdater();

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

        predictionButton = findViewById(R.id.predictionButton);
        predictionButton.setOnClickListener(v -> {
            Intent intentPredict = new Intent(MainActivity.this, PredictionActivity.class);
            startActivity(intentPredict);
        });

        teamInsightsButton = findViewById(R.id.teamIsnightsButton);
        teamInsightsButton.setOnClickListener(v -> {
            Intent intentTeamInsights = new Intent(MainActivity.this, TeamInsightsActivity.class);
            startActivity(intentTeamInsights);
        });
    }
}
