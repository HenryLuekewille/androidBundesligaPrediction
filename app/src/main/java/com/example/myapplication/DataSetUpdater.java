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

    private static final String LATEST_CSV_URL = "https://www.football-data.co.uk/mmz4281/2425/D1.csv";
    private static final String OUTPUT_FILE_NAME = "2015-2024_Bundesligadata.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    public interface UpdateCallback {
        void onUpdateCompleted(boolean success);
    }

    public void updateDataset(Context context, UpdateCallback callback) {
        new UpdateDatasetTask(context, callback).execute(LATEST_CSV_URL);
    }

    private static class UpdateDatasetTask extends AsyncTask<String, Void, Boolean> {
        private final Context context;
        private final UpdateCallback callback;

        public UpdateDatasetTask(Context context, UpdateCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            File outputFile = new File(context.getFilesDir(), OUTPUT_FILE_NAME);
            String header = null;
            List<String> allData = new ArrayList<>();
            boolean isFirstFile = true;
            Map<String, Integer> existingGamedays = loadExistingGamedays(outputFile);

            try {
                // Letzte CSV-Datei herunterladen und parsen
                HttpURLConnection connection = (HttpURLConnection) new URL(urls[0]).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                Calendar calendar = Calendar.getInstance();
                String currentSeason = "";
                int currentGameday = 1;
                int gamesInCurrentGameday = 0;

                boolean skipHeader = false;
                while ((line = reader.readLine()) != null) {
                    if (header == null) {
                        header = line.replaceAll(",Time", ""); // Remove Time column
                        skipHeader = true;
                        continue;
                    }

                    if (skipHeader) {
                        skipHeader = false;
                        continue;
                    }

                    String[] columns = line.split(",");
                    if (columns.length < 2) continue; // Skip invalid rows

                    // Remove Time column if present
                    if (columns.length > 2 && columns[2].matches("\\d{2}:\\d{2}")) {
                        String[] newColumns = new String[columns.length - 1];
                        System.arraycopy(columns, 0, newColumns, 0, 2);
                        System.arraycopy(columns, 3, newColumns, 2, columns.length - 3);
                        columns = newColumns;
                    }

                    String gameDateString = columns[1];
                    Date gameDate;
                    try {
                        gameDate = dateFormat.parse(gameDateString);
                    } catch (Exception e) {
                        Log.e("DatasetUpdater", "Invalid date format: " + gameDateString, e);
                        continue;
                    }

                    calendar.setTime(gameDate);
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH) + 1;

                    String season = getSeasonForDate(year, month);
                    if (!season.equals(currentSeason)) {
                        currentSeason = season;
                        currentGameday = 1;
                        gamesInCurrentGameday = 0;
                    }

                    if (gamesInCurrentGameday < 9) {
                        gamesInCurrentGameday++;
                    } else {
                        currentGameday++;
                        gamesInCurrentGameday = 1;
                    }

                    // Skip rows where the gameday is less than or equal to the maximum gameday for the season in the existing file
                    if (existingGamedays.containsKey(currentSeason) && currentGameday <= existingGamedays.get(currentSeason)) {
                        continue;
                    }

                    StringBuilder modifiedLine = new StringBuilder();
                    modifiedLine.append(currentSeason).append(",").append(currentGameday).append(",");
                    for (String column : columns) {
                        modifiedLine.append(column).append(",");
                    }
                    allData.add(modifiedLine.toString().replaceAll(",$", ""));
                }
                reader.close();
                connection.disconnect();
            } catch (Exception e) {
                Log.e("DatasetUpdater", "Error updating dataset", e);
                return false;
            }

            // Neue Zeilen an die bestehende Datei anhängen
            return appendNewLinesToFile(outputFile, header, allData);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            callback.onUpdateCompleted(success);
        }

        private Map<String, Integer> loadExistingGamedays(File file) {
            Map<String, Integer> seasonGamedays = new HashMap<>();
            if (!file.exists()) return seasonGamedays;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                boolean isFirstLine = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) { // Skip header
                        isFirstLine = false;
                        continue;
                    }

                    String[] columns = line.split(",");
                    if (columns.length < 3) continue; // Skip invalid rows

                    String season = columns[0];
                    int gameday;
                    try {
                        gameday = Integer.parseInt(columns[1]);
                    } catch (NumberFormatException e) {
                        Log.e("DatasetUpdater", "Invalid gameday format: " + columns[1], e);
                        continue;
                    }

                    seasonGamedays.put(season, Math.max(seasonGamedays.getOrDefault(season, 0), gameday));
                }
            } catch (IOException e) {
                Log.e("DatasetUpdater", "Error reading existing file", e);
            }
            return seasonGamedays;
        }

        private boolean appendNewLinesToFile(File file, String header, List<String> newLines) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                if (!file.exists()) {
                    writer.write("Season,Gameday," + header);
                    writer.newLine();
                }

                for (String line : newLines) {
                    writer.write(line);
                    writer.newLine();
                }
                return true;
            } catch (IOException e) {
                Log.e("DatasetUpdater", "Error appending to file", e);
                return false;
            }
        }

        private String getSeasonForDate(int year, int month) {
            return (month >= 8) ? year + "/" + (year + 1) : (year - 1) + "/" + year;
        }
    }
}
