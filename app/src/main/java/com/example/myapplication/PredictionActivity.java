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

import java.util.ArrayList;
import java.util.List;
import android.content.Context;

public class PredictionActivity extends AppCompatActivity {
    private Spinner gamedaySpinner;
    private static final String TAG = "PredictionActivity";
    private PredictionEngine predictionEngine;
    private TableLayout predictionTable;
    private TextView bestBetsContent;
    private Button backButtonP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        // Initialize views
        gamedaySpinner = findViewById(R.id.gamedaySpinner);
        predictionTable = findViewById(R.id.predictionTable);
        bestBetsContent = findViewById(R.id.bestBetsContent);
        backButtonP = findViewById(R.id.backButtonP);


        // Set up back button
        backButtonP.setOnClickListener(v -> {
            finish();
        });


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

        // Update table as before
        int childCount = predictionTable.getChildCount();
        if (childCount > 1) {
            predictionTable.removeViews(1, childCount - 1);
        }

        for (FutureMatch match : predictions) {
            addMatchRow(match);
        }

        // Update best bets section
        updateBestBets(predictions);
    }

    private void updateBestBets(List<FutureMatch> predictions) {
        StringBuilder bestBets = new StringBuilder();

        // Find top 2 matches with highest win probabilities
        List<FutureMatch> topWinProbabilities = new ArrayList<>(predictions);
        topWinProbabilities.sort((m1, m2) -> {
            double m1MaxProb = Math.max(m1.homeProbability, m1.awayProbability);
            double m2MaxProb = Math.max(m2.homeProbability, m2.awayProbability);
            return Double.compare(m2MaxProb, m1MaxProb);
        });

        bestBets.append("Highest win probabilities:\n");
        for (int i = 0; i < Math.min(2, topWinProbabilities.size()); i++) {
            FutureMatch match = topWinProbabilities.get(i);
            if (match.homeProbability > match.awayProbability) {
                bestBets.append(String.format("• %s vs %s: Home win %.1f%%\n",
                        match.homeTeam, match.awayTeam, match.homeProbability * 100));
            } else {
                bestBets.append(String.format("• %s vs %s: Away win %.1f%%\n",
                        match.homeTeam, match.awayTeam, match.awayProbability * 100));
            }
        }

        // Find matches with highest over 2.5 goals probability
        List<FutureMatch> topOverProbabilities = new ArrayList<>(predictions);
        topOverProbabilities.sort((m1, m2) -> Double.compare(m2.over25Probability, m1.over25Probability));

        bestBets.append("\nHighest over 2.5 goals probability:\n");
        for (int i = 0; i < Math.min(2, topOverProbabilities.size()); i++) {
            FutureMatch match = topOverProbabilities.get(i);
            bestBets.append(String.format("• %s vs %s: Over 2.5 goals %.1f%%\n",
                    match.homeTeam, match.awayTeam, match.over25Probability * 100));
        }

        bestBetsContent.setText(bestBets.toString());
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