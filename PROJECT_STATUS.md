# LiftAnt — aktuálně rozpracovaný kandidát

Stav 2026-10-24: uživatel schválil název **LiftAnt**, přiloženou ikonu mravence, jazyky **cs/en/de/it/fr/ru**, zachování výpisu pro AI, sdílení souhrnu tréninku a nazvedanou hmotnost. Následně výslovně přidal monetizaci: základ zdarma a jednorázové Pro. Konkrétní cena nebyla schválena.

## Pracovní větev a návrat

- Pracovní větev `feat/liftant-international` vychází z main `98ceff15e3da144edbf0c88d1138e3e692538db3`. Main se během přípravy nemění. Místní záloha je `backup/pre-liftant-2026-10-24`; všechny starší zálohy a APK zůstávají zachované.
- Kandidát 0.11: applicationId `cz.rezim.app`, původní kompatibilní podpis, DB `rezim_local_v1.db`, nově schéma 10 s importem 1–10. Migrace přidává pouze load_multiplier a jeho hodnoty pro známé původní cviky se dvěma jednoručkami; žádné původní pole série se nepřepisuje.
- Dokumentace změn a přesné oddělení kompatibilního APK od produkčního Play balíčku: [LIFTANT_V011.md](docs/LIFTANT_V011.md).
- Před každou instalací znovu ověřit aktuální zálohu a podpis. Nová APK se automaticky neinstaluje. Poslední potvrzení telefonu ani záloha z předchozího dne nenahrazují kontrolu současných dat.

## Kontroly před předáním

[CI 39](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40038038212) na zdrojovém commitu `a421ef49da23b5d91e7764ed7f1127de4877426e` uspělo: **93 Android testů v debug i release, v každé variantě bez selhání či přeskočení, 25 Python kontrol, debug APK a náhledové AAB**. Lint neměl chyby. Robolectric používá API 34; nenahrazuje test na skutečném Samsungu ani testy přes Play. Součástí výstupu je 12 vykreslených sdílecích karet (6 jazyků × 2 velikosti); vizuálně ověřené vzorky nemají oříznutý text.

Navazující závěrečná kontrola ještě ověřuje vypnutí jazykových splitů AAB, aby všechny jazyky byly dostupné offline, a dědění nepřekládané značky z výchozího katalogu. Podepsané APK se ověřuje samostatně původním release workflow; dosud nelze z této poznámky odvozovat dokončení nového APK.

Starší soukromá záloha ze **2026-10-21, schéma 7**, prošla místní SQL simulací upgradu i importu do schématu 10, kontrolou integrity, vazeb a JSON roundtripu se shodou všech původních polí a ID. Původní soubor zůstal nezměněný a není v repozitáři. Nejde o aktuální zálohu telefonu ani o provedenou obnovu na Androidu.

GitHub connector nadále vrací 403 pro zápis; uživatel dříve výslovně schválil práci přes přihlášený pracovní prohlížeč. Pro zápis se používá tento postup, čtení je dostupné přes connector. Nikam neodesílat soukromé exporty, klíče ani platební údaje.

## Co vyžaduje Play Console

Uživatel čeká na ověření účtu. Zde se zatím neprovádělo přihlášení do Play Console ani nahrání či publikace. Je třeba ověřit typ účtu a oprávnění, vytvořit produkt `liftant_pro`, vybrat cenu, připravit produkční podpis a upload key a provést skutečné licenční testy. Produkční přepínač bez klíče a upload keystore sestavení odmítne. Náhledové APK mají všechny funkce Pro odemčené bez platby.

V novém Work chatu načíst tento soubor, aktuální vzdálenou pracovní větev a stav CI/PR; zkontrolovat čistotu pracovního stromu. Nezačínat z historických ZIPů. Pokračovat v existující větvi, dokud není kandidát otestovaný. Rozsvícení displeje, další jazyky, cloud a serverové ověřování nákupů jsou odložené.

---

