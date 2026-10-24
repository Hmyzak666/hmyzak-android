# LiftAnt 0.12 — příprava veřejné verze

Stav 2026-10-24. Větev `feat/public-ready-v012` navazuje na přesný strom LiftAnt 0.11.24 z `feat/liftant-international` (`5cf091538d3e3329e16fae8b48e23f2209d3edf2`), který obsahuje stabilní v0.10 a volbu prvních/posledních/konkrétních kol supersérie. Main ani původní podepisovací secrets se nemění. Tato větev není vydání na Google Play.

## Chování pro nové a stávající uživatele

- Čistá databáze začíná bez osobních plánů a historie. Úvod nabízí vlastní prázdný plán, tři upravitelné šablony a obnovení JSON zálohy. Rozhraní včetně nových textů podporuje cs/en/de/it/fr/ru.
- Šablony Full Body (3), Upper/Lower (4), Push/Pull/Legs (3) obsahují obecný výběr cviků po třech sériích, bez předepsané zátěže či osobní historie. Jsou výchozím příkladem, nikoli individuálním trenérským doporučením.
- Každé vložení vytvoří vlastní nová ID. Názvy, cviky, počty sérií, pořadí i supersérie jsou upravitelné. Lze smazat i poslední plán; historie a odkazované série zůstávají uložené. Nastavení umožňuje přidat další šablonu bez nahrazení stávajících plánů.
- Migrace starších instalací označí onboarding jako dokončený, i když už uživatel všechny plány smazal. Úspěšný import staré či nové JSON zálohy rovněž dokončí onboarding. Prázdná obnovená záloha zůstane prázdná.
- Historická osobní definice `Plans` slouží výhradně obnově schématu 1 a migraci instalace před podporou vlastních plánů. Nový uživatel ji nikdy nedostane. Historické regresní testy používají samostatnou testovací fixture.
- HIIT je obecný. Volitelná aktivita je Other/unspecified, Elliptical, Running, Cycling nebo Rowing. Staré záznamy mají OTHER; jejich aktivita se neodhaduje, ostatní hodnoty se zachovají.

## Domov, historie a rekordy

Domov ukazuje poslední 3 dokončené silové tréninky, sestupně podle dokončení (při shodě podle ID). Celá historie je samostatný seznam bez horního limitu. Rozkliknutí je pouze pro čtení: datum/čas, délka, cviky, všechny uložené série včetně nedokončených hodnot, hmotnosti a opakování. Upravit trénink otevírá dosavadní bezpečný editor, který odmítne souběh s rozpracovaným tréninkem. Zrušení nic neukládá; potvrzení zapisuje délku a série atomicky. Smazání plánu neodstraní dokončenou historii; pokud už název smazaného vlastního cviku ve staré DB neexistuje, zobrazí se zachované ID. Chybějící názvy nelze zpětně vymyslet.

Osobní rekordy mají samostatnou Free sekci a upozornění při dokončení tréninku. Porovnává se skutečná nejvyšší zátěž nebo více opakování při stejné zátěži; samotné zvýšení odhadu 1RM se nevydává za nový skutečný rekord. Základní historie a minulé výkony zůstávají zdarma. Všechny analytické grafy a grafické karty používají jediné stávající oprávnění Billing Pro; nevzniká další produkt ani platební systém.

## Migrace a kompatibilita

ApplicationId i namespace `cz.rezim.app`, databáze `rezim_local_v1.db`. Schéma DB a JSON **11**, import JSON **1–11**. Migrace 10→11 pouze přidává `hiit.activity` s výchozím OTHER a kontrolou přípustných hodnot a tabulku `app_state` s dokončením úvodu. Nemění žádný původní sloupec, ID, sérii, poznámku, vlastní míru ani kolo supersérie. SQLiteOpenHelper provádí migraci transakčně. Fixture `schema-v10.sql` zachovává původní strukturu pro regresní test.

JSON import ověřuje obsah a zapisuje data v jedné transakci; neplatný soubor vrátí původní stav. Export používá konzistentní SQLite transakci. JSON nyní navíc obsahuje whitelist vzhledu, jazyka, vibrací, automatického posouvání a nastavení pauz. Staré zálohy bez nastavení zůstávají podporované. Nákupní stav ani běžící odpočty nejsou součástí JSON.

