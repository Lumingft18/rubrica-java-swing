# Rubrica

Per avviarla:

```
java -jar Rubrica.jar
```

Login: `admin` / `admin`

I dati finiscono in `informazioni.txt`, nella stessa cartella del jar. La prima volta il file non c'è: la rubrica parte vuota e non dà errore.

## Cosa fa

Gestisce i contatti.

- ogni persona ha nome, cognome, indirizzo, telefono, età
- la finestra principale mostra una tabella: solo nome, cognome e telefono
- in basso: Nuovo, Modifica, Elimina. Le stesse azioni sono anche nella toolbar, con le icone
- Nuovo e Modifica aprono l'editor. Una riga per campo: etichetta e casella. Bottoni Salva e Annulla
- Modifica o Elimina senza una riga selezionata: messaggio di errore
- Elimina chiede `Eliminare la persona NOME COGNOME?`. No non fa niente. Sì toglie la riga
- Salva scrive la persona e chiude l'editor. Annulla chiude e non salva
- all'avvio legge `informazioni.txt`. A ogni salvataggio e a ogni eliminazione riscrive tutto il file
- doppio click su una riga apre la modifica. Clic sull'intestazione ordina la colonna
- da tastiera: Invio sulla riga = Modifica, Canc = Elimina, Esc nell'editor = Annulla

Il telefono è una stringa, così restano gli zeri davanti (`00000111`).

Se `informazioni.txt` ha una riga che non si legge (campi mancanti, età non numerica), la riga viene saltata, all'accesso compare un avviso e il file com'era viene copiato in `informazioni.txt.bak`. Il file si scrive prima in `informazioni.txt.tmp` e poi prende il posto del vecchio: se il programma si chiude a metà, il file non resta a pezzi.

Formato di una riga:

```
nome;cognome;indirizzo;telefono;eta
Steve;Jobs;via Cupertino 13;0612344;56
```

Nome e cognome sono obbligatori. L'età è un intero da 0 a 150. Lunghezze massime come le colonne del database: nome e cognome 80, indirizzo 200, telefono 40. I campi non possono contenere `;` né andare a capo, altrimenti la riga del file si spezza.

Oltre al file unico, i contatti stanno anche nella cartella `informazioni/`, un file per persona: `Cognome-Nome.txt`. Se due persone hanno lo stesso nome, il secondo file è `Cognome-Nome-2.txt`. Se manca `informazioni.txt`, il programma ricostruisce l'elenco da quella cartella. L'ordine stabile è quello del file unico.

## Login

All'avvio si vede solo la finestra di accesso. Se utente o password sono sbagliati esce `Login errato` e la rubrica non si apre.

Il primo avvio crea `utenti.txt` con l'utente `admin` / `admin`. Le password sono salvate come hash (PBKDF2 con sale), non in chiaro. Per aggiungere un utente si scrive una riga `username;password` in chiaro: al primo login giusto il programma la sostituisce con l'hash.

## Come è fatto

```
src/rubrica/Persona.java              contatto
src/rubrica/Utente.java               chi fa il login
src/rubrica/Password.java             hash delle password
src/rubrica/ElencoPersone.java        la lista
src/rubrica/Validatore.java           controlli prima di salvare
src/rubrica/RubricaMain.java          main
src/rubrica/archivio/                 file, cartella, utenti, MySQL
src/rubrica/gui/                      finestre, tabella, icone, stile
test/rubrica/test/                    prove automatiche
```

Compilare: `./compila.sh`
Test: `./test.sh`

Le prove delle finestre si attivano con `RUBRICA_TEST_GUI=true ./test.sh` e richiedono una sessione grafica. Le prove MySQL usano un database separato `rubrica_test`: istruzioni in `test/README.md`. Per compilare serve un JDK 9 o successivo.

In Eclipse il main è `rubrica.RubricaMain`. Il jar è già pronto: dentro c'è anche il driver MySQL. Serve Java 8 o più recente.

## Database

Di default `modalita=file` in `credenziali_database.properties`. MySQL non serve.

Per usare il database (lo zip è `dist/Rubrica.zip`; se l'hai ricevuto da solo, parti dal punto 1 nella cartella dove l'hai scompattato):

1. scompattare lo zip
2. mettere utente, password, host e porta nel properties
3. mettere `modalita=database`
4. lanciare `schema_database.sql` su MySQL
5. `java -jar Rubrica.jar`

Lo zip `Rubrica-sorgenti.zip` contiene il progetto Eclipse, i sorgenti, le librerie e i test.

Lo zip `Rubrica.zip` contiene `Rubrica.jar`, `schema_database.sql`, `credenziali_database.properties` e questo file.

Se il database non risponde, un avviso e si continua coi file.

La connessione usa SSL quando il server lo permette (MySQL 8 sì, di serie).

## Prove a mano

1. `java -jar Rubrica.jar`
2. entra con admin / admin
3. Nuovo: René Ferretti, via Roma 1, 06123, 30. Salva. La riga c'è
4. clic sulla riga, Modifica: i campi sono pieni. Annulla non deve cambiarli
5. Modifica, cambia il telefono, Salva: in tabella c'è il numero nuovo
6. Elimina senza selezione: esce l'errore
7. Elimina con la riga, No: resta. Sì: sparisce
8. chiudi e riapri: i dati ci sono ancora, e `informazioni.txt` ha la stessa riga
