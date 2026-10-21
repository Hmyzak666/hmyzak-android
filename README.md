# Hmyzák pro Android

Osobní offline tréninkový deník v Kotlinu a Jetpack Compose. Ověřený výchozí bod tohoto sjednocení je **v0.7.14** (`versionCode 7014`, vydání [`hmyzak-build-14`](https://github.com/Hmyzak666/hmyzak-android/releases/tag/hmyzak-build-14)). Starší v0.6 už není poslední vydaná verze.

Sjednocená aktualizace **v0.7.15 / 7015** prošla podepsaným CI a uživatel 2026-10-21 potvrdil její funkčnost i zachování dat na Samsungu. Výchozí větev pro další vývoj je **`main`**; průběh sjednocení je v [PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1).

**Zdroj pravdy je nyní `app/`.** Sestavení nepřepisuje soubory aktualizačními ZIPy. Historické `Hmyzak_update*.zip` zůstávají v historii i v repozitáři pro srovnání a návrat; do dalšího vývoje se nepoužívají.

## Co aplikace obsahuje

- Vlastní tréninkové šablony, cviky, supersérie se dvěma i více cviky, trvalé poznámky upravitelné i během tréninku a historii sérií.
- Předvyplnění předchozích výkonů, automatické posouvání a vibrace při potvrzení série.
- Běh, HIIT, grafy a měření samotné hmotnosti i libovolných vlastních obvodů (biceps, stehno atd.). Všechna pole jsou volitelná, alespoň jedno musí být vyplněné.
- Stále viditelnou délku probíhajícího tréninku, její průběžnou opravu a opravu v historii.
- Časovač pauz: ručně / po sérii / po kole supersérie, pozastavení, ±30 s a pouze vibrační upozornění.
- Samostatné Nastavení se zachováním dřívějšího vzhledu, vibrací a automatického posouvání.
- JSON export/import a ruční sdílení textového výpisu. Aplikace nemá internetové oprávnění.

**v0.9 / tréninkové nástroje** spojuje pět uživatelem schválených úprav do jedné aktualizace. [Použití časovače, supersérií a poznámek](docs/TRENINKOVE_NASTROJE.md). Poslední uživatelem potvrzená instalace na Samsungu je **0.8.17 / 8017**. v0.9 zatím čeká na ověření v telefonu.

Databáze i export používají schéma **8**, import podporuje **1–8**. Migrace 7 → 8 přidává pouze označení supersérie a opravu uplynulé délky; původní sloupce, ID, historie a tělesné míry se nemění. Původní vlastní míry z v0.8 jsou popsané [zde](docs/VLASTNI_TELESNE_MIRY.md). Stav projektu a navázání v novém chatu jsou v [PROJECT_STATUS.md](PROJECT_STATUS.md).

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

První sestavení stáhne závislosti, včetně Android runtime pro Robolectric. `assembleRelease` vždy závisí také na `testReleaseUnitTest`. Bez původního klíče vzniká nepodepsaná release varianta; místní vývojová verze je `0.9.0 / 9000`. **Místní debug ani nepodepsané APK neinstaluj přes používanou aplikaci.**

Místní ověření v0.9: **65 JUnit/Robolectric testů v každé variantě**, **19 Python kontrol**, obě sestavení a lint bez chyb. Fyzické vibrace a chování zamčeného Samsungu vyžadují ověření v telefonu.

## Automatické kontroly a podepsané APK

- **Hmyzák – kontrola zdrojů** (`android-ci.yml`): push/PR spustí Python testy migrace a pravidel vydání, JUnit/Robolectric testy, lint a debug sestavení. Nepoužívá podepisovací secrets.
- **Hmyzák – podepsané APK** (`android-release.yml`): ruční spuštění sestaví kandidáta z vybrané větve, otestuje jej a ověří certifikát, `applicationId` a navazující verzi. Výchozí `publish_release=false` uloží pouze Actions artefakt.
- Zveřejnění v Releases vyžaduje samostatnou volbu `publish_release=true` a větev `main`. Push do `main` sám nic nevydá.

Názvy dosavadních secrets zůstávají stejné: `HMYZAK_KEYSTORE_BASE64`, `HMYZAK_STORE_PASSWORD`, `HMYZAK_KEY_ALIAS`, `HMYZAK_KEY_PASSWORD`. Klíč se nemění ani znovu negeneruje. [Podrobnosti sestavování](docs/AUTOMATICKE_SESTAVOVANI_APK.md).

## Zachování dat a návrat

| Vlastnost | Zachovaná hodnota |
| --- | --- |
| `applicationId` a namespace | `cz.rezim.app` |
| Databáze | `rezim_local_v1.db`, verze 8 |
| JSON záloha | schéma 8, import schémat 1–8 |
| SHA-256 certifikátu | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Původní commit | `ea5f134910111ba65c0b051c2512f9a63ac02998` |

Produkční Kotlin, manifest a prostředky jsou při sjednocení převzaté beze změny z původního ZIPu v0.7 používaného vydáním 14. Sjednocení 14 → 15 nezvyšovalo verzi databáze ani formátu záloh. Navazující v0.8 přidala vlastní míry a migraci na 7; v0.9 přidává dva sloupce pro tréninkové nástroje a schéma 8. Soubor `config/release-compatibility.json` uchovává veřejné kontrolní údaje původního ověřeného vydání 14; před další aktualizací navíc porovnej verzi se skutečně nainstalovaným APK (poslední potvrzené je 8017).

Před aktualizací uchovej JSON export mimo telefon a ověř, že jde o čitelnou zálohu s očekávanými záznamy. Obnovu nejdřív zkoušej v oddělené testovací instalaci: import nahrazuje její data. Aplikaci neodinstalovávej a nemaž její data. [Postup aktualizace v telefonu](docs/AKTUALIZACE_V_TELEFONU.md).

Návrat ke zdrojům zajišťuje tag `hmyzak-build-14`, záložní větev a ověřený úplný gitový bundle. Návrat kódu v telefonu znamená nové APK se stejným certifikátem, kompatibilní databází a vyšším `versionCode`; samotná instalace starého APK není plán návratu. [Přehled sjednocení a ověření](docs/SJEDNOCENI_ZDROJU.md).

## Pokračování vývoje

Začni souborem [PROJECT_STATUS.md](PROJECT_STATUS.md), načti aktuální `main`, zkontroluj stav CI a založ z něj samostatnou větev. Historické poznámky `CO_JE_NOVEHO_*` a `OVERENI_V06/V07` popisují tehdejší vydání; aktuální postup je tento README. Pomocný `opravit-zavislosti.sh` je historický ruční nástroj, běžné sestavení jej nepoužívá.

Soukromé klíče, hesla, APK, lokální databáze a osobní exporty nepatří do repozitáře. `.gitignore` pokrývá obvyklé soubory; před commitem zkontroluj `git diff --cached`.