Starší aplikace s DB 9/10 neumí otevřít DB ani JSON 11. Před upgradem uchovat původní APK a čitelnou **předaktualizační** JSON zálohu zvlášť. Návrat se řeší další kompatibilní aktualizací, nikoli instalací starého APK naslepo. Nepoužívat odinstalaci nebo vymazání dat jako test na hlavním telefonu.

## Android Auto Backup

Manifest nově povoluje plné zálohy, používá `LiftAntBackupAgent`, `backupInForeground=false`, `fullBackupOnly=true` a pravidla pro obě generace Androidu. Přesný whitelist obsahuje:

| Doména | Soubor | Obsah |
| --- | --- | --- |
| database | `rezim_local_v1.db` | Plány, série, historie, běh, HIIT, měření, míry a dokončení onboardingu |
| sharedpref | `hmyzak_prefs.xml` | Vzhled, ovládání, jazyk a trvalá nastavení pauz |

Před zálohou se uloží i aktuální systémová volba jazyka. Po obnově se volba ze staršího Androidu přenese do LocaleManageru novějšího Androidu, pokud systém dosud jazyk neobnovil. Obnovovací callback odstraní pouze lokální nákupní cache a živý odpočet; tréninková data a trvalá nastavení nemění.

Jiné soubory se nepřidávají. Nákupní cache `liftant_pro_purchase.xml`, živé odpočty `hmyzak_rest_timer.xml`, cache, sdílené obrázky, dočasné soubory, journal/WAL/SHM ani cizí tokeny nejsou na whitelistu. Před plnou zálohou agent otevře skutečný DB soubor bez migrace, provede `PRAGMA wal_checkpoint(TRUNCATE)`, ověří, že databáze není zaneprázdněná, a spojení zavře. Pokud není checkpoint bezpečný, záloha selže a systém ji může zopakovat; nezálohuje se neúplná DB.

Android 12+ požaduje `disableIfNoEncryptionCapabilities=true` pro cloud a samostatně povoluje přenos mezi zařízeními. Android 9–11 agent povolí pouze transport s příznakem klientského šifrování nebo přenosu mezi zařízeními. Na Androidu 8/8.1 agent automatickou zálohu odmítá, protože podporované vynucení tohoto šifrování není dostupné; funguje ruční JSON. Pravidla na starších systémech jsou záměrně doplněna kontrolou příznaků v agentu.

Cloud má limit 25 MB na aplikaci. Systém rozhoduje o čase, účtu, přenosu a dostupnosti obnovy. Aplikace netvrdí, že jde o synchronizaci, neukazuje smyšlené datum poslední zálohy a negarantuje obnovu. Je nutné testovat reálný cloud i přenos mezi zařízeními. Jde o podporovanou systémovou službu; **nepoužívá se Drive REST API, OAuth ani vlastní server**.

JSON export zůstává zdarma přes Android CreateDocument: uživatel vybere místní soubor nebo soukromý cloudový dokumentový provider (například Google Disk, pokud jej telefon nabízí). Před obnovou je výrazné upozornění a možnost vyexportovat současná data. JSON je nešifrovaný; před výměnou telefonu uložit soukromě a ověřit i ruční kopii.

## Free a Lifetime Pro

| Zdarma, bez reklam a časového omezení | Jednorázové Pro |
| --- | --- |
| Zápis síly, běhu, HIIT a měr, předchozí výkony, celá historie a detail všech sérií, osobní rekordy i upozornění, plány a všechny šablony, supersérie, časovač, AI textový výpis, automatické a JSON zálohy | Grafy progresu a měření, dlouhodobé trendy síly/běhu/HIIT, odhad 1RM, pokročilé statistiky, grafické karty výkonů, porovnání dvou období 7/30/90 dní a CSV export |

Produkt `liftant_pro` je INAPP/non-consumable, bez consumeAsync a bez předplatného. Ceny v aplikaci přicházejí výhradně z Google Play `formattedPrice`. Navržené ceny k nastavení v Console: ČR **199 Kč**, USA **8,99 USD**, eurozóna **7,99 EUR**; lokální daně, zaokrouhlování a výsledné zobrazení ověřit v Console.

