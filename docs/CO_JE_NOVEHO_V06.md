# Hmyzák v0.6 — pohodlnější zápis

## Co je hotové ve zdrojovém kódu

- Podržení celé plochy políčka kg otevře rychlou změnu váhy o ±2,5 kg. Obsluha gesta probíhá před výběrem textu; běžné klepnutí zachovává zadávání z klávesnice. Tlačítka změny váhy mají větší dotykovou plochu. Dokončenou sérii je nejdřív potřeba vrátit mezi nedokončené.
- Hlavní tlačítko na úvodu je **Kopírovat minulý týden**. Sdílení minulého týdne je v nabídce s ostatním sdílením a kopírováním.
- **Studio** je výchozí vzhled, pokud ještě nebyl uložený výběr. Již uložený motiv se zachová; ostatní dva vzhledy zůstávají.
- Po úspěšném odkliknutí i vrácení série se používá systémová potvrzovací haptika AndroidX namísto jemného impulzu pro posun textového kurzoru. Vypínač zůstává a na úvodu přibylo tlačítko **Vyzkoušet vibraci**. Respektuje se nastavení systémových vibrací dotyku; funkci na konkrétním telefonu je třeba ověřit.
- Po dokončení série se přibližně za 0,3 s obrazovka plynule přesune na další nedokončenou sérii. V supersérii se cviky střídají. Po dokončení dvojice následuje další nedokončený cvik, případně další dvojice. Různý počet sérií není problém. Nakonec se zobrazí **Uložit trénink**.
- Dříve přeskočené série se před závěrečným uložením znovu nabídnou. Posouvání žádnou sérii samo neoznačuje a trénink samo neukládá. Vrácení fajfky neposouvá obrazovku; další klepnutí zruší čekající přesun.
- Přepínač **Automaticky posouvat na další sérii** na úvodu je ve výchozím stavu zapnutý. Jeho volba se ukládá v telefonu.
- Přibylo 15 regresních testů pořadí sérií. Stávající release workflow je spouští přes závislost úlohy `assembleRelease`; workflow ani podepisovací Secrets není potřeba měnit.

## Aktualizace z telefonu

1. Stáhnout `Hmyzak_update_v0.6.zip` a bez rozbalování nahrát do kořene `Hmyzak666/hmyzak-android` přes **Add file → Upload files → Commit changes**.
2. Stávající workflow vybere v0.6 před v0.5, v0.4 i opravnou v00.4. Starší ZIPy se nemusí mazat. Nový ZIP obsahuje kompletní potřebné soubory `app/`, nikoli jen rozdíl proti v0.5.
3. Po zeleném sestavení stáhnout `Hmyzak-v0.6-build-<číslo>.apk` z Releases. Před instalací ověřit podpis oproti používanému APK a vytvořit čerstvou JSON zálohu.
4. V telefonu zvolit **Aktualizovat**. Zachování historie a konkrétní ovládání ověřit po instalaci.

Přímé odesílání změn z ChatGPT do GitHubu vyžaduje samostatné připojení GitHubu s odpovídajícím přístupem k repozitáři. Tato verze používá dosavadní nahrávání ZIPu; změna způsobu vývoje nevyžaduje změnu aplikace ani podpisového klíče.

## Kompatibilita

- `applicationId=cz.rezim.app`, stejný název databáze a **databázová verze 5**. `LocalStore.kt` zůstává beze změny; tato aktualizace nevyžaduje databázovou migraci.
- Formát JSON záloh zůstává `schemaVersion=5`. Historie, běhy, HIIT, měření a rozpracované série používají dosavadní ukládání.
- `versionBaseCode=6000`, výsledná verze `0.6.<GITHUB_RUN_NUMBER>`. Při dosavadním číslování je kód vyšší než v0.5.11 (`5011`).
- Podepisovací konfigurace a GitHub Secrets zůstávají původní. Místní testovací APK s jiným podpisem není aktualizace určená pro používanou aplikaci.

## Po instalaci vyzkoušet

1. Zachované tréninky, měření i rozepsaný trénink a vybraný vzhled Studio.
2. Krátké klepnutí do kg a opakování; podržení kg uprostřed i při okrajích pole; běžné posouvání přes pole.
3. Opakované ±2,5 kg v dialogu a zavření bez klávesnice přes dialog.
4. Tlačítko pro zkoušku haptiky, dokončení i vrácení série a vypnutí haptiky.
5. Střídání supersérie, dokončení všech kol, přechod na další cvik a na Uložit trénink; následně vypnutí automatického posouvání.
6. Kopírování minulého týdne přímo a sdílení z rozbalovací nabídky.
