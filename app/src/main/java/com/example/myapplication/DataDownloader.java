package com.example.myapplication;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class DataDownloader {

    private static final String URL = "https://www.football-data.co.uk/mmz4281/2425/D1.csv";
    private static final String FILE_NAME = "D1.csv";
    private Context context;

    public DataDownloader(Context context) {
        this.context = context;
    }

    public void downloadCSV() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(URL).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                byte[] responseData = response.body().bytes();

                // Speichern der Datei im internen Speicher
                File file = new File(context.getFilesDir(), FILE_NAME);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(responseData);
                    System.out.println("CSV-Datei erfolgreich heruntergeladen: " + file.getAbsolutePath());
                }
            } else {
                System.out.println("Fehler beim Herunterladen der Datei.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

