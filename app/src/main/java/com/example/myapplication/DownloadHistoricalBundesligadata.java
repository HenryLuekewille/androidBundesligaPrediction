package com.example.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DownloadHistoricalBundesligadata {

    private static final String[] CSV_URLS = {
            "https://www.football-data.co.uk/mmz4281/1516/D1.csv",
            "https://www.football-data.co.uk/mmz4281/1617/D1.csv",
            "https://www.football-data.co.uk/mmz4281/1718/D1.csv",
            "https://www.football-data.co.uk/mmz4281/1819/D1.csv",
            "https://www.football-data.co.uk/mmz4281/1920/D1.csv",
            "https://www.football-data.co.uk/mmz4281/2021/D1.csv",
            "https://www.football-data.co.uk/mmz4281/2122/D1.csv",
            "https://www.football-data.co.uk/mmz4281/2223/D1.csv",
            "https://www.football-data.co.uk/mmz4281/2324/D1.csv",
            "https://www.football-data.co.uk/mmz4281/2425/D1.csv"
    };

    public void downloadAndMergeCSV(Context context) {
        new MergeCSVTask(context).execute(CSV_URLS);
    }

    private static class MergeCSVTask extends AsyncTask<String, Void, String> {

        private Context context;

        public MergeCSVTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... urls) {
            List<String> allData = new ArrayList<>();
            boolean isFirstFile = true;
            String header = null;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Calendar calendar = Calendar.getInstance();
            String currentSeason = "";
            int currentGameday = 1;
            int gamesInCurrentGameday = 0;

            for (String url : urls) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;

                    boolean skipHeader = !isFirstFile;
                    while ((line = reader.readLine()) != null) {
                        if (skipHeader) {
                            skipHeader = false;
                            continue;
                        }

                        String[] columns = line.split(",");
                        if (columns.length < 2) continue; // Skip invalid rows

                        // Entferne die Uhrzeit-Spalte (3. Spalte), falls vorhanden
                        if (columns.length > 2 && columns[2].matches("\\d{2}:\\d{2}")) {
                            // Erstelle ein neues Array, das alle Spalten außer der 3. Spalte enthält
                            String[] newColumns = new String[columns.length - 1]; // Entferne die 3. Spalte (Uhrzeit)
                            System.arraycopy(columns, 0, newColumns, 0, 2);  // Kopiere die ersten 2 Spalten (Season, Gameday, etc.)
                            System.arraycopy(columns, 3, newColumns, 2, columns.length - 3);  // Kopiere alle weiteren Spalten (ab der 4. Spalte)
                            columns = newColumns;  // Setze die Spalten auf das neue Array ohne die Uhrzeit
                        }

                        String gameDateString = columns[1];
                        Date gameDate = null;
                        try {
                            gameDate = dateFormat.parse(gameDateString);
                        } catch (Exception e) {
                            Log.e("DownloadMergeCSV", "Invalid date format: " + gameDateString, e);
                        }

                        if (gameDate != null) {
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
                                if (gamesInCurrentGameday == 9) {
                                    currentGameday++;
                                    gamesInCurrentGameday = 0;
                                }
                                gamesInCurrentGameday++;
                            }

                            StringBuilder modifiedLine = new StringBuilder();
                            modifiedLine.append(currentSeason).append(",");
                            modifiedLine.append(currentGameday).append(",");

                            // Füge die modifizierten Spalten hinzu
                            for (String column : columns) {
                                modifiedLine.append(column).append(",");
                            }

                            // Entferne das letzte Komma und füge die Zeile der Datenliste hinzu
                            allData.add(modifiedLine.toString().replaceAll(",$", ""));
                        }

                        if (isFirstFile && header == null) {
                            // Entferne die "Time"-Spalte aus dem Header
                            if (header != null) {
                                header = header.replaceAll(",Time", "");
                            }
                            header = line;
                        }
                    }

                    reader.close();
                    connection.disconnect();

                    isFirstFile = false;
                } catch (Exception e) {
                    Log.e("DownloadMergeCSV", "Error downloading file: " + url, e);
                }
            }


            File outputFile = new File(context.getFilesDir(), "2015-2024_Bundesligadata.csv");
            if (outputFile.exists() && !outputFile.delete()) {
                Log.e("DownloadMergeCSV", "Failed to delete existing file.");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                if (header != null) {
                    // Fügt "Season,Gameday," vor dem ursprünglichen Header hinzu
                    writer.write("Season,Gameday," + header);
                    writer.newLine();
                }

                for (String dataLine : allData) {
                    writer.write(dataLine);
                    writer.newLine();
                }
            } catch (IOException e) {
                Log.e("DownloadMergeCSV", "Error writing merged file", e);
                return "Error saving file.";
            }

            return "File saved: " + outputFile.getAbsolutePath();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("DownloadMergeCSV", result);
        }

        private String getSeasonForDate(int year, int month) {
            if (month >= 8) {
                return year + "/" + (year + 1);
            } else {
                return (year - 1) + "/" + year;
            }
        }
    }
}
