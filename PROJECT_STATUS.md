# Hmyzák – stav projektu a pokračování

Aktualizováno **2026-10-21**. Nejprve přečti tento soubor, README a `docs/SJEDNOCENI_ZDROJU.md`.

## Aktuální stav

Navazující **v0.8 / vlastní tělesné míry** je vedená v **[PR #2](https://github.com/Hmyzak666/hmyzak-android/pull/2)** z větve `feat/custom-body-measurements`. Přidává vlastní pojmenované obvody, jejich historii, grafy a opravy. Lze uložit váhu bez břicha i obvody bez váhy. Databáze i export přecházejí **6 → 7**, import podporuje **1–7**; migrace zachovává původní ID a hodnoty. Viz `docs/VLASTNI_TELESNE_MIRY.md`. Nové APK zatím nebylo potvrzeno uživatelem na Samsungu; poslední potvrzená instalace je 0.7.15. Testovací sada v0.8 obsahuje 37 JUnit/Robolectric testů a 16 Python kontrol; aktuální běhy před pokračováním ověř v PR #2 a Actions. Technická údržba záměrně nemění Android/Compose závislosti ani ikonu a nepotlačuje jejich zbývající lint upozornění.

Ověřená nainstalovaná verze je **v0.7.15 / versionCode 7015**. Uživatel 2026-10-21 potvrdil úspěšnou aktualizaci na Samsungu, funkčnost aplikace a zachování původních tréninků, měření a plánů. Původní výchozí bod sjednocení je v0.7.14 / 7014, tag `hmyzak-build-14`, commit `ea5f134910111ba65c0b051c2512f9a63ac02998`; audit doložil, že v0.6 už nebyla poslední vydaná verze.

Výchozí větev pro další vývoj je **`main`**, sjednocení zdrojů je vedené v **[PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1)**. Obsah ZIPu v0.7 je materializovaný přímo do `app/` a `docs/`; sestavování, dokumentace a testy jsou součástí verzovaného stromu. Při sjednocení 14 → 15 byly produkční Kotlin, manifest a prostředky byte-for-byte shodné s funkčním základem; navazující v0.8 mění měření a popsanou technickou údržbu. Historické ZIPy zůstávají zachované a sestavení je nepoužívá.

Vzdálená záložní větev **`backup/pre-source-unification-2026-10-20`** zůstává na původním commitu `ea5f134910111ba65c0b051c2512f9a63ac02998`. Všech 62 souborů commitu `1309cca` bylo přes GitHub API porovnáno s lokálně ověřenou předlohou; souhlasí obsah i režimy souborů. Původní místní historie včetně `8296f1e` je navíc zachovaná v předaném gitovém bundle. GitHub konektor odmítal zápis chybou 403, proto byl přenos proveden přes uživatelem schválený přihlášený prohlížeč.

Místně prošlo debug i nepodepsané release sestavení, 27 JUnit/Robolectric testů v každé variantě, 12 Python testů a lint obou variant. Na GitHubu uspělo nové **[push CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39495862678)** i **[PR CI](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39495868445)** nad `604f871`. **[Podepsaný běh 15](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494987115)** uspěl se stávajícími secrets: APK **0.7.15 / 7015** má původní certifikát a applicationId. Stažené výsledky potvrzují 27 testů bez chyb a přeskočení v debug i release variantě; kontrolní součet APK souhlasí s ověřovacím reportem. Hosted lint má 0 chyb a 30 upozornění; 5 z nich se týká dostupných aktualizací závislostí. Publikační úloha byla přeskočena, takže nevzniklo nové vydání v Releases. Podrobné výsledky jsou v `docs/SJEDNOCENI_ZDROJU.md`; před pokračováním vždy ověř aktuální stav [Actions](https://github.com/Hmyzak666/hmyzak-android/actions), zejména větev `main`.

Před instalací byla skutečná JSON záloha ověřena: formát a schéma 6, úplnost polí, vazby, SQLite integrita a načtení se zpětným porovnáním všech hodnot v oddělené databázi. Poté uživatel potvrdil funkční aktualizaci a zachovaná data na telefonu. Osobní export, jeho obsah ani soukromé kontrolní údaje nepatří do veřejného repozitáře.

## Hodnoty, které musí zůstat kompatibilní

| Oblast | Hodnota / pravidlo |
| --- | --- |
| Identita | `applicationId` i namespace `cz.rezim.app` |
| Certifikát SHA-256 | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Databáze | `rezim_local_v1.db`, SQLiteOpenHelper verze 7 (migrace z 6) |
| Export | `rezim-local-backup`, schéma 7, import 1–7 |
| Číslování vydání | `versionBaseCode + GITHUB_RUN_NUMBER`, v0.8: `8000 + běh`; v0.7 používala `7000 + běh` |
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
| `WorkoutNavigation.kt` | Výběr další série včetně dvojic supersérií |
| `HmyzakHaptics.kt` | Vibrace při potvrzení série |
| `BodyMeasurementInput.kt` | Validace volitelných měr a názvů |
| `BodyMeasurementsScreen.kt` | Zápis, opravy, vlastní míry a volba grafu |

Stabilní ID dnů a cviků propojují šablony s historií; nepřejmenovávat je bez migrace. Smazání ze šablony nesmí mazat staré tréninky. Nová instalace má historicky předvyplněné běhy z původních zdrojů; sjednocení toto chování nemění.

## Jak navázat v novém Work chatu

1. Načti aktuální `main` z `https://github.com/Hmyzak666/hmyzak-android`. Historie sjednocení je v [PR #1](https://github.com/Hmyzak666/hmyzak-android/pull/1); pro další práci založ novou větev z aktuálního `main`, nepokračuj ve staré větvi `chore/unify-sources`.
2. Přečti `PROJECT_STATUS.md`, README a výsledek sjednocení. Spusť `git status`, ověř větev, poslední commity a skutečný stav Actions/Releases. Neodvozuj stav telefonu jen z názvu verze v tomto textu.
3. Pokud existuje `AGENTS.md`, přečti jej před změnami. Obsah starého chatu není automaticky dostupný; tento soubor slouží jako předání kontextu.
4. Začni novou změnu v samostatné větvi. **Neuplatňuj žádný `Hmyzak_update*.zip`** a nevracej do CI rozbalování aktualizací. Upravuj přímo `app/`.
5. Použij JDK 17, SDK 36 / Build Tools 35.0.0 a oficiální Wrapper. Kontroly jsou uvedené v README. Při změně dat doplň test původní databáze i export/import a kontrolu transakčního návratu při chybě.
6. Před sloučením další změny ověř zelené CI. Před aktualizací telefonu ověř nový JSON export, původní certifikát a vyšší `versionCode` než skutečně nainstalovaná verze (poslední potvrzená je 7015). Instalaci, odinstalování a vymazání dat neprováděj automaticky.

Návrat zajišťuje původní tag, místní i vzdálená větev `backup/pre-source-unification-2026-10-20` a ověřený úplný gitový bundle. Návrat kódu v telefonu se připravuje jako další kompatibilní aktualizace se stejným certifikátem a vyšší verzí; databázi nesnižovat.

## Dohodnutý další vývoj – zatím neimplementovat při sjednocení

- Časovač pauz: uživatel délky mění, chce upozornění pouze vibrací. Návrh je nastavování přímo v aplikaci a možnost změny i během tréninku. Přesné spouštění, ovládání a chování při zamčeném telefonu ještě potvrdit při návrhu.
- Více cviků v supersérii: uživatel si skupinu nastavuje sám, případně i během tréninku. Konkrétně leg curl 4×, leg extension 4×, side band abduction 4× a side adduction 1× **hned v prvním kole**; další kola mají poslední cvik přeskočit. Současný model podporuje pouze dvojice.
- Trvalou poznámku chce upravovat i během aktivního tréninku. Dnes je v editoru šablony a při tréninku se zobrazuje.
- Délka tréninku už existuje v datech a jde opravit v detailu historie; požadavek na lepší viditelnost během tréninku nejdříve porovnat s aktuálním UI.
- Až vznikne obrazovka Nastavení, přesunout tam test vibrací a nastavení vibrací. Vibrace na Samsungu po zapnutí systémového nastavení fungovaly; nejde nyní o hlášenou chybu.

Uživatel před těmito úpravami výslovně chtěl nejdříve návrh a nic nesestavovat. Po dokončení sjednocení uživatel schválil technickou údržbu a požádal o další tělesné míry; to pokrývá v0.8. Časovač, rozšířené supersérie a úpravy tréninkových poznámek zůstávají plánované pro další samostatnou verzi.
