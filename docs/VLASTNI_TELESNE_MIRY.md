# Vlastní tělesné míry · v0.8

Tato funkce vznikla ve v0.8 se schématem 7. Navazující v0.9 používá databázi a export **8** (import **1–8**) kvůli tréninkovým nástrojům; význam a hodnoty tělesných měr zůstávají stejné. Viz [tréninkové nástroje](TRENINKOVE_NASTROJE.md).

Na kartě **Míry** otevři **+ Vlastní míra**, zadej název (např. Biceps, Stehno, Hrudník, Levá paže) a ulož. Vlastní obvody jsou v centimetrech. Názvy lze přejmenovat; stabilní ID zachová vazbu na stará měření a graf.

Přes **+ Měření** zapiš datum a jen hodnoty, které jsi ten den skutečně měřil. Stačí samotná váha, samotné břicho nebo libovolná vlastní míra. Prázdné pole znamená „neměřeno“, nikoli nulu ani převzetí minulé hodnoty. Celý prázdný záznam nelze uložit. Lze používat desetinnou čárku i tečku. Opravy jsou dostupné u každého záznamu; vymazání hodnoty ji odstraní jen z tohoto měření, ostatní dny zůstanou zachované.

Ve **Vývoji měření** zvol hmotnost, břicho nebo vlastní míru. Graf používá pouze vyplněné hodnoty dané míry a zobrazí se od dvou záznamů. Přehled poslední hmotnosti přeskočí dny, ve kterých byly zapsané jen obvody. Vlastní míry jsou i v textovém výpisu a JSON exportu.

## Kompatibilita dat

- Název databáze zůstává `rezim_local_v1.db`, její verze se mění **6 → 7**. Přestaví se pouze tabulka měření se zachováním ID, dat, hmotností a prázdných obvodů; nově může zůstat prázdná i hmotnost. Přibudou tabulky definic a hodnot vlastních měr. Tréninky, série, běhy, HIIT a šablony se nemění.
- Nové JSON zálohy mají **schéma 7**. Import podporuje **1–7**; při obnově starší zálohy jsou vlastní míry prázdné. Nová záloha obsahuje názvy i historii vlastních měr.
- Starší aplikace se schématem 6 neumí načíst nový export 7. Původní export 6 uchovej. Návrat po aktualizaci vyžaduje novou kompatibilní aktualizaci s databází 7, stejným certifikátem a vyšším `versionCode`; staré APK nepoužívej jako downgrade.
- `applicationId` i podpisový certifikát zůstávají stejné. Verze v0.8 používá `8000 + GITHUB_RUN_NUMBER` a musí být vyšší než potvrzená nainstalovaná 7015. Původní podpis se ověřuje přímo z hotového APK.

## Ověření

Automatické testy pokrývají přechody databáze 5/6 → 7, zachování ID a starých hodnot, samotnou váhu i samotný obvod, vlastní názvy, přejmenování, opravy, JSON 1–7, úplný zpětný export a návrat celé transakce při poškozené záloze. Soukromý uživatelský export nepatří do repozitáře.

Před instalací kandidáta uchovej aktuální JSON export. V telefonu následně ověř původní historii, záznam samotné váhy, přidání Bicepsu/Stehna, měření bez váhy, graf, opravu a nový export. Sestavení a testy nemohou nahradit toto potvrzení uživatelem na jeho Samsungu.

Technická údržba této verze používá aktuální akce GitHubu s Node 24, typované výchozí běhy, KTX transakce a preference, sjednocená volání Compose a explicitní pravidla pro vypnuté automatické zálohování. Android/Compose knihovny a vzhled ikony zůstávají na ověřeném základu; lint jejich informační upozornění dál hlásí, nejsou plošně potlačená. Časovač a rozšířené supersérie nejsou součástí této změny.
