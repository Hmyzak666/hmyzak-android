# Hmyzák – tréninkové nástroje a kola supersérií

Poslední potvrzená instalace před v0.10 je **0.9.18 / 9018**. Nové APK musí mít původní applicationId, certifikát a vyšší versionCode. Neinstaluje se automaticky.

## Během tréninku

Horní lišta zůstává viditelná při posouvání cviků. Ukazuje uplynulou délku tréninku, čas pauzy a tlačítko Spustit / Pozastavit / Pokračovat. Klepnutí na délku otevře opravu v minutách; od opravené hodnoty čas znovu přibývá. Původní čas zahájení, série a historie se přitom nemění. Dodatečná oprava uloženého tréninku zůstává v historii.

Klepnutí na Pauza otevře odpočet, ±30 s, zrušení, nové spuštění a nastavení. Délku lze volit z předvoleb nebo zapsat jako `90` / `1:30`. Povolených je 1 s až 60 min. Uložená délka platí pro příští spuštění; běžící pauzu mění ±30 s nebo Spustit znovu.

Výchozí režim je **Ručně**. Další volby jsou **Po každé sérii** a **Po celém kole supersérie**. U samostatného cviku je kolo jedna série. Po poslední sérii celého tréninku se už pauza nespouští. Potvrzení další série ukončí předchozí pauzu; odškrtnutí série zruší automatickou pauzu, kterou tato série spustila. Uložení, zahození tréninku nebo úspěšný import zálohy pauzu zruší.

Na konci zazní pouze vibrace, pokud je přepínač zapnutý; žádný zvuk. Vibrace potvrzení série a vibrace pauzy mají samostatné přepínače. Zobrazení v liště a na zamčené obrazovce vyžaduje povolená oznámení. Povolení lze vyžádat z dialogu pauzy nebo Nastavení.

Odpočet používá monotónní čas, takže posun systémových hodin nemění okamžik dokončení. Běží v uživatelem spuštěné foreground službě se ztišeným oznámením a časově omezeným CPU wake lockem pouze během aktivní pauzy. Pozastavení wake lock uvolní; z oznámení lze pokračovat nebo pauzu zrušit. Stav se ukládá před oznámením konce. Restart telefonu odpočet zruší; vynucené zastavení aplikace je systémové zastavení i její služby. Skutečné chování vibrací a zamčeného Samsungu se musí vyzkoušet na telefonu.

## Supersérie a trvalé poznámky

V editoru plánu i pod rozpracovaným cvikem otevře tlačítko Supersérie výběr členů. Dva nebo více cviků vytvoří skupinu; pořadí určuje jejich pořadí v plánu. Odebrat tento cvik odstraní pouze vybraného člena a zachová zbytek skupiny, pokud zůstanou alespoň dva. Přesun do jiné skupiny nezdvojuje cvik. Členství se ukládá i pro další tréninky a nikdy nepřepisuje už zapsané série.

Příklad **leg curl 4×, leg extension 4×, side band abduction 4×, side adduction 1×**: první kolo obsahuje všechny čtyři cviky. Ve druhém až čtvrtém kole se adduction vynechá. Přidané série během tréninku se zapojí podle skutečného počtu rozpracovaných sérií. Automatické posouvání respektuje tyto skupiny, ale zůstává samostatně vypínatelné.

### Volba kol ve v0.10

V dialogu **Supersérie** má každý vybraný cvik nabídku **Zařadit do supersérie**:

- **První kola** zachovají dosavadní chování.
- **Poslední kola** přesunou kratší cvik na konec. Bench 4× + kliky 1× znamená kliky po čtvrté sérii benche. Dvě série kratšího cviku se zařadí do třetího a čtvrtého kola.
- **Od 2. kola**, **Od 3. kola** atd. zvolí konkrétní začátek. Jedna addukce tak může být třeba ve třetím kole skupiny 4/4/4/1.

Pod nabídkou je výsledné kolo nebo rozsah kol; je také vidět u cviku v plánu a při tréninku. Počet kol určuje nejdelší cvik skupiny. Více sérií kratšího cviku navazuje bez mezer, a nabídka dovolí jen začátek, při kterém se vejdou všechny. Pokud se počet sérií nebo členové změní, konkrétní začátek se omezí na dostupný rozsah; volba **Poslední kola** automaticky sleduje aktuální konec. Při probíhajícím tréninku se počítají skutečné série včetně přidaných.

Uložení platí pro tento i další tréninky, zachovává všechny dokončené série a jejich hodnoty. Mění pouze jejich zařazení do kol pro posouvání a časovač. Pauza **Po celém kole supersérie** začne až po dokončení všech cviků v daném kole; po závěrečné sérii tréninku se nespustí.

Tlačítko Trvalá poznámka pod cvikem otevře text rovnou při cvičení. Uložení změní poznámku u stabilního ID cviku pro tento i další tréninky. Název, zátěž, již dokončené série a předchozí výkony zůstávají zachované.

## Nastavení a data

Nastavení se otevírá z horní lišty nebo přehledu. Obsahuje dosavadní tři vzhledy, automatické posouvání, nastavení a test vibrací série, nastavení a test vibrace pauzy, délku a režim časovače a odkaz na systémové vibrace. Původní preference používají stejné klíče a po aktualizaci zůstanou zachované.

V0.9 přešla z 7 na **8** dvěma přidanými sloupci: `plan_exercises.superset_id`, `sessions.elapsed_offset_seconds`. Původní dvojice se převedou na skupiny a jejich původní sloupce se nezmění. V0.10 přechází na **9** jedním sloupcem `superset_start_round` s výchozí hodnotou 1. JSON export má schéma 9, import podporuje 1–9 a staré dvojice doplní. Databázový soubor zůstává **rezim_local_v1.db**. Stav právě běžícího časovače je místní preference; nepřenáší se jako běžící budík při importu do jiného telefonu. Import chybných skupin nebo délek se transakčně vrací zpět.

Před aktualizací vytvoř nový JSON export mimo aplikaci. Regresní testy ověřují skutečné staré schéma, starší exporty, převod dvojic, změny za běhu, pořadí 4/4/4/1, pauzu/pokračování/zrušení časovače, jeho uložený stav i Compose ovládání (Nastavení také při 360 dp). Fyzické vibrace a chování Samsungu nenahrazují.

Robolectric má známý problém s editovatelnými dialogy při uměle zvětšené šířce okna ([#8460](https://github.com/robolectric/robolectric/issues/8460)). Dialogové UI testy proto používají výchozí platformní rozměry, rozměr 360 dp je omezený na test Nastavení. Zobrazovaný čas lze v UI testu zafixovat; testy skutečného odpočtu ve službě zůstávají nezávislé.
