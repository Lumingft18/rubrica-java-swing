package rubrica.gui;

import javax.swing.table.AbstractTableModel;

import rubrica.ElencoPersone;
import rubrica.Persona;

/**
 * La tabella mostra solo nome, cognome e telefono.
 * Indirizzo ed età si vedono nell'editor.
 */
public class ModelloTabellaPersone extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final ElencoPersone elenco;
    private final String[] colonne = { "Nome", "Cognome", "Telefono" };

    public ModelloTabellaPersone(ElencoPersone elenco) {
        this.elenco = elenco;
    }

    public int getRowCount() {
        return elenco.size();
    }

    public int getColumnCount() {
        return colonne.length;
    }

    public String getColumnName(int column) {
        return colonne[column];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Persona persona = elenco.get(rowIndex);
        if (columnIndex == 0) {
            return persona.getNome();
        }
        if (columnIndex == 1) {
            return persona.getCognome();
        }
        return persona.getTelefono();
    }

    public void rigaAggiunta(int indice) {
        fireTableRowsInserted(indice, indice);
    }

    public void rigaCambiata(int indice) {
        fireTableRowsUpdated(indice, indice);
    }

    public void rigaTolta(int indice) {
        fireTableRowsDeleted(indice, indice);
    }
}
