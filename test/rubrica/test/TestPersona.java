package rubrica.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import rubrica.Persona;
import rubrica.Validatore;

class TestPersona {

    @Test
    @DisplayName("la riga di esempio torna una persona, telefono con gli zeri")
    void rigaDiEsempio() {
        Persona persona = Persona.daRiga("Steve;Jobs;via Cupertino 13;0612344;56");
        assertEquals("Steve", persona.getNome());
        assertEquals("Jobs", persona.getCognome());
        assertEquals("via Cupertino 13", persona.getIndirizzo());
        assertEquals("0612344", persona.getTelefono());
        assertEquals(56, persona.getEta());
        assertEquals("Steve;Jobs;via Cupertino 13;0612344;56", persona.aRiga());
    }

    @Test
    @DisplayName("Babbo Natale: il telefono non diventa un numero")
    void telefonoConZeri() {
        Persona persona = Persona.daRiga("Babbo;Natale;via del Polo Nord;00000111;99");
        assertEquals("00000111", persona.getTelefono());
        assertEquals(99, persona.getEta());
        assertEquals("Babbo Natale", persona.nomeCompleto());
    }

    @Test
    @DisplayName("una riga rotta non diventa una persona")
    void rigaRotta() {
        assertNull(Persona.daRiga("solo;tre;campi"));
        assertNull(Persona.daRiga("a;b;c;d;non-numero"));
        assertNull(Persona.daRiga(null));
    }

    @Test
    @DisplayName("nome vuoto, eta storta, punto e virgola")
    void validatore() {
        assertEquals("Nome e cognome sono obbligatori.",
                Validatore.errore("  ", "Ferretti", "", "", "10"));
        assertEquals("L'età deve essere un numero intero.",
                Validatore.errore("Arianna", "Ferretti", "", "", "dieci"));
        assertEquals("L'età non può essere negativa.",
                Validatore.errore("Arianna", "Ferretti", "", "", "-1"));
        assertTrue(Validatore.errore("An;na", "Ferretti", "via Roma", "333", "20").contains(";"));

        Persona persona = Validatore.crea("  Alessandro ", "Santoro", "via Roma 1", " 333 ", "30");
        assertEquals("Alessandro", persona.getNome());
        assertEquals("333", persona.getTelefono());
        assertEquals(30, persona.getEta());
    }

    @Test
    void creaConDatiStortiLancia() {
        assertThrows(IllegalArgumentException.class, () -> Validatore.crea("A", "B", "", "", "x"));
    }
}
