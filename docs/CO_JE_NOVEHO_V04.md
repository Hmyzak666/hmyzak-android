# Hmyzák v0.4 — plán a změny

- Rychlé tlačítko `+` u každé rozpracované série: přidá 1 opakování, aniž by otevřelo klávesnici. Dokončená série se musí nejdříve odškrtnout.
- `0 kg` je možné zadat u všech cviků, včetně vlastních REPS cviků (např. Push-ups). Typ jednotek lze upravit také v editoru šablony.
- U každého cviku lze během tréninku přidat další sérii nebo odebrat poslední **nedokončenou**; minimálně jedna série zůstává.
- Dokončenou sérii lze i nadále odškrtnout opakovaným klepnutím na ✓ (funkce existovala už před v0.4).
- HIIT: nové **nepovinné** údaje vzdálenost, warm-up, cool-down, odpor při práci a odpor při pauze. Dvě hodnoty odporu se předvyplňují z posledního záznamu.
- Celkové minuty lze odhadnout tlačítkem z počtu intervalů, délky práce a pauz + warm-up + cool-down. Výpočet předpokládá (N-1) pauz a zaokrouhluje nahoru; uživatel může čas ručně přepsat.
- HIIT údaje se zobrazují v historii, dají se opravit, jsou součástí JSON zálohy a týdenního exportu do ChatGPT.
- Týdenní sdílení (Po–Ne) zůstává hlavní volbou. Kopírování, poslední výkony a export celé historie jsou v nabídce „Další možnosti“.
- Stávající silové tréninky, měření, běhy, šablony a dříve uložené HIIT záznamy se při migraci databáze zachovají. Nové HIIT sloupce mají u starších záznamů prázdnou hodnotu.
- Nepřidáváme RPE.

## Bezpečnost aktualizace

Před instalací vytvoř v aplikaci zálohu JSON. Současná databáze se migruje z verze 3 na 4 pouze přidáním pěti sloupců do tabulky HIIT. Identifikátor balíčku `cz.rezim.app` je nezměněný. Použij stejný podpisový klíč jako u nainstalované aplikace. APK musí sestavit GitHub Actions; lokální sestavení v tomto prostředí nebylo ověřeno.