## Historický stav před LiftAnt

# Hmyzák – stav projektu a pokračování

Aktualizováno **2026-10-23**. Nejprve přečti tento soubor, README a `docs/SJEDNOCENI_ZDROJU.md`.

## Aktuální stav

Poslední verze **potvrzená uživatelem na Samsungu je 0.9.18 / 9018**. Po kontrole skutečné zálohy ze schématu 7 uživatel potvrdil funkční aktualizaci a nové tréninkové nástroje. Sjednocení, vlastní míry a tréninkové nástroje jsou v PR #1–3; [CI po sloučení #3](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39602557421) i [podepsané APK 18](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39570418833) uspěly. Obě varianty měly 65 testů bez chyb; kontrola podpisu a součtu APK prošla. Telefonní testy nelze odvozovat pouze ze zeleného CI.

Uživatel 2026-10-23 požádal o zařazení cviku do **prvního, posledního nebo kteréhokoliv konkrétního kola supersérie**. Větev `feat/superset-rounds` připravuje v0.10: volba u každého vybraného cviku v dialogu Supersérie, v plánu i během tréninku. Více sérií navazuje v dalších kolech; dostupný začátek se omezí tak, aby se všechny vešly do skupiny. Poslední kola se přepočítávají ze skutečných pracovních sérií. Uložení nemění zátěže, opakování, dokončení ani ID sérií. Pauzy po kole používají stejné přiřazení jako automatické posouvání.

Databáze a export mají verzi **9**, import podporuje **1–9**. Migrace 8 → 9 pouze přidává `plan_exercises.superset_start_round` (`0` = poslední kola; `1` = první; `2–15` = konkrétní začátek). Výchozí hodnota 1 zachová původní chování. Skutečné schéma v0.9 je zachované ve fixture `schema-v8.sql`. Nové regresní testy ověřují migraci všech sloupců, starší zálohy 7/8, JSON 9, transakční návrat, změnu během tréninku, poslední kliky po benchi, konkrétní kola, více kratších sérií, přidané série a odpovídající pauzy. Výsledky nové větve a podpis kandidáta ověř v Actions před instalací; v0.10 zatím není potvrzená na telefonu.

Místní záložní větev `backup/pre-superset-rounds-2026-10-23` zachovává potvrzené zdroje 0.9 na `18c17a8aafd9c9489cc083218f30e1657e2a49bd`. Původní zálohy sjednocení zůstávají zachované.

Do budoucna uživatel chce vyjasnit statický text „Zbývá“ v oznámení (živý odpočet je v záhlaví), prověřit odpočet na AOD/Now bar a volitelné běžné rozsvícení zamykací obrazovky při konci pauzy spolu s vibrací. Tyto nápady jsou odložené a nepatří do změny kol supersérií.

Původní výchozí bod sjednocení byl v0.7.14 / 7014, tag `hmyzak-build-14`, commit `ea5f134910111ba65c0b051c2512f9a63ac02998`; audit doložil, že v0.6 už nebyla poslední vydaná verze.

Výchozí větev pro další vývoj je **`main`**, sjednocení zdrojů je vedené v **[PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1)**. Obsah ZIPu v0.7 je materializovaný přímo do `app/` a `docs/`; sestavování, dokumentace a testy jsou součástí verzovaného stromu. Při sjednocení 14 → 15 byly produkční Kotlin, manifest a prostředky byte-for-byte shodné s funkčním základem; navazující v0.8 mění měření a popsanou technickou údržbu. Historické ZIPy zůstávají zachované a sestavení je nepoužívá.

Vzdálená záložní větev **`backup/pre-source-unification-2026-10-20`** zůstává na původním commitu `ea5f134910111ba65c0b051c2512f9a63ac02998`. Všech 62 souborů commitu `1309cca` bylo přes GitHub API porovnáno s lokálně ověřenou předlohou; souhlasí obsah i režimy souborů. Původní místní historie včetně `8296f1e` je navíc zachovaná v předaném gitovém bundle. GitHub konektor odmítal zápis chybou 403, proto byl přenos proveden přes uživatelem schválený přihlášený prohlížeč.

