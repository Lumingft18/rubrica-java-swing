package rubrica;

import java.util.Vector;

/**
 * La lista di contatti. Vector, non un array fisso: cresce e si accorcia.
 */
public class ElencoPersone {

    private final Vector<Persona> persone = new Vector<Persona>();

    public void aggiungi(Persona persona) {
        persone.add(persona);
    }

    public void inserisci(int indice, Persona persona) {
        persone.add(indice, persona);
    }

    public void sostituisci(int indice, Persona persona) {
        persone.set(indice, persona);
    }

    public void elimina(int indice) {
        persone.remove(indice);
    }

    public Persona get(int indice) {
        return persone.get(indice);
    }

    public int size() {
        return persone.size();
    }

    public void carica(Vector<Persona> nuove) {
        persone.clear();
        persone.addAll(nuove);
    }

    public Vector<Persona> getPersone() {
        return persone;
    }
}
