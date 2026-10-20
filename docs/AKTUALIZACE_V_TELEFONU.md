# Aktualizace Hmyzáka v telefonu

Původní výchozí vydání tohoto sjednocení je **v0.7.14 / 7014**, tag [`hmyzak-build-14`](https://github.com/Hmyzak666/hmyzak-android/releases/tag/hmyzak-build-14). Sjednocená aktualizace **v0.7.15 / 7015** prošla podepsaným CI a uživatel 2026-10-21 potvrdil její funkčnost a zachování dat na Samsungu. Následující postup platí i pro další aktualizace.

1. V používané aplikaci zvol **Domů → Záloha JSON**. Uchovej datovaný export také mimo telefon a ponech předchozí export. Záloha není šifrovaná; nedávej ji do veřejného repozitáře.
2. Ověř, že JSON lze přečíst, má `format: rezim-local-backup`, podporované `schemaVersion` (aktuálně 6) a očekávané tréninky, šablony, běhy a měření. Obnovu zkoušej v oddělené testovací instalaci se syntetickými nebo kopírovanými daty; import nahrazuje data cílové instalace. Neověřuj zálohu přepsáním své jediné živé databáze.
3. Zkontroluj úspěšné zdrojové testy i podepsaný běh a jeho `overeni-apk.json`: `application_id` musí být `cz.rezim.app`, certifikát musí odpovídat původnímu a `version_code` musí být vyšší než **skutečně nainstalovaná verze** (poslední potvrzená je 7015).
4. Teprve potom stáhni nové ověřené APK z příslušného Release nebo schváleného Actions artefaktu. Vývojové `app-debug.apk` a `app-release-unsigned.apk` k této aktualizaci nepoužívej.
5. Při otevření APK Android nabídne **Aktualizovat**. Pokud instalaci odmítne, zastav se a ověř podpis, číslo verze a zálohu. **Neodinstalovávej aplikaci a nepoužívej Vymazat data.**
6. Po aktualizaci porovnej poslední hotový i rozpracovaný trénink, vlastní plány, poznámky a supersérie, běhy/HIIT a vážení včetně záznamů bez obvodu. Vyzkoušej vibrace, uložení série a opětovné otevření aplikace. Pak vytvoř další JSON export.

Sjednocení zachovává `rezim_local_v1.db`, databázovou verzi 6, JSON schéma 6 a původní certifikát. Automatické testy používají syntetické údaje. Před aktualizací 0.7.15 byla navíc samostatně zkontrolována skutečná záloha a uživatel potvrdil zachování dat na telefonu; pro každou další aktualizaci ověř aktuální export znovu. Podrobnosti jsou v [záznamu ověření](SJEDNOCENI_ZDROJU.md).

Pro návrat k předchozímu chování sestav původní zdroje v nové kompatibilní aktualizaci se stejným certifikátem a vyšším číslem verze. Staré APK neinstaluj naslepo a nesnižuj databázovou verzi. Podrobnosti: [sjednocení a návrat](SJEDNOCENI_ZDROJU.md), [GitHub Actions](AUTOMATICKE_SESTAVOVANI_APK.md).
