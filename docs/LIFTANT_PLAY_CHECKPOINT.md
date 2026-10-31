# LiftAnt: první Play vydání — checkpoint 30. 10. 2026

## Zdroj a rozsah

Release větev `release/liftant-play-v1` vznikla z `feat/liftant-profile-gps`
HEAD `538af8f7dea2b4ef9367b7c396243daf2ee3f370`.
Poslední ověřený APK základ je 0.13.37, zdroj
`a35f5fb98d39cfd7b902c9ba1b2aa698e438ef26`; navazující HEAD mění jen dokumentaci.
CI 58 a APK 37: 231 testů v každé variantě, lint bez chyb.
Uživatel potvrzuje funkčnost při dosavadním testování; jednotlivé fyzické GPS
scénáře tím nejsou samostatně protokolované.

Jediné nové funkční zadání pro release: ilustrační náhledy Pro. Existující
SparkLine, jasné označení ukázky, žádné ukládání fixture dat. Zamčené sekce
zobrazují kontextový náhled; nastavení Pro ukazuje progres cviku, hmotnost a
porovnání dvou období. Cena pochází výhradně z Billing. Bez ceny je nákup
zakázaný. Nákupní ověřování, Free/Pro rozdělení, DB/JSON 13, GPS i zoom mapy
zůstávají beze změny. Deset jazyků. UI testy telefon/tablet a reaktivní entitlement.

CI 59 odhalilo překlep v názvu existujícího resource pro objem; opraveno.
Výsledek následného CI doplnit podle skutečného běhu, nikoli předpokládat.
Lokálně prošlo 33 Python regresních kontrol. Android SDK/JDK 21 zde nejsou;
Android testy a sestavení běží v GitHub Actions.

## Skutečný stav Play Console

Účet Hmyzak, osobní účet, byl přístupný. Neobsahoval žádné aplikace.
Formulář vytvoření LiftAnt byl předvyplněn: `cz.rezim.app` (dostupné), čeština,
App, Free. **Formulář nebyl odeslán, aplikace ještě nebyla založena.**
Je potřeba potvrdit podmínky Play App Signing, pravidla programu a exportní
prohlášení. Žádný produkt, cena, testovací track ani AAB zatím nejsou uložené.
V sekci Help byl nalezen otevřený ticket „Account type required to publish fitness app“,
aktualizovaný 29. 10. 2026. Console obsahuje původní podání; odpovědi posílá
e-mailem. Odpověď pracovníka podpory nebyla přečtena/ověřena.

## Zbývající závislosti produkčního AAB

- Založení aplikace a uživatelovo potvrzení smluvních prohlášení.
- Skutečný veřejný RSA Billing klíč do `config/play-billing-public-key.txt`.
  Jde o veřejný klíč aplikace, nikoli soukromý upload klíč.
- Nový samostatný upload keystore. Nevytvářet jedinou nezálohovanou kopii
  v dočasném prostředí. Zvolit uživatelem spravované bezpečné úložiště;
  šifrovaný keystore zálohovat na dvou místech a heslo odděleně ve správci hesel.
  Ověřit obnovení kopie a shodu veřejného certifikátu před prvním uploadem.
  Soukromé klíče, hesla a tokeny nepatří do repozitáře ani této dokumentace.
- Sestavovat s `-PliftantPlayBuild=true`, bez `liftantTestPro=true` a bez
  legacy HMYZAK signing proměnných. Stávající Gradle guard tyto požadavky hlídá.
  Použít LIFTANT_UPLOAD_* env přes bezpečné úložiště secrets; nepropisovat je do logů.
- Vyšší versionCode než 13037; před sestavením určit rezervovanou hodnotu dle
  skutečných existujících Play uploadů. Neměnit applicationId.
- CI běžné `bundleRelease` bez Play klíče/podpisu je pouze náhledový kandidát,
  **nikoli produkční AAB pro nahrání do Play**.
- Veřejný kontakt a URL zásad soukromí; odkaz také v aplikaci v deseti jazycích.
- Skutečné screenshoty telefonu/tabletu, nikoli marketingové mockupy.
  Existující grafika: `docs/branding/play-icon-512.png` a `play-feature-1024x500.png`.

## Texty obchodu k revizi