Při sjednocení v0.7 místně prošlo debug i nepodepsané release sestavení, 27 JUnit/Robolectric testů v každé variantě, 12 Python testů a lint obou variant. Na GitHubu uspělo nové **[push CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39495862678)** i **[PR CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39495868445)** nad `604f871`. **[Podepsaný běh 15](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494987115)** uspěl se stávajícími secrets: APK **0.7.15 / 7015** má původní certifikát a applicationId. Stažené výsledky potvrzují 27 testů bez chyb a přeskočení v debug i release variantě; kontrolní součet APK souhlasí s ověřovacím reportem. Hosted lint má 0 chyb a 30 upozornění; 5 z nich se týká dostupných aktualizací závislostí. Publikační úloha byla přeskočena, takže nevzniklo nové vydání v Releases. Podrobné výsledky jsou v `docs/SJEDNOCENI_ZDROJU.md`; před pokračováním vždy ověř aktuální stav [Actions](https://github.com/Hmyzak666/hmyzak-android/actions), zejména větev `main`.

Před instalací byla skutečná JSON záloha ověřena: formát a schéma 6, úplnost polí, vazby, SQLite integrita a načtení se zpětným porovnáním všech hodnot v oddělené databázi. Poté uživatel potvrdil funkční aktualizaci a zachovaná data na telefonu. Osobní export, jeho obsah ani soukromé kontrolní údaje nepatří do veřejného repozitáře.

## Hodnoty, které musí zůstat kompatibilní

| Oblast | Hodnota / pravidlo |
| --- | --- |
| Identita | `applicationId` i namespace `cz.rezim.app` |
| Certifikát SHA-256 | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Databáze | `rezim_local_v1.db`, SQLiteOpenHelper verze 9 (migrace z 8; zachované starší migrace) |
| Export | `rezim-local-backup`, schéma 9, import 1–9 |
| Číslování vydání | `versionBaseCode + GITHUB_RUN_NUMBER`, v0.10: `10000 + běh`, v0.9 používala `9000 + běh`, v0.8 používala `8000 + běh`; v0.7 používala `7000 + běh` |
| Původní APK SHA-256 | `7fc1722626f3edabf55585437d3706a2117258342af21c32b0016deb005d03b5` |
| Výchozí ZIP v0.7 SHA-256 | `a0445f738f7221cb2e372a39b891079814bee2af6896e0512a0a3588fa789409` |

Stejné secrets zachovávají původní klíč: `HMYZAK_KEYSTORE_BASE64`, `HMYZAK_STORE_PASSWORD`, `HMYZAK_KEY_ALIAS`, `HMYZAK_KEY_PASSWORD`. Soukromý klíč ani osobní export nikdy necommitovat. Veřejné kontrolní údaje jsou v `config/release-compatibility.json`.

## Mapa kódu

Cesty Kotlinu jsou relativní k `app/src/main/java/cz/rezim/app/`.

| Soubor | Odpovědnost |
| --- | --- |
| `LocalStore.kt` | SQLite, migrace, tréninky/plány, JSON export/import |
| `RezimState.kt` | Stav a operace obrazovek, ViewModel |
| `Screens.kt` | Compose obrazovky, zápis tréninku, historie a plány |
| `MainActivity.kt` | Vstup aplikace, sdílení a soubory |
| `Plans.kt` | Modely a výchozí šablony |
| `WorkoutNavigation.kt` | Výběr další série včetně skupin supersérií |
| `HmyzakHaptics.kt` | Vibrace potvrzení série a konce pauzy |
| `RestTimer.kt`, `RestTimerService.kt` | Uložený odpočet a služba při zhasnuté obrazovce |
| `WorkoutToolsUi.kt`, `SettingsScreen.kt` | Tréninková lišta, dialogy a Nastavení |
| `BodyMeasurementInput.kt` | Validace volitelných měr a názvů |
| `BodyMeasurementsScreen.kt` | Zápis, opravy, vlastní míry a volba grafu |

Stabilní ID dnů a cviků propojují šablony s historií; nepřejmenovávat je bez migrace. Smazání ze šablony nesmí mazat staré tréninky. Nová instalace má historicky předvyplněné běhy z původních zdrojů; sjednocení toto chování nemění.

## Jak navázat v novém Work chatu

1. Načti aktuální `main` z `https://github.com/Hmyzak666/hmyzak-android`. Historie sjednocení je v [PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1); pro další práci založ novou větev z aktuálního `main`, nepokračuj ve staré větvi `chore/unify-sources`.
2. Přečti `PROJECT_STATUS.md`, README a výsledek sjednocení. Spusť `git status`, ověř větev, poslední commity a skutečný stav Actions/Releases. Neodvozuj stav telefonu jen z názvu verze v tomto textu.
3. Pokud existuje `AGENTS.md`, přečti jej před změnami. Obsah starého chatu není automaticky dostupný; tento soubor slouží jako předání kontextu.
4. Začni novou změnu v samostatné větvi. **Neuplatňuj žádný `Hmyzak_update*.zip`** a nevracej do CI rozbalování aktualizací. Upravuj přímo `app/`.
5. Použij JDK 17, SDK 36 / Build Tools 35.0.0 a oficiální Wrapper. Kontroly jsou uvedené v README. Při změně dat doplň test původní databáze i export/import a kontrolu transakčního návratu při chybě.
6. Před sloučením další změny ověř zelené CI. Před aktualizací telefonu ověř nový JSON export, původní certifikát a vyšší `versionCode` než skutečně nainstalovaná verze (poslední potvrzená je 9018). Instalaci, odinstalování a vymazání dat neprováděj automaticky.

Návrat zajišťuje původní tag, místní i vzdálená větev `backup/pre-source-unification-2026-10-20` a ověřený úplný gitový bundle. Návrat kódu v telefonu se připravuje jako další kompatibilní aktualizace se stejným certifikátem a vyšší verzí; databázi nesnižovat.

## Dohodnutý rozsah v0.9

- Časovač: ruční spuštění je výchozí. Volitelně po každé sérii nebo po celém kole supersérie; poslední série tréninku už pauzu nespouští. Nastavitelných 1 s až 60 min, pozastavení, pokračování, ±30 s a nové spuštění. Bez zvuku, samostatný přepínač vibračního upozornění. Odpočet je místní, založený na monotónním čase a běží ve foreground službě; po restartu telefonu se ruší. K zobrazení v liště může být potřeba povolit oznámení. Ověření na skutečném Samsungu při zamčeném displeji zbývá.
- Supersérie: uživatel vybírá členy sám v plánu i během tréninku. Pořadí určuje plán. Příklad leg curl 4×, leg extension 4×, side band abduction 4×, side adduction 1× znamená adduction **v prvním kole**, poté se vynechává. Změny členství jsou trvalé a nemění už zapsané série.
- Trvalá poznámka: tlačítko u každého rozpracovaného cviku, změna platí i pro další tréninky.
- Délka tréninku: stále viditelná nahoře a opravitelná během cvičení; čas potom dále přibývá. Oprava v historii zůstává.
- Nastavení: vlastní obrazovka z horní lišty a přehledu, původní vzhled, přepínače vibrací, oba testy vibrací, systémové vibrace a nastavení pauz.

Tyto položky už nejsou pouze návrh: uživatel je 2026-10-21 výslovně schválil společně. Další nové funkce nepřidávat bez zadání. Instalaci APK, odinstalování ani vymazání dat neprovádět automaticky. Před aktualizací ověř nový JSON export, původní podpis a vyšší versionCode než skutečně nainstalované APK.
