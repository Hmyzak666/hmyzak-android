# Sjednocení zdrojů – audit, návrat a ověření

Datum: **2026-10-20**. Pracovní větev: `chore/unify-sources`. Původní `main` a vydání 14 zůstávají zachované.

## Co audit doložil

Původní soubory v `app/` nebyly zdrojem posledního APK: obsahovaly starší kód, zatímco workflow před každým buildem rozbalovalo nejvyšší číselně pojmenovaný `Hmyzak_update_v*.zip`. Poslední použitý balíček byl **v0.7**, nikoli v0.6. Soubor `Hmyzak_update_v0.7-1.zip` má totožný obsah, ale staré pravidlo jeho název ignorovalo.

| Srovnání s původním commitem `ea5f134` | ZIP v0.6 | ZIP v0.7 |
| --- | --- | --- |
| Počet souborů v ZIPu | 30 | 35 |
| Odlišné nebo chybějící soubory v původním stromu | 17 | 22 |
| Databázová verze / schéma exportu | 5 / 5 | 6 / 6 |
| SHA-256 ZIPu | `01257c7a3886fb11b247cf68bc5d357c6a83b4aae2b9d161b9a7298b7c0f5385` | `a0445f738f7221cb2e372a39b891079814bee2af6896e0512a0a3588fa789409` |

Rozdíl 5 → 6 ve vydané aplikaci umožňuje hmotnost bez obvodu. Sjednocení **nepřidává další migraci**: přebírá existující LocalStore z v0.7 beze změny, včetně všech starších migrací.

Prohlédnuté oblasti: build konfigurace, Wrapper a pomocné skripty; manifest a všechny produkční Kotlin soubory; ikony a ostatní prostředky; databáze/migrace/export/import; dosavadní testy; dokumentace; Actions, vydání, tagy a všech sedm ZIPů. Soukromé klíče ani osobní databáze audit nepotřebuje a repozitář je nemá obsahovat.

## Bod návratu vytvořený před úpravami

