# Budoucí propojení s ChatGPT – pouze čtení

Výchozí verze v0.7.14 automatické propojení **neobsahuje**. Je offline; ruční sdílení textového výpisu není synchronizace. Tento dokument je návrh pro budoucnost, sjednocení zdrojů jej neimplementuje.

## Požadavky na případný konektor

- V aplikaci se uživatel výslovně přihlásí / udělí souhlas ke sdílení s konkrétním poskytovatelem.
- Android zařízení synchronizuje na soukromý backend, **nikoli přímo do konverzace**.
- Backend zpřístupní ChatGPT samostatné MCP rozhraní s operacemi pouze pro čtení:
  - `list_workouts(start_date, end_date)`
  - `get_workout(workout_id)`
  - `list_running_sessions(start_date, end_date)`
  - `list_hiit_sessions(start_date, end_date)`
  - `list_body_measurements(start_date, end_date)`
- Neimplementovat nástroje pro úpravu, odstranění, ani zapisování tréninkového plánu.
- Autorizace na serveru musí skutečně odmítat všechny metody měnící data. Pouhá slovní instrukce modelu nestačí.
- Přístupový token uložit šifrovaně, vázat na uživatele, s krátkou expirací a možností okamžitého zrušení.
- Oddělit přístup ChatGPT (jen čtení) od zápisu synchronizace z Androidu.
- Žádné zdravotní údaje nesdílet bez souhlasu; minimalizovat požadavky a logování.
- Při nedostupnosti internetu vždy funguje offline zápis tréninků.
- Reálná dostupnost vlastních propojení se musí ověřit pro uživatelův aktuální účet/klienta při implementaci.

**NIKDY** do Android APK neukládat OpenAI API klíč nebo tajný klíč backendu.
