# Fragestellungen

## Was sind die Hauptmerkmale von JSON?

Textbasiertes, kompaktes Datenformat mit Objekten, Arrays, Strings, Zahlen, Booleans und null. Leicht lesbar, einfach zu parsen und sprachunabhängig.

Quelle: [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259)

## Was sind die Hauptmerkmale von XML?

Erweiterbare Auszeichnungssprache mit Baumstruktur aus Elementen und Attributen. Validierbar über DTD oder XSD, unterstützt Namespaces, XPath und XSLT, ist aber umfangreicher als JSON.

Quelle: [W3C XML 1.0](https://www.w3.org/TR/xml/)

## Wie unterscheidet die Applikation in diesem Beispiel, ob die Rückgabewerte in JSON oder XML zurückgegeben werden?

Über den `Accept`-Header. Die Controller deklarieren `produces` für JSON und XML, Spring wählt den passenden HttpMessageConverter. Ohne Header wird JSON geliefert, bei anderen Formaten kommt 406.

Quelle: [Spring Content Negotiation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html)

## Was zeichnet ein REST Interface aus und warum wird es heute sehr oft als standardisierte Schnittstelle verwendet?

Ressourcen werden über URIs adressiert und mit den HTTP-Methoden zustandslos bearbeitet. Die Schnittstelle ist einheitlich und cachebar. REST ist verbreitet, weil es auf HTTP aufbaut, plattformunabhängig, einfach und gut skalierbar ist.

Quelle: [Fielding, Kapitel 5](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

## Was ist der Unterschied zwischen den HTTP Methoden GET, POST, PUT und PATCH?

GET liest eine Ressource, POST legt eine neue an, PUT ersetzt eine Ressource vollständig und PATCH ändert sie teilweise. GET und PUT sind idempotent, POST und PATCH im Allgemeinen nicht.

Quelle: [RFC 9110, Abschnitt 9](https://www.rfc-editor.org/rfc/rfc9110#section-9), [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789)

## Ich will eine neue Request Methode / Pfad in meinem REST Interface zur Verfügung stellen, welche Stellen des Quellcodes muss ich verändern?

Im Controller eine Methode mit Mapping-Annotation (`@GetMapping`, `@PostMapping`, ...) anlegen, die Logik im Service umsetzen, bei Bedarf Repository und Model erweitern. Dazu Fehlerbehandlung, OpenAPI-Annotationen, Tests und gegebenenfalls den Webclient anpassen.

Quelle: [Spring, Building a RESTful Web Service](https://spring.io/guides/gs/rest-service)

## Kann man OpenAPI und Swagger vergleichen? Wenn ja, dann vergleichen Sie beide Themengebiete. Wenn nein, dann begründe die Antwort.

Nur bedingt, da sie verschiedene Dinge sind. OpenAPI ist die Spezifikation zur Beschreibung von REST-APIs (früher Swagger Specification). Swagger ist das Werkzeugset dazu, z.B. Swagger UI, Swagger Editor und Swagger Codegen.

Quelle: [Swagger vs. OpenAPI](https://swagger.io/blog/api-strategy/difference-between-swagger-and-openapi/)

## Wo liegt im folgenden Code-Snippet der Fehler? Begründe die Antwort.

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

GET muss sicher sein und darf keine Daten verändern. Browser, Crawler oder Caches könnten so ungewollt Stationen anlegen. Außerdem gehört kein Verb wie `add` in die URI. Richtig wäre `POST /stations` mit Request-Body und Antwort `201 Created` mit der erzeugten Station statt eines Strings.

Quelle: [RFC 9110, 9.2.1 Safe Methods](https://www.rfc-editor.org/rfc/rfc9110#section-9.2.1)
