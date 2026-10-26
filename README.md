> **Ověřené doplnění 0.13.29 / 13029 (2026-10-26).** Ruční kardio, vlastní zkratky s HIIT předvolbami a časové vstupy jsou uložené ve větvi `feat/liftant-v013-cardio`. [CI 48](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40307850906) i [podepsané APK 29](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40308252295) uspěly nad commitem `7d7cd80483ba4607fc6664ee8f87cb46456324fa`. 179 Android testů v debug i release, 36 Python kontrol; CI lint 0 chyb / 62 upozornění (místně 56). APK `LiftAnt-v0.13-build-29.apk` má původní podpis a `cz.rezim.app`; SHA-256 `4faae667fa28f0e4467a688f50c29b57074d2358aeb509ca70731012f7f5cdc0`. Nová DB/JSON je 12, import 1–12. Testovací Pro je výslovně odemčené; nejde o veřejné Play vydání. Větev je nesloučená, publikace přeskočena a nový PR nevznikl. [Konečné předání a checklist telefonu](docs/LIFTANT_V013_CARDIO.md). Tento odstavec nahrazuje níže uvedené historické údaje a předchozí zprávu o odmítnutém zápisu.

> **Ověřeno 0.13.28 / 13028:** [CI 47](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40220949707) a [podepsané APK 28](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40221140081) uspěly nad zdroji `36f53cb39b8adbe8e24986c9f6dc98ed8bfd5574`. 169 Android testů debug i release, 34 Python kontrol, lint 0 chyb / 36 upozornění. APK `LiftAnt-v0.13-build-28.apk` má původní podpis a `cz.rezim.app`; SHA-256 `26ddb089c069adbb6cb29c1855c4f0a6cd508d76f9aca354488ac2edfb65975f`. Testovací Pro je odemčené; veřejný Play režim ne. Podrobný konečný protokol a checklist jsou v [docs/LIFTANT_V013.md](docs/LIFTANT_V013.md); tato informace nahrazuje níže uvedený průběžný stav čekání na CI. [Draft PR #7](https://github.com/Hmyzak666/hmyzak-android/pull/7) zůstává nesloučený. Před aktualizací vytvořit JSON mimo telefon; neodinstalovávat. Samsung S23 Ultra / Android 16, rozsvícení, vibrace, AOD/Now Bar, cloud/D2D a skutečné nákupy čekají na fyzické ověření.

# LiftAnt pro Android

LiftAnt (dříve Hmyzák) je tréninkový deník pro Android s lokální databází. Zdroj pravdy je `app/`; sestavení nepoužívá historické aktualizační ZIPy. Aktuální vývojová větev **`feat/liftant-v013`** připravuje LiftAnt **0.13** z ověřené verze **0.12.27 / 12027**. Zachovává celý veřejný základ 0.12, Free rekordy a úplnou historii i supersérie se začátky kol z v0.10. Main ani původní větve se nepřepisují.

## Funkce

- Síla, libovolné vlastní plány, cviky, poznámky, série, historie a grafy.
- První spuštění bez osobního plánu autora: prázdný začátek, upravitelné šablony Full Body 3×, Upper/Lower 4× a Push/Pull/Legs 3× týdně, nebo obnova vlastní JSON zálohy.
- Supersérie více cviků s prvními, posledními nebo konkrétními koly, automatický posun, měřitelná/opravitelná délka tréninku a vibrační časovač pauz.
- Běh, univerzální HIIT s volitelnou aktivitou, váha bez obvodu a vlastní tělesné míry. **+ Měření** na Domově otevírá přímo stávající formulář.
- Deset offline jazyků: čeština, angličtina, němčina, italština, francouzština, ruština, španělština, brazilská portugalština, polština a turečtina. Jazyk se přenáší v záloze; vlastní názvy a poznámky se nemění.
- Nazvedaný objem dokončených sérií s volbou jedné/dvou činek, ruční AI textový výpis a sdílené obrázkové souhrny.
- Ruční úplná JSON záloha/obnova včetně trvalých nastavení, výrazné varování před nahrazením a možnost nejdřív uložit dosavadní data.
- Android Auto Backup s databází a trvalými nastaveními, vynuceným podporovaným šifrováním cloudu a podporou přenosu mezi zařízeními. Limit 25 MB a čas záloh řídí Android. Android 8/8.1 používá ruční JSON; dostupnost ani obnova nejsou garantované. Obnova přenáší i jazyk ze starších verzí Androidu a vyžádá nové ověření nákupu přes Play.

Zápis, předchozí výkony, celá historie, osobní rekordy i upozornění na nový rekord, šablony, časovač, textový AI výpis a oba druhy záloh jsou zdarma a bez reklam či časového omezení. **Lifetime Pro** je jediný jednorázový INAPP nákup `liftant_pro` pro grafy progresu a měření, dlouhodobé trendy síly/běhu/HIIT, pokročilé statistiky, grafické karty výkonů, porovnání období 7/30/90 dní a CSV; žádné předplatné. Ceny se nastavují v Play Console, aplikace je načítá z obchodu. Skutečné nákupy ještě vyžadují aktivní Console a licenční testy.

## Historie a rekordy

Domov ukazuje pouze tři nejnovější dokončené silové tréninky podle času dokončení. Klepnutí otevírá detail pouze pro čtení se všemi uloženými sériemi a délkou; Upravit trénink přechází do dosavadního editoru. Zrušení nic nezapisuje, uložení oprav zapisuje série i délku v jedné transakci. Celá historie je samostatný bezplatný seznam bez limitu 12/15 záznamů. Rozpracované tréninky se do něj nemíchají. Změna zobrazení nevyžaduje novou DB migraci.

## Časovač v 0.13

Notifikace ukazuje systémový odpočet bez matoucího statického času. Pozastavení, pokračování a ±30 s zachovávají skutečný termín. Po dokončení se starý chronometr odstraní. Obnova procesu a návrat do popředí porovnávají uložený monotónní čas; restart telefonu odpočet ruší.

Volba **Rozsvítit obrazovku po skončení pauzy** je výchozí vypnutá a přenáší se v zálohách. Používá standardní tiché oznámení s vysokou prioritou. Rozsvícení řídí Android a nastavení telefonu, nelze je zaručit; telefon zůstane zamčený. Android 16+ dostává veřejnou žádost o Live Update, ale AOD ani Samsung Now Bar nelze vynutit. Žádné full-screen intenty, obcházení zámku nebo aktivity na pozadí. [Podrobnosti a test telefonu](docs/LIFTANT_V013.md).

## Identita a zachování dat

| Vlastnost | Hodnota |
| --- | --- |
| applicationId / namespace | `cz.rezim.app` |
| Databáze | `rezim_local_v1.db`, schéma 11 |
| JSON | schéma 11, import 1–11 |
| Původní podpis kompatibilního APK | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |

Migrace 10→11 přidává pouze typ HIIT aktivity a tabulku dokončení onboardingu. U starších uživatelů nemění plány, historii, poznámky, měření ani supersérie. Šablony se nikdy automaticky nevkládají po aktualizaci. Import staré zálohy se zachovává. Podpis stávajícího testovacího APK je Android Debug a nelze jej použít pro Play; přechod vyžaduje zvlášť ověřenou ruční zálohu a obnovu. Starší aplikace neumí schéma 11: uchovat předaktualizační export a neinstalovat staré APK naslepo.

## Sestavení a kontroly

JDK 21 pro testy Androidu 16 (zdrojová a bajtová kompatibilita Java/Kotlin 17), Android SDK Platform 36, Build Tools 35.0.0. Gradle Wrapper 8.13, AGP 8.13.2, Kotlin 2.2.21; minSdk 26, targetSdk 36, Play Billing 9.1.0. SDK nastav přes `ANDROID_HOME` nebo necommitovaný `local.properties`.

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

## Aktuální ověření 0.13

Místní kontrola uspěla: **169 Android testů v debug i release**, 0 selhání/chyb/přeskočení, **34 Python kontrol**, lint **0 chyb / 30 upozornění**, debug APK a náhledové release AAB. Navazující CI a kompatibilní podepsané APK budou doplněny po ověření v Actions. [Rozsah, API omezení a postup pro Samsung/tablet](docs/LIFTANT_V013.md). Data zůstávají ve schématu 11 bez nové migrace.

## Předchozí ověřený kandidát 0.12.27

[PR #6](https://github.com/Hmyzak666/hmyzak-android/pull/6) je samostatný draft navazující na #5. [CI 46](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117795571) ověřilo **128 Android testů v každé variantě (debug/release)** a 31 Python kontrol, lint bez chyb (30 upozornění), APK a náhledové AAB. [Kompatibilní APK 27](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117853952) má verzi **0.12.27 / 12027** a původní podpis. Stažený soubor, podpis, identita a kontrolní součty byly nezávisle ověřené; podrobnosti jsou v PROJECT_STATUS.md. Testovací Pro je výslovně odemčené; Free obrazovky ověřují automatické UI testy, jejichž pomocník se do APK nebalí. Skutečná obnova cloudem a nákupy přes Play dosud nejsou ověřené.

## Pokračování a bezpečné ověření

Nejdřív načti [PROJECT_STATUS.md](PROJECT_STATUS.md) a aktuální větve/Actions; nepoužívej staré ZIPy. Kompletní popis migrace, záloh, Free/Pro, zbývající konfigurace Play, první instalace na tabletu i bezpečného testu obnovy je v [přípravě veřejné verze](docs/LIFTANT_PUBLIC_RELEASE.md). Starší [technický základ](docs/SJEDNOCENI_ZDROJU.md) a [tréninkové nástroje](docs/TRENINKOVE_NASTROJE.md) zůstávají zdokumentované.

Před aktualizací používaného telefonu ověř aktuální čitelnou JSON zálohu mimo telefon, stejný certifikát a vyšší versionCode. Obnovu testuj na odděleném zařízení/profilu. Nikdy automaticky neodinstalovávej aplikaci ani nemaž její data. Zelené automatické testy nenahrazují skutečný cloudový přenos, Samsung vibrace ani nákupy přes Play.

Osobní exporty, databáze, APK, klíče, hesla a tokeny nepatří do repozitáře. Historické ZIPy a rollback větev `backup/pre-source-unification-2026-10-20` zůstávají zachované.
