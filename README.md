# FloodMonitor | EK7.1

Die Zentrale sammelt die Messwerte von vier aktiven Stationen. Jede der fünf konfigurierten Stationen läuft in einem eigenen Prozess. Graz bleibt als inaktive Station im Verzeichnis. Webclient und REST-API liefern Messwerte, Warnungen und Statistiken.

## Start und Tests

Voraussetzungen sind Bash, JDK 17 bis 25 und für das folgende Beispiel OpenSSL. Die Schlüssel gelten für die aktuelle Shell. Alternativ die Felder aus [.env.example](.env.example) in einer lokalen Datei `.env` setzen.

```bash
export VIEWER_KEY="$(openssl rand -hex 32)"
export ADMIN_KEY="$(openssl rand -hex 32)"
export STATION_KEY="$(openssl rand -hex 32)"
./gradlew test
./start.sh
```

[Webclient](http://127.0.0.1:8080/) öffnen und den Wert von `VIEWER_KEY` eingeben. `Strg+C` beendet die Prozessgruppe. Beim ersten Build lädt der Gradle Wrapper die Abhängigkeiten.

## API und Pagination

Alle fachlichen Endpunkte liegen unter `/api/v1`. Der Header `Accept` wählt JSON (Standard) oder XML.

| Methode | Pfad | Auswahl |
|---|---|---|
| GET | `/stations` | river, isActive, warningLevel |
| GET, DELETE | `/stations/{id}` | Stammdaten lesen oder Station samt Historie löschen |
| POST | `/stations` | Stammdaten als JSON oder XML anlegen |
| GET | `/stations/{id}/measurements/latest` | Letzter empfangener Wert, ohne Daten 404 |
| GET | `/stations/{id}/measurements` | from, to, warningLevel, page, limit, sort |
| GET | `/measurements/latest` | Stationsübergreifende Zusammenfassung |
| GET | `/stations/{id}/statistics` | from, to |
| GET | `/alerts` | river, warningLevel, standardmäßig WARNING und CRITICAL |

Die Historie filtert zuerst nach Zeitraum (ISO-8601, beide Grenzen inklusive) und Warnstufe. Danach sortiert sie nach Zeit und schneidet die Seite aus.

| Parameter | Bedeutung | Standard |
|---|---|---|
| `page` | Seitennummer ab 1 | 1 |
| `limit` | 1 bis 1000 Messwerte pro Seite | 100 |
| `sort` | `asc` für älteste zuerst, `desc` für neueste zuerst | desc |

Beispiel für Seite 3 bei zwei Treffern und einem Treffer pro Seite:

```json
{"page":3,"limit":1,"sort":"desc","total":2,"totalPages":2,"measurements":[]}
```

`total` zählt alle gefilterten Treffer. Bei leerem Ergebnis gilt `totalPages = 0`. Seiten hinter dem Ende liefern 200 mit leerer Liste, ungültige Parameter 400. XML enthält dieselben Metadaten und je Treffer ein Element `measurement` unter `measurements`. Bei laufender Erfassung hält ein festes `to` den Zeitraum beim Blättern stabil. Der Webclient setzt die Grenze anhand des neuesten Messwerts und aktualisiert Seite 1 bei aktiviertem Auto-Refresh alle zehn Sekunden.

## Architektur und Datenmodell

Ein gemeinsames JAR startet entweder die Spring-Boot-Zentrale oder einen Stationsprozess mit eigenem HTTP-Server. `STATION_PORTS` ordnet jeder ID einen Loopback-Port zu. `POLL_INTERVAL_MS` steuert den Abstand zwischen Abfragen, standardmäßig 10000 ms. Die Zentrale pollt nur aktive, konfigurierte IDs. Ein POST registriert Stammdaten. Für zusätzliche Sensoren müssen Stationsprozess und Portzuordnung eingerichtet sein.

Beim ersten Abruf simuliert jede Station 60 Zeitpunkte als Startbestand. Bei weiteren Abfragen erzeugt sie nach Ablauf des Intervalls einen neuen Wert. Der interne Endpunkt `GET /measurements?after=<Zeitpunkt>` liefert höchstens die letzten 60 Werte, die neuer als der angegebene Zeitpunkt sind. Die Zentrale prüft ID und Zeitfolge und hält die jüngsten 10000 Werte je Station im Arbeitsspeicher. Ein Neustart der Zentrale verwirft deren Historie und manuell angelegte Stammdaten.

| Modell | Inhalt |
|---|---|
| Station | ID, Name, Fluss, Koordinaten, Aktivität, Status und drei Pegelschwellen in cm |
| Measurement | Stations-ID, UTC-Zeit, Pegel (cm), Durchfluss (m³/s), Regen (mm/h), Temperatur (°C), Akku (%), Status, Warnstufe |
| Alert | Station, Zeitpunkt, Pegel, Warnschwellen und aktuelle Warnstufe |
| Statistics | Anzahl, Minimum, Maximum und Mittelwert des Pegels, mittlerer Durchfluss, Regensumme und Warnungsanzahl |

Die Simulation nutzt den vorherigen Messwert. Regen erhöht den Pegel, ein Rückstellterm führt ihn zum Normalpegel. Der Durchfluss steigt mit dem Pegel. Unter 10 % Akku wechselt der Status für einen Messzyklus auf MAINTENANCE. Die Zentrale berechnet WARNING ab Warnpegel und CRITICAL ab kritischem Pegel. Fehlende, veraltete oder inaktive Daten ergeben als aktuelle Warnstufe UNKNOWN.

## Webclient

![JSON-Ansicht mit erster Messwertseite](docs/screenshots/webclient-overview.png)

![XML-Ansicht mit zweiter Messwertseite](docs/screenshots/webclient-xml.png)

## Kurze Reflexion

Neue Messwerte können die Seiten einer absteigend sortierten Historie verschieben. Der Webclient bindet Folgeseiten deshalb an die obere Zeitgrenze der ersten Abfrage. Beim erneuten Polling verhindert der zuletzt gespeicherte Zeitstempel doppelte Einträge. Der begrenzte Stationspuffer erlaubt nach einem Ausfall nur eine begrenzte Nachlieferung.

[Fragestellungen der Grundübung](FRAGESTELLUNGEN.md)