Název: LiftAnt
Kategorie: Health & Fitness; zdarma, bez reklam, volitelný nákup v aplikaci.

### Čeština

Krátký popis: Tréninkový deník pro sílu, kardio a HIIT. Historie, GPS a měření bez účtu.

LiftAnt vám pomůže zapisovat tréninky a sledovat vlastní pokrok. Bez registrace
si vytvoříte silové plány, uložíte série a opakování, zapíšete kardio nebo HIIT
 a zaznamenáte tělesná měření. Celá historie a osobní rekordy zůstávají zdarma.

Běh, chůzi a cyklistiku můžete zaznamenat pomocí GPS telefonu. Samotný GPS
záznam nepotřebuje internet; volitelná online mapa používá OpenStreetMap.
Nechybí časovač pauz, vlastní rychlé zkratky, orientační odhad aktivních i
celkových kcal a ruční JSON záloha s obnovou.

LiftAnt Pro je jednorázový nákup bez předplatného. Odemkne grafy progresu a
měření, dlouhodobé statistiky, porovnání období, obrázkové karty výkonů a CSV
export. Ilustrační náhledy si můžete prohlédnout i zdarma. Aktuální cenu
zobrazuje Google Play.

Tréninkové záznamy jsou uložené lokálně. Systémové zálohy řídí Android;
při exportu si umístění souboru volíte sami. Při zapnuté online mapě dostává
poskytovatel požadavky na zobrazené mapové dlaždice a síťové údaje.

LiftAnt je fitness deník, nikoli zdravotnický prostředek. Výdej kcal je
orientační odhad, nikoli měření ani zdravotní doporučení.

### English

Short description: Log strength, cardio and HIIT. Track history, GPS routes and body measurements.

LiftAnt helps you record workouts and follow your progress without an account.
Create strength plans, log sets and repetitions, record cardio and HIIT, and
keep body measurements. Your full history and personal records are free.

Record outdoor runs, walks and cycling with your phone's GPS. GPS recording
works without an internet connection; optional online maps use OpenStreetMap.
Rest timers, custom shortcuts, estimated active and total calories, and manual
JSON backup and restore are included.

LiftAnt Pro is a one-time purchase with no subscription. Unlock exercise and
measurement charts, long-term statistics, period comparisons, performance
image cards and CSV export. Clearly labelled sample previews are available
for Free users. Google Play displays the current price.

Training records are stored locally. Android controls system backups; you
choose where to save manual exports. When online maps are enabled, the map
provider receives tile requests for the viewed area and network information.

LiftAnt is a fitness diary, not a medical device. Calories are estimates,
not measurements or medical advice.

## Soukromí a deklarace: nedokončené položky

Znovu neprovádět celý audit; navázat na LIFTANT_PUBLIC_RELEASE.md a skutečný kód.
Přímá síťová komunikace mapy: HTTPS tile.openstreetmap.org/{z}/{x}/{y}.png,
User-Agent LiftAnt/verze, max zoom 19, aktivace uživatelem. Souřadnice celé
trasy se neodesílají jako soubor, ale indexy dlaždic odhalují zobrazenou oblast.
Na vysokém zoomu je velmi přesná: **nelze automaticky deklarovat pouze
přibližnou polohu ani aplikaci bez jakéhokoli sběru dat**. Prověřit pravidla
Data Safety a aktuální privacy policy OSM Foundation včetně IP/logů/uchování.

Billing komunikuje přes Google Play; žádný vlastní server pro ověření nákupu.
Zálohy Androidu jsou podmíněné podporovaným šifrováním, JSON není sám o sobě
šifrovaný. Lokální diagnostika GPS není součástí whitelistu systémových záloh.
Health apps: Activity and Fitness. Žádná diagnostika/léčba ani Health Connect.
FGS deklarovat location (GPS) i specialUse (uživatelem spuštěný časovač pauz).
Bez background location. Připravit skutečné demonstrační video obou služeb.
Dokončit formulář až po ověření aktuálních pravidel a celého merged manifestu
skutečného produkčního balíčku, nikoli podle neověřených předpokladů.

## Nákup a testování

