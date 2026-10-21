# Sestavování APK přímo ze zdrojů

Platí pro sjednocenou větev navazující na v0.7.14. Stávající klíč je již použitý vydáním `hmyzak-build-14`; neměň jej. Aktualizační ZIPy se při sestavení nerozbalují.

## Běžná kontrola změn

Workflow `android-ci.yml` běží při pushi, PR do `main` a ručním spuštění. Ověřuje oficiální Wrapper, Python testy, JUnit/Robolectric testy, Android Lint a debug sestavení. Nemá přístup k podepisovacím secrets. Výsledky jsou v artefaktu `Hmyzak-kontroly-*`; debug APK není aktualizace pro telefon.

## Podepsaný kandidát bez vydání

1. Otevři **Actions → Hmyzák – podepsané APK → Run workflow**.
2. Vyber kontrolovanou pracovní větev a nech **publish_release vypnuté**.
3. Workflow odvodí verzi z `app/release-version.properties` a původního čítače běhů: `9000 + run_number`, název `0.9.run_number`. Například běh 18 vytvoří `9018 / 0.9.18`. Číslo musí být vyšší než ověřená nainstalovaná hodnota 8017.
4. Nejprve proběhnou testy a lint. Pak se původní klíč dočasně obnoví na runneru a sestaví release APK.
5. `tools/release_tools.py verify` ověří platnost podpisu, **přesnou shodu certifikátu**, `cz.rezim.app`, navazující číslo a očekávaný název verze. Chyba ukončí sestavení před zveřejněním artefaktu.
6. Artefakt `Hmyzak-APK-<běh>` obsahuje podepsané APK, `SHA256SUMS.txt` a `overeni-apk.json`. Podepisovací klíč se po použití odstraní; artefakt jej neobsahuje.

Soubor `android-release.yml` nepřejmenovávej bez řešení návaznosti čísel. Při resetu čítače nelze snížit verzi: nejdříve zkontroluj všechny dosud vydané verze a uprav základ tak, aby příští `versionCode` byl vyšší. Kontrolní minimum v `config/release-compatibility.json` aktualizuj při příštím ověřeném vydání; nevypínej kontrolu podpisu.

## Zveřejnění

Teprve po kontrole zdrojů, zeleném CI a zálohy dat sluč změny do `main`. Ručně spusť podepsané workflow z `main` s **publish_release zapnuté**. V samostatné úloze s právem zápisu se zveřejní jen ověřený artefakt z daného běhu. Vydání má tag `hmyzak-build-<běh>` a APK `Hmyzak-v0.9-build-<běh>.apk`.

Push do `main` nepodepisuje ani nepublikuje APK. Zveřejnění z pracovní větve workflow odmítne. Nejdříve ověř [postup pro telefon](AKTUALIZACE_V_TELEFONU.md); nic zde samo neinstaluje aplikaci.

## Zachované secrets

| Secret | Účel |
| --- | --- |
| `HMYZAK_KEYSTORE_BASE64` | Stávající keystore v base64, stejný jako u funkční aplikace |
| `HMYZAK_STORE_PASSWORD` | Dosavadní heslo úložiště |
| `HMYZAK_KEY_ALIAS` | Dosavadní alias |
| `HMYZAK_KEY_PASSWORD` | Dosavadní heslo klíče |

Secrets nezapisuj do zdrojů, logů ani chatu. Při chybějícím klíči nebo neshodě certifikátu oprav konfiguraci původního klíče; negeneruj náhradní. Veřejný kontrolní otisk je v `config/release-compatibility.json`.

Nové workflow prošlo místními kontrolami i skutečnými GitHub Actions běhy. [Podepsaný běh 15](https://github.com/Hmyzak666/hmyzak-android/actions/runs/39494987115) potvrdil přístup ke stávajícím secrets, sestavil `0.7.15 / 7015` a ověřil původní certifikát, applicationId i verzi. Publikační úloha byla přeskočena. Uživatel následně potvrdil funkční aktualizaci a zachování dat na Samsungu. Další aktualizace musí mít vyšší `versionCode` než poslední potvrzených 8017; neměň cestu workflow ani nevytvářej nový klíč. Podrobný záznam je v [ověření sjednocení](SJEDNOCENI_ZDROJU.md).
