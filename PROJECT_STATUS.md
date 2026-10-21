# Hmyzák – stav projektu a pokračování

Aktualizováno **2026-10-21**. Nejprve přečti tento soubor, README a `docs/SJEDNOCENI_ZDROJU.md`.

## Aktuální stav

Poslední verze **potvrzená uživatelem na Samsungu je 0.8.17 / 8017**. Uživatel potvrdil funkčnost nové verze s vlastními tělesnými mírami. Sjednocení zdrojů je sloučené v [PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1), vlastní míry a technická údržba v [PR #2](https://github.com/Hmyzak666/hmyzak-android/pull/2). Po sloučení #2 uspělo také [CI na main](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39557210949).

Uživatel následně požádal udělat **všech pět tréninkových úprav najednou**. Připravená v0.9 ve větvi `feat/workout-tools-v09` obsahuje časovač pauz, skupiny supersérií, úpravy trvalých poznámek při tréninku, průběžnou délku a samostatné Nastavení. Viz `docs/TRENINKOVE_NASTROJE.md`. Nejde o potvrzenou instalaci na telefonu; aktuální výsledky vždy ověř v Actions a PR této větve.

Databáze a export mají verzi **8**, import podporuje **1–8**. Přechod 7 → 8 pouze přidává `plan_exercises.superset_id` a `sessions.elapsed_offset_seconds`. Staré dvojice se převedou na skupiny; původní sloupce zůstávají zachované. Oprava průběžné délky nemění původní čas zahájení. Testy pokrývají skutečné schéma v0.8, starší zálohy, změny při rozpracovaném tréninku, čtyřčlennou skupinu 4/4/4/1, časovač a UI (Nastavení také při šířce 360 dp). SDK/Compose závislosti, ikona, applicationId a podpis zůstávají stejné; lint upozornění k nim se nepotlačují.

Místní kontrola v0.9 prošla: **65 JUnit/Robolectric testů v debug i release variantě**, **19 Python kontrol**, debug a nepodepsané release sestavení, lint obou variant bez chyb. Místní lint hlásil 9 neblokujících upozornění; počty kontrol dostupnosti novějších závislostí se mohou v hosted CI lišit. Oddělený neveřejný test načetl skutečnou původní zálohu přímo přes Kotlin import do schématu 8 a ověřil všechny původní hodnoty, integritu i vazby. Osobní export ani tento test nejsou v repozitáři. Aktuální remote CI a podpis nového APK je nutné ověřit v Actions; telefon zatím potvrdil pouze v0.8.17.

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
| Databáze | `rezim_local_v1.db`, SQLiteOpenHelper verze 8 (migrace z 7; zachované starší migrace) |
| Export | `rezim-local-backup`, schéma 8, import 1–8 |
| Číslování vydání | `versionBaseCode + GITHUB_RUN_NUMBER`, v0.9: `9000 + běh`, v0.8 používala `8000 + běh`; v0.7 používala `7000 + běh` |
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
6. Před sloučením další změny ověř zelené CI. Před aktualizací telefonu ověř nový JSON export, původní certifikát a vyšší `versionCode` než skutečně nainstalovaná verze (poslední potvrzená je 8017). Instalaci, odinstalování a vymazání dat neprováděj automaticky.

Návrat zajišťuje původní tag, místní i vzdálená větev `backup/pre-source-unification-2026-10-20` a ověřený úplný gitový bundle. Návrat kódu v telefonu se připravuje jako další kompatibilní aktualizace se stejným certifikátem a vyšší verzí; databázi nesnižovat.

## Dohodnutý rozsah v0.9

- Časovač: ruční spuštění je výchozí. Volitelně po každé sérii nebo po celém kole supersérie; poslední série tréninku už pauzu nespouští. Nastavitelných 1 s až 60 min, pozastavení, pokračování, ±30 s a nové spuštění. Bez zvuku, samostatný přepínač vibračního upozornění. Odpočet je místní, založený na monotónním čase a běží ve foreground službě; po restartu telefonu se ruší. K zobrazení v liště může být potřeba povolit oznámení. Ověření na skutečném Samsungu při zamčeném displeji zbývá.
- Supersérie: uživatel vybírá členy sám v plánu i během tréninku. Pořadí určuje plán. Příklad leg curl 4×, leg extension 4×, side band abduction 4×, side adduction 1× znamená adduction **v prvním kole**, poté se vynechává. Změny členství jsou trvalé a nemění už zapsané série.
- Trvalá poznámka: tlačítko u každého rozpracovaného cviku, změna platí i pro další tréninky.
- Délka tréninku: stále viditelná nahoře a opravitelná během cvičení; čas potom dále přibývá. Oprava v historii zůstává.
- Nastavení: vlastní obrazovka z horní lišty a přehledu, původní vzhled, přepínače vibrací, oba testy vibrací, systémové vibrace a nastavení pauz.

Tyto položky už nejsou pouze návrh: uživatel je 2026-10-21 výslovně schválil společně. Další nové funkce nepřidávat bez zadání. Instalaci APK, odinstalování ani vymazání dat neprovádět automaticky. Před aktualizací ověř nový JSON export, původní podpis a vyšší versionCode než skutečně nainstalované APK.