Produkt `liftant_pro`: jednorázový, ne spotřební, bez předplatného.
Požadované ceny CZ 199 CZK / US 8.99 USD / eurozóna 7.99 EUR ještě nejsou
nastavené ani regionálně ověřené. UI nesmí mít natvrdo žádnou z nich.
Po založení produktu zkontrolovat lokální měny, daň a povolené cenové rozsahy.

Licenční testery přidat v nastavení Console a zároveň do testovacího tracku;
instalovat z Play pod stejným účtem. Použít pouze testovací platební metody.
Ověřit PURCHASED + podpis + acknowledge, pending bez entitlement, zrušení,
obnovu po reinstalaci na testovacím profilu, refundaci po úspěšném refresh,
offline zachování dříve ověřeného nákupu a Free bez platného nákupu.
Skutečný placený nákup není autorizovaný.

Přechod z debug certifikátu: původní APK ponechat na hlavním telefonu. Vytvořit
JSON, dvě soukromé kopie mimo aplikaci, ověřit validitu a obnovu na samostatném
zařízení/profilu. V Play instalaci porovnat počty a detaily silových tréninků,
kardia, HIIT, měření, profilu, tras a zkratek. Android záloha nenahrazuje ověřený
JSON při změně podpisu. Neprovádět automatickou odinstalaci ani reset dat.

Interní test: nahrát až skutečně podepsaný Play AAB, přidat testery, získat
opt-in odkaz z Console a poskytnout instrukce. Testeři se přihlásí správným
účtem, přijmou účast a instalují přes Play. Zpětná vazba: zařízení/Android,
verze, kroky, očekávaný a skutečný výsledek; neposílat veřejně soukromé trasy.

Uzavřený test nového osobního účtu: nejméně 12 testerů přihlášených nepřetržitě
14 dní; interní test tuto podmínku nenahrazuje. Počátek evidovat podle skutečného
opt-in, ne podle vytvoření seznamu emailů. Průběžně sbírat reálnou zpětnou vazbu
a opravy. Po splnění podmínky podat žádost o produkční přístup v Console;
přístup ani zveřejnění nejsou automatické. Produkci nepublikovat bez uživatele.

Oficiální zdroje ověřené při přípravě:
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/13634885
- https://support.google.com/googleplay/android-developer/answer/10787469

## Dodatečně ověřené zdroje pro dokončení deklarací

OSMF privacy policy potvrzuje protokolování požadavků včetně IP a času;
nelze slibovat, že dlaždicové požadavky jsou jen pomíjivé zpracování v RAM.
FAQ uvádí globální CDN Fastly. Neodvozovat retenční dobu všech dlaždicových
logů od 180denní retence Piwik v obecné policy.
- https://osmfoundation.org/wiki/Privacy_Policy
- https://osmfoundation.org/wiki/Services_and_tile_users_privacy_FAQ
- https://operations.osmfoundation.org/policies/tiles/

Jednorázové produkty mají regionální ceny a purchase options; regionální
dostupnost aplikace má přednost. V daňově inkluzivních regionech zahrnout
příslušnou daň do zobrazené ceny; ověřit konkrétní finální ceny v Console.
- https://support.google.com/googleplay/android-developer/answer/16430488
- https://support.google.com/googleplay/android-developer/answer/138000
- https://developer.android.com/privacy-and-security/declare-data-use


## Play Console update — 2026-10-30

LiftAnt was created successfully following explicit user consent to the three creation declarations. App ID: 4975186999097434941. Package: cz.rezim.app. Default language: Czech (cs-CZ). App, free. Nothing published and no bundle uploaded.

Dashboard: https://play.google.com/console/u/0/developers/6169922858758507757/app/4975186999097434941/app-dashboard

Monetisation currently requires a merchant/payments profile. Console shows “Create a payments profile to get started”. User must complete this financial-account setup; product liftant_pro and regional prices have not been configured.

Payments profile: https://play.google.com/console/u/0/developers/6169922858758507757/paymentssettings

This update supersedes earlier statements that the app has not been created. Production AAB, upload-key custody, remaining CI failure and release declarations remain pending.

## Billing unblocked — 2026-10-30

After user completed the payments profile, Console exposed monetisation setup and the real RSA licensing public key. Stored only the PUBLIC key at config/play-billing-public-key.txt. No private key or password added.

One-time products page still requires uploading a billing-enabled application before product creation. liftant_pro and prices have NOT been configured.

