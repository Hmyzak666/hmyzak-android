# Hmyzák v0.3 – návrh aktualizace k ověření

## Opravy podle požadavků

1. Hlavička je odsazená pod systémovou stavovou lištu (`statusBarsPadding`).
2. Rozpracovaný trénink lze zahodit po potvrzení; lze také uložit jen dokončené série.
3. Výpony na lýtka akceptují 0 kg a nové cvičení předvyplní 0 kg, pokud jde o klíč Calf Raises; i po přejmenování.
4. Rozpracovaný trénink se ukládá do lokální databáze při každé změně série; lze v něm pokračovat po obnovení aplikace.
5. „Uložit trénink“ je také pod posledním cvikem.
6. Cviky lze přejmenovat a doplnit trvalou poznámku přes „Upravit plán“ → ikona ✎. Poznámka je vždy viditelná pod názvem cviku.
7. Minulý týden (Po–Ne) lze sdílet nebo zkopírovat na domovské obrazovce; pondělní tělesná měření se připojí při exportu v pondělí.
8. Celou historii lze sdílet jako textový soubor přes bezpečný lokální FileProvider; aplikace stále nevyžaduje internetové oprávnění.
9. Při odchodu z otevřeného tréninku se zobrazí upozornění. Opuštění nevymaže dosavadní práci.
10. Dokončené silové tréninky lze opravovat ze seznamu na domovské stránce; v bězích, HIIT a měřeních je tlačítko „Opravit záznam“.

## Ochrana dat

- Identifikátor aplikace zůstává `cz.rezim.app`.
- SQLite databáze `rezim_local_v1.db` přechází ze schématu 2 na 3 jediným přidáním sloupce `note` k `plan_exercises`; starší záznamy se nemažou.
- Obnova ze záloh JSON schématu 1 a 2 zůstává podporovaná; nová záloha používá schéma 3.
- GitHub Actions používá dosavadní uložený podepisovací klíč (z repository secrets), ne nový. Do archivu se nedává žádný klíč.
- Číslo verze buildu: `3000 + GITHUB_RUN_NUMBER`, aby bylo vyšší než dosavadní v0.2.x.

## Ověření po sestavení

1. Nejdřív zálohujte JSON ve stávající v0.2.
2. Ověřte v GitHub Actions, že build je zelený. Pokud je červený, nový APK neinstalujte a pošlete chybu.
3. Ověřte podpis a ID APK. Nikdy neodinstalovávejte původního Hmyzáka kvůli aktualizaci.
4. Po instalaci prověřte, že zůstaly staré silové tréninky a běhy.
5. Zkušební rozpracovaný trénink spusťte, vyplňte jednu sérii, opusťte a obnovte, pak jej zahoďte.
6. Otestujte Calf Raises na 0 kg, poznámku a její zachování, editaci již uložené série a týdenní export.

## Omezení

Kód nebyl v tomto prostředí sestaven proti kompletnímu Android SDK. Finální kompilaci musí ověřit GitHub Actions. Toto ZIP je **zdrojový projekt**, nikoli hotové APK.
