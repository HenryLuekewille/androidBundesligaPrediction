package com.example.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataSetUpdater {

    private static final String LATEST_CSV_URL = "https://www.football-data.co.uk/mmz4281/2425/D1.csv"; // Letzte Saison
    private static final String OUTPUT_FILE_NAME = "2015-2024_Bundesligadata.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    public void updateDataset(Context context) {
        new UpdateDatasetTask(context).execute(LATEST_CSV_URL);
    }

    private static class UpdateDatasetTask extends AsyncTask<String, Void, String> {
        private final Context context;

        public UpdateDatasetTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... urls) {
            File outputFile = new File(context.getFilesDir(), OUTPUT_FILE_NAME);

            // Bestehende Zeilen aus der CSV-Datei laden
            Set<String> existingLines = loadExistingLines(outputFile);

            List<String> newLines = new ArrayList<>();
            String header = null;

            try {
                // Letzte CSV-Datei herunterladen und parsen
                HttpURLConnection connection = (HttpURLConnection) new URL(urls[0]).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                boolean isFirstLine = true;
                Calendar calendar = Calendar.getInstance();
                String currentSeason = "";
                int currentGameday = 1;
                int gamesInCurrentGameday = 0;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        header = line; // Speichere den Header
                        isFirstLine = false;
                        continue;
                    }

                    String[] columns = line.split(",");
                    if (columns.length < 2) continue; // Zeilen mit unvollständigen Daten ignorieren

                    // Entfernen der Uhrzeit-Spalte (3. Spalte)
                    if (columns.length > 2 && columns[2].matches("\\d{2}:\\d{2}")) {
                        // Erstelle ein neues Array, das alle Spalten außer der 3. Spalte enthält
                        String[] newColumns = new String[columns.length - 1];
                        System.arraycopy(columns, 0, newColumns, 0, 2);  // Kopiere die ersten 2 Spalten (Season, Gameday, etc.)
                        System.arraycopy(columns, 3, newColumns, 2, columns.length - 3);  // Kopiere alle weiteren Spalten (ab der 4. Spalte)
                        columns = newColumns;  // Setze die Spalten auf das neue Array ohne die Uhrzeit
                    }

                    Date gameDate = DATE_FORMAT.parse(columns[1]);
                    if (gameDate == null) continue;

                    calendar.setTime(gameDate);
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH) + 1;

                    // Saison bestimmen
                    String season = getSeasonForDate(year, month);
                    if (!season.equals(currentSeason)) {
                        currentSeason = season;
                        currentGameday = 1;
                        gamesInCurrentGameday = 0;
                    }

                    // Spieltag-Logik: Ein Spieltag umfasst 9 Spiele
                    if (gamesInCurrentGameday < 9) {
                        gamesInCurrentGameday++;
                    } else {
                        currentGameday++;
                        gamesInCurrentGameday = 1;
                    }

                    // Zeile erstellen: Saison und Spieltag hinzufügen
                    StringBuilder modifiedLine = new StringBuilder();
                    modifiedLine.append(currentSeason).append(",");
                    modifiedLine.append(currentGameday).append(",");
                    modifiedLine.append(String.join(",", columns));

                    String finalLine = modifiedLine.toString();

                    // Prüfen, ob Zeile bereits existiert
                    if (!existingLines.contains(finalLine)) {
                        newLines.add(finalLine);
                    }
                }

                reader.close();
                connection.disconnect();

            } catch (Exception e) {
                Log.e("DatasetUpdater", "Error updating dataset", e);
                return "Error updating dataset.";
            }

            // Neue Zeilen an die bestehende Datei anhängen
            return appendNewLinesToFile(outputFile, header, newLines);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("DatasetUpdater", result);
        }

        private Set<String> loadExistingLines(File file) {
            Set<String> lines = new HashSet<>();
            if (!file.exists()) return lines;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) { // Überspringe Header
                        isFirstLine = false;
                        continue;
                    }
                    lines.add(line);
                }
            } catch (IOException e) {
                Log.e("DatasetUpdater", "Error reading existing file", e);
            }
            return lines;
        }

        private String appendNewLinesToFile(File file, String header, List<String> newLines) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Schreibe Header, falls Datei neu erstellt wird
                if (!file.exists() && header != null) {
                    writer.write("Season,Gameday," + header);
                    writer.newLine();
                }

                for (String line : newLines) {
                    writer.write(line);
                    writer.newLine();
                }
                return "Dataset updated: " + newLines.size() + " new rows added.";
            } catch (IOException e) {
                Log.e("DatasetUpdater", "Error appending to file", e);
                return "Error updating dataset file.";
            }
        }

        private String getSeasonForDate(int year, int month) {
            return (month >= 8) ? year + "/" + (year + 1) : (year - 1) + "/" + year;
        }
    }
}