Knihovna Play Billing **9.1.0**, ověřená proti aktuálním oficiálním release notes 2026-10-24. Integrace ověřuje RSA podpis skutečným veřejným klíčem konkrétní aplikace, produkt, package, neprázdný token a PURCHASED. PENDING, zrušení a neplatné podpisy nic neodemknou. Dokončený nákup se potvrdí; chyba potvrzení neudělí nárok a další dotaz jej zkusí zpracovat znovu. Obnovení používá queryPurchasesAsync včetně nového telefonu a reinstalace se stejným Play účtem. Ověřená potvrzená lokální cache podporuje offline použití; není součástí záloh. Neúspěšný offline dotaz nárok nemaže, úspěšný dotaz bez vlastněného produktu jej odebere. Při návratu do popředí a po dobu otevřené aplikace každých 60 sekund se stav znovu ověřuje; aplikace bezdůvodně neběží kvůli nákupům na pozadí.

Bezpečné režimy sestavení:

- Výchozí sestavení má `TEST_PRO_UNLOCK=false`; žádné implicitní odemčení.
- Kompatibilní testovací workflow výslovně používá `-PliftantTestPro=true`. V Nastavení je jasně uvedeno, že jde o testovací odemčení. Toto APK nic neprodává.
- Play režim `-PliftantPlayBuild=true` odmítá testovací přepínač, chybějící skutečný veřejný Billing klíč nebo chybějící samostatný upload keystore a nesmí používat původní kompatibilní klíč. Náhledové CI AAB bez produkční konfigurace není pro obchod.

Ověření nákupů je lokální RSA, nikoli serverové. Backend s Google Play Developer API je doporučené další posílení ochrany proti zásahům do klienta; zatím neexistuje. Synteticky podepsané unit testy a testovací adaptér potvrzení ověřují stavový tok kódu, **nejsou skutečným nákupem ani potvrzením od Google**.

## Co zbývá udělat v Google Play Console

1. Dokončit ověření identity, kontaktů a případně zařízení. Ověřit správný typ účtu: Google uvádí Health apps mezi kategoriemi vyžadujícími organizaci a mezi health apps řadí fitness trackery; vhodnost osobního účtu pro tuto konkrétní aplikaci vyjasnit s podporou před vydáním. Nový osobní účet má případně i požadavek 12 testerů nepřetržitě 14 dní před žádostí o produkci.
2. Vytvořit aplikaci LiftAnt pro Android. Rezervovat zachované package `cz.rezim.app`. Vybrat země a podporované jazyky, cílový věk a kategorii.
3. Připravit platební profil pro příjem plateb, kontakty a požadované ověření. Přidat jednorázový produkt `liftant_pro`, nákupní možnost/offer a požadované místní ceny. Zkontrolovat dostupnost produktu ve vybraných zemích.
4. Dodat skutečný **veřejný RSA licenční klíč** do `config/play-billing-public-key.txt`. Soukromé klíče, hesla ani tokeny nikdy nevkládat do repozitáře nebo do chatu.
5. Bez přepsání stávajících HMYZAK secrets připravit samostatný produkční upload keystore a Play App Signing. Bezpečně dodat `LIFTANT_UPLOAD_STORE_FILE`, `LIFTANT_UPLOAD_STORE_PASSWORD`, `LIFTANT_UPLOAD_KEY_ALIAS`, `LIFTANT_UPLOAD_KEY_PASSWORD`. Sestavit `:app:bundleRelease -PliftantPlayBuild=true`, bez `liftantTestPro`.
6. Připravit veřejnou webovou stránku ochrany soukromí, kontakt podpory a odkaz i uvnitř aplikace. Vyplnit Data safety podle skutečných funkcí, Billing SDK, systémových záloh a uživatelského sdílení. Doplnit Health apps declaration, obsahové hodnocení, reklamy (žádné), přístup k aplikaci (bez účtu) a deklaraci foreground služby časovače `specialUse` s požadovaným vysvětlením/demonstrací.
7. Doplnit popisy a skutečné screenshoty pro telefon/tablet v potřebných jazycích, grafiku a ikonu. Zkontrolovat případnou existenci podobné značky; jméno není právně prověřeno.
8. Nahrát řádně podepsané **AAB pro interní test**, nastavit seznam testerů a samostatně **license testers**. Použít testovací platební metody, jinak mohou být i v testovacím tracku nákupy skutečně účtované.
9. Ověřit nákup, zrušení, čekající→dokončený a čekající→zamítnutý nákup, potvrzení, obnovu po reinstalaci/výměně telefonu, offline cache a odebrání po refundaci. Ověřit neodemčení Pro na novém zařízení pouze z automatické/JSON zálohy.
10. Dokončit požadované uzavřené testování a kontroly Play. Zveřejnění do produkce je samostatný krok se souhlasem uživatele; zde se neprovádí.

