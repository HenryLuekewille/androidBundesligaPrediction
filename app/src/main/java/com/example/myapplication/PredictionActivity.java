package com.example.myapplication;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PredictionActivity extends AppCompatActivity {
    private Spinner gamedaySpinner;
    private String currentSeason;
    private static final String TAG = "PredictionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        gamedaySpinner = findViewById(R.id.gamedaySpinner);

        // Lade die aktuelle Saison dynamisch aus der CSV
        currentSeason = getCurrentSeasonFromCSV();
        if (currentSeason != null) {
            loadGamedays();
        } else {
            Log.e(TAG, "Aktuelle Saison konnte nicht geladen werden.");
        }
    }

    private String getCurrentSeasonFromCSV() {
        String lastSeason = null;
        AssetManager assetManager = getAssets();
        try (InputStream inputStream = assetManager.open("gameplan_24_25.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;

            // Überspringe die Kopfzeile
            reader.readLine();

            // Lies alle Zeilen und speichere die letzte Saison aus Spalte 1
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 1) {
                    lastSeason = data[0].trim(); // Spalte 1 enthält die Saison
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Lesen der CSV-Datei für die aktuelle Saison.", e);
        }

        return lastSeason != null ? lastSeason : "2023/2024"; // Fallback, falls Datei leer ist
    }

    private void loadGamedays() {
        // Format definieren
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Heutiges Datum abrufen
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        List<String> gamedays = new ArrayList<>();
        AssetManager assetManager = getAssets();

        try (InputStream inputStream = assetManager.open("gameplan_24_25.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;

            // Überspringe die Kopfzeile
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                // Sicherstellen, dass die notwendigen Spalten existieren (z.B. Spalte für Datum)
                if (data.length >= 3) {
                    String season = data[0].trim();
                    String gameday = data[1].trim();
                    String matchDateStr = data[2].trim(); // Spalte für Datum (z.B. "2024-12-20")

                    try {
                        // Match-Datum parsen
                        Date matchDate = sdf.parse(matchDateStr);

                        // Spiele hinzufügen, die größer oder gleich dem aktuellen Datum sind
                        if (season.equals(currentSeason) && matchDate != null && !gamedays.contains(gameday) && !matchDate.before(currentDate)) {
                            gamedays.add(gameday);
                        }
                    } catch (Exception e) {
                        Log.e("loadGamedays", "Fehler beim Parsen des Datums: " + matchDateStr, e);
                    }
                }
            }

            // Spinner mit den gefilterten Spieltagen befüllen
            ArrayAdapter<String> gamedayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    gamedays
            );
            gamedayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            gamedaySpinner.setAdapter(gamedayAdapter);

        } catch (IOException e) {
            Log.e("loadGamedays", "Fehler beim Lesen der CSV-Datei", e);
        }
    }
}

