# Test

`./test.sh` ricompila sorgenti e prove, poi esegue JUnit. Serve un JDK 9 o successivo.

Per includere le finestre Swing, da una sessione grafica:

```
RUBRICA_TEST_GUI=true ./test.sh
```

Le prove aprono le finestre e premono i bottoni. Controllano login, inserimento, modifica, annullamento, eliminazione, ordinamento, toolbar, scorciatoie e ripristino dopo un errore di salvataggio.

## MySQL

Queste prove vanno eseguite su un server di prova. Svuotano le tabelle `persona` e `utente` del database `rubrica_test` prima di ciascun caso. Non usano il database normale `rubrica` né le credenziali dell'applicazione.

Per creare un server separato con Docker, dalla cartella del progetto:

```
docker run -d --name rubrica-test-mysql -e MYSQL_ROOT_PASSWORD=rubrica-test -p 127.0.0.1:33316:3306 mysql:8.0
```

Quando MySQL è pronto, creare lo schema di prova:

```
sed 's/rubrica/rubrica_test/g' schema_database.sql | docker exec -i -e MYSQL_PWD=rubrica-test rubrica-test-mysql mysql -uroot
```

Eseguire tutte le prove, comprese finestre e database:

```
RUBRICA_TEST_GUI=true RUBRICA_TEST_MYSQL_PORT=33316 ./test.sh
```

Le prove MySQL controllano accesso, conversione delle password, query con parametri, inserimento, modifica, eliminazione e rollback. Senza le variabili, JUnit segnala le prove opzionali come saltate.

Terminata la verifica, rimuovere il solo server di prova:

```
docker rm -f rubrica-test-mysql
```
