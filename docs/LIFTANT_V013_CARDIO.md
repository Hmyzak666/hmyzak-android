# LiftAnt 0.13 — kardio, vlastní rychlé spuštění a časy

## Výchozí práce a větev

Repozitář: https://github.com/Hmyzak666/hmyzak-android

Výchozí commit: `7535583d0ee0f6c9a49b7b472557b89658542b11`, větev `feat/liftant-v013`, draft PR #7. Obsahuje předchozí 0.13.28: rychlé měření, časovač/notifikace, volitelné vizuální upozornění a deset jazyků. Zůstává zachovaný celý včetně historie. Nové doplnění je v místní větvi `feat/liftant-v013-cardio`.

**Zápis na GitHub byl odmítnut API 403 „Resource not accessible by integration“ při vytváření nové větve. Nová větev tedy zatím není na serveru.** GitHub čtení funguje. Nic nebylo sloučeno ani publikováno, původní PR #7 se neměnil. Místní větev a její historie jsou součástí přiloženého Git bundle.

## Implementace

- Sekce Kardio: běh venku, pás, chůze, eliptical, cyklistika, rotoped, veslování a jiná aktivita s vlastním názvem. Pouze ruční záznam, bez GPS a dalších stopek. Datum, povinná délka, nepovinné km, oba tepy, odpor/sklon podle typu a poznámka. Přidávání i oprava záznamu jsou Free.
- Původní běhy zůstávají v původní tabulce `runs`, se stejnými ID a hodnotami. Čtyři nové sloupce jsou přidané přes ALTER TABLE. Starý záznam se označí `RUNNING`. Nezadaná vzdálenost se v existujícím nenulovém sloupci ukládá jako 0; formulář ji zobrazí prázdnou a nevypočítává z ní tempo. `RezimState.cardio` obsahuje všechno, `runs` je projekce běhu/pásu s kladnou vzdáleností pro původní běžecké grafy a tempo. Chůze/eliptical/cyklistika se do běžeckých grafů nepřimíchají.
- Pokročilé běžecké grafy stále používají dosavadní Pro oprávnění. Silové Free/Pro a skutečný Billing se nemění. HIIT zůstává samostatný.
- Rychlé spuštění: uživatelské zkratky na vlastní silové plány, libovolný typ kardia, více HIIT předvoleb a měření. Přidání, změna názvu/cíle/intervalů, přesun výš/níž a odebrání. Na malém telefonu se tlačítka zalamují. Klepnutí pouze otevře plán nebo předvyplněný formulář, neukládá záznam ani nedokončuje trénink. Probíhající jiný silový trénink nadále chrání původní navigace.
- Výchozí zkratky jsou obecné: běh, HIIT a měření. Žádné osobní dny autora. Silový plán si uživatel přidá v editoru. Odkaz na odstraněný plán zobrazí lokalizovanou zprávu a lze jej upravit/odebrat.
- Zkratky jsou validované JSON v `hmyzak_prefs.quick_shortcuts`, včetně stabilních ID, pořadí, názvů a HIIT předvoleb. Patří do existujícího Auto Backup i whitelistu exportu/obnovy JSON. Chybějící klíč ve staré záloze není chybou. Neplatné zkratky se odmítnou ještě před importem databáze.
- Časový vstup: délky kardia, celková délka HIIT a opravy délky silových tréninků mají HH:MM:SS. Práce/odpočinek HIIT, předvolby a silová pauza mají MM:SS. Oddělená číselná pole, pevné dvojtečky, numerická klávesnice, označení obsahu při zaměření a kontrola rozsahů. Formát není odhadován podle počtu číslic. Např. 00:01:30 = 90 s a 01:30:00 = 5400 s. Změna výběru/zaměření nesmí přepsat sousední pole; testuje se i obnova obrazovky.
- HIIT uchovává přesnou novou délku v `total_seconds`, zatímco původní `minutes` nadále existují a obsahují zaokrouhlení nahoru pro kompatibilní datovou strukturu. Staré HIIT bez nového pole používají původní minuty. Nový přehled i textové sdílení respektují přesné sekundy. Zahřátí a zklidnění si zachovávají své původní explicitní číselné vstupy v celých minutách, bez nutnosti psát dvojtečky.
- Nové texty jsou ve všech 10 katalozích `cs/en/de/it/fr/ru/es/pt-BR/pl/tr`, soubory `cardio.xml`. Vlastní názvy a poznámky se nepřekládají.

## Kompatibilita

