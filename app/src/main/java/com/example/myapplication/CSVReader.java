package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class CSVReader {

    private static final String FILE_NAME = "2015-2024_Bundesligadata.csv";
    private Context context;

    public CSVReader(Context context) {
        this.context = context;
    }

    public void printLastLines(int numberOfLines) {
        File file = new File(context.getFilesDir(), FILE_NAME);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            LinkedList<String> lastLines = new LinkedList<>();

            // Lese die Datei zeilenweise und speichere nur die letzten `numberOfLines` in einer LinkedList
            while ((line = br.readLine()) != null) {
                if (lastLines.size() == numberOfLines) {
                    lastLines.poll(); // Entferne die älteste Zeile, wenn die Liste voll ist
                }
                lastLines.add(line); // Füge die aktuelle Zeile hinzu
            }

            // Gib die letzten `numberOfLines` aus
            for (String lastLine : lastLines) {
                Log.i("CSVReader", lastLine); // Ausgabe in Logcat
                System.out.println(lastLine); // Ausgabe in der Konsole (falls Debugging auf Emulator)
            }

        } catch (IOException e) {
            Log.e("CSVReader", "Fehler beim Lesen der Datei: ", e);
        }
    }
}

