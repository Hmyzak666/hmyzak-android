# Hmyzák v0.7

## Denní váha bez povinného obvodu břicha

V Mírách stačí zadat datum a hmotnost. Obvod břicha je volitelný při přidání
i při opravě záznamu. Můžeš ho později doplnit nebo vymazat. Nový formulář
při otevření nabídne dnešní datum.

Prázdný obvod je uložený jako nezměřený, nikoli jako nula nebo hodnota
z minulého dne. Graf břicha používá jen skutečně zadaná měření. Ve sdíleném
výpisu jsou u denních vah uvedené pouze hodnoty, které jsi vyplnil.

Stávající data se převádějí do databáze verze 6. Zachovají se ID i hodnoty
dosavadních měření a ostatní tabulky s tréninky, běhy, HIIT a plány.
Nové JSON zálohy používají schéma 6; zálohy schémat 1–5 jdou dál importovat.
Nové zálohy se obnovují ve v0.7 nebo novější kompatibilní aplikaci.

## Vibrace

Potvrzení i odškrtnutí série nyní spouští konkrétní krátký impulz přes
vibrátor telefonu. Test na Domů je delší, aby šel snáze rozpoznat.

Když jsou systémové vibrace dotyku vypnuté nebo telefon nehlásí vibrátor,
test to oznámí. Tlačítko Nastavení vibrací otevře nastavení zvuku, kde lze
vibrace dotyku zapnout a upravit jejich intenzitu. Nastavení telefonu se
respektuje. Předvolba vibrací v Hmyzákovi je zachovaná.

Skutečnou sílu a funkci motoru je ještě potřeba ověřit na telefonu.

## Instalace z telefonu

1. Ve stávající aplikaci vytvoř čerstvou JSON zálohu.
2. Nahraj nerozbalený `Hmyzak_update_v0.7.zip` do kořene repozitáře
   `Hmyzak666/hmyzak-android` přes Add file → Upload files → Commit changes.
3. Po zeleném sestavení stáhni nové podepsané APK z příslušného Release.
4. Při instalaci zvol Aktualizovat. Stávající aplikaci neodinstalovávej.

ZIP obsahuje úplnou složku app/ a dokumentaci. Stávající workflow vybere v0.7
před staršími aktualizačními ZIPy a použije dosavadní podpisový klíč.
