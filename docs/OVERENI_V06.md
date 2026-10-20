# Ověření Hmyzáka v0.6

Datum: 20. 10. 2026. Výchozí projekt: v0.5; aktualizační ZIP v0.5 na GitHubu byl porovnán se zdroji a odpovídá jim.

| Kontrola | Výsledek |
| --- | --- |
| Překlad a sestavení testovacího Android APK | Úspěch (`testDebugUnitTest` a `assembleDebug`) |
| Sestavení release APK | Úspěch (`assembleRelease`), místně nepodepsané |
| Regresní testy navigace | 15/15 úspěšných pro debug i release, nic přeskočeno |
| Povinná kontrola Lint při release | Úspěch (`lintVitalRelease`) |
| Automatické testy při příkazu assembleRelease | Ověřeno; `testReleaseUnitTest` se spustil bez výslovného zadání testovací úlohy |
| Identifikátor a místní verze | `cz.rezim.app`, `versionCode=6000`, `versionName=0.6.0` |
| Databázový kód a schéma | `LocalStore.kt` je shodný s v0.5; databáze a JSON zůstávají ve verzi 5 |
| Konfigurace podpisu a GitHub workflow | Používá se původní nastavení a klíč v GitHub Secrets |

Testy pokrývají běžné série, dvě navazující supersérie, nestejný počet sérií, již hotové a dříve přeskočené série, nepřilehlou dvojici, skutečně přidané série, chybějícího partnera, vrácení fajfky i přechod na Uložit trénink.

Místní prostředí bylo doplněno o přesné verze závislostí z oficiálních Google Maven a Maven Central a o připnuté sestavovací nástroje projektu. Release kontrola běžela offline s místní kopií těchto závislostí, bez změny repozitářů či verzí v dodávaném projektu.

Místní APK není určeno k aktualizaci používané aplikace. Distribuované APK musí podepsat GitHub původním klíčem. Ovládání dotykem, sílu vibrace na Samsungu a zachování skutečných dat v telefonu je ještě nutné ověřit po instalaci; před ní vytvořit aktuální JSON zálohu. Připojení GitHubu bylo ověřeno, ale pokus nahrát aktualizační ZIP přes integraci skončil chybou GitHub API 403: `Resource not accessible by integration`. Tímto pokusem nebyla větev repozitáře změněna. Pro tuto aktualizaci je připravený dosavadní postup ručního nahrání ZIPu; přímé odesílání potřebuje integraci se skutečným oprávněním zapisovat obsah repozitáře.
