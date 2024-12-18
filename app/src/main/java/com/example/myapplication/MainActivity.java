package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

//        // Überprüfen, ob der Benutzer eingeloggt ist
//        if (!isUserLoggedIn()) {
//            // Wenn nicht eingeloggt, öffne die RegisterActivity
//            Intent intent = new Intent(this, RegisterActivity.class);
//            startActivity(intent);
//            finish();  // Verhindert, dass der Benutzer zur MainActivity zurückkehren kann
//        } else {
            // Andernfalls die reguläre MainActivity laden
            setContentView(R.layout.activity_main);


            DownloadHistoricalBundesligadata downloadHistoricalBundesligadata = new DownloadHistoricalBundesligadata();
            downloadHistoricalBundesligadata.downloadAndMergeCSV(this);



//            DataSetUpdater dataSetUpdater = new DataSetUpdater();
//            dataSetUpdater.updateDataset(this);



//            CSVReader csvReader = new CSVReader(this);
//            csvReader.printLastLines(200);


            //Tabelle anzeigen, wenn die Datei existiert
            TableLayout tableLayout = findViewById(R.id.tableLayout);
            File csvFile = new File(getFilesDir(), "2015-2024_Bundesligadata.csv");
            Spinner gamedaySpinner = findViewById(R.id.gamedaySpinner);


            if (csvFile.exists()) {
                TableManager tableManager = new TableManager(this, tableLayout, gamedaySpinner);
                tableManager.displayBundesligaTable(csvFile);
            }

        }
    }

//    // Methode zur Überprüfung des Anmelde-Status (Firebase Auth)
//    private boolean isUserLoggedIn() {
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        return currentUser != null;
//    }
//}
