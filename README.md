# Hmyzák v0.2 – Android (Kotlin / Jetpack Compose)

**Projekt navazuje na původní REŽIM v0.1.3.** Jeho interní `applicationId` zůstává `cz.rezim.app`, název zobrazený uživateli je **Hmyzák**. Aplikace používá stejný soubor lokální databáze `rezim_local_v1.db`. Databázová verze se zvyšuje z 1 na 2 a migrace *nemaže žádné původní tabulky s tréninky*. Nové editovatelné šablony jsou v `plan_days` a `plan_exercises`.

## Funkce v0.2

- Vybraná ikona: **zelený brouk s činkou na zádech**; AMOLED černá.
- Klávesnice: `adjustResize`, `imePadding`, schování spodní navigace při psaní, automatický posun zaměřeného pole na obrazovku a spodní prostor pro poslední cvik.
- Pracovní série i původní `Previous`; opakování a váha z minula se předvyplní; ✓ vpravo. Žádný povinný RIR ani časovač.
- Libovolný počet vlastních cviků a tréninkových šablon (UI limit 20 dnů); změna pořadí šipkami nahoru/dolů, odebrání z šablony bez vymazání historických tréninků.
- Editovatelné dvojice supersérií: po potvrzení série se seznam posune na partnerský cvik. Ve výchozím úterku jsou propojené **Hip Thrust + Calf Raises** a **Leg Curls + Leg Extensions**. Front squat a RDL jsou samostatně.
- Nová šablona **NE · Mini Lower**: step-downs (2 série na nohu), tibialis raises (3), seated calf raises (3), TrainMax Finger Extensors (1), wrist extension levá (1). Hodnoty série jsou editovatelné přidáním vlastní varianty cviku; počáteční šablona pouze pro další tréninky.
- Rychlé spuštění **PO / ÚT / ST / PÁ / NE mini lower / Běh / HIIT** na domovské obrazovce.
- Grafy: tempo a průměrný tep při běhu; tep v HIIT; hmotnost a obvod pasu; síla či celková opakování v jednotlivých dokončených trénincích.
- **Sdílet výkony s ChatGPT** – výpis posledních 7 silových tréninků, 5 běhů, 3 HIIT a 3 měření přes standardní androidí sdílení. Je také **Kopírovat výpis**. Nejde o automatické propojení/MCP; aplikace nemá internetové oprávnění a neodesílá nic bez tvé volby.
- JSON zálohy jsou nyní schéma v2 a lze také importovat starší zálohy v1; staré zálohy neměly upravitelné plány, proto po jejich importu vzniknou výchozí šablony.

## Bezpečný postup aktualizace existující aplikace

**Než začneš: v původní aplikaci REŽIM otevři `Domů → Záloha JSON` a bezpečně ulož kopii.** Máš tam svůj středeční trénink. APK zatím NEMAŽ a nepoužívej „Vymazat data“.

**Doporučený postup pro stávající Bazzite počítač:**

1. Zálohuj si původní složku projektu `~/Downloads/REZIM_Android` pro případ návratu.
2. Ve složce `Downloads` rozbal `Hmyzak_v0.2_AKTUALIZACE_souboru.zip` přes stávající `REZIM_Android`, potvrď přepsání souborů. Tento balíček obsahuje **jen změněné zdrojové soubory**. Připravený a ověřený `gradle/wrapper/gradle-wrapper.jar` proto zůstává zachovaný.
3. Otevři v Android Studiu **tutéž** složku `REZIM_Android`. Zkontroluj Gradle JDK 17 nebo 21, připojený Android SDK 36.
4. Spusť **File → Sync Project with Gradle Files** a **Build → Build APK(s)**. Výsledné APK najdeš v `app/build/outputs/apk/debug/app-debug.apk`.
5. Pokud používáš **stejný Bazzite počítač, stejné ID aplikace a stejný debug podpisový klíč** (výchozí `~/.android/debug.keystore`), lze APK nainstalovat jako aktualizaci přímo přes stávající aplikaci bez odinstalování a bez ztráty dat. Při chybě `App not installed` aplikaci neodinstalovávej; nejdřív zkontroluj podpis a zálohu.
6. Po aktualizaci ověř, že tvůj středeční výkon stále existuje, že otevřeš **NE Mini Lower** a že funguje **Sdílet výkony s ChatGPT**. Znovu proveď JSON zálohu.

**Pozor:** Android při instalaci odmítá aktualizace s jiným podpisem. APK sestavené v GitHub Actions s jeho výchozím debug klíčem *nelze* instalovat přes místně podepsanou debug verzi bez migrace. Pro budoucí telefon-only aktualizace je potřeba jednorázově přejít na stabilní release klíč a nastavit automatický build. To zatím není zřízené.

## Stáhnutí nového projektu místo aktualizace

Obsah plného ZIP je zdrojový projekt; pro jeho sestavení na Bazzite je při úplně prvním otevření třeba v kořeni projektu spustit `bash ./prepare-gradle-wrapper.sh`. U existujícího projektu při použití *patch* balíčku tento krok znovu neděláš.

## Důležitá omezení

- Tento projekt jsem v prostředí bez Android SDK **nemohla skutečně sestavit do APK ani otestovat na telefonu**. Všechny změny je ještě potřeba ověřit po Gradle kompilaci v Android Studiu na Bazzite.
- Přímý přístup ChatGPT do lokální databáze není hotový; tlačítko Sdílet předává jen textový výpis přes Android share sheet.
- Zálohovací JSON není šifrovaný; nesdílej veřejně celý export.
- Graf jednotlivých cviků umožňuje přepnout maximum kg a součet dokončených opakování (u cviků bez závaží jen opakování). Není to odhad svalového růstu.


## GitHub Actions – APK ke stažení v telefonu

Připraveno je workflow `.github/workflows/android-release.yml`, které sestavuje podepsané APK, zvyšuje `versionCode` a publikuje GitHub Release. **Není aktivní, dokud projekt nenahraješ do GitHub repozitáře a nenastavíš čtyři Secrets s původním podpisovým klíčem.** Podrobný postup: [`docs/AUTOMATICKE_SESTAVOVANI_APK.md`](docs/AUTOMATICKE_SESTAVOVANI_APK.md).
