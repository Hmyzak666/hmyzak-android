# Hmyzák v0.5 — rozhraní a progres

## V nové verzi
- Série: vedle tlačítka `+1` je `−1`; hodnoty nikdy neklesnou pod nulu. Mezi dvojicí tlačítek a fajfkou je mezera. Na užších telefonech se řádek může vodorovně posouvat.
- Číslice v sériích mají 18 sp a větší váhu písma. Klepnutí na vstupní pole označí celé číslo; na klávesnici lze přejít na další pole.
- Dlouhé podržení pole kg otevře nepovinný dialog pro změny ±2,5 kg; ruční editace zůstává.
- Dokončení série spustí krátkou haptickou odezvu. Přepínač na úvodní obrazovce ji může vypnout.
- Trénink počítá dobu od Zahájit do Uložit; u již uloženého tréninku lze opravit délku v minutách. Přerušení během tréninku se automaticky neodečítají.
- Pod cvikem lze rozbalit poslední tři dokončené výkony.
- Graf v části Síla umožňuje přepnout maximální zátěž, součet opakování a přibližný odhad 1RM. Lze vybrat interval 1/3/6/12 měsíců.
- Rekordy vycházejí ze skutečně dokončených sérií; počty opakování se porovnávají při stejné přidané zátěži. Odhad 1RM se počítá jen pro cviky KG při >0 kg a 1–30 opakováních.
- Grafy běhu lze filtrovat na všechny běhy, 4,5–5,5 km a 9–11 km. Stávající grafy tepu a tělesných měření zůstávají.
- Lze sdílet obrázkovou kartu výkonu PNG přes systémové sdílení Androidu. Sdílení je výhradně po stisku uživatelem, není potřeba účet ani Internet v samotném Hmyzákovi.
- Na úvodní obrazovce lze přepnout tři varianty karet: **Kompaktní**, **Vyvážený** a **Studio**. AMOLED pozadí a zelené akcenty zůstávají. Nastavení se ukládá pouze v telefonu.
- Průběh tréninku má jemnější animaci dokončených sérií.

## Bezpečnost a migrace
- DB se mění z verze 4 na 5 pouze přidáním `duration_seconds` v `sessions`. Nemažte databázi ani aplikaci.
- Starší historie, HIIT, běhy, šablony a uložené série se migrací nemažou.
- JSON záloha má `schemaVersion=5`; obnovit lze verze 1–5. Nové pole délky se při starších zálohách nechá prázdné.
- `applicationId=cz.rezim.app` zůstává nezměněný, aktualizace vyžaduje **stejný podepisovací certifikát jako dosud**.
- `app/release-version.properties` nastavuje v0.5 a kód verze `5000 + GITHUB_RUN_NUMBER`.
- Aktualizační ZIP `Hmyzak_update_v0.5.zip` patří do **kořene** repozitáře. Dosavadní workflow v0.4 samo vybere tuto verzi před v0.4 i v00.4; workflow **neupravovat**.
- Starý podepisovací klíč debug slouží k zachování kompatibility osobních aktualizací, nikoli jako bezpečný podpis pro veřejnou distribuci.
- Kompilace plné APK musí ještě proběhnout na GitHub Actions; bez tohoto kroku aplikace není otestovaná na Androidu.

## Po instalaci ověřit
1. Vytvořit JSON zálohu **před** instalací.
2. Ověřit digitální podpis, `applicationId`, `versionCode>4010` u výsledného APK.
3. Zkontrolovat historické tréninky a rozpracovaný trénink.
4. Otestovat +1/−1, označení čísel, dialog váhy, vibraci a přepínání vzhledů.
5. Otevřít grafy s historií a zkontrolovat obrázkovou kartu a export.
6. U uloženého tréninku zkusit změnu délky a ověřit, že se zachovala po restartu.
