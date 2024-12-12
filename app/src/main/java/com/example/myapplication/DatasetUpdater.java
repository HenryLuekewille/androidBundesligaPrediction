package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatasetUpdater {

    private Context context;

    public DatasetUpdater(Context context) {
        this.context = context;
    }

    public void updateDataset() {
        File inputFile = new File(context.getFilesDir(), "D1.csv");
        File outputFile = new File(context.getFilesDir(), "Updated_Games.csv");

        if (!inputFile.exists()) {
            Log.e("DatasetUpdater", "Datei nicht gefunden: " + inputFile.getAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            List<String[]> data = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                data.add(row);
            }

            // Beispielhafte Bearbeitung der CSV-Daten: Modifizieren der Spaltennamen
            List<String[]> updatedData = new ArrayList<>();
            for (String[] row : data) {
                // Hier könnten wir logische Datenbearbeitungen einfügen, z.B. Umbenennung der Spalten
                updatedData.add(row);
            }

            // Speichern der bearbeiteten Daten
            try (FileWriter writer = new FileWriter(outputFile)) {
                for (String[] row : updatedData) {
                    writer.write(String.join(",", row) + "\n");
                }
                Log.i("DatasetUpdater", "CSV-Datei erfolgreich aktualisiert: " + outputFile.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

