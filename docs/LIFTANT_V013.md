# LiftAnt 0.13 — časovač, rychlé měření a deset jazyků

Vývojová větev `feat/liftant-v013` navazuje na ověřené 0.12.27 / 12027 (`4ed58c2be4cdcb6a07061d595ebf1fc64946283e`), nikoli na starší main. PR #5 a #6 zůstávají zachované. Místní kontroly uspěly: 169 testů debug i release, v obou 0 selhání/chyb/přeskočení, 34 Python kontrol, lint 0 chyb / 30 upozornění, debug APK a náhledové release AAB. Výsledky Actions a podepsaného APK budou doplněny po ověření. Android 16 (API 36) v Robolectric vyžaduje JDK 21; obě Actions proto používají Javu 21, zatímco zdrojová/bajtová kompatibilita aplikace zůstává Java/Kotlin 17.

## Chování aplikace

- Domov / Rychlé spuštění má **+ Měření**. Otevírá přímo existující formulář BodyScreen se stejnou validací a ukládáním. Samotná váha i vlastní pojmenované obvody zůstávají zdarma. Tlačítka se zalamují podle dostupné šířky.
- V nastavení pauz, včetně dialogu během tréninku, je **Rozsvítit obrazovku po skončení pauzy**, výchozí hodnota vypnuto. Stav `rest_wake_screen` patří do trvalých nastavení, Auto Backup a validované JSON zálohy. Staré JSON bez tohoto pole zůstávají platné. Databáze ani JSON schéma se nezvyšují.
- Zapnutá volba po skončení pauzy používá standardní tiché oznámení s vysokou důležitostí v odděleném kanálu. Telefon zůstane zamčený. Zda oznámení skutečně rozsvítí displej, závisí na Androidu, One UI, oprávnění k oznámením, nastavení kanálu, zamčené obrazovce a režimu Nerušit. Vysvětlení omezení i odkaz na nastavení kanálu jsou přímo v aplikaci. Výslovná změna během odpočtu se použije při jeho dokončení.
- Běžící notifikace používá systémový odpočítávající chronometr s explicitním `showWhen`. Tělo již neopakuje statickou původní délku. Pauza ukazuje zmrazený zbývající čas bez chronometru, pokračování a ±30 sekund obnoví správný termín. Dokončení odstraňuje běžící oznámení; nový odpočet odstraní staré dokončení. Identifikátory oznámení se nemění.
- Zdrojem pravdy je uložený monotónní termín a číslo bootu. Služba se po běžném restartu procesu snaží obnovit stav; návrat aplikace do popředí jej znovu porovná a obnoví notifikaci. Opožděná změna již vypršelý odpočet neoživí. Restart telefonu odpočet ruší jako dosud. Vynucené ukončení uživatelem / omezení výrobce mohou doručení zpozdit nebo zastavit; nejde o budík s garantovaným probuzením procesu.
- Bez oprávnění k oznámením nelze slibovat odpočet na zamčené obrazovce. Zápis, vnitřní odpočet a samostatné nastavení vibrací zůstávají. Aplikace vysvětluje povolení oznámení a znovu stav kontroluje po návratu.
- CPU wake lock zůstává jen po dobu běžícího odpočtu, s časovým limitem. Žádné sekundové publikování notifikací ani držení rozsvícené obrazovky; pauza a dokončení CPU lock uvolňují.

## Android 16, Samsung AOD a Now Bar

Používáme běžnou veřejnou notifikaci s viditelností pro zamčenou obrazovku. Na Androidu 16+ je připojena veřejně zdokumentovaná žádost o promoted ongoing / Live Update pro právě běžící, uživatelem spuštěnou pauzu. Zobrazení i povýšení rozhoduje systém a výrobce; při pozastavení a dokončení se žádost nepoužívá. Fungování standardní notifikace na povýšení nezávisí.

Aplikace neřídí AOD, nemůže vynutit zobrazení v Now Bar a nepoužívá soukromé Samsung extras, předstírané přehrávání médií, overlay, odemykání, spouštění aktivity na pozadí, `USE_FULL_SCREEN_INTENT` ani `TURN_SCREEN_ON`. Systém může zobrazit jen ikonu; klepnutí nebo probuzení ovládá uživatel. Samsung uvádí, že dostupné aplikace pro Now Bar závisí na modelu a softwaru. Přítomnost LiftAntu v nabídce nelze zaručit.

Rozsvícení vypnutého displeje nelze v této fitness aplikaci aktuálními povolenými API spolehlivě vynutit. `setTurnScreenOn()` předpokládá viditelnou/resumovanou aktivitu a neuděluje výjimku ze spouštění na pozadí; staré screen wake lock mechanismy jsou zastaralé. Nastavení proto výslovně představuje pokus standardním vizuálním oznámením.

Podklady ověřené 2026-10-25:

- [Android — Live Updates, požadavky a omezení OEM](https://developer.android.com/develop/ui/views/notifications/live-update)
- [Android — veřejná requestPromotedOngoing extra](https://developer.android.com/reference/android/app/Notification#EXTRA_REQUEST_PROMOTED_ONGOING)
- [Android — chronometr notifikace](https://developer.android.com/reference/android/app/Notification.Builder#setChronometerCountDown(boolean))
- [Android — setTurnScreenOn](https://developer.android.com/reference/android/app/Activity#setTurnScreenOn(boolean))
- [Android — omezení full-screen intentů](https://source.android.com/docs/core/permissions/fsi-limits)
- [Android — TURN_SCREEN_ON pouze pro domácí automatizaci](https://developer.android.com/reference/android/Manifest.permission#TURN_SCREEN_ON)
- [Samsung — oznámení, zamčená obrazovka a Now Bar](https://www.samsung.com/us/support/answer/ANS10002524/)

## Lokalizace

Deset offline jazyků: **cs, en, de, it, fr, ru, es, pt-BR, pl, tr**. Angličtina zůstává výchozím fallbackem; AAB nevylučuje další jazyky pomocí language splitů. Nové katalogy pokrývají všechny textové klíče včetně onboardingu, šablon, silového deníku, supersérií, rekordů, historie, běhu, HIIT, měření, časovače, záloh, nákupů a chyb.

Brazilská portugalština používá `values-pt-rBR`, plný tag zůstává v nabídce i v záloze. Systémová portugalština bez regionu se mapuje na brazilskou; výslovné pt-PT nemá vlastní katalog. Turecké nadpisy a nově tvořené zkratky respektují aktuální jazyk. Stabilní interní ID a původní názvy, poznámky a vlastní měření se nepřekládají. Nové šablony se při ručním výběru kopírují v právě vybraném jazyce, potom jsou vlastními plány.

Čísla na obrazovkách používají aktuální locale, vstupy dál přijímají čárku i tečku. Počty sérií a délky období používají Android plurals, včetně českých, ruských a polských tvarů. Technický JSON/CSV zůstává stabilní. Před veřejným vydáním je vhodná jazyková kontrola rodilými mluvčími; automatická úplnost sama nepotvrzuje stylistickou správnost.

## Data, nákupy a podpis

`cz.rezim.app`, `rezim_local_v1.db` a schéma DB/JSON **11**, import **1–11**. Žádná destruktivní ani nová datová migrace. Historie, ID, cviky, vlastní plány a míry, poznámky, supersérie a začátky kol, PR, nastavení a nákupy se zachovávají. Free historie a PR včetně upozornění zůstávají zdarma. Pro grafy, dlouhodobé trendy, pokročilé statistiky a obrázkové karty používají původní Billing oprávnění.

Původní certifikát kompatibilních aktualizací: `c1cc11c540d457393be9bfd8a1752b8577cda168f8fe9464e2d9d173eb70d908`. Verze používá `13000 + číslo běhu` a musí přesáhnout 12027. Kompatibilní testovací workflow explicitně odemyká a označuje Pro stejně jako u 0.12.27; produkční Play režim takové odemčení odmítá. Toto APK není produkční balíček pro Google Play.

## Co ověřit na Galaxy S23 Ultra s Androidem 16

1. Před aktualizací vytvořit čitelnou aktuální JSON zálohu a uložit kopii mimo telefon. Ověřit shodný certifikát a vyšší versionCode. **Neodinstalovávat aplikaci, nemazat data.**
2. Po aktualizaci porovnat historii, vlastní plány, poznámky, supersérie se začátky kol, měření a nastavení. PR a historie musí zůstat dostupné.
3. Domov → Rychlé spuštění → + Měření: otevře se přímo formulář. Zapsat samotnou hmotnost; další test vlastní míry provést vědomě, bez zásahů do starých záznamů.
4. Časovač zkusit na krátké pauze, například 30 s: pozastavit, pokračovat a ±30 s. Odpočet v aplikaci a notifikaci musí souhlasit. Dokončení nesmí nechat běžet starý chronometr. Nové spuštění nesmí vytvořit duplicitní oznámení.
5. Opakovat v popředí, na pozadí a se zamčenou obrazovkou. Samostatně vyzkoušet vypnutou/zapnutou volbu vizuálního upozornění a vypnuté/zapnuté vibrace. Displej se nemusí rozsvítit; telefon se nesmí odemknout.
6. Ověřit nastavení kanálů oznámení, Nerušit a Samsung Lock screen and AOD. V Now Bar zapnout LiftAnt pouze pokud jej Samsung skutečně nabídne. Zaznamenat verzi One UI a pozorované chování AOD/Now Bar; nelze ho vyvozovat z unit testu.
7. Ztrátu oprávnění, usmrcení procesu a obnovu JSON/Auto Backup ověřovat na odděleném testovacím telefonu/profilu se zálohou, nikoli mazáním používané aplikace. Běžné ukončení procesu se liší od vynuceného zastavení v Nastavení; po force-stop systém automatický návrat negarantuje.
8. Na telefonu a čistém tabletu zkontrolovat deset jazyků, dlouhé texty na úzké obrazovce, pt-BR v nabídce, turecké znaky, formulář a jeho validaci. Přepnutí jazyka nesmí měnit vlastní data.

Skutečné rozsvícení displeje, vibrace, AOD, Now Bar, cloud/D2D obnova a reálné nákupy vyžadují fyzický telefon nebo aktivní Play Console. Tato verze je automaticky neověřuje a neslibuje.
