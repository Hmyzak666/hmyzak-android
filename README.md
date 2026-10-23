# LiftAnt pro Android

**LiftAnt (dříve Hmyzák)** — offline tréninkový deník. Kandidát **0.11** se připravuje ve větvi `feat/liftant-international`; stabilní main zatím obsahuje Hmyzáka 0.10. Níže je zachovaná historie technického základu.

Nový kandidát přidává značku a ikonu LiftAnt, šest jazyků, nazvedanou hmotnost se započítáním dvou jednoruček, obrázkové souhrny tréninku a základ LiftAnt Pro (jednorázový nákup, pokročilé statistiky a CSV). Běžný deník, celá historie, časovač a JSON zálohy zůstávají zdarma. Aktuální databáze a JSON kandidáta mají schéma 10 s importem 1–10. Existující názvy, poznámky, ID a historie se zachovávají; noví uživatelé nedostávají osobní běžecké záznamy.

[Podrobný popis, testovací APK a zbývající kroky pro Google Play](docs/LIFTANT_V011.md). Produkční platby ještě vyžadují konfiguraci a ověření v Play Console; běžný kandidát má Pro odemčené a nic neúčtuje.

[Návrh změn PR #5](https://github.com/Hmyzak666/hmyzak-android/pull/5) zůstává oddělený od main. Dosavadní CI ověřilo 93 testů v debug i release, 25 Python kontrol a sestavení APK/AAB; finální stav podepsaného kandidáta a další kroky jsou v `PROJECT_STATUS.md`. Skutečné nákupy a test na telefonu se ověřují zvlášť.

## Historie technického základu Hmyzáka


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

**v0.10 / kola supersérií** přidává pro každý cvik volbu **První kola / Poslední kola / Od konkrétního kola**. Bench 4× a kliky 1× tak mohou skončit kliky až po čtvrté sérii benche; lze zvolit i druhé nebo třetí kolo. Volba se ukládá v plánu a jde měnit při rozpracovaném tréninku. [Použití tréninkových nástrojů](docs/TRENINKOVE_NASTROJE.md).

Poslední uživatelem potvrzená instalace na Samsungu je **0.9.18 / 9018**. Zdrojové sjednocení, vlastní míry a pět tréninkových nástrojů jsou sloučené v PR #1–3. Nová v0.10 vyžaduje samostatné ověření v telefonu.

Databáze a export používají schéma **9**, import podporuje **1–9**. Migrace 8 → 9 přidává jediný sloupec `plan_exercises.superset_start_round` s výchozí hodnotou 1; staré cviky tedy dál začínají v prvním kole. Historie, ID, poznámky, délky a míry se nemění. [Vlastní míry](docs/VLASTNI_TELESNE_MIRY.md), [stav a pokračování v novém chatu](PROJECT_STATUS.md).

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

První sestavení stáhne závislosti, včetně Android runtime pro Robolectric. `assembleRelease` vždy závisí také na `testReleaseUnitTest`. Bez původního klíče vzniká nepodepsaná release varianta; místní vývojová verze je `0.10.0 / 10000`. **Místní debug ani nepodepsané APK neinstaluj přes používanou aplikaci.**

Historické místní ověření v0.9: **65 JUnit/Robolectric testů v každé variantě**, **19 Python kontrol**, obě sestavení a lint bez chyb. Fyzické vibrace a chování zamčeného Samsungu vyžadují ověření v telefonu.

## Automatické kontroly a podepsané APK

- **Hmyzák – kontrola zdrojů** (`android-ci.yml`): push/PR spustí Python testy migrace a pravidel vydání, JUnit/Robolectric testy, lint a debug sestavení. Nepoužívá podepisovací secrets.
- **Hmyzák – podepsané APK** (`android-release.yml`): ruční spuštění sestaví kandidáta z vybrané větve, otestuje jej a ověří certifikát, `applicationId` a navazující verzi. Výchozí `publish_release=false` uloží pouze Actions artefakt.
- Zveřejnění v Releases vyžaduje samostatnou volbu `publish_release=true` a větev `main`. Push do `main` sám nic nevydá.

Názvy dosavadních secrets zůstávají stejné: `HMYZAK_KEYSTORE_BASE64`, `HMYZAK_STORE_PASSWORD`, `HMYZAK_KEY_ALIAS`, `HMYZAK_KEY_PASSWORD`. Klíč se nemění ani znovu negeneruje. [Podrobnosti sestavování](docs/AUTOMATICKE_SESTAVOVANI_APK.md).

## Zachování dat a návrat

| Vlastnost | Zachovaná hodnota |
| --- | --- |
| `applicationId` a namespace | `cz.rezim.app` |
| Databáze | `rezim_local_v1.db`, verze 9 |
| JSON záloha | schéma 9, import schémat 1–9 |
| SHA-256 certifikátu | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Původní commit | `ea5f134910111ba65c0b051c2512f9a63ac02998` |

Produkční Kotlin, manifest a prostředky jsou při sjednocení převzaté beze změny z původního ZIPu v0.7 používaného vydáním 14. Sjednocení 14 → 15 nezvyšovalo verzi databáze ani formátu záloh. Navazující v0.8 přidala vlastní míry a migraci na 7; v0.9 přidala dva sloupce pro tréninkové nástroje a schéma 8, v0.10 přidává počáteční kolo supersérie a schéma 9. Soubor `config/release-compatibility.json` uchovává veřejné kontrolní údaje původního ověřeného vydání 14; před další aktualizací navíc porovnej verzi se skutečně nainstalovaným APK (poslední potvrzené je 9018).

Před aktualizací uchovej JSON export mimo telefon a ověř, že jde o čitelnou zálohu s očekávanými záznamy. Obnovu nejdřív zkoušej v oddělené testovací instalaci: import nahrazuje její data. Aplikaci neodinstalovávej a nemaž její data. [Postup aktualizace v telefonu](docs/AKTUALIZACE_V_TELEFONU.md).

Návrat ke zdrojům zajišťuje tag `hmyzak-build-14`, záložní větev a ověřený úplný gitový bundle. Návrat kódu v telefonu znamená nové APK se stejným certifikátem, kompatibilní databází a vyšším `versionCode`; samotná instalace starého APK není plán návratu. [Přehled sjednocení a ověření](docs/SJEDNOCENI_ZDROJU.md).

## Pokračování vývoje

Začni souborem [PROJECT_STATUS.md](PROJECT_STATUS.md), načti aktuální `main`, zkontroluj stav CI a založ z něj samostatnou větev. Historické poznámky `CO_JE_NOVEHO_*` a `OVERENI_V06/V07` popisují tehdejší vydání; aktuální postup je tento README. Pomocný `opravit-zavislosti.sh` je historický ruční nástroj, běžné sestavení jej nepoužívá.

Soukromé klíče, hesla, APK, lokální databáze a osobní exporty nepatří do repozitáře. `.gitignore` pokrývá obvyklé soubory; před commitem zkontroluj `git diff --cached`.
