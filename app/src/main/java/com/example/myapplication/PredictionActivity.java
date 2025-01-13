package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import android.content.Context;

public class PredictionActivity extends AppCompatActivity {
    private Spinner gamedaySpinner;
    private static final String TAG = "PredictionActivity";
    private PredictionEngine predictionEngine;
    private TableLayout predictionTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        // Initialize views
        gamedaySpinner = findViewById(R.id.gamedaySpinner);
        predictionTable = findViewById(R.id.predictionTable);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish()); // Close the current activity


        // Initialize PredictionEngine and load data
        setupPredictionEngine();

        // Setup spinner with available gamedays
        setupGamedaySpinner();
    }

    private void setupPredictionEngine() {
        predictionEngine = new PredictionEngine(getAssets());
        predictionEngine.loadCurrentSeason();
        predictionEngine.loadHistoricalData(this);
    }

    private void setupGamedaySpinner() {
        List<String> gamedays = predictionEngine.getAvailableGamedays();
        ArrayAdapter<String> gamedayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                gamedays
        );
        gamedayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamedaySpinner.setAdapter(gamedayAdapter);

        gamedaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGameday = (String) parent.getItemAtPosition(position);
                updatePredictions(Integer.parseInt(selectedGameday));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updatePredictions(int gameday) {
        List<FutureMatch> predictions = predictionEngine.calculatePredictions(gameday);

        // Remove all rows except the header
        int childCount = predictionTable.getChildCount();
        if (childCount > 1) {
            predictionTable.removeViews(1, childCount - 1);
        }

        // Add prediction rows
        for (FutureMatch match : predictions) {
            addMatchRow(match);
        }
    }

    private void addMatchRow(FutureMatch match) {
        TableRow row = new TableRow(this);

        // Add all match data
        row.addView(createTextView(match.date));
        row.addView(createTextView(match.homeTeam));
        row.addView(createTextView(match.awayTeam));
        row.addView(createTextView(formatProbability(match.homeProbability)));
        row.addView(createTextView(formatProbability(match.drawProbability)));
        row.addView(createTextView(formatProbability(match.awayProbability)));
        row.addView(createTextView(String.format("%.1f", match.totalAvgGoals)));
        row.addView(createTextView(formatProbability(match.over15Probability)));
        row.addView(createTextView(formatProbability(match.over25Probability)));

        predictionTable.addView(row);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return textView;
    }

    private String formatProbability(double probability) {
        return String.format("%.1f%%", probability * 100);
    }
}