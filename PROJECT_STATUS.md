> **Profil, kcal a GPS – 28. 10. 2026.** Nová samostatná větev `feat/liftant-profile-gps` navazuje na `feat/liftant-v013-cardio` / `84cc2b7`. DB/JSON 13, import 1–13, zachované ruční záznamy i původní podpis. Místně 200 Android testů debug + 200 release, 39 Python kontrol, lint 0 chyb / 62 upozornění; debug APK a release AAB bez test Pro sestaveny. Nahrané zdroje `120dfd85ea1a41d901fce9d1767d03746d9f5351` mají přesně stejný strom jako testovaný checkout (`1cf8f827781e60862df5a87c76f8b4ffb85e67ec`). [Podepsané testovací APK 30](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40667823283) právě běží; výsledek a kontrolní součet budou doplněny. [Metodika, omezení a checklist](docs/LIFTANT_PROFILE_GPS.md). Mapa je offline náhled bez ulic. Skutečné zamčení telefonu/tabletu, terénní GPS a systémová obnova čekají na fyzické ověření. Produkční upload podpis a skutečný Billing public key nejsou nastavené; AAB je kandidát, nikoli hotové Play vydání. Nic se neslučuje ani nepublikuje.

> **Ověřené doplnění 0.13.29 / 13029 (2026-10-26).** Ruční kardio, vlastní zkratky s HIIT předvolbami a časové vstupy jsou uložené ve větvi `feat/liftant-v013-cardio`. [CI 48](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40307850906) i [podepsané APK 29](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40308252295) uspěly nad commitem `7d7cd80483ba4607fc6664ee8f87cb46456324fa`. 179 Android testů v debug i release, 36 Python kontrol; CI lint 0 chyb / 62 upozornění (místně 56). APK `LiftAnt-v0.13-build-29.apk` má původní podpis a `cz.rezim.app`; SHA-256 `4faae667fa28f0e4467a688f50c29b57074d2358aeb509ca70731012f7f5cdc0`. Nová DB/JSON je 12, import 1–12. Testovací Pro je výslovně odemčené; nejde o veřejné Play vydání. Větev je nesloučená, publikace přeskočena a nový PR nevznikl. [Konečné předání a checklist telefonu](docs/LIFTANT_V013_CARDIO.md). Tento odstavec nahrazuje níže uvedené historické údaje a předchozí zprávu o odmítnutém zápisu.

