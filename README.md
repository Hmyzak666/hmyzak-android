# Hmyzák pro Android

Osobní offline tréninkový deník v Kotlinu a Jetpack Compose. Ověřený výchozí bod tohoto sjednocení je **v0.7.14** (`versionCode 7014`, vydání [`hmyzak-build-14`](https://github.com/Hmyzak666/hmyzak-android/releases/tag/hmyzak-build-14)). Starší v0.6 už není poslední vydaná verze.

**Zdroj pravdy je nyní `app/`.** Sestavení nepřepisuje soubory aktualizačními ZIPy. Historické `Hmyzak_update*.zip` zůstávají v historii i v repozitáři pro srovnání a návrat; do dalšího vývoje se nepoužívají.

## Co aplikace obsahuje

- Vlastní tréninkové šablony, cviky, dvojice supersérií, trvalé poznámky a historii sérií.
- Předvyplnění předchozích výkonů, automatické posouvání a vibrace při potvrzení série.
- Běh, HIIT, grafy a denní měření hmotnosti s nepovinným obvodem pasu.
- Zobrazení délky tréninku a opravu délky v detailu historie.
- JSON export/import a ruční sdílení textového výpisu. Aplikace nemá internetové oprávnění.

Časovač pauz a supersérie se třemi či více cviky jsou teprve plánované. Stav a další dohodnuté požadavky jsou v [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Místní vývoj

Použij JDK 17, Android SDK Platform 36 a Build Tools 35.0.0. Projekt zachovává Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21 a Compose BOM 2025.08.00; `minSdk 26`, `targetSdk 35`. Oficiální Gradle Wrapper včetně JAR je součástí projektu a má připnuté kontrolní součty.

Nastav `ANDROID_HOME` na svůj SDK nebo vlož `sdk.dir=/cesta/k/android-sdk` do necommitovaného `local.properties`. Pak z kořene projektu:

```bash
bash ./prepare-gradle-wrapper.sh
python3 docs/overit_migraci_mereni.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
bash ./gradlew :app:assembleRelease
```

První sestavení stáhne závislosti, včetně Android runtime pro Robolectric. `assembleRelease` vždy závisí také na `testReleaseUnitTest`. Bez původního klíče vzniká nepodepsaná release varianta; místní vývojová verze je `0.7.0 / 7000`. **Místní debug ani nepodepsané APK neinstaluj přes používanou aplikaci.**

## Automatické kontroly a podepsané APK

- **Hmyzák – kontrola zdrojů** (`android-ci.yml`): push/PR spustí Python testy migrace a pravidel vydání, JUnit/Robolectric testy, lint a debug sestavení. Nepoužívá podepisovací secrets.
- **Hmyzák – podepsané APK** (`android-release.yml`): ruční spuštění sestaví kandidáta z vybrané větve, otestuje jej a ověří certifikát, `applicationId` a navazující verzi. Výchozí `publish_release=false` uloží pouze Actions artefakt.
- Zveřejnění v Releases vyžaduje samostatnou volbu `publish_release=true` a větev `main`. Push do `main` sám nic nevydá.

Názvy dosavadních secrets zůstávají stejné: `HMYZAK_KEYSTORE_BASE64`, `HMYZAK_STORE_PASSWORD`, `HMYZAK_KEY_ALIAS`, `HMYZAK_KEY_PASSWORD`. Klíč se nemění ani znovu negeneruje. [Podrobnosti sestavování](docs/AUTOMATICKE_SESTAVOVANI_APK.md).

## Zachování dat a návrat

| Vlastnost | Zachovaná hodnota |
| --- | --- |
| `applicationId` a namespace | `cz.rezim.app` |
| Databáze | `rezim_local_v1.db`, verze 6 |
| JSON záloha | schéma 6, import schémat 1–6 |
| SHA-256 certifikátu | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Původní commit | `ea5f134910111ba65c0b051c2512f9a63ac02998` |

Produkční Kotlin, manifest a prostředky jsou při sjednocení převzaté beze změny z původního ZIPu v0.7 používaného vydáním 14. Sjednocení nezvyšuje verzi databáze ani formátu záloh. Soubor `config/release-compatibility.json` uchovává veřejné kontrolní údaje posledního ověřeného vydání.

Před aktualizací uchovej JSON export mimo telefon a ověř, že jde o čitelnou zálohu s očekávanými záznamy. Obnovu nejdřív zkoušej v oddělené testovací instalaci: import nahrazuje její data. Aplikaci neodinstalovávej a nemaž její data. [Postup aktualizace v telefonu](docs/AKTUALIZACE_V_TELEFONU.md).

Návrat ke zdrojům zajišťuje tag `hmyzak-build-14`, záložní větev a ověřený úplný gitový bundle. Návrat kódu v telefonu znamená nové APK se stejným certifikátem, kompatibilní databází a vyšším `versionCode`; samotná instalace starého APK není plán návratu. [Přehled sjednocení a ověření](docs/SJEDNOCENI_ZDROJU.md).

## Pokračování vývoje

Začni souborem [PROJECT_STATUS.md](PROJECT_STATUS.md), zkontroluj větev a stav CI a pracuj v samostatné větvi. Historické poznámky `CO_JE_NOVEHO_*` a `OVERENI_V06/V07` popisují tehdejší vydání; aktuální postup je tento README. Pomocný `opravit-zavislosti.sh` je historický ruční nástroj, běžné sestavení jej nepoužívá.

Soukromé klíče, hesla, APK, lokální databáze a osobní exporty nepatří do repozitáře. `.gitignore` pokrývá obvyklé soubory; před commitem zkontroluj `git diff --cached`.
