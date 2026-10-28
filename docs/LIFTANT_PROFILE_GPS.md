# LiftAnt – profil, energetické odhady a GPS

Výchozí vzdálený commit `84cc2b742df1018b8a46c6e8ba9bc8ecb13c37ff`, větev
`feat/liftant-v013-cardio`, ověřeno 28. 10. 2026. Navazující větev
`feat/liftant-profile-gps`. Původní práce ani main se nepřepisují.

## Datový kontrakt

DB a JSON 13, import 1–13. Upgrade 12→13 pouze přidává `gps_tracks` a
`energy_options`; existující řádky a sloupce se nemění. Profil je nepovinná
validovaná preference `personal_profile` v `hmyzak_prefs`, zahrnutá v ručním
JSON i existujících pravidlech šifrované systémové zálohy. Rok narození určuje
rozmezí věku, protože den narozenin není známý. Hmotnost pochází z měření.

GPS checkpoint obsahuje čas, segmenty trasy a skutečně přijatou vzdálenost.
Dokončení atomicky vloží běžný kardio záznam a jeho vazbu; opakované dokončení
vrací stejné ID. Import odmítá poškozené body/vazby v transakci. Obnovená běžící
aktivita je pozastavená. Za běhu GPS je JSON obnova zablokovaná, aby služba
nemohla znovu zapsat právě nahrazená data. Zálohy obsahují citlivé souřadnice;
platí stejná bezpečná pravidla jako pro původní databázi.

## Energetické odhady

