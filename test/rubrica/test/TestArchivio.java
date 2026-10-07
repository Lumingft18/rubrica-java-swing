package rubrica.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import rubrica.Persona;
import rubrica.archivio.ArchivioSuFile;
import rubrica.archivio.GestoreDati;

class TestArchivio {

    @TempDir
    Path temp;

    @Test
    @DisplayName("se il file non c'è non succede niente")
    void fileMancante() throws Exception {
        ArchivioSuFile archivio = archivio();
        Vector<Persona> persone = archivio.carica();
        assertEquals(0, persone.size());
        assertFalse(new File(temp.toFile(), "informazioni.txt").exists());
    }

    @Test
    @DisplayName("salvo, ricarico, il txt ha le righe di esempio")
    void salvaERicarica() throws Exception {
        ArchivioSuFile archivio = archivio();
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        persone.add(new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60));
        persone.add(new Persona("Babbo", "Natale", "via del Polo Nord", "00000111", 99));
        archivio.salva(persone);

        Vector<String> righe = leggiRighe(new File(temp.toFile(), "informazioni.txt"));
        assertEquals(3, righe.size());
        assertEquals("Steve;Jobs;via Cupertino 13;0612344;56", righe.get(0));
        assertEquals("Bill;Gates;via Redmond 10;06688989;60", righe.get(1));
        assertEquals("Babbo;Natale;via del Polo Nord;00000111;99", righe.get(2));

        Vector<Persona> ricaricate = archivio.carica();
        assertEquals(3, ricaricate.size());
        assertEquals("00000111", ricaricate.get(2).getTelefono());
        assertEquals(99, ricaricate.get(2).getEta());
        assertEquals("via Redmond 10", ricaricate.get(1).getIndirizzo());
    }

    @Test
    @DisplayName("righe vuote e rotte si saltano, il resto entra")
    void righeIgnorate() throws Exception {
        File file = new File(temp.toFile(), "informazioni.txt");
        PrintStream out = new PrintStream(new FileOutputStream(file), true, "UTF-8");
        try {
            out.println("");
            out.println("questa;riga;e;rotta");
            out.println("Alessandro;Santoro;via Roma 1;333123;30");
            out.println("   ");
        } finally {
            out.close();
        }

        Vector<Persona> persone = archivio().carica();
        assertEquals(1, persone.size());
        assertEquals("Alessandro", persone.get(0).getNome());
        assertEquals("Santoro", persone.get(0).getCognome());
    }

    @Test
    @DisplayName("due persone con lo stesso nome: due file, nessuno si perde")
    void cartellaConDoppione() throws Exception {
        ArchivioSuFile archivio = archivio();
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("René", "Ferretti", "via A", "111", 20));
        persone.add(new Persona("René", "Ferretti", "via B", "222", 21));
        archivio.salva(persone);

        File cartella = new File(temp.toFile(), "informazioni");
        String[] nomi = cartella.list();
        Set<String> trovati = new HashSet<String>();
        for (int i = 0; i < nomi.length; i++) {
            trovati.add(nomi[i]);
        }
        assertTrue(trovati.contains("Ferretti-René.txt"));
        assertTrue(trovati.contains("Ferretti-René-2.txt"));

        assertTrue(new File(temp.toFile(), "informazioni.txt").delete());
        Vector<Persona> dallaCartella = archivio.carica();
        assertEquals(2, dallaCartella.size());
        Set<String> telefoni = new HashSet<String>();
        telefoni.add(dallaCartella.get(0).getTelefono());
        telefoni.add(dallaCartella.get(1).getTelefono());
        assertTrue(telefoni.contains("111"));
        assertTrue(telefoni.contains("222"));
    }

    @Test
    @DisplayName("il gestore scrive txt e cartella, e al riavvio l'ordine resta")
    void gestoreTieneOrdine() throws Exception {
        GestoreDati dati = new GestoreDati(temp.toFile());
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("Arianna", "Dell’Arti", "via 1", "111", 20));
        persone.add(new Persona("Duccio", "Patane", "via 2", "222", 30));
        persone.add(new Persona("Corinna", "Negri", "via 3", "333", 40));
        dati.salvaPersone(persone);

        assertTrue(new File(temp.toFile(), "informazioni.txt").exists());
        assertTrue(new File(temp.toFile(), "informazioni").isDirectory());
        assertEquals(3, new File(temp.toFile(), "informazioni").list().length);

        GestoreDati riaperto = new GestoreDati(temp.toFile());
        Vector<Persona> ricaricate = riaperto.caricaPersone();
        assertEquals("Arianna", ricaricate.get(0).getNome());
        assertEquals("Duccio", ricaricate.get(1).getNome());
        assertEquals("Corinna", ricaricate.get(2).getNome());
        assertEquals("333", ricaricate.get(2).getTelefono());
    }

    private ArchivioSuFile archivio() {
        return new ArchivioSuFile(
                new File(temp.toFile(), "informazioni.txt"),
                new File(temp.toFile(), "informazioni"));
    }

    private Vector<String> leggiRighe(File file) throws Exception {
        Vector<String> righe = new Vector<String>();
        Scanner scanner = new Scanner(new java.io.FileInputStream(file), "UTF-8");
        try {
            while (scanner.hasNextLine()) {
                righe.add(scanner.nextLine());
            }
        } finally {
            scanner.close();
        }
        return righe;
    }
}
