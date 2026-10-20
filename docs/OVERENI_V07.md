# Ověření Hmyzáka v0.7

Datum: 20. 10. 2026. Výchozí zdroje: přesný aktualizační ZIP v0.6 z repozitáře,
SHA-256 `01257c7a3886fb11b247cf68bc5d357c6a83b4aae2b9d161b9a7298b7c0f5385`.

| Kontrola | Výsledek |
| --- | --- |
| `assembleRelease` | Úspěch, místní APK je nepodepsané a není určeno k aktualizaci telefonu |
| `lintVitalRelease` | Úspěch |
| `testReleaseUnitTest` | 20/20 úspěšných, 0 chyb, 0 přeskočených |
| Navigace sérií a supersérií | 15 dosavadních regresních testů úspěšných |
| Samostatná váha a validace obvodu | 5 nových testů úspěšných |
| Migrace měření v5 → v6 | 6/6 kontrol skutečných SQL příkazů na SQLite, viz skript níže |
| Manifest sestaveného APK | `cz.rezim.app`, 7000 / 0.7.0, oprávnění VIBRATE, bez INTERNET |
| Podpisový klíč a workflow | Původní konfigurace zachovaná; distribuci podepisuje stávající GitHub workflow |
| Zálohy | Kód exportuje schéma 6 a přijímá schémata 1–6; skutečná obnova na Androidu zatím neověřena |

`python3 docs/overit_migraci_mereni.py` načte a provede přímo SQL nové migrace.
Kontroluje původní hodnoty a ID, prázdnou databázi, následné denní váhy bez
obvodu, doplnění/vymazání obvodu, novou instalaci a zachování ostatních tabulek
s tréninky, běhy, HIIT a vlastními plány. Nejde o emulaci Android SQLiteOpenHelper.

Nové testy vstupu zahrnují desetinnou čárku, prázdný a mezerový obvod,
vyplněný obvod, neplatná čísla, NaN/nekonečno, nulu, záporné hodnoty a meze.

Po instalaci na telefonu zbývá ověřit:

- Zachování skutečné historie a rozpracovaného tréninku po aktualizaci.
- Uložení dnešní váhy bez břicha a doplnění/vymazání obvodu v opravě záznamu.
- Správný výpis a graf při střídání denní váhy a občasného měření břicha.
- Fyzickou odezvu při potvrzení i vrácení série a při delším testu na Domů.
- Hlášení při vypnutých vibracích dotyku a otevření nastavení zvuku.
- JSON zálohu a obnovu v této verzi, ideálně nejprve na testovacích datech.

Před instalací vytvořit čerstvou JSON zálohu. Instalovat přes původní aplikaci
jako Aktualizovat, bez odinstalování. Místní testovací APK se nedistribuuje,
protože nemá původní podpisový klíč z GitHub Secrets.