CI 60: 234 debug tests, one failure at RestTimerServiceTest.kt:109 (wall-clock drift assertion). Replaced the arbitrary 100 ms post-command deadline comparison with bounds sampled around notification creation; preserved countdown assertions and exact injected-clock unit test. New CI verification pending. No timer production code changed.

## Verified CI 61 — 2026-10-31

CI https://github.com/Hmyzak666/hmyzak-android/actions/runs/41142179410 succeeded on source commit 5adae9691975b5b0567dc5b63e41ff2d1fd7e1ed. Downloaded artifact 13019477445 and independently parsed XML reports: 234 debug + 234 release tests, zero failures/errors/skips. Lint: zero errors, 71 warnings. Local checks: 33 Python + 6 migration tests passed.

Artifact ZIP SHA256: 099135a1a233852dec428221528287ff0ee3bb7abac7595ec7c1dc3ced4bf283. Included AAB: 8,722,444 bytes, SHA256 bd4c561f88cfdd87e5c926ce4209c673ed7dab6463bc2efe979ca85f6fd48a7c. This is the default unsigned CI bundle, NOT a signed Play-distribution build and NOT uploaded to Console. No new APK delivered.

User confirmed phone-only access and wants a separate public support email; actual address still missing. Separate upload-key generation, secure custody and backup remain unresolved. Do not place secrets in GitHub or documentation. No product/prices configured, no internal/closed release published.

## Owner-held upload key — 2026-10-31

Public support email supplied by owner: liftant.support@gmail.com (Console contact not yet saved).
Owner generated an RSA 3072 upload key in Termux, alias liftant-upload, PKCS12.
Owner confirmed encrypted keystore backup on private Google Drive and password separately in password manager.
Only the PUBLIC certificate was supplied to this workspace: config/play-upload-certificate.pem.
Subject CN=LiftAnt Upload; valid 2026-10-31 through 2054-03-18.
SHA-256 EAF5175CB25A4C933D4D502DC0214D393CA3191A1AD55332223B4BDE32991193.
No private key/password was received or committed. Do not regenerate the owner's key.

Added explicit -PliftantExternalSigning=true together with Play mode. It rejects test Pro,
legacy signing keys, upload keystore injection, and missing/old version identity.
Release-branch CI builds 0.13.38 / 13038 with PLAY_DISTRIBUTION=true and TEST_PRO_UNLOCK=false;
verifies generated configuration, release manifest, and unsigned AAB structure before packaging
LiftAnt-0.13.38-13038-UNSIGNED.aab. CI verification of this change is pending.
The bundle must be signed on the owner's phone with jarsigner (interactive password prompt),
then verified against the public certificate before any Play upload. The CI61 bundle remains NOT FOR PLAY.
Public key certificate is not the Billing licensing key; those files must remain separate.
No AAB upload, internal/closed release, product creation, main merge or publication performed.

## Verified Play intermediate CI62 — 2026-10-31

Source HEAD 95222bb7c71d4f844e72616795258a2560a21d9b; local and remote source trees match.
https://github.com/Hmyzak666/hmyzak-android/actions/runs/41202052193 succeeded.
Downloaded artifact 13042883381; ZIP SHA256 d491f303dc86ceabb576d7723e80a6c0b11ed1311d015bdb93065f70c33093a1.
Independently parsed reports: 234 debug + 234 release tests, zero failures/errors/skips;
lint 0 errors / 71 warnings. Local 36 Python tests + 6 migration checks passed.
Generated Play configuration and merged release manifest passed the intermediate verifier.
AAB 8,722,351 bytes, SHA256 2eee2b0dfe2067b3809d1d0e2e4ec107df006f36bffecd7d9054173261c2d7f2.
Name LiftAnt-0.13.38-13038-UNSIGNED.aab; applicationId cz.rezim.app;
PLAY_DISTRIBUTION=true, TEST_PRO_UNLOCK=false, versionCode=13038.
Delivered for owner-side signing; still UNSIGNED and NOT uploaded to Play.
Next: sign with owner-held liftant-upload alias using jarsigner, verify complete entry signatures
and exact expected certificate, then upload to internal testing with Google-generated app signing key.
Privacy policy, listing/screenshots, declarations, product and license testing remain pending.