## Bezpečný přechod od nynějšího APK na Play

Původní certifikát Android Debug (`c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`) zachovává možnost aktualizace současného telefonu. Google Play ho nepřijímá jako produkční podpis. Stejné applicationId samo nestačí k aktualizaci jiným certifikátem ani k automatické obnově přes podpisovou hranici.

Nejdříve na původní aplikaci vytvořit aktuální ruční JSON, uložit dvě soukromé kopie mimo telefon a zkontrolovat počet záznamů, vlastní míry, poznámky, supersérie a čitelnost. Na odděleném tabletu/profilu ověřit import do Play instalace, porovnat obsah a znovu vyexportovat. Teprve po úspěšném ověření domluvit přechod hlavního telefonu. Žádná automatická odinstalace, vymazání dat, změna podpisu či spoléhání na neověřenou cloudovou zálohu.

## Kontrola první instalace na tabletu

1. Použít tablet nebo samostatný Android profil bez dosavadních dat LiftAntu. Pokud Android automaticky obnoví dřívější zálohu stejného účtu/podpisu, přeskočení úvodu je správné; pro skutečně čistý scénář použít oddělený testovací profil.
2. Po instalaci kompatibilního testovacího APK má být uvítání, prázdný začátek, šablony a obnova. Prázdný začátek neobsahuje žádný autorův plán ani historii. Opětovné otevření úvod nezobrazí.
3. Přes Síla založit vlastní den a cvik, změnit série, zapsat a uložit trénink. Přes Nastavení přidat šablonu, přejmenovat ji, změnit pořadí a supersérii a smazat libovolný den včetně posledního. Ověřit zachování uložené historie.
4. Každou další první volbu testovat pouze v dalším čistém profilu nebo na výslovně prázdné testovací instalaci. Neresetovat hlavní telefon.
5. Na šířku i na výšku zkontrolovat čitelnost, posun, klávesnici, dialogy, šest jazyků a obecné prázdné stavy běhu/HIIT/měr. Fyzické vibrace testovat na podporovaném zařízení; některé tablety vibrátor nemají.

## Kontrola navazující aktualizace historie a Pro na telefonu

1. Před aktualizací hlavního telefonu vytvořit aktuální čitelnou JSON zálohu mimo telefon. Použít pouze APK s původním certifikátem a versionCode vyšším než 12026; neodinstalovávat a nemazat data.
2. Na Domově mají být nejvýše tři dokončené silové tréninky, od nejnovějšího. Otevřít záznam, porovnat datum, délku včetně sekund a všechny série. Samotné otevření nesmí zobrazit editovatelná pole ani změnit hodnoty.
3. Zobrazit celou historii a otevřít i záznam starší než posledních 15 tréninků. Zpět se má vrátit do historie; další Zpět na Domov. Ověřit i historii plánů, které již byly smazané.
4. Upravit trénink otevře známý editor. Zrušení nic neukládá. Uložení beze změn zachová původní sekundy; pokud se opravuje délka, uloží se spolu se sériemi. Zkoušky skutečných oprav dělat nejdřív v oddělené testovací instalaci.
5. Ověřit tlačítka libovolných vlastních plánů. Pokud už je rozpracovaný jiný trénink, má se zachovat a nabídnout jeho pokračování. Ověřit také časovač, vibrace a nastavená kola supersérií při běžném tréninku.
6. Osobní rekordy a upozornění při dokončení jsou zdarma. Kompatibilní testovací APK má stejně jako předchozí kandidát explicitně odemčené Pro; obrazovky Free a změny oprávnění proto ověřují také automatické UI testy s odděleným testovacím stavem. Testovací pomocník není součástí APK. Výchozí a Play režim zůstávají zamčené bez platného nákupu. Skutečný nákup/obnova přes Play stále vyžadují aktivní Console.

## Bezpečný test ručních a systémových záloh

