# LiftAnt pro Android

LiftAnt (dříve Hmyzák) je tréninkový deník pro Android s lokální databází. Zdroj pravdy je `app/`; sestavení nepoužívá historické aktualizační ZIPy. Nová větev `feat/public-ready-v012` připravuje kandidáta **0.12.26 / 12026** pro běžné uživatele a vychází z aktuálního LiftAntu 0.11.24 včetně změn supersérií z v0.10. Main zůstává samostatným stabilním základem.

## Funkce

- Síla, libovolné vlastní plány, cviky, poznámky, série, historie a grafy.
- První spuštění bez osobního plánu autora: prázdný začátek, upravitelné šablony Full Body 3×, Upper/Lower 4× a Push/Pull/Legs 3× týdně, nebo obnova vlastní JSON zálohy.
- Supersérie více cviků s prvními, posledními nebo konkrétními koly, automatický posun, měřitelná/opravitelná délka tréninku a vibrační časovač pauz.
- Běh, univerzální HIIT s volitelnou aktivitou, váha bez obvodu a vlastní tělesné míry.
- Čeština, angličtina, němčina, italština, francouzština a ruština; všechny jazyky jsou dostupné offline.
- Nazvedaný objem dokončených sérií s volbou jedné/dvou činek, ruční AI textový výpis a sdílené obrázkové souhrny.
- Ruční úplná JSON záloha/obnova včetně trvalých nastavení, výrazné varování před nahrazením a možnost nejdřív uložit dosavadní data.
- Android Auto Backup s databází a trvalými nastaveními, vynuceným podporovaným šifrováním cloudu a podporou přenosu mezi zařízeními. Limit 25 MB a čas záloh řídí Android. Android 8/8.1 používá ruční JSON; dostupnost ani obnova nejsou garantované. Obnova přenáší i jazyk ze starších verzí Androidu a vyžádá nové ověření nákupu přes Play.

Zápis, celá historie, šablony, základní grafy, časovač, sdílení a oba druhy záloh jsou zdarma a bez reklam či časového omezení. **Lifetime Pro** je jednorázový INAPP nákup `liftant_pro` pro porovnání období 7/30/90 dní a CSV; žádné předplatné. Ceny se nastavují v Play Console, aplikace je načítá z obchodu. Skutečné nákupy ještě vyžadují aktivní Console a licenční testy.

## Identita a zachování dat

| Vlastnost | Hodnota |
| --- | --- |
| applicationId / namespace | `cz.rezim.app` |
| Databáze | `rezim_local_v1.db`, schéma 11 |
| JSON | schéma 11, import 1–11 |
| Původní podpis kompatibilního APK | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |

Migrace 10→11 přidává pouze typ HIIT aktivity a tabulku dokončení onboardingu. U starších uživatelů nemění plány, historii, poznámky, měření ani supersérie. Šablony se nikdy automaticky nevkládají po aktualizaci. Import staré zálohy se zachovává. Podpis stávajícího testovacího APK je Android Debug a nelze jej použít pro Play; přechod vyžaduje zvlášť ověřenou ruční zálohu a obnovu. Starší aplikace neumí schéma 11: uchovat předaktualizační export a neinstalovat staré APK naslepo.

## Sestavení a kontroly

JDK 17, Android SDK Platform 36, Build Tools 35.0.0. Gradle Wrapper 8.13, AGP 8.13.2, Kotlin 2.2.21; minSdk 26, targetSdk 36, Play Billing 9.1.0. SDK nastav přes `ANDROID_HOME` nebo necommitovaný `local.properties`.

```bash
bash ./prepare-gradle-wrapper.sh
python3 docs/overit_migraci_mereni.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease
```

Regresní kontroly pokrývají i čistý telefon/tablet, obnovení prázdného plánu, samostatnou kopii SQLite po checkpointu WAL a odmítnutí neověřeného nákupu.

Výchozí sestavení má Pro zamčené. Výslovný **testovací režim** je `-PliftantTestPro=true`; kompatibilní ruční workflow jej používá a aplikace jej označuje. Veřejný Play režim `-PliftantPlayBuild=true` testovací přepínač odmítne, vyžaduje skutečný veřejný Billing klíč a samostatný upload keystore. Žádné soukromé klíče nebo hesla se negenerují či neukládají do zdrojů. Release APK i AAB závisí na release regresních testech.

- `android-ci.yml`: kontroly a náhledové APK/AAB bez secrets. Náhledové AAB není balíček určený do obchodu.
- `android-release.yml`: ručně spouštěné testovací APK s původními `HMYZAK_*` secrets, kontrolou identity, podpisu a vyšší verze. `publish_release=false` uloží pouze artefakt; main ani Google Play se automaticky nevydávají.

## Ověřený kandidát

[PR #6](https://github.com/Hmyzak666/hmyzak-android/pull/6) je samostatný draft navazující na #5. [CI 45](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082505438) ověřilo 112 Android testů v každé variantě (debug/release), 31 Python kontrol, lint bez chyb (27 upozornění), APK a náhledové AAB. [Kompatibilní APK 26](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082544715) má verzi 0.12.26 a původní podpis. Stažený soubor, podpis, identita a kontrolní součty byly nezávisle ověřené; podrobnosti jsou v PROJECT_STATUS.md. Testovací Pro je výslovně odemčené. Skutečná obnova cloudem a nákupy přes Play dosud nejsou ověřené.

## Pokračování a bezpečné ověření

Nejdřív načti [PROJECT_STATUS.md](PROJECT_STATUS.md) a aktuální větve/Actions; nepoužívej staré ZIPy. Kompletní popis migrace, záloh, Free/Pro, zbývající konfigurace Play, první instalace na tabletu i bezpečného testu obnovy je v [přípravě veřejné verze](docs/LIFTANT_PUBLIC_RELEASE.md). Starší [technický základ](docs/SJEDNOCENI_ZDROJU.md) a [tréninkové nástroje](docs/TRENINKOVE_NASTROJE.md) zůstávají zdokumentované.

Před aktualizací používaného telefonu ověř aktuální čitelnou JSON zálohu mimo telefon, stejný certifikát a vyšší versionCode. Obnovu testuj na odděleném zařízení/profilu. Nikdy automaticky neodinstalovávej aplikaci ani nemaž její data. Zelené automatické testy nenahrazují skutečný cloudový přenos, Samsung vibrace ani nákupy přes Play.

Osobní exporty, databáze, APK, klíče, hesla a tokeny nepatří do repozitáře. Historické ZIPy a rollback větev `backup/pre-source-unification-2026-10-20` zůstávají zachované.
