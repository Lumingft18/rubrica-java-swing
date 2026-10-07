package rubrica.archivio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import rubrica.Persona;

/**
 * Persistenza su disco.
 * informazioni.txt, una riga per persona:
 * Steve;Jobs;via Cupertino 13;0612344;56
 *
 * In più, cartella informazioni con un file per persona.
 * Il txt è la copia principale e conserva l'ordine.
 */
public class ArchivioSuFile {

    private final File file;
    private final File cartella;
    private final Vector<String> righeIgnorate = new Vector<String>();

    public ArchivioSuFile(File file, File cartella) {
        this.file = file;
        this.cartella = cartella;
    }

    public Vector<Persona> carica() throws IOException {
        righeIgnorate.clear();
        if (file.exists()) {
            Vector<Persona> persone = leggiFileUnico();
            if (!righeIgnorate.isEmpty()) {
                // al prossimo salvataggio queste righe sparirebbero: tengo una copia del file com'era
                Files.copy(file.toPath(), copiaDiSicurezza().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return persone;
        }
        if (cartella.exists()) {
            return leggiCartella();
        }
        return new Vector<Persona>();
    }

    /**
     * Righe di informazioni.txt (o file della cartella) che non sono diventate persone
     * nell'ultimo carica().
     */
    public Vector<String> getRigheIgnorate() {
        return righeIgnorate;
    }

    public File copiaDiSicurezza() {
        return new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".bak");
    }

    /**
     * Il txt nuovo è pronto in un .tmp, poi scrivo la cartella, poi il .tmp va al posto del txt.
     * Se la cartella fallisce, informazioni.txt resta quello di prima,
     * così il file e la tabella (che torna indietro) dicono la stessa cosa.
     */
    public void salva(Vector<Persona> persone) throws IOException {
        Vector<String> righe = new Vector<String>();
        for (int i = 0; i < persone.size(); i++) {
            righe.add(persone.get(i).aRiga());
        }
        File tmp = FileTesto.preparaTemporaneo(file, righe);
        try {
            scriviCartella(persone);
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
        FileTesto.sostituisci(tmp, file);
    }

    private Vector<Persona> leggiFileUnico() throws IOException {
        Vector<Persona> persone = new Vector<Persona>();
        Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8");
        try {
            boolean prima = true;
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine();
                if (prima) {
                    riga = FileTesto.senzaBom(riga);
                    prima = false;
                }
                riga = riga.trim();
                if (riga.length() == 0) {
                    continue;
                }
                Persona persona = Persona.daRiga(riga);
                if (persona == null) {
                    righeIgnorate.add(riga);
                    continue;
                }
                persone.add(persona);
            }
        } finally {
            scanner.close();
        }
        return persone;
    }

    private Vector<Persona> leggiCartella() throws IOException {
        Vector<Persona> persone = new Vector<Persona>();
        File[] files = cartella.listFiles();
        if (files == null) {
            throw new IOException("Non leggo la cartella " + cartella.getAbsolutePath());
        }
        // ordine alfabetico, così due file con lo stesso cognome restano stabili
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            File corrente = files[i];
            if (!corrente.isFile() || !corrente.getName().endsWith(".txt")) {
                continue;
            }
            Scanner scanner = new Scanner(new FileInputStream(corrente), "UTF-8");
            try {
                if (!scanner.hasNextLine()) {
                    continue;
                }
                String riga = FileTesto.senzaBom(scanner.nextLine()).trim();
                Persona persona = Persona.daRiga(riga);
                if (persona == null) {
                    // il salvataggio cancella i .txt della cartella: questo lo tengo come .bak
                    righeIgnorate.add(corrente.getName() + ": " + riga);
                    Files.copy(corrente.toPath(), new File(cartella, corrente.getName() + ".bak").toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                persone.add(persona);
            } finally {
                scanner.close();
            }
        }
        return persone;
    }

    private void scriviCartella(Vector<Persona> persone) throws IOException {
        if (!cartella.exists() && !cartella.mkdirs()) {
            throw new IOException("Non creo la cartella " + cartella.getAbsolutePath());
        }
        File[] vecchi = cartella.listFiles();
        if (vecchi != null) {
            for (int i = 0; i < vecchi.length; i++) {
                if (vecchi[i].isFile() && vecchi[i].getName().endsWith(".txt")) {
                    if (!vecchi[i].delete()) {
                        throw new IOException("Non cancello " + vecchi[i].getName());
                    }
                }
            }
        }
        Set<String> usati = new HashSet<String>();
        for (int i = 0; i < persone.size(); i++) {
            Persona persona = persone.get(i);
            File destinazione = new File(cartella, nomeFile(persona, usati));
            PrintStream out = new PrintStream(new FileOutputStream(destinazione), true, "UTF-8");
            try {
                out.println(persona.aRiga());
                if (out.checkError()) {
                    throw new IOException("Scrittura fallita: " + destinazione.getAbsolutePath());
                }
            } finally {
                out.close();
            }
        }
    }

    /**
     * Cognome-Nome.txt. Se il nome c'è già, Cognome-Nome-2.txt e così via.
     * Su Mac il disco non distingue maiuscole e minuscole, quindi confronto in minuscolo.
     */
    private String nomeFile(Persona persona, Set<String> usati) {
        String base = pulisci(persona.getCognome()) + "-" + pulisci(persona.getNome());
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        String candidato = base + ".txt";
        int n = 2;
        while (usati.contains(candidato.toLowerCase(Locale.ITALIAN))) {
            candidato = base + "-" + n + ".txt";
            n++;
        }
        usati.add(candidato.toLowerCase(Locale.ITALIAN));
        return candidato;
    }

    private String pulisci(String valore) {
        String pulito = valore.trim().replace(' ', '_');
        pulito = pulito.replaceAll("[\\\\/:*?\"<>|]", "");
        if (pulito.length() == 0) {
            return "sconosciuto";
        }
        return pulito;
    }
}