> **Ověřeno 0.13.28 / 13028:** [CI 47](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40220949707) a [podepsané APK 28](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40221140081) uspěly nad zdroji `36f53cb39b8adbe8e24986c9f6dc98ed8bfd5574`. 169 Android testů debug i release, 34 Python kontrol, lint 0 chyb / 36 upozornění. APK `LiftAnt-v0.13-build-28.apk` má původní podpis a `cz.rezim.app`; SHA-256 `26ddb089c069adbb6cb29c1855c4f0a6cd508d76f9aca354488ac2edfb65975f`. Testovací Pro je odemčené; veřejný Play režim ne. Podrobný konečný protokol a checklist jsou v [docs/LIFTANT_V013.md](docs/LIFTANT_V013.md); tato informace nahrazuje níže uvedený průběžný stav čekání na CI. [Draft PR #7](https://github.com/Hmyzak666/hmyzak-android/pull/7) zůstává nesloučený. Před aktualizací vytvořit JSON mimo telefon; neodinstalovávat. Samsung S23 Ultra / Android 16, rozsvícení, vibrace, AOD/Now Bar, cloud/D2D a skutečné nákupy čekají na fyzické ověření.

# LiftAnt 0.13 — časovač, rychlé měření a deset jazyků

Stav 2026-10-25. Aktivní samostatná větev **`feat/liftant-v013`** vychází z přesného ověřeného HEAD 0.12.27 **`4ed58c2be4cdcb6a07061d595ebf1fc64946283e`**. Před změnami byly ověřené větve, otevřené drafty PR #5/#6, zelené CI 46 a kompatibilní APK 27. Novější funkční větev neexistovala. Main a oba předchozí PR se nemění.

Implementováno: přímé **+ Měření** ze sekce Rychlé spuštění přes existující formulář; zalamování tlačítek; výchozí vypnutá a zálohovaná volba vizuálního oznámení konce pauzy; systémový chronometr bez statického času; stav pauzy/pokračování/±30 s, ochrana před oživením vypršelého odpočtu a obnova při návratu do popředí. Standardní tiché upozornění je bezpečný pokus o rozsvícení displeje, nikoli jeho garance. Žádné full-screen oprávnění, odemykání nebo aktivity na pozadí.

Deset jazyků **cs/en/de/it/fr/ru/es/pt-BR/pl/tr**, kompletní katalogy, množná čísla, zachovaný region pt-BR, turecké velké znaky a locale čísla. Vlastní názvy a ID se nepřekládají. Původní anglický fallback i všechny jazyky jsou offline v APK/AAB.

Identita **cz.rezim.app**, DB **rezim_local_v1.db / 11**, JSON **11**, import **1–11**, původní podpis **c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908**. Žádná nová/destruktivní DB migrace. `rest_wake_screen` je pouze volitelná validovaná preference; starší JSON zůstávají platné. Auto Backup pravidla a Billing se nemění; historie, PR a oznámení rekordů jsou Free, pokročilé grafy a statistiky Lifetime Pro. Testovací workflow má výslovně označené Pro stejně jako 0.12.27; veřejný Play režim odemčení odmítá.

Místní kontrola dokončená: **169 Android testů debug + 169 release**, v obou variantách bez selhání, chyb a přeskočení; **34 Python kontrol** (28 tools + 6 migrace měření), lint **0 chyb / 30 upozornění**, debug APK a náhledové release AAB. Ověření v Actions a podepsané aktualizační APK bude doplněné po dokončení. Testy API 36 v Robolectric vyžadují **JDK 21**, obě Actions proto přecházejí na 21; zdrojová a bajtová kompatibilita aplikace zůstává 17. Fyzické rozsvícení displeje, vibrace, AOD/Now Bar na S23 Ultra / Android 16, cloud/D2D obnova a reálné nákupy zůstávají k ověření na zařízení/Play Console.

## Pokračování

1. Ověřit vzdálený HEAD **feat/liftant-v013**, pracovní změny a poslední Actions. Nezačínat ze staršího main nebo ZIPů.
2. Přečíst README a [LIFTANT_V013.md](docs/LIFTANT_V013.md); veřejné základy, Play kroky a podpisový přechod zůstávají v [LIFTANT_PUBLIC_RELEASE.md](docs/LIFTANT_PUBLIC_RELEASE.md).
3. Před aktualizací ověřit aktuální JSON mimo telefon, certifikát a vyšší versionCode než 12027. Používanou aplikaci nikdy automaticky neodinstalovávat ani nemazat data.
4. Fyzické testy telefonu/tabletu a obnovení provádět podle checklistu 0.13. Automatické testy nedokazují chování Samsungu ani cloudovou obnovu/skutečný nákup.
5. PR zůstává draft do ověření zařízení a vyřešení návazností #5/#6. Žádný automatický merge, publikace ani změna podpisových klíčů.

---

## Historický stav 0.12.27 a starších kandidátů

# LiftAnt 0.12.27 — Free rekordy, úplná historie a Pro analytika

Stav 2026-10-24. Nové zadání je implementováno ve větvi `feat/public-ready-v012`, založené ze shodného ověřeného stromu `5cf091538d3e3329e16fae8b48e23f2209d3edf2` / LiftAnt 0.11.24. Obsahuje stabilní změny v0.10 včetně začátků kol supersérií. Main a existující vývojová větev 0.11 se nepřepisují.

Nové: onboarding s prázdným plánem/3 šablonami/obnovou, veřejné prázdné stavy, plně upravitelné šablony, přejmenování a počty sérií, odstranění posledního dne bez smazání historie, obecné HIIT s volitelnou aktivitou, bezpečně omezený Android Auto Backup, trvalá nastavení v JSON a výslovně oddělený testovací Pro režim. Všech šest jazyků zachováno.

DB/JSON **11**, import **1–11**. Migrace 10→11 je pouze přidávací (`hiit.activity`, `app_state`). ApplicationId `cz.rezim.app`, DB `rezim_local_v1.db`, všechna původní data a původní kompatibilní podpis zůstávají. Automatická instalace, odinstalace, vymazání dat, změna původních klíčů, merge ani publikace do Play se neprovádí.

Free: zápis, předchozí výkony, plány/šablony, celá historie, osobní rekordy a upozornění na nový rekord, míry, HIIT, časovač, AI textový export a zálohy. Pro: grafy progresu a měření, dlouhodobé trendy, pokročilé statistiky, grafické karty výkonů, porovnání 7/30/90 dní a CSV, jediný trvalý non-consumable `liftant_pro`. Uživatel navrhl 199 Kč / 8,99 USD / 7,99 EUR; nastavení a ověření místních cen zbývá v Console. Výchozí sestavení Pro neodemyká; kompatibilní testovací workflow používá explicitní `liftantTestPro=true`. Play režim tento přepínač odmítá.

Automatické zálohy: přesný DB soubor a `hmyzak_prefs.xml`; agent bezpečně checkpointuje SQLite. Cloud Android 12+ vyžaduje šifrování, 9–11 kontroluje příznaky šifrování nebo přenosu; Android 8 používá ruční JSON. Cache, nákupní tokeny a živé časovače se nepřenášejí. Skutečná obnova cloudem/zařízeními a platby přes Play ještě vyžadují fyzické/Console testy.

## Navazující úprava: bezplatné rekordy a úplná historie

Pokračuje z přesného vzdáleného HEAD `6e92dc8b7aa32d13fe8dd5eb9c3f43e90c541aed`, v téže samostatné větvi a PR #6. Místní návratová větev `backup/pre-free-history-20261024` zachovává hotový stav 0.12.26. Main se nemění. DB a JSON zůstávají 11 bez nové migrace, certifikát a zálohovací pravidla beze změn. Nové APK musí projít vyšší hranicí versionCode 12026.

Domov zobrazuje poslední 3 dokončené silové tréninky, samostatná celá historie čte všechny dokončené tréninky. Detail je pouze pro čtení, včetně všech sérií; editace zůstává odděleným vědomým krokem a série s délkou se zapisují atomicky. Nové PR a upozornění jsou Free, odhad 1RM a analytické grafy jsou Pro. Grafické karty rovněž kontrolují stávající nákupní oprávnění. Rychlé spuštění používá libovolné uživatelské plány. Text soukromí už nepopírá možnost systémové zálohy. Všech šest jazyků je doplněno. Ověřené aplikační zdroje **`88beb7f18b7df5e286e6b2392def840a1f586050`**, strom **`0455ff567c87eaf2087b47b880c216f66320ef6a`**. Navazující předávací commity mění jen dokumentaci.

- [CI 46](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117795571): **128 Android testů v debug i release**, 0 selhání/chyb/přeskočení v obou variantách, **31 Python kontrol**, APK/AAB. XML bylo stažené a počty nezávisle přepočtené. Lint **0 chyb / 30 upozornění**; tři nová upozornění jsou pouze nyní nepoužívané staré texty, které byly ponechané.
- [Podepsané APK 27](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117853952): **0.12.27 / 12027**, **9 186 412 B**. Testy/lint prošly před použitím původního klíče, publikace přeskočená.
- APK **`LiftAnt-v0.12-build-27.apk`**, SHA-256 **`70b6134bd3b8f9e8571daabecb4676e0f09672a077141e05f9df44b738ab6cdc`**. SHA souhlasí s reportem i SHA256SUMS, archiv s GitHub digestem. Místní apksigner ověřil původní certifikát `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`; aapt potvrdil identitu a vyšší verzi.
- Nové testy zahrnují 20 dokončených tréninků, tři nejnovější podle dokončení včetně shodných časů, ignorování rozpracovaných, starší historii i po odstranění plánu, otevření bez změny JSON, transakční opravy a rollback, zachování sekund při uložení beze změn, skutečné/BW rekordy a upozornění zdarma, reaktivní zamknutí všech grafů i karet a vlastní rychlé spuštění s otevřeným tréninkem. UI testy a snímky telefonu 360 dp a tabletu 800 dp byly ověřené. Testovací ovládání oprávnění je pouze v test source setu a není v APK.
- Fyzická aktualizace telefonu, jeho vibrace, skutečný cloud/D2D transport a nákupy přes Play zůstávají k samostatnému ověření; zelené CI je nenahrazuje. Nic se neinstalovalo, neodinstalovalo, nesloučilo do main ani nevydalo na Play. Postup telefonu je v LIFTANT_PUBLIC_RELEASE.md.

## Dokončené ověření předchozího kandidáta 0.12.26

Aplikační zdroje: **`c21e420e78151ebf34ae6892f5f44082442bde2c`**, strom **`72ab33f5e0ae0f7049ec56ffdc492b883050dbde`**. Navazující předávací commity mění pouze dokumentaci. Samostatný [PR #6](https://github.com/Hmyzak666/hmyzak-android/pull/6) je draft, základ `feat/liftant-international` z #5. Před sloučením vyřešit návaznost #5; main zůstává nedotčený.

- [CI 45](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082505438): **112 Android testů v debug i release**, v obou variantách 0 selhání/chyb/přeskočení. **31 Python kontrol**, debug APK a náhledové release AAB. Lint **0 chyb / 27 upozornění**; převažují styl/KTX, záměrné synchronní preference při záloze, staré ikony/prostředky a aktualizace závislostí. Zprávy byly stažené a počty nezávisle přepočtené z XML.
- [Podepsané APK 26](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082544715): **0.12.26 / 12026**, původní certifikát a identita ověřené. Release testy a lint prošly před načtením původního klíče; publikace byla přeskočená.
- Předaný soubor **`LiftAnt-v0.12-build-26.apk`**, **9 158 216 B**. SHA-256 **`d01c91d41bc4d01568cc873626401bf8aa10f405644ea2e90953a130e7e7553d`**.
- Certifikát SHA-256 **`c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`**. Stažené APK ověřeno i místním apksigner/aapt; souhlasí s reportem CI i SHA256SUMS. SHA-256 ZIP artefaktu souhlasí s GitHub digestem.
- Skutečné spuštění Gradlu s oběma přepínači Play + test Pro bylo odmítnuto očekávanou chybou; veřejná sestavení nelze omylem odemknout tímto režimem.
- Soukromá původní záloha ze 2026-10-21, schéma 7, prošla SQL simulací upgradu, importu a roundtripu do 11 se shodou všech původních hodnot a ID, integritou a vazbami. Soubor zůstal beze změny a není v repozitáři. Nenahrazuje aktuální export telefonu nebo fyzickou obnovu Androidem.
- UI testy ověřily telefon 360 dp a tablet 800 dp; snímky uvítání byly vizuálně zkontrolované. Robolectric běží na API 28/34 podle testu. Skutečný telefon/tablet, vibrace, systémový cloud/D2D transport a skutečné licenční nákupy zůstávají samostatnými kroky.

Technické postupy a úplný seznam kroků pro uživatele: [LIFTANT_PUBLIC_RELEASE.md](docs/LIFTANT_PUBLIC_RELEASE.md). Google Play účet se stále ověřuje. Nic se neinstalovalo, nesloučilo do main ani nezveřejnilo.


## Pokračování v novém Work chatu

1. Přečíst tuto úvodní část, README a LIFTANT_PUBLIC_RELEASE.md; ověřit skutečný vzdálený HEAD nové větve a výsledky Actions. Níže je pouze historický stav.
2. Zkontrolovat pracovní změny a používat aktuální větev 0.12; nevracet starý osobní seed nebo implicitní Pro unlock.
3. Před instalací na hlavní telefon ověřit aktuální předaktualizační JSON mimo telefon, jeho skutečnou verzi a certifikát APK. Starší aplikace neumí DB/JSON 11. Nezačínat odinstalací.
4. Testovat čisté spuštění a obnovu na odděleném tabletu/profilu. Zelené CI nedokazuje fyzickou obnovu ani skutečný nákup.
5. Console se stále ověřuje. Produkční klíče, skutečný produkt/ceny, ochrana soukromí a deklarace, interní/uzavřené Play testování a samostatný podpisový přechod zůstávají v návodu. Zveřejnění vyžaduje samostatný krok.

---

## Historie kandidáta 0.11 a starších verzí

# LiftAnt 0.11.24 — ověřený kandidát, čeká na test telefonu

Stav **2026-10-24**. Uživatel schválil název LiftAnt, dodanou ikonu mravence, jazyky cs/en/de/it/fr/ru, ruční výpis pro AI, sdílení souhrnu tréninku, nazvedanou hmotnost a monetizaci základ zdarma + jednorázové Pro. Konkrétní cena není zvolená. Zdrojový kód a kandidátní APK jsou připravené; instalace na telefonu ani skutečné platby přes Play dosud nejsou ověřené.

## Zdroj pravdy a návrat

- Vše je v [PR #5](https://github.com/Hmyzak666/hmyzak-android/pull/5), větev `feat/liftant-international`. Ověřený aplikační commit je `f0f6506a9f0830803502824060676c769beadce7`; navazující commity mění jen dokumentaci a pravidla ignorování osobních exportů.
- Main zůstává na `98ceff15e3da144edbf0c88d1138e3e692538db3` (Hmyzák 0.10). Místní záloha je `backup/pre-liftant-2026-10-24`; starší vzdálené zálohy, historické ZIPy a APK zůstávají zachované. Nový vývoj nepoužívá ZIP overlay.
- Zachované applicationId `cz.rezim.app`, DB `rezim_local_v1.db`, původní kompatibilní podpis a klíče nastavení. Schéma **10**, import **1–10**. Migrace přidává násobitel zátěže 1/2 a inicializuje jej pro známé původní cviky se dvěma jednoručkami; původní zátěže, opakování, ID a vazby nemění.
- Před instalací je nutné ověřit aktuální export dat. Starší Hmyzák neumí DB/JSON schéma 10: pro případný návrat uchovat předaktualizační zálohu odděleně. Zpětná migrace ani automatická instalace se neprovádí; nezačínat odinstalací.

## Dokončené ověření

[CI 43](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40039599164) uspělo: **94 Android testů v debug i release**, v obou variantách bez selhání a přeskočení, **25 Python kontrol**, debug APK i náhledové AAB. Lint má **0 chyb / 20 upozornění**; zbývají zejména aktualizace nástrojů/závislostí, zachované staré prostředky a ikony a drobné stylové podněty. Všech šest jazyků zůstává v základním balíčku pro offline přepínání. Značka se dědí z výchozího katalogu.

Nové testy zahrnují migraci 9→10, starší zálohy a uživatelské názvy měr, výpočet objemu se dvěma činkami, CSV, porovnání období, kryptografické ověření nákupu a odmítnutí neověřených/čekajících plateb. UI test ověřuje souhrn i obě možnosti sdílení při uložení spodním tlačítkem. Vygenerovalo se 12 sdílecích karet (6 jazyků × 2 rozměry); vizuálně kontrolované vzorky nemají oříznutý text. Robolectric běží na API 34; nejde o test skutečného Samsungu nebo skutečných plateb Google Play.

[Kompatibilní APK 24](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40039856123) také uspělo, včetně release testů a lintu před použitím klíče, podpisu, identity a navazující verze. Publikace do Releases byla vypnutá. Stažený archiv byl porovnán s GitHub digestem a APK s ověřeným reportem i SHA256SUMS:

- Soubor `LiftAnt-v0.11-build-24.apk`, verze **0.11.24 / 11024**, 9 080 460 B.
- SHA-256 APK: `0ce2bf49fc826035505dbffb75fa6a7cd4537d8ed9a33c079dad512bd55137cd`.
- SHA-256 původního certifikátu: `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`.
- Artifact `Hmyzak-APK-24` má omezenou retenci; APK bylo předáno uživateli také jako samostatný soubor. Žádná instalace se neprováděla.

Starší soukromá záloha ze **2026-10-21, schéma 7**, prošla místní SQL simulací upgradu i importu do schématu 10, kontrolou integrity, vazeb a JSON roundtripu se shodou všech původních polí a ID. Původní soubor zůstal nezměněný a není v repozitáři. Nenahrazuje aktuální zálohu telefonu ani Android obnovu.

## Monetizace a zbývající Play kroky

Běžný deník, neomezená historie, základní grafy, časovač, sdílení a JSON zálohy zůstávají zdarma. Pro přidává porovnání 7/30/90denních období a CSV, jednorázovým neobnovovaným nákupem `liftant_pro`. Testovací APK má Pro odemčené a neotevírá platbu.

Uživatel čeká na ověření Play Console. Zde se neprovádělo přihlášení, nahrání AAB ani publikace. Zbývá ověřit typ účtu, vytvořit produkt, zvolit cenu, dodat veřejný RSA klíč, připravit samostatný produkční podpis/upload key a skutečně otestovat licenční nákupy, čekání/zrušení/obnovu/refundaci/offline. Původní certifikát Android Debug je jen pro kompatibilní testovací APK, není produkční Play podpis. Produkční přepínač bez skutečné konfigurace sestavení odmítne. CI AAB je nepodepsaný náhled s odemčeným Pro, **nenahrávat do obchodu**.

Návod a další potřeby obchodu (ochrana soukromí/kontakt, Data safety, fitness deklarace, posouzení služby časovače, screenshoty a případné uzavřené testování) jsou v [LIFTANT_V011.md](docs/LIFTANT_V011.md). Serverové ověřování nákupů, cloud, další jazyky a rozsvícení displeje na konci pauzy zůstávají odložené.

## Pokračování v novém Work chatu

1. Načíst tento soubor, README a LIFTANT_V011.md z aktuální vzdálené větve; zkontrolovat PR #5 a stav main. Vycházet z existující větve, ne ze starých ZIPů ani z domnělé historie chatu.
2. Zkontrolovat lokální změny a vzdálený stav před dalšími commity. Místní a webové commity mohou mít jiné SHA při shodném stromu; nepřepisovat historii ani dělat force push.
3. Nejprve ověřit aktuální zálohu a verzi telefonu. Teprve potom s uživatelem ověřit kompatibilní aktualizaci, zachování dat, oba způsoby uložení/sdílení, jazyky a časovač. Telefonní úspěch nelze odvozovat ze zeleného CI.
4. Sloučení, veřejné vydání a Google Play řešit až s odpovídajícím ověřením. Produkt pro skutečné platby zatím není v Console připravený.

GitHub connector vrací 403 pro zápis; uživatel dříve schválil přihlášený pracovní prohlížeč jako náhradu. Čtení je dostupné přes connector. Nikam neodesílat soukromé exporty, klíče ani platební údaje.

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

## Místní navazující doplnění 0.13 (2026-10-26)

Větev `feat/liftant-v013-cardio` navazuje na `7535583` / předání 0.13.28. Implementuje ruční kardio, vlastní rychlé spuštění s HIIT předvolbami a explicitní časové vstupy; DB/JSON 12, import 1–12. Podrobnosti, ověření a zbývající podepsání jsou v `docs/LIFTANT_V013_CARDIO.md`. Zápis nové větve přes připojení GitHubu byl odmítnut 403; větev je zatím místní a předává se v Git bundle. Main, PR #7 a veřejné vydání se nemění.
