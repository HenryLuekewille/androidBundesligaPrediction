package com.example.myapplication;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableManager {

    private final Context context;
    private final TableLayout tableLayout;
    private final Spinner gamedaySpinner;

    public TableManager(Context context, TableLayout tableLayout, Spinner gamedaySpinner) {
        this.context = context;
        this.tableLayout = tableLayout;
        this.gamedaySpinner = gamedaySpinner;
    }

    public void displayBundesligaTable(File csvFile) {
        List<MatchData> matches = parseCSV(csvFile);
        String latestSeason = getLatestSeason(matches);
        int latestGameday = getLatestGameday(matches, latestSeason);

        // Initiale Tabelle für den letzten Spieltag anzeigen
        updateTable(matches, latestSeason, latestGameday);

        // Dropdown-Menü (Spinner) mit Spieltagen der aktuellen Saison füllen
        List<Integer> gamedays = getGamedaysForSeason(matches, latestSeason);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, gamedays);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamedaySpinner.setAdapter(adapter);

        // Listener für Spinner-Auswahl
        gamedaySpinner.setSelection(gamedays.indexOf(latestGameday)); // Setze Standardauswahl auf letzten Spieltag
        gamedaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedGameday = (int) parent.getItemAtPosition(position);
                updateTable(matches, latestSeason, selectedGameday); // Aktualisiere Tabelle
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nichts zu tun, wenn keine Auswahl getroffen wird
            }
        });
    }

    private void updateTable(List<MatchData> matches, String season, int gameday) {
        // Filtere Spiele der aktuellen Saison und des ausgewählten Spieltags
        List<MatchData> filteredMatches = filterMatches(matches, season, gameday);

        // Berechne die Tabelle
        List<TeamStats> standings = calculateStandings(filteredMatches);

        // Sortiere die Tabelle
        Collections.sort(standings, Comparator.comparingInt((TeamStats team) -> team.points)
                .thenComparingInt(team -> team.goalDifference)
                .thenComparingInt(team -> team.goalsScored)
                .reversed());

        // Zeige die Tabelle an
        displayStandings(standings);
    }

    private List<Integer> getGamedaysForSeason(List<MatchData> matches, String season) {
        List<Integer> gamedays = new ArrayList<>();
        for (MatchData match : matches) {
            if (match.season.equals(season) && !gamedays.contains(match.gameday)) {
                gamedays.add(match.gameday);
            }
        }
        Collections.sort(gamedays); // Sortiere die Spieltage
        return gamedays;
    }

    private List<MatchData> parseCSV(File csvFile) {
        List<MatchData> matches = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Überspringe die Kopfzeile
                }

                String[] columns = line.split(",");
                if (columns.length < 15) continue;

                try {
                    // Behandle Season als String, um das "&" zu berücksichtigen
                    String season = columns[0]; // Season, z.B. "2023&2024"
                    int gameday = Integer.parseInt(columns[1]); // Gameday
                    String homeTeam = columns[4];  // HomeTeam
                    String awayTeam = columns[5];  // AwayTeam
                    int homeGoals = Integer.parseInt(columns[6].trim()); // FTHG
                    int awayGoals = Integer.parseInt(columns[7].trim()); // FTAG

                    MatchData match = new MatchData(season, gameday, homeTeam, awayTeam, homeGoals, awayGoals);
                    matches.add(match);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    System.out.println("Fehler in der Zeile: " + line);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matches;
    }

    private String getLatestSeason(List<MatchData> matches) {
        // Gibt alle Saisonen aus, um das Problem besser zu verstehen
        matches.stream().map(match -> match.season).forEach(System.out::println);

        // Gibt den letzten Eintrag der 'season' Liste zurück
        return matches.stream()
                .map(match -> match.season)
                .reduce((first, second) -> second)  // Nimmt den letzten Wert
                .orElse("0");
    }



    private int getLatestGameday(List<MatchData> matches, String season) {
        return matches.stream()
                .filter(match -> match.season.equals(season))
                .mapToInt(match -> match.gameday)
                .max()
                .orElse(0);
    }

    private List<MatchData> filterMatches(List<MatchData> matches, String season, int gameday) {
        List<MatchData> filtered = new ArrayList<>();
        for (MatchData match : matches) {
            if (match.season.equals(season) && match.gameday <= gameday) {
                filtered.add(match);
            }
        }
        return filtered;
    }

    private List<TeamStats> calculateStandings(List<MatchData> matches) {
        Map<String, TeamStats> teamStatsMap = new HashMap<>();

        for (MatchData match : matches) {
            // Home-Team
            TeamStats homeStats = teamStatsMap.getOrDefault(match.homeTeam, new TeamStats(match.homeTeam));
            homeStats.gamesPlayed++;
            homeStats.goalsScored += match.homeGoals;
            homeStats.goalsConceded += match.awayGoals;

            if (match.homeGoals > match.awayGoals) {
                homeStats.points += 3;
            } else if (match.homeGoals == match.awayGoals) {
                homeStats.points += 1;
            }
            teamStatsMap.put(match.homeTeam, homeStats);

            // Away-Team
            TeamStats awayStats = teamStatsMap.getOrDefault(match.awayTeam, new TeamStats(match.awayTeam));
            awayStats.gamesPlayed++;
            awayStats.goalsScored += match.awayGoals;
            awayStats.goalsConceded += match.homeGoals;

            if (match.awayGoals > match.homeGoals) {
                awayStats.points += 3;
            } else if (match.awayGoals == match.homeGoals) {
                awayStats.points += 1;
            }
            teamStatsMap.put(match.awayTeam, awayStats);
        }

        List<TeamStats> standings = new ArrayList<>(teamStatsMap.values());
        for (TeamStats stats : standings) {
            stats.goalDifference = stats.goalsScored - stats.goalsConceded;
        }
        return standings;
    }

    private void displayStandings(List<TeamStats> standings) {
        tableLayout.removeAllViews(); // Tabelle bereinigen

        // Kopfzeile hinzufügen
        TableRow headerRow = new TableRow(context);
        String[] headers = {"Platz", "Team", "Spiele", "Punkte", "Tore", "Gegentore", "Diff"};
        for (String header : headers) {
            TextView textView = new TextView(context);
            textView.setText(header);
            headerRow.addView(textView);
        }
        tableLayout.addView(headerRow);

        // Datenzeilen hinzufügen
        int rank = 1;
        for (TeamStats stats : standings) {
            TableRow row = new TableRow(context);

            row.addView(createTextView(String.valueOf(rank++)));
            row.addView(createTextView(stats.teamName));
            row.addView(createTextView(String.valueOf(stats.gamesPlayed)));
            row.addView(createTextView(String.valueOf(stats.points)));
            row.addView(createTextView(String.valueOf(stats.goalsScored)));
            row.addView(createTextView(String.valueOf(stats.goalsConceded)));
            row.addView(createTextView(String.valueOf(stats.goalDifference)));

            tableLayout.addView(row);
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        return textView;
    }

    private static class MatchData {
        String season;
        int gameday, homeGoals, awayGoals;
        String homeTeam, awayTeam;

        MatchData(String season, int gameday, String homeTeam, String awayTeam, int homeGoals, int awayGoals) {
            this.season = season;
            this.gameday = gameday;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
        }
    }

    private static class TeamStats {
        String teamName;
        int gamesPlayed, points, goalsScored, goalsConceded, goalDifference;

        TeamStats(String teamName) {
            this.teamName = teamName;
        }
    }
}