DB/JSON schéma je nově **12**; import podporuje **1–12**. Migruje se přidáním čtyř sloupců do `runs` a jednoho do `hiit`; žádné mazání či přepis starých záznamů. JSON zachovává dosavadní tabulky a přidává sloupce. Neplatný nový záznam nebo vazba se při importu vrací transakcí zpět. Původní migrační testy porovnávají původní sloupce, nové testy ověřují i nové výchozí hodnoty a přechod konkrétní databáze verze 11.

Application ID zůstává `cz.rezim.app`. Podepisovací workflow a očekávaný certifikát se nemění: SHA-256 `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`. Klíč je pouze v GitHub Actions secrets, do zdrojů ani balíčku nepatří. Místní debug/unsigned release nelze vydávat za kompatibilní podepsané APK.

## Ověření

Přesné výsledky závěrečného sestavení jsou v `VALIDATION.json` v předávacím balíčku. Zdrojové testy jsou v repozitáři; XML výsledky a lint reporty jsou přiloženy zvlášť v balíčku.

Příkazy pro ověření s JDK 21 a Android SDK 36:

```sh
python3 docs/overit_migraci_mereni.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
bash ./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:bundleRelease --no-daemon
```

Nové testy pokrývají sekundy/minuty/hodiny, chybný čas, migraci DB 11, starou JSON 11, všech osm aktivit bez vzdálenosti, oddělení běžeckých grafů, rollback neplatného importu, obnovu pořadí/názvů/HIIT předvoleb, chybějící plán, přesné délky HIIT a formuláře na telefonu 320/360 dp i tabletu 800 dp ve všech jazycích. Staré testy časovače, notifikací, měření, historie, supersérií a Free/Pro zůstávají součástí celé sady.

## Jak pokračovat bez opakování hotové práce

1. Obnovit větev z bundle nebo použít přiložené úplné zdroje. Bundle obsahuje historii i výchozí 0.13.28. Soubor `changes.patch` je navíc rozdíl proti původnímu commitu, včetně nových souborů.
2. Po zpřístupnění zápisu nahrát pouze `feat/liftant-v013-cardio`. Nepřepisovat starší větev a neslučovat do main. Pro nový draft PR použít základ `feat/liftant-v013`, aby zobrazoval jen toto doplnění.
3. Nechat proběhnout `android-ci.yml` nad skutečným serverovým commitem.
4. Spustit dosavadní `android-release.yml` na nové větvi s `publish_release=false`. Použít původní secret; nevytvářet nový klíč. Workflow má navazující číslo běhu, kontroluje původní certifikát a ID. Ověřit versionCode vyšší než 13028 a případná novější instalovaná verze. Testovací Pro se aktivuje explicitním dosavadním `-PliftantTestPro=true`; veřejné Play oprávnění se tím nemění.
5. Zkontrolovat APK report, signer, versionCode, hash a artefakt workflow; teprve toto APK předat jako kompatibilní aktualizaci. Před finálním závěrem uvést odkaz na konkrétní běh a název APK. Aktuální předávací balíček toto podepsané APK neobsahuje.
6. Žádná publikace do Play nebo GitHub Releases, žádný merge bez dalšího pokynu.

## Co ověřit na telefonu

- Aktualizace s původním podpisem zachová plány, historii, supersérie, poznámky, míry, nastavení a původní běhy/grafy. Použít aktualizaci dosavadní instalace.
- Kardio: každá aktivita, záznam bez km, jiná aktivita s názvem, odpor/sklon, oprava starého běhu a čas 00:01:30. Srovnat původní běžecké statistiky; eliptical je nesmí změnit.
- Zkratky: vlastní silový plán, dvě odlišné HIIT předvolby, chůze/eliptical, měření; změna názvu a pořadí, odebrání, restart a JSON export/obnova. Klepnutí předvyplní a nic samo neuloží. Ověřit i odkaz po odstranění plánu.
- Čas: numerická klávesnice, postupné psaní a mazání číslic, přechod mezi poli, 01:30 v HIIT a 00:01:30 v kardiu, odmítnutí sekund/minut 60 v HH:MM:SS. Otočení obrazovky a návrat k rozepsanému formuláři.
- Původní timer: pauza/obnovení/±30 s, aplikace na pozadí a zamčená obrazovka, vibrace a volitelné vizuální upozornění. AOD/Now Bar a fyzické rozsvícení závisí na Androidu/Samsungu jako u 0.13.28.
- Na malém telefonu i tabletu zkontrolovat dlouhé názvy a všech deset jazyků. Fyzické notifikace, skutečné nákupy a cloud/D2D se nedají potvrdit místními unit testy.