- Původní commit: `ea5f134910111ba65c0b051c2512f9a63ac02998`.
- Existující vzdálený tag: [`hmyzak-build-14`](https://github.com/Hmyzak666/hmyzak-android/releases/tag/hmyzak-build-14).
- Místní větev: `backup/pre-source-unification-2026-10-20` na témže commitu.
- Úplný bundle původní historie: `.git/backups/hmyzak-before-unification-20261020.bundle`, ověřený pomocí `git bundle verify`.
- Původní APK `Hmyzak-v0.7-build-14.apk` bylo staženo a ověřeno proti vydání. SHA-256: `7fc1722626f3edabf55585437d3706a2117258342af21c32b0016deb005d03b5`.

GitHub konektor vrátil při pokusu vytvořit vzdálenou záložní větev **403 Resource not accessible by integration**. Následně uživatel schválil přenos přes prohlížeč. Vzdálená záložní větev i pracovní větev byly takto vytvořené a jejich výchozí odkazy ověřené přes GitHub API na původním commitu `ea5f134910111ba65c0b051c2512f9a63ac02998`. Přenos je dokončený pouze do pracovní větve; `main` i záložní větev dál ukazují na původní commit. Předání obsahuje také úplný bundle místní pracovní historie, aby práce nezávisela na dočasném pracovním prostředí.

Původní stav lze prohlížet bez změny aktivní větve:

```bash
git worktree add --detach ../hmyzak-puvodni hmyzak-build-14
```

Původní strom pro tehdejší build stále vyžadoval ZIP overlay. Pro obnovu aktuálního kódu použij sjednocenou pracovní větev a ověřenou build konfiguraci. Místní historie v předaném bundle obsahuje také commit `8296f1e` s přesně materializovaným ZIPem v0.7. `main` nepřepisuj příkazem reset/force-push. Obnovu chování připrav jako běžnou změnu na nové větvi.

**Zdrojová záloha není záloha dat telefonu.** Návrat v telefonu vyžaduje stejné `applicationId`, stejný certifikát, kompatibilní databázi 6 a nové vyšší číslo APK. Neinstaluj starou databázovou verzi ani neodinstalovávej používanou aplikaci.

## Připravené změny

1. Commit `8296f1e` převzal všech 35 souborů ZIPu v0.7 byte-for-byte do verzovaného stromu. Produkční `app/src/main/` se pak již nemění.
2. Actions sestavuje přímo strom commitu. ZIPy zůstávají beze změny jako historický podklad; žádné workflow je nerozbaluje.
3. Běžné PR/push kontroly nepoužívají klíče. Ruční podepsaný kandidát se automaticky nezveřejňuje; samostatná publikace je možná pouze z `main`.
4. Ověření APK nově hlídá nejen platnost podpisu, ale jeho přesný certifikát, identitu a číslo verze. Zachované secrets nejsou přejmenované.
5. Oficiální Wrapper JAR je součástí projektu, připnutý je kontrolní součet JARu i distribuce. `.gitignore` chrání výstupy sestavení, místní konfiguraci a obvyklé soukromé exporty/klíče.
6. README, postup pro telefon, sestavování a PROJECT_STATUS popisují aktuální zdroje a návaznost vývoje.

## Výsledky ověření

Místní kontroly prošly na JDK 17, Gradle 8.13 a Android SDK 36 / Build Tools 35.0.0:

| Kontrola | Výsledek |
| --- | --- |
| `testDebugUnitTest` | 27 testů, 0 chyb, 0 přeskočených |
| `testReleaseUnitTest` | 27 testů, 0 chyb, 0 přeskočených |
| `assembleDebug` | Úspěch, vývojové APK |
| `assembleRelease` | Úspěch, nepodepsané APK bez původního klíče |
| `lintDebug` a `lintRelease` | Úspěch, bez chyb; upozornění viz níže |
| Kontrola nového vydání na místním debug APK | Správně odmítnuto kvůli jinému certifikátu |
| Kontrola nového vydání na nepodepsaném APK | Správně odmítnuto kvůli chybějícímu podpisu |

Příkaz `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --no-daemon --console=plain` skončil **BUILD SUCCESSFUL** (113 úloh, první stažení závislostí). Samostatně prošlo `./gradlew :app:lintRelease --no-daemon`. Při prvním doplňkovém lint běhu byla poškozená jedna položka místní Gradle transform cache; po jejím přesunu mimo cache a znovuvytvoření kontrola uspěla bez změn projektu. Síťová konfigurace zdejšího prostředí je místní a není součástí repozitáře.

Další ověření:

- Všech **20 souborů `app/src/main/`** přesně odpovídá ZIPu v0.7. LocalStore, manifest, ikony a prostředky se nemění.
- Všech **7 původních ZIPů** má nezměněný obsah proti původnímu commitu.
- **6 Python testů pravidel vydání** prošlo.
- **6 Python testů SQL migrace měření** prošlo.
- **actionlint 1.7.12** ověřil obě workflow bez chyb.
- Původní APK prošlo `apksigner verify`, kontrolou přesného certifikátu, applicationId, verze 7014 / 0.7.14 a SHA-256.

Lint ponechává 25 upozornění v každé variantě: stávající target SDK, parametry Compose Modifier, pravidla systémových záloh, nadbytečnou kontrolu SDK, nepoužitý prostředek, tvary a monochromatickou variantu ikony a doporučení KTX. Kompilátor také upozorňuje na starší DSL a několik zachovaných API/typových konstrukcí. Neměníme je v tomto sjednocení, aby produkční zdroje zůstaly shodné s funkčním vydáním; lze je řešit samostatnou změnou s cíleným ověřením.

Při přenosu přes web se nezachovává příznak spustitelnosti nového unixového Wrapperu. CI a aktuální návody jej proto spouštějí explicitně přes `bash ./gradlew`; obsah oficiálního Wrapperu ani jeho připnuté kontrolní součty se nemění. Po přenosu všech 40 změněných souborů bylo přes GitHub API porovnáno všech 62 souborů vzdáleného commitu `1309cca4675a6f567e995814245df38959363b71` s lokální předlohou: žádné rozdíly obsahu ani režimů, žádné chybějící či přidané soubory.

Na tomto commitu uspělo nové [push CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494342209) i [PR CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494518660). První build skončil `BUILD SUCCESSFUL in 3m 31s`. Stažený artifact `Hmyzak-kontroly-1` má SHA-256 `db61485277805c547d8e3138d071ccc49789abf689ee55ecced3b7a1588036f2`; jeho JUnit XML potvrzují 5 testů vstupu, 7 testů LocalStore a 15 testů navigace, celkem 27 bez chyb a přeskočení.

Stažené lint XML z GitHubu mají v obou variantách **0 chyb a 30 upozornění**. Oproti místním reportům přibylo 5 upozornění na dostupné novější verze Android Gradle Pluginu a čtyř knihoven (`AndroidGradlePluginVersion`, `GradleDependency`); ostatní upozornění se shodují. Verze závislostí se v tomto sjednocení nemění.

Ze stejného commitu uspěl také [podepsaný kandidát, běh 15](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494987115), se zachovanými původními secrets. Release testy a lint prošly před obnovou klíče; následovalo sestavení, ověření skutečného podpisu přes `apksigner`, přesného certifikátu, applicationId a navazující verze. Úloha `publish` byla přeskočena a dočasný klíč odstraněn. Stažené JUnit XML release varianty potvrzují všech 27 testů bez chyb a přeskočení.

| Ověřený kandidát | Hodnota |
| --- | --- |
| APK | `Hmyzak-v0.7-build-15.apk`, verze `0.7.15`, versionCode `7015` |
| applicationId | `cz.rezim.app` |
| Certifikát SHA-256 | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| APK SHA-256 | `e8640703e9e8e743ecf5fc095736244ba72b164de82b94b079d0c81175bde83f` |
| Artifact `Hmyzak-APK-15` SHA-256 | `1745e84b9d61502dbcaeb9ccc6ca2cebb690dc5087c36f48e3a658ef0edfe43e` |

Artifact byl stažen přes GitHub konektor. Jeho kontrolní součet odpovídá GitHub digestu; skutečný SHA-256 souboru APK souhlasí s `SHA256SUMS.txt` i `overeni-apk.json`. Report potvrzuje původní certifikát, applicationId, očekávanou verzi a platný podpis. Jde o kandidáta v Actions artifacts, nikoli nové zveřejněné vydání ani ověřenou instalaci na telefonu.

## Testovací základ a další ověření

Automatická sada zahrnuje původních 15 testů navigace supersérií a 5 testů vstupu měření. Přidaných 7 Robolectric testů používá skutečný LocalStore a nativní SQLite: migraci celé syntetické databáze v5 → 6, opětovné otevření v6, export/import v6, import starších schémat 1–5, rozpracovaný trénink, poznámky, dvojice, měření bez obvodu a návrat transakce při vadném importu. Staré schéma v5 je nezávislá fixture vytažená ze ZIPu v0.6.

Pro další vývoj navrhuji doplnit testy přímých databázových upgradů z každé historické verze 1–4 a malou sadu testů na zařízení/emulátoru: zápis série a obnovení rozpracovaného tréninku po restartu, vážení bez obvodu a export/import přes systémový výběr souboru. Časovač a vícenásobné supersérie budou potřebovat vlastní testy až při implementaci.

**Stav navazujících kroků:** přenos je dokončený v konceptu [PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1), nové push i PR CI a podepsaný kandidát jsou zelené. Před instalací nezávisle ověřit skutečnou JSON zálohu a teprve s uživatelem zkontrolovat chování aktualizace na telefonu. Nové APK nebylo instalováno, `main` není sloučené a žádné staré soubory či ZIPy nebyly odstraněny. Následující dokumentační commity pouze zaznamenávají výsledky; produkční zdroje a konfigurace sestavení zůstávají stejné jako v ověřeném `1309cca`.
