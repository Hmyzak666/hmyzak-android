# Hmyzák – aktualizace přes telefon (GitHub Actions)

Projekt obsahuje workflow `.github/workflows/android-release.yml`. Po nastavení vytvoří **podepsané APK** a uloží je jako GitHub Release, odkud se dá stáhnout přímo do Samsungu bez Android Studia. Vše se spouští při změně kódu na větvi `main` a ručně tlačítkem **Run workflow**.

## Proč potřebujeme podpis staré aplikace

Původní REŽIM/Hmyzák v0.1 byl instalován jako lokální *debug* APK. Každý počítač může mít odlišný debug klíč. Android přijme aktualizaci pouze pokud `applicationId` (`cz.rezim.app`) a podpisový certifikát odpovídají původní aplikaci. **Nepoužívej nově vytvořený klíč ani náhodné debug APK z GitHubu.**

Abys zachoval už uložený středeční trénink, při prvním nastavení GitHub Actions použij **stejný soubor `debug.keystore`, jakým byla podepsána původní aplikace na Bazzite**. Ten se ukládá jako zabezpečený Secret, nikdy se nedává do repozitáře. To umožní přejít na CI sestavované *release* APK bez odinstalování současné aplikace, protože certifikát bude shodný. Přechod na samostatný silnější release klíč by později vyžadoval řízenou migraci dat.

## Zřízení na GitHubu

1. Ověř, že máš aktuální neprázdnou **JSON zálohu** ze Samsungu, a ulož ji i mimo telefon. Původní aplikaci neodinstalovávej.
2. Vytvoř nový **soukromý** GitHub repozitář, například `hmyzak-android` (bez inicializačního README).
3. Nahraj do něj **obsah složky `REZIM_Android`**, tedy tak, aby `app/`, `build.gradle.kts`, `gradlew` a `.github/workflows/android-release.yml` byly přímo v kořeni repozitáře. Při uploadu **nikdy neodesílej** `debug.keystore`, žádné `.jks`, `local.properties` ani zálohy tréninků. `.gitignore` tyto soubory ignoruje.
4. Na počítači najdi **skutečný** debug keystore používaný při sestavení původního APK. Lze to zjistit v Android Studiu v Gradle tasku `:app:signingReport` (`Variant: debug`, položka `Store:` a SHA-256). Obvyklé umístění je `~/.android/debug.keystore`, ale u Flatpaku může být jinde. Důležitá je shoda podpisového certifikátu.
5. V GitHub repozitáři otevři **Settings → Secrets and variables → Actions → New repository secret** a vytvoř čtyři secrets:
   - `HMYZAK_KEYSTORE_BASE64` = obsah **správného** `debug.keystore` převedený do base64. Lokálně na Bazzite ho přenes do schránky příkazem `base64 -w0 /SKUTECNA/CESTA/debug.keystore | wl-copy`, potom vlož přímo do Secret Value na GitHubu. **Nikdy mi neposílej výsledek příkazu.**
   - `HMYZAK_STORE_PASSWORD` = standardně `android` (pro výchozí debug keystore)
   - `HMYZAK_KEY_ALIAS` = standardně `androiddebugkey`
   - `HMYZAK_KEY_PASSWORD` = standardně `android`
   Hesla a alias jsou uvedeny pro *výchozí* Android Studio debug keystore. U vlastní konfigurace mohou být odlišné.
6. Otevři záložku **Actions → Hmyzák – podepsané APK → Run workflow**, zvol `main` a spusť sestavení. Pokud bylo workflow nahráno do main, poběží také samo po pushi.
7. Při úspěšném sestavení otevři **Releases**, vyber poslední Hmyzák build a stáhni `Hmyzak-v0.2-build-*.apk` přímo na Samsungu (u soukromého repozitáře se přihlas k GitHubu).
8. **Před instalací** musí odpovídat podpis (shodný klíč jako původní APK). V telefonu otevři stažené APK. Android by měl nabídnout **Aktualizovat**, nikoli novou instalaci. Při chybě podpisu aplikaci NIKDY neodinstalovávej; nejdříve ověř výběr keystore a zálohu.
9. Po instalaci ověř středeční trénink, vyzkoušej sdílení s ChatGPT a exportuj novou zálohu.

## Další aktualizace

Upravené soubory commitni do stejného repozitáře na větev `main`. GitHub Actions sestaví další vyšší `versionCode`, podepíše APK stejným klíčem a přidá nový GitHub Release. Stáhneš APK do telefonu a klikneš na **Aktualizovat**. Kód projektu se nemění na novou aplikaci, interní `cz.rezim.app` zůstává.

## Bezpečnost a omezení

- Tento podpis je stále původní **debug certifikát**, vhodný pro osobní soukromé používání a pokračování v existující instalaci. Není doporučen pro Google Play ani širší veřejnou distribuci. Klíč dlouhodobě bezpečně zálohuj a nikde ho veřejně nevystavuj.
- GitHub ukládá Actions Secrets odděleně od repozitáře. Člověk se zápisovým přístupem a možností měnit workflow může ale potenciálně klíč zneužít; repozitář ponech soukromý a nesdílej práva zápisu.
- Neupravuj nebo nepřejmenovávej `applicationId`. Každý další build musí mít vyšší verzi a stejný podepisovací klíč.
- Tlačítko Sdílet s ChatGPT již existuje ve zdrojovém projektu v0.2; plně automatické připojení ChatGPT k datům **zatím není implementované**.
- Tento ZIP je připravený a staticky zkontrolovaný projekt, ale nebyl zde kompilován přes Android SDK. První build bude potřeba ověřit na GitHub Actions.
