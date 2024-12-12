package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamNameHarmonizer {

    private static final String TAG = "TeamNameHarmonizer";
    private static final int SIMILARITY_THRESHOLD = 60; // Ähnlichkeitsschwellenwert

    private final Context context;

    public TeamNameHarmonizer(Context context) {
        this.context = context;
    }

    public void harmonizeTeamNames() {
        try {
            // Dateipfade auf Android anpassen
            File updatedGamesFile = new File(context.getFilesDir(), "Updated_Games.csv");
            File gameplanFile = new File(context.getFilesDir(), "gameplan_24_25.csv");

            // CSV-Dateien laden
            List<Map<String, String>> updatedGames = readCsv(updatedGamesFile);
            List<Map<String, String>> gameplan = readCsv(gameplanFile);

            // Extrahiere die offiziellen Teamnamen aus dem Spielplan
            Set<String> officialTeams = gameplan.stream()
                    .flatMap(row -> Stream.of(row.get("HomeTeam"), row.get("AwayTeam")))
                    .collect(Collectors.toSet());

            // Aktualisiere Teamnamen basierend auf den offiziellen Namen
            for (Map<String, String> row : updatedGames) {
                row.put("HomeTeam", findBestMatch(row.get("HomeTeam"), officialTeams));
                row.put("AwayTeam", findBestMatch(row.get("AwayTeam"), officialTeams));
            }

            // CSV-Datei mit harmonisierten Teamnamen speichern
            writeCsv(updatedGamesFile, updatedGames);
            Log.d(TAG, "Harmonisierte Daten gespeichert unter: " + updatedGamesFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Harmonisieren der Teamnamen: ", e);
        }
    }

    private String findBestMatch(String teamName, Set<String> choices) {
        if ("FC Koln".equals(teamName)) {
            return teamName; // Spezielle Behandlung für "FC Koln"
        }

        LevenshteinDistance distance = new LevenshteinDistance();
        String bestMatch = teamName;
        int bestScore = Integer.MAX_VALUE;

        for (String choice : choices) {
            int score = distance.apply(teamName, choice);
            if (score < bestScore && score * 100 / Math.max(teamName.length(), choice.length()) < (100 - SIMILARITY_THRESHOLD)) {
                bestScore = score;
                bestMatch = choice;
            }
        }

        return bestMatch;
    }

    private List<Map<String, String>> readCsv(File file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        if (!file.exists()) {
            Log.e(TAG, "Datei nicht gefunden: " + file.getAbsolutePath());
            return rows;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            for (org.apache.commons.csv.CSVRecord record : parser) {
                rows.add(record.toMap());
            }
        }
        return rows;
    }

    private void writeCsv(File file, List<Map<String, String>> data) throws IOException {
        if (data.isEmpty()) {
            Log.w(TAG, "Keine Daten zum Schreiben in die Datei: " + file.getAbsolutePath());
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])));
            for (Map<String, String> row : data) {
                printer.printRecord(row.values());
            }
            printer.flush();
        }
    }
}

