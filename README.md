# MarciScores

Színes, prémium magyar futballközpont – Kotlin + Jetpack Compose.

## StatPal API-kulcs beállítása

1. Telepítsd és indítsd el az alkalmazást.
2. A főképernyőn nyomd meg a fogaskereket (API beállítása).
3. Illeszd be a saját StatPal API-kulcsodat, majd mentsd el.
4. Nyisd meg az **Élő** menüpontot.

A kulcsot az alkalmazás titkosított helyi tárhelyen tárolja. Nem kell a kulcsot a forráskódba vagy a `local.properties` fájlba írni, és ne töltsd fel GitHubra.

## Élő mérkőzések

Az alkalmazás a StatPal Soccer V2 élő mérkőzés végpontját használja:

`https://statpal.io/api/v2/soccer/matches/live`

A StatPal dokumentációja szerint az API-kulcsot `access_key` lekérdezési paraméterként kell átadni. Az élő lista az alkalmazás megnyitott Élő képernyőjén 30 másodpercenként frissül, és kézzel is frissíthető.

## Fontos biztonsági megjegyzés

A kulcs titkosított tárolása védi a készüléken nyugalmi állapotban, de egy közvetlenül API-t hívó mobilalkalmazásból egy hozzáértő támadó elméletileg kinyerheti a kulcsot. Nyilvános, kereskedelmi alkalmazásnál a biztonságosabb megoldás egy saját szerveres közvetítő (backend).

## Projekt állapota

Az élő mérkőzések lekérése és megjelenítése be van kötve. A Bajnokságok és Elemzés képernyők még fejlesztés alatt állnak.
