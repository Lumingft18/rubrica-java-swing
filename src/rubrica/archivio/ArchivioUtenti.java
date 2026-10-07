package rubrica.archivio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

import rubrica.Password;
import rubrica.Utente;

/**
 * utenti.txt, una riga: username;password
 * La password è salvata come hash (vedi Password). Una riga in chiaro scritta a mano
 * funziona lo stesso e al primo login giusto viene riscritta con l'hash.
 * Se il file non c'è, creo admin/admin così si può entrare subito.
 */
public class ArchivioUtenti {

    private final File file;

    public ArchivioUtenti(File file) {
        this.file = file;
    }

    public void assicuraUtenteDiProva() throws IOException {
        Vector<Utente> utenti = carica();
        if (!utenti.isEmpty()) {
            return;
        }
        utenti.add(new Utente("admin", Password.hash("admin")));
        salva(utenti);
    }

    public boolean verifica(String username, String password) throws IOException {
        Vector<Utente> utenti = carica();
        for (int i = 0; i < utenti.size(); i++) {
            Utente utente = utenti.get(i);
            if (utente.coincide(username, password)) {
                if (Password.inChiaro(utente.getPassword())) {
                    utenti.set(i, new Utente(username, Password.hash(password)));
                    salva(utenti);
                }
                return true;
            }
        }
        return false;
    }

    public Vector<Utente> carica() throws IOException {
        Vector<Utente> utenti = new Vector<Utente>();
        if (!file.exists()) {
            return utenti;
        }
        Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8");
        try {
            while (scanner.hasNextLine()) {
                String riga = FileTesto.senzaBom(scanner.nextLine()).trim();
                if (riga.length() == 0) {
                    continue;
                }
                // limite 2: la password può contenere un punto e virgola
                String[] parti = riga.split(";", 2);
                if (parti.length != 2 || parti[0].trim().length() == 0) {
                    System.err.println("Riga utente ignorata: " + riga);
                    continue;
                }
                utenti.add(new Utente(parti[0].trim(), parti[1]));
            }
        } finally {
            scanner.close();
        }
        return utenti;
    }

    public void salva(Vector<Utente> utenti) throws IOException {
        Vector<String> righe = new Vector<String>();
        for (int i = 0; i < utenti.size(); i++) {
            Utente utente = utenti.get(i);
            righe.add(utente.getUsername() + ";" + utente.getPassword());
        }
        FileTesto.scriviRighe(file, righe);
    }
}
