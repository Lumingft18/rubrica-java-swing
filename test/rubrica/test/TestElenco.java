package rubrica.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import rubrica.ElencoPersone;
import rubrica.Persona;
import rubrica.gui.ModelloTabellaPersone;

class TestElenco {

    @Test
    @DisplayName("tolgo quella in mezzo e modifico l'ultima")
    void eliminaInMezzoEModificaUltima() {
        ElencoPersone elenco = new ElencoPersone();
        elenco.aggiungi(persona("Arianna", "Dell’Arti", "111"));
        elenco.aggiungi(persona("Duccio", "Patane", "222"));
        elenco.aggiungi(persona("Corinna", "Negri", "333"));

        elenco.elimina(1);
        assertEquals(2, elenco.size());
        assertEquals("Arianna", elenco.get(0).getNome());
        assertEquals("Corinna", elenco.get(1).getNome());

        Persona modificata = new Persona("Corinna", "Ferretti", "via Nuova", "444", 40);
        elenco.sostituisci(1, modificata);

        assertEquals("Arianna", elenco.get(0).getNome());
        assertEquals("Dell’Arti", elenco.get(0).getCognome());
        assertEquals("111", elenco.get(0).getTelefono());
        assertEquals("Corinna", elenco.get(1).getNome());
        assertEquals("Ferretti", elenco.get(1).getCognome());
        assertEquals("via Nuova", elenco.get(1).getIndirizzo());
        assertEquals("444", elenco.get(1).getTelefono());
        assertEquals(40, elenco.get(1).getEta());
    }

    @Test
    @DisplayName("la tabella ha solo nome, cognome e telefono")
    void tabella() {
        ElencoPersone elenco = new ElencoPersone();
        elenco.aggiungi(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        ModelloTabellaPersone modello = new ModelloTabellaPersone(elenco);

        assertEquals(1, modello.getRowCount());
        assertEquals(3, modello.getColumnCount());
        assertEquals("Nome", modello.getColumnName(0));
        assertEquals("Cognome", modello.getColumnName(1));
        assertEquals("Telefono", modello.getColumnName(2));
        assertEquals("Steve", modello.getValueAt(0, 0));
        assertEquals("Jobs", modello.getValueAt(0, 1));
        assertEquals("0612344", modello.getValueAt(0, 2));
        assertFalse(modello.isCellEditable(0, 0));
    }

    private Persona persona(String nome, String cognome, String telefono) {
        return new Persona(nome, cognome, "via Roma", telefono, 30);
    }
}