[2024 Adult Compendium](https://pacompendium.com/), pro věk 19–59:
[Running](https://pacompendium.com/running/), [Walking](https://pacompendium.com/walking/),
[Bicycling](https://pacompendium.com/bicycling/),
[Conditioning Exercise](https://pacompendium.com/conditioning-exercise/).

Použita konvence Compendia 1 MET = 1 kcal/kg/h. Celkový výdej během aktivity
MET×kg×h; aktivní (nad standardní klid) max(MET−1,0)×kg×h. Není to celodenní
TDEE, individuální BMR ani doporučený příjem jídla. Výška, pohlaví a tep nejsou
povinné a nevstupují do standardního MET vzorce. Nepoužíváme domyšlený tep,
watty, odpor stroje ani sklon. Hmotnost je poslední měření nejpozději k datu
aktivity; chybí-li, odhad se nevypočte. Změna starého měření záměrně přepočte
související odhady; kcal nejsou zamčený historický laboratorní výsledek.

Rychlost běhu se přiřadí nejbližší publikované kategorii, chůze a kolo pásmům.
Kategorie předpokládají rovinu, nikoli kalibrovanou korekci terénu či větru.
Bez rychlosti běh/chůze ukazují scénáře kategorií, nikoli přesnou intenzitu.
Eliptical 5/9 MET, veslovací ergometr 5/7,3, HIIT 7/11, síla 3,5/6.
Rotoped používá obecnou kategorii 6,8 MET, nikoli vymyšlené watty. U neznámé
intenzity se zobrazí rozsah. Rozsahy nejsou statistické intervaly spolehlivosti.
Veslování je aproximované kategoriemi ergometru; venkovní veslování se může
výrazně lišit. Síla a HIIT zahrnují zaznamenané pauzy a mají zvlášť nízkou
individuální přesnost. Vlastní OTHER nemá automaticky vymyšlenou MET hodnotu.
Přehled a jednoduché denní/týdenní/měsíční součty jsou Free; dosavadní Pro
analytika a Billing zůstávají beze změny.

## GPS a oprávnění

Zahájení a pokračování pouze výslovným tlačítkem z viditelné aplikace.
`ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` a `FOREGROUND_SERVICE_LOCATION`.
Žádné `ACCESS_BACKGROUND_LOCATION`, INTERNET, boot receiver ani automatický
restart sběru. Foreground service typu location s trvalým tichým oznámením,
pozastavením z oznámení a částečným wake lockem během běhu. `START_NOT_STICKY`:
po ukončení procesu se pokračuje výslovně z uloženého checkpointu.

Filtr odmítá body bez přesnosti, starší než 15 s, přesnost nad 35 m, nemožné
rychlosti, duplikované časy, mock-provider a drobný jitter. Výpadek nad 30 s,
pauza a restart začínají nový segment bez přičtení spojnice. Checkpoint každých
5 s a při přijetí bodu. Vynucené ukončení může ztratit posledních několik sekund;
Android nezaručuje pokračování po force-stop, vypnutí nebo omezení výrobcem.

Mapa je **offline zeměpisný náhled bez ulic**, se severem, souřadnicemi,
rozsahem a oddělenými segmenty. Není to mapový podklad OpenStreetMap/Google.
Žádné mapové servery nedostávají polohu ani dotazy na dlaždice.

Ověřené podklady (28. 10. 2026):
- https://developer.android.com/develop/background-work/services/fgs/service-types#location
- https://developer.android.com/develop/sensors-and-location/location/permissions
- https://support.google.com/googleplay/android-developer/answer/9799150

Před skutečným vydáním vyplnit Play deklaraci FGS a ověřit Data Safety a zásady
soukromí podle finální distribuce; automatické testy nenahrazují Play review.

## Ikony

Přiložený motiv mravence s činkou zpracovaný nástrojem ImageGen jako transparentní
vrstva; původní motiv, barvy a póza zachovány, odstraněn rám a podlaha.
Prompt: „Extract exactly the neon green glossy ant holding the large barbell
behind its head; remove only background, rounded border and floor glow;
transparent background, entire ant and barbell, no text.“

Adaptivní ikona má samostatné pozadí a rezervu pro kruhové masky, legacy rastry
48–192 px. `docs/branding/play-icon-512.png` a `play-feature-1024x500.png` jsou
podklady pro obchod, bez automatického zveřejnění.

## Ověření na skutečném zařízení – zatím neprovedeno

1. Exportovat aktuální JSON. Instalovat vyšší kompatibilní APK přes stávající
   instalaci, neodinstalovávat. Zkontrolovat staré běhy, plány, HIIT, míry a zkratky.
2. Venku start běh/chůze/kolo, přesná poloha; zamknout displej na 15–30 minut.
   Ověřit oznámení, přibývání bodů/času a baterii (Samsung i tablet pokud má GPS).
3. Pozastavit, přesunout se a pokračovat. Přes pauzu se nepřičte spojnice.
4. Tunel/zakrytý výhled, vypnutí GPS a odebrání oprávnění. Záznam musí zůstat
   dostupný a pokračovat až na výslovný pokyn. Ověřit i force-stop a reboot.
5. Dokončit, znovu otevřít historii a mapu. JSON export/obnova v testovací
   instalaci, systémový cloud/D2D zvlášť. Nikdy nemažeme uživatelovu instalaci.
6. Profil, věkové rozmezí, desetinná hmotnost, označení/částečná oprava čísla,
   součty kcal a nezapočtené aktivity bez hmotnosti. Malý telefon i tablet.

## Oddělení Play sestavení

Kompatibilní APK musí mít původní certifikát
`c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` a versionCode
vyšší než 13029. Klíč zůstává jen v existujícím CI. Veřejný Play AAB vyžaduje
samostatný upload keystore a skutečný Billing public key; stávající
`liftantPlayBuild=true` guard odmítá původní kompatibilní klíč i test Pro.
Defaultní release AAB je kandidát bez testovacího odemčení, dokud nejsou tyto
produkční údaje nastavené a podpis/platby ověřené. Není označen za připravený
k publikaci. Žádný merge, Play ani GitHub Releases se automaticky neprovádí.

## Dokončené sestavení 0.13.30

Místně 200 debug + 200 release testů bez selhání, 39 Python kontrol, lint 0 chyb
/ 62 upozornění. Podepsaný [běh 30](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40667823283)
uspěl nad zdroji `120dfd85ea1a41d901fce9d1767d03746d9f5351`; dalších 200 release
testů v CI. APK `LiftAnt-v0.13-build-30.apk`, 13030, 9 570 716 B. SHA-256
`cd72e3edf225fdbdd5a079fccff7b8832a753ab00ba61e54fa2baff6ad2b807a`.
Původní podpis, applicationId a zvýšená verze ověřeny i nezávisle lokálně
(apksigner/aapt), oba ZIPy stažené z Actions mají shodný digest s GitHubem.
APK je výslovný test Pro; oddělený defaultní release AAB má
`TEST_PRO_UNLOCK=false`, `PLAY_DISTRIBUTION=false` a čeká na skutečnou
produkční konfiguraci. Původní kompatibilní klíč nelze zaměnit za Play upload klíč.

Aktualizaci na skutečném zařízení provede uživatel; žádná aplikace se během
vývoje neinstalovala, neodinstalovala ani nemazala. Fyzické testy výše zůstávají
neprovedené. Změny jsou na samostatné větvi, bez merge a bez publikace.
