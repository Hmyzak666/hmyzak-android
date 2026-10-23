# LiftAnt 0.11 — kandidát k ověření

LiftAnt navazuje na Hmyzáka 0.10.21. Větev `feat/liftant-international` zachovává `cz.rezim.app`, databázi `rezim_local_v1.db`, všechny původní klíče nastavení a kompatibilní podpis pro samostatně distribuované testovací APK. Nová ikona je přesná uživatelem dodaná grafika mravence; původní prostředky a historické ZIPy zůstávají zachované.

## Funkce

- Ovládání v češtině, angličtině, němčině, italštině, francouzštině a ruštině. Výchozí je jazyk telefonu, při nepodporovaném jazyku angličtina. Přepínač je v Nastavení; Android 13+ nabízí i systémový výběr jazyka aplikace.
- Všechny překlady jsou součástí základního balíčku (`bundle.language.enableSplit=false`), aby ruční přepnutí fungovalo i offline po instalaci z Play. Značka LiftAnt se nepřekládá.
- Vlastní názvy plánů, cviků, měr a poznámky se nepřekládají a nepřepisují. Původní tréninkové šablony zůstávají uživatelskými daty; názvy existujících cviků se nemění.
- Ruční kopírování a sdílení textu pro libovolného AI asistenta, nikoli automatické propojení účtů. JSON záloha a obnova zůstávají zdarma.
- Nazvedaná externí hmotnost = součet **dokončených** sérií `zapsaná váha × opakování × násobitel`. Tělesná hmotnost ani odpor gum se neodhadují. U původních známých cviků se dvěma jednoručkami se používá násobitel 2, protože se vždy zapisovala váha jedné. U cviku lze zvolit celkovou zátěž (1) nebo jednu ze dvou činek (2); přepočítá se i historie tohoto cviku, ale původní série a zadané hodnoty se nepřepisují.
- Objem právě probíhajícího tréninku, objem historických tréninků a celkový objem dokončených tréninků.
- Po uložení tréninku grafický souhrn, sdílení čtverce 1080×1080 nebo obrázku pro příběh 1080×1920. Android nabídne aplikace schopné obrázek přijmout; pro Instagram je potřeba nainstalovaná aplikace a případně její vlastní volba příběhu/příspěvku. LiftAnt nic sám nepublikuje. Karta obsahuje pouze zvolený trénink, délku, série, opakování a objem; ne tělesné míry ani osobní poznámky.
- LiftAnt Pro: porovnání dvou stejně dlouhých období 7/30/90 dní a export dokončených sérií do CSV. Běžný deník, neomezená historie, základní grafy, časovač, sdílení a zálohy zůstávají zdarma.

## Databáze a zálohy

Schéma 10 přidává jediný sloupec `plan_exercises.load_multiplier`, `NOT NULL DEFAULT 1 CHECK(IN (1,2))`. Stará data, vazby a ID se zachovávají. Nový export má schéma 10; import nadále přijímá 1–10. Násobitel je součástí JSON zálohy. Záloha neobsahuje nákupní token ani platební údaje; nákup se obnovuje prostřednictvím Google Play.

Nové instalace již nevkládají osobní běžeckou historii. Aktualizace existujícího telefonu žádnou historii nemaže. Nikdy neodinstalovávat stávající aplikaci jen kvůli této změně.

## Dva odlišné druhy sestavení

### Kompatibilní testovací APK

Běžná sestavení a stávající `android-release.yml` mají `PLAY_DISTRIBUTION=false`. Všechny funkce Pro jsou v tomto kandidátovi odemčené, nákupní tok se nespouští. Ruční release workflow používá pouze původní `HMYZAK_*` secrets a ověřuje původní podpis, applicationId a vyšší versionCode. Na telefonu se před instalací znovu ověřuje aktuální JSON záloha. Automatická instalace se neprovádí.

AAB z kontrolního CI je **nepodepsaný testovací náhled s odemčeným Pro**, není určen k nahrání do Google Play.

### Skutečná distribuce přes Google Play

Nejdříve dokončit a ověřit Play Console a správný typ vývojářského účtu. Pak:

1. V aplikaci se stejným package name vytvořit neobnovovaný jednorázový produkt `liftant_pro` s jednou trvalou nákupní volbou. Žádné předplatné ani opakované spotřebovávání produktu.
2. Vybrat cenu a země v Play Console; návrh 199–299 Kč zatím není schválená ani pevně nastavená cena. Aplikace ukazuje cenu vrácenou Google Play v místní měně.
3. Veřejný RSA licenční klíč této aplikace uložit do `config/play-billing-public-key.txt` (jediný řádek Base64, nejde o soukromý klíč).
4. Samostatně připravit produkční Play App Signing a upload key. **Původní debug certifikát není produkční Play podpis.** Neukládat žádný soukromý klíč ani heslo do GitHubu. Pro sestavení nastavit `LIFTANT_UPLOAD_STORE_FILE`, `LIFTANT_UPLOAD_STORE_PASSWORD`, `LIFTANT_UPLOAD_KEY_ALIAS`, `LIFTANT_UPLOAD_KEY_PASSWORD` bezpečným způsobem.
5. Sestavit `:app:bundleRelease -PliftantPlayBuild=true`. Tento režim bez skutečného veřejného klíče a samostatného upload keystore skončí chybou; odmítá současně přítomný starý kompatibilní klíč.
6. Nejprve interní test přes Play. Pro přechod z původního podpisu ověřit zálohu/obnovu v oddělené instalaci. Změnu podpisu na používaném telefonu neprovádět naslepo.
7. Nastavit licenční testery a používat testovací platební metody. Samotná účast na interním/uzavřeném testu automaticky neznamená bezplatný nákup.
8. Ověřit úspěch, zrušení, zamítnutí a čekající platbu, potvrzení nákupu, obnovení po reinstalaci, chování offline a odebrání nároku po refundaci. Teprve pak zapnout skutečný prodej.

Integrace používá Play Billing 9.1.0, ověření podpisu nákupu veřejným RSA klíčem, kontrolu produktu, package a stavu PURCHASED, potvrzení neobnovovaného nákupu a obnovení přes queryPurchasesAsync. Čekající/neověřené nákupy nic neodemykají; neúspěšný offline dotaz neruší dříve ověřený nárok. Úspěšný dotaz bez vlastněného produktu jej odstraní. Ověření je aktuálně na klientovi; pro silnější ochranu plateb je doporučen samostatný server s Google Play Developer API. Takový server ani nové odesílání tréninkových dat v této verzi nevzniká.

Podklady: [Billing](https://developer.android.com/google/play/billing/integrate), [testování plateb](https://developer.android.com/google/play/billing/test), [podpis aplikace](https://developer.android.com/studio/publish/app-signing), [jazyky v AAB](https://developer.android.com/guide/app-bundle/configure-base#disable_config_apks).

## Co ještě není dokončené pro veřejný obchod

Ověřený produkční podpis a přenos dat na něj, skutečné testovací nákupy, schválená cena a produkt v Console, veřejná stránka ochrany soukromí s kontaktem vydavatele, Data safety a fitness deklarace, posouzení foreground služby časovače, screenshoty obchodu a případné povinné uzavřené testování. Rozsvícení displeje na konci pauzy je stále odložené.
