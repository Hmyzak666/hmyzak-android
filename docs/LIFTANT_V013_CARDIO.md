# LiftAnt 0.13 — kardio, vlastní rychlé spuštění a časy

## Výchozí práce a větev

Repozitář: https://github.com/Hmyzak666/hmyzak-android

Výchozí commit: `7535583d0ee0f6c9a49b7b472557b89658542b11`, větev `feat/liftant-v013`, draft PR #7. Obsahuje předchozí 0.13.28: rychlé měření, časovač/notifikace, volitelné vizuální upozornění a deset jazyků. Zůstává zachovaný celý včetně historie. Nové doplnění je v místní větvi `feat/liftant-v013-cardio`.

**Dokončeno na GitHubu 2026-10-26:** větev `feat/liftant-v013-cardio` je nahraná. Kód byl ověřen pod commitem `7d7cd80483ba4607fc6664ee8f87cb46456324fa`, stromem `352e5a4fb0fe41c6734df27da73b61c96090824b` shodným s místními testovanými zdroji. [CI 48](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40307850906) a [podepsané APK 29](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40308252295) uspěly. Následné předání mění pouze dokumentaci. Původní odmítnutí připojení 403 se vyřešilo nahráním přes uživatelem schválený prohlížeč. Main, stará větev a PR #7 zůstaly zachované. Nový PR není vytvořený.

K dispozici je **LiftAnt-v0.13-build-29.apk**, **0.13.29 / 13029**, původní certifikát `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`, SHA-256 APK `4faae667fa28f0e4467a688f50c29b57074d2358aeb509ca70731012f7f5cdc0`. Ověření certifikátu, hashe, ID a verze bylo nezávisle zopakováno pomocí apksigner/aapt. Podepsaný workflow explicitně odemyká dosavadní testovací Pro; veřejný Play režim se nemění. Publikace byla přeskočená.

Stažené CI XML potvrdily **179 Android testů v debug i release**, v každé variantě 0 selhání/chyb/přeskočení. Podepsaný běh navíc znovu prošel 179 release testy v testovacím Pro režimu. **36 Python kontrol**; CI lint **0 chyb / 62 upozornění** (místně 56; upozornění zahrnují zachované staré překlady, ikony a dosavadní doporučení).

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

Přesné výsledky závěrečného sestavení jsou v `docs/LIFTANT_V013_CARDIO_VALIDATION.json`. Zdrojové testy jsou v repozitáři; XML výsledky a lint reporty jsou přiloženy zvlášť v balíčku.

Příkazy pro ověření s JDK 21 a Android SDK 36:

```sh
python3 docs/overit_migraci_mereni.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
bash ./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:bundleRelease --no-daemon
```

Nové testy pokrývají sekundy/minuty/hodiny, chybný čas, migraci DB 11, starou JSON 11, všech osm aktivit bez vzdálenosti, oddělení běžeckých grafů, rollback neplatného importu, obnovu pořadí/názvů/HIIT předvoleb, chybějící plán, přesné délky HIIT a formuláře na telefonu 320/360 dp i tabletu 800 dp ve všech jazycích. Staré testy časovače, notifikací, měření, historie, supersérií a Free/Pro zůstávají součástí celé sady.

## Jak pokračovat bez opakování hotové práce

1. Výchozím bodem je nynější serverová větev `feat/liftant-v013-cardio`, nikoli starý main nebo původní předávací ZIP. Po stažení zkontrolovat aktuální stav; nepřepisovat větev místním historickým bundle bez porovnání.
2. Testovací APK je v artefaktu `Hmyzak-APK-29` [podepsaného běhu 29](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40308252295). Obsahuje APK, `SHA256SUMS.txt` a `overeni-apk.json`. Archiv SHA-256 `51018ab926eabd978bdc693adf83d2f9e6b5ccb1f59d4e2bfa53a633ef060553` byl také ověřen proti GitHub digestu.
3. Po fyzickém ověření řešit případné opravy na této větvi a použít vyšší versionCode. Původní klíč se nemění; žádná automatická odinstalace ani downgrade databáze.
4. Žádný merge ani publikace do Play/Releases bez dalšího výslovného pokynu. Nový PR nebyl vytvořen; případný draft by měl porovnávat tuto větev s `feat/liftant-v013`.

## Co ověřit na telefonu

- Aktualizace s původním podpisem zachová plány, historii, supersérie, poznámky, míry, nastavení a původní běhy/grafy. Použít aktualizaci dosavadní instalace.
- Kardio: každá aktivita, záznam bez km, jiná aktivita s názvem, odpor/sklon, oprava starého běhu a čas 00:01:30. Srovnat původní běžecké statistiky; eliptical je nesmí změnit.
- Zkratky: vlastní silový plán, dvě odlišné HIIT předvolby, chůze/eliptical, měření; změna názvu a pořadí, odebrání, restart a JSON export/obnova. Klepnutí předvyplní a nic samo neuloží. Ověřit i odkaz po odstranění plánu.
- Čas: numerická klávesnice, postupné psaní a mazání číslic, přechod mezi poli, 01:30 v HIIT a 00:01:30 v kardiu, odmítnutí sekund/minut 60 v HH:MM:SS. Otočení obrazovky a návrat k rozepsanému formuláři.
- Původní timer: pauza/obnovení/±30 s, aplikace na pozadí a zamčená obrazovka, vibrace a volitelné vizuální upozornění. AOD/Now Bar a fyzické rozsvícení závisí na Androidu/Samsungu jako u 0.13.28.
- Na malém telefonu i tabletu zkontrolovat dlouhé názvy a všech deset jazyků. Fyzické notifikace, skutečné nákupy a cloud/D2D se nedají potvrdit místními unit testy.
