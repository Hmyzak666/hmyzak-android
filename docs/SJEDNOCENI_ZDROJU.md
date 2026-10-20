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

GitHub konektor vrátil při pokusu vytvořit vzdálenou záložní větev **403 Resource not accessible by integration**. Následně uživatel schválil přenos přes prohlížeč. Vzdálená záložní větev i pracovní větev byly takto vytvořené a jejich odkazy ověřené přes GitHub API na původním commitu `ea5f134910111ba65c0b051c2512f9a63ac02998`; přenos souborů pokračuje pouze do pracovní větve. Předání obsahuje také úplný bundle nové pracovní větve, aby práce nezávisela na dočasném pracovním prostředí.

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

Při přenosu přes web se nezachovává příznak spustitelnosti nového unixového Wrapperu. CI a aktuální návody jej proto spouštějí explicitně přes `bash ./gradlew`; obsah oficiálního Wrapperu ani jeho připnuté kontrolní součty se nemění.

## Testovací základ a další ověření

Automatická sada zahrnuje původních 15 testů navigace supersérií a 5 testů vstupu měření. Přidaných 7 Robolectric testů používá skutečný LocalStore a nativní SQLite: migraci celé syntetické databáze v5 → 6, opětovné otevření v6, export/import v6, import starších schémat 1–5, rozpracovaný trénink, poznámky, dvojice, měření bez obvodu a návrat transakce při vadném importu. Staré schéma v5 je nezávislá fixture vytažená ze ZIPu v0.6.

Pro další vývoj navrhuji doplnit testy přímých databázových upgradů z každé historické verze 1–4 a malou sadu testů na zařízení/emulátoru: zápis série a obnovení rozpracovaného tréninku po restartu, vážení bez obvodu a export/import přes systémový výběr souboru. Časovač a vícenásobné supersérie budou potřebovat vlastní testy až při implementaci.

**Stav navazujících kroků:** vzdálená záložní a pracovní větev existují na GitHubu. Dokončení přenosu souborů, návrh PR, nové CI a podepsaného kandidáta se stávajícími secrets ověř v aktuálních Actions a PR. Pak nezávisle ověřit skutečnou JSON zálohu a teprve s uživatelem zkontrolovat aktualizaci na telefonu. Nové APK zatím nebylo instalováno, `main` není sloučené a žádné staré soubory či ZIPy nebyly odstraněny.
