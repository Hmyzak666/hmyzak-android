# Hmyzák – stav projektu a pokračování

Aktualizováno **2026-10-20**. Nejprve přečti tento soubor, README a `docs/SJEDNOCENI_ZDROJU.md`.

## Výchozí stav a probíhající změna

Poslední ověřené nasazené vydání je **v0.7.14 / versionCode 7014**, tag `hmyzak-build-14`, commit `ea5f134910111ba65c0b051c2512f9a63ac02998`. Uživatel původně uvedl v0.6, ale audit Actions a vydání doložil novější v0.7.14; následně schválil sjednocení z tohoto základu.

Pracovní větev je **`chore/unify-sources`**. Obsah ZIPu v0.7 je materializovaný přímo do `app/` a `docs/`; navazující změny upravují sestavování, dokumentaci a testy. Původní místní commity včetně `8296f1e` a úplné historie jsou navíc zachované v předaném gitovém bundle. Přenos přes web vytváří samostatné commity na vzdálené pracovní větvi. `main` ani historické ZIPy se při této práci nemění. Produkční Kotlin, manifest a prostředky zůstávají byte-for-byte shodné s vydaným základem.

Vzdálená záložní větev **`backup/pre-source-unification-2026-10-20`** je potvrzená na původním commitu `ea5f134910111ba65c0b051c2512f9a63ac02998`. Pracovní větev je vytvořená ve stejném repozitáři. GitHub konektor odmítal zápis chybou 403; přenos proto probíhá přes uživatelem schválený přihlášený prohlížeč. Aktuální dokončení přenosu, návrh PR a výsledky CI ověř přímo na GitHubu. Do `main` zatím neslučovat bez kontrol. Žádné nové APK nebylo při sjednocení instalováno.

Místně prošlo debug i nepodepsané release sestavení, 27 JUnit/Robolectric testů v každé variantě, 12 Python testů a lint obou variant (bez chyb, 25 upozornění). Podrobné výsledky jsou v `docs/SJEDNOCENI_ZDROJU.md`. Podepsaného kandidáta je nutné ověřit přes skutečný Actions běh se zachovanými secrets.

## Hodnoty, které musí zůstat kompatibilní

| Oblast | Hodnota / pravidlo |
| --- | --- |
| Identita | `applicationId` i namespace `cz.rezim.app` |
| Certifikát SHA-256 | `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908` |
| Databáze | `rezim_local_v1.db`, SQLiteOpenHelper verze 6 |
| Export | `rezim-local-backup`, schéma 6, import 1–6 |
| Číslování vydání | `versionBaseCode + GITHUB_RUN_NUMBER`, aktuálně `7000 + běh` |
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
| `BodyMeasurementInput.kt` | Validace hmotnosti a nepovinného obvodu |

Stabilní ID dnů a cviků propojují šablony s historií; nepřejmenovávat je bez migrace. Smazání ze šablony nesmí mazat staré tréninky. Nová instalace má historicky předvyplněné běhy z původních zdrojů; sjednocení toto chování nemění.

## Jak navázat v novém Work chatu

1. Otevři `https://github.com/Hmyzak666/hmyzak-android` a vyber `chore/unify-sources`, dokud nebude sjednocení po ověření sloučené. Záložní kopie celé historie je v předaném gitovém bundle. Samotné původní `main` zatím obsahuje ZIP postup.
2. Přečti `PROJECT_STATUS.md`, README a výsledek sjednocení. Spusť `git status`, ověř větev, poslední commity a skutečný stav Actions/Releases. Neodvozuj stav telefonu jen z názvu verze v tomto textu.
3. Pokud existuje `AGENTS.md`, přečti jej před změnami. Obsah starého chatu není automaticky dostupný; tento soubor slouží jako předání kontextu.
4. Začni novou změnu v samostatné větvi. **Neuplatňuj žádný `Hmyzak_update*.zip`** a nevracej do CI rozbalování aktualizací. Upravuj přímo `app/`.
5. Použij JDK 17, SDK 36 / Build Tools 35.0.0 a oficiální Wrapper. Kontroly jsou uvedené v README. Při změně dat doplň test původní databáze i export/import a kontrolu transakčního návratu při chybě.
6. Před prvním sloučením tohoto sjednocení musí být zelené nové CI a ověřený podepsaný kandidát. Ověř JSON zálohu před instalací na používaný telefon. Instalaci, odinstalování a vymazání dat neprováděj automaticky.

Návrat zajišťuje původní tag, místní i vzdálená větev `backup/pre-source-unification-2026-10-20` a ověřený úplný gitový bundle. Návrat kódu v telefonu se připravuje jako další kompatibilní aktualizace se stejným certifikátem a vyšší verzí; databázi nesnižovat.

## Dohodnutý další vývoj – zatím neimplementovat při sjednocení

- Časovač pauz: uživatel délky mění, chce upozornění pouze vibrací. Návrh je nastavování přímo v aplikaci a možnost změny i během tréninku. Přesné spouštění, ovládání a chování při zamčeném telefonu ještě potvrdit při návrhu.
- Více cviků v supersérii: uživatel si skupinu nastavuje sám, případně i během tréninku. Konkrétně leg curl 4×, leg extension 4×, side band abduction 4× a side adduction 1× **hned v prvním kole**; další kola mají poslední cvik přeskočit. Současný model podporuje pouze dvojice.
- Trvalou poznámku chce upravovat i během aktivního tréninku. Dnes je v editoru šablony a při tréninku se zobrazuje.
- Délka tréninku už existuje v datech a jde opravit v detailu historie; požadavek na lepší viditelnost během tréninku nejdříve porovnat s aktuálním UI.
- Až vznikne obrazovka Nastavení, přesunout tam test vibrací a nastavení vibrací. Vibrace na Samsungu po zapnutí systémového nastavení fungovaly; nejde nyní o hlášenou chybu.

Uživatel před těmito úpravami výslovně chtěl nejdříve návrh a nic nesestavovat. Poslední schválená implementace se týká **sjednocení zdrojů a CI**, nikoli přidávání těchto funkcí.
