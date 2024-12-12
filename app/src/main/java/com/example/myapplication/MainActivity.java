package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aktiviert Edge-to-Edge-Modus
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Stellt sicher, dass bei der Anwendung der Fensterinsets die Systemleisten berücksichtigt werden
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Überprüfe, ob der Benutzer eingeloggt ist
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Benutzer ist nicht eingeloggt, leite ihn zur RegisterActivity weiter
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();  // Beende die MainActivity, um sie aus dem Backstack zu entfernen
        }
        // Wenn der Benutzer eingeloggt ist, bleibt die MainActivity einfach auf dem Bildschirm
        // Keine Notwendigkeit für zusätzliche Weiterleitungen
    }
}