- Hlavní telefon: pouze exportovat a ověřit aktuální zálohu. Neimportovat do něj testovací data, nic nemazat a neodinstalovávat.
- Oddělený tablet/testovací profil: vytvořit reprezentativní sílu se supersérií a konkrétním kolem, poznámku, rozpracovaný i uložený trénink, běh, HIIT s aktivitou, samotnou váhu i vlastní obvod, změnit jazyk, vzhled a pauzy.
- Uložit JSON do zvoleného umístění. Před importem jiné zálohy využít tlačítko pro uložení současných dat; zrušení pickeru ani dialogu nesmí nic přepsat. Neplatný JSON nesmí poškodit původní data.
- Obnovit do druhé oddělené instalace, porovnat záznamy i nastavení a znovu exportovat. U všech podporovaných starých schémat musí zůstat historie, vlastní názvy a poznámky.
- Auto Backup: na testovacím zařízení zapnout systémové zálohy a zabezpečení obrazovky. Ověřit skutečný cloudový backup/restore a samostatně přenos na druhé zařízení se stejným applicationId a vhodným certifikátem. Použít oficiální postup `Test backup and restore` a případně `bmgr` přes ADB **jen na testovacím profilu/zařízení**; příkazy, které vymažou/odinstalují data, nespouštět na hlavním telefonu.
- Po obnově musí plány, data a dokončený onboarding zůstat zachované; nevloží se duplicity. Živý odpočet se nepřenese a Pro musí ověřit Google Play. Obnova skutečným transportem, jeho šifrování a certifikát nejsou prokázány pouze unit testem XML nebo kopie SQLite.

## Oficiální podklady

- [Android Auto Backup](https://developer.android.com/identity/data/autobackup)
- [Bezpečnost záloh](https://developer.android.com/privacy-and-security/risks/backup-best-practices)
- [Test zálohy a obnovy](https://developer.android.com/identity/data/testingbackup)
- [Play Billing release notes](https://developer.android.com/google/play/billing/release-notes)
- [Testování plateb](https://developer.android.com/google/play/billing/test)
- [Podpis aplikace](https://developer.android.com/studio/publish/app-signing)
- [Ověření účtu](https://support.google.com/googleplay/android-developer/answer/10841920)
- [Požadavky Console](https://support.google.com/googleplay/android-developer/answer/10788890)
- [Health app categories](https://support.google.com/googleplay/android-developer/answer/13996367)
- [Testování nových osobních účtů](https://support.google.com/googleplay/android-developer/answer/14151465)

## Výsledek navazujícího sestavení 0.12.27

Aplikační commit `88beb7f18b7df5e286e6b2392def840a1f586050` prošel [CI 46](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117795571): **128 testů v debug i release**, bez chyb/přeskočení, **31 Python kontrol**, APK/AAB, lint **0 chyb / 30 upozornění** (tři nová jsou pouze ponechané staré nepoužívané texty). [Podepsané APK 27](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40117853952) má **0.12.27 / 12027**, původní certifikát a ověřenou identitu. `LiftAnt-v0.12-build-27.apk`, **9 186 412 B**, SHA-256 **`70b6134bd3b8f9e8571daabecb4676e0f09672a077141e05f9df44b738ab6cdc`**. ZIP i APK odpovídají ověřeným kontrolním součtům; podpis a metadata zkontrolované i místními apksigner/aapt. Dokumentace navazuje bez další změny aplikace. Telefonní test, systémový cloud/přenos a skutečné nákupy ještě nejsou prokázané. Main a Play se nevydávají.

## Výsledek předchozího sestavení 0.12.26

Aplikační commit `c21e420e78151ebf34ae6892f5f44082442bde2c` prošel [CI 45](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082505438): 112 testů v debug i release, bez chyb a přeskočení, 31 Python kontrol, APK/AAB, lint 0 chyb / 27 upozornění. [Podepsané APK 26](https://github.com/Hmyzak666/hmyzak-android/actions/runs/40082544715) uspělo s původním certifikátem a verzí 12026. Stažené APK odpovídá ověřenému reportu a kontrolním součtům; SHA-256 `d01c91d41bc4d01568cc873626401bf8aa10f405644ea2e90953a130e7e7553d`. Předávaný soubor je `LiftAnt-v0.12-build-26.apk`, 9 158 216 B. Main ani Google Play se nevydávají. Navazující předávací commity obsahují pouze dokumentaci.
