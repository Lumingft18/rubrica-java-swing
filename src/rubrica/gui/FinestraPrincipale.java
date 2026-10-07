package rubrica.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import rubrica.ElencoPersone;
import rubrica.Persona;
import rubrica.archivio.GestoreDati;

/**
 * Finestra principale. Tabella con nome, cognome, telefono.
 * Nuovo, Modifica ed Elimina stanno in basso e anche nella toolbar.
 * Stesse azioni, le icone sono solo sulla toolbar.
 */
public class FinestraPrincipale extends JFrame {

    private static final long serialVersionUID = 1L;

    private final GestoreDati dati;
    private final ElencoPersone elenco;
    private final ModelloTabellaPersone modello;
    private final JTable tabella;
    private final JLabel stato = Stile.tenue("");

    public FinestraPrincipale(GestoreDati dati, ElencoPersone elenco, String username) {
        super("Rubrica - " + username);
        this.dati = dati;
        this.elenco = elenco;
        this.modello = new ModelloTabellaPersone(elenco);
        this.tabella = new TabellaConVuoto(modello);

        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.setFillsViewportHeight(true);
        Stile.tabella(tabella);
        tabella.setRowSorter(new TableRowSorter<ModelloTabellaPersone>(modello));
        tabella.getColumnModel().getColumn(0).setPreferredWidth(180);
        tabella.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabella.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabella.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && rigaSelezionata() >= 0) {
                    modificaPersona();
                }
            }
        });

        // Invio modifica, Canc elimina: stesse azioni dei bottoni
        tabella.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "modifica");
        tabella.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "elimina");
        tabella.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "elimina");
        tabella.getActionMap().put("modifica", azione(this::modificaPersona));
        tabella.getActionMap().put("elimina", azione(this::eliminaPersona));

        JToolBar barra = Barre.nuova();
        Barre.aggiungi(barra, "Nuovo", Icone.nuovo(), e -> creaPersona());
        Barre.aggiungi(barra, "Modifica", Icone.modifica(), e -> modificaPersona());
        barra.addSeparator();
        Barre.aggiungi(barra, "Elimina", Icone.elimina(), e -> eliminaPersona());

        JButton bottoneNuovo = new JButton("Nuovo", Icone.nuovo());
        bottoneNuovo.addActionListener(e -> creaPersona());
        JButton bottoneModifica = new JButton("Modifica", Icone.modifica());
        bottoneModifica.addActionListener(e -> modificaPersona());
        JButton bottoneElimina = new JButton("Elimina", Icone.elimina());
        bottoneElimina.addActionListener(e -> eliminaPersona());

        JPanel bottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottoni.add(bottoneNuovo);
        bottoni.add(bottoneModifica);
        bottoni.add(bottoneElimina);

        // il percorso intero sta nel tooltip, nella barra solo la parte utile
        stato.setToolTipText(dati.descrizioneSalvataggio());
        stato.setBorder(new EmptyBorder(5, 12, 5, 12));
        aggiornaStato();

        JPanel sud = new JPanel(new BorderLayout());
        sud.setBorder(Stile.bordoSopra());
        sud.add(bottoni, BorderLayout.CENTER);
        sud.add(stato, BorderLayout.SOUTH);

        JScrollPane scorrimento = new JScrollPane(tabella);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());

        getContentPane().add(barra, BorderLayout.NORTH);
        getContentPane().add(scorrimento, BorderLayout.CENTER);
        getContentPane().add(sud, BorderLayout.SOUTH);

        modello.addTableModelListener(e -> aggiornaStato());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(Stile.iconeFinestra());
        setSize(720, 480);
        setMinimumSize(new Dimension(480, 320));
        setLocationRelativeTo(null);
    }

    private void creaPersona() {
        EditorPersona editor = new EditorPersona(this, null);
        editor.setVisible(true);
        Persona persona = editor.getPersonaSalvata();
        if (persona == null) {
            return;
        }
        elenco.aggiungi(persona);
        int indice = elenco.size() - 1;
        modello.rigaAggiunta(indice);
        if (!salvaSuDisco()) {
            elenco.elimina(indice);
            modello.rigaTolta(indice);
            return;
        }
        seleziona(indice);
    }

    private void modificaPersona() {
        int riga = rigaSelezionata();
        if (riga < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Per modificare è necessario prima selezionare una persona.",
                    "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Persona prima = elenco.get(riga);
        EditorPersona editor = new EditorPersona(this, prima);
        editor.setVisible(true);
        Persona aggiornata = editor.getPersonaSalvata();
        if (aggiornata == null) {
            return;
        }
        elenco.sostituisci(riga, aggiornata);
        modello.rigaCambiata(riga);
        if (!salvaSuDisco()) {
            elenco.sostituisci(riga, prima);
            modello.rigaCambiata(riga);
            return;
        }
        seleziona(riga);
    }

    private void eliminaPersona() {
        int riga = rigaSelezionata();
        if (riga < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Per eliminare è necessario prima selezionare una persona.",
                    "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Persona persona = elenco.get(riga);
        int scelta = JOptionPane.showConfirmDialog(
                this,
                "Eliminare la persona " + persona.nomeCompleto() + "?",
                "Conferma",
                JOptionPane.YES_NO_OPTION);
        if (scelta != JOptionPane.YES_OPTION) {
            return;
        }
        // Salvo anche in cancellazione, altrimenti al riavvio la persona torna.
        elenco.elimina(riga);
        modello.rigaTolta(riga);
        if (!salvaSuDisco()) {
            elenco.inserisci(riga, persona);
            modello.rigaAggiunta(riga);
            return;
        }
    }

    /**
     * Resta sul thread grafico: dopo il salvataggio decido se tenere o annullare la modifica.
     * Col database è al massimo qualche secondo (socketTimeout), intanto cursore di attesa.
     */
    private boolean salvaSuDisco() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            dati.salvaPersone(elenco.getPersone());
            return true;
        } catch (IOException e) {
            setCursor(Cursor.getDefaultCursor());
            JOptionPane.showMessageDialog(
                    this,
                    "Non sono riuscito a salvare.\n" + e.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private int rigaSelezionata() {
        int vista = tabella.getSelectedRow();
        if (vista < 0) {
            return -1;
        }
        return tabella.convertRowIndexToModel(vista);
    }

    private void seleziona(int indiceModello) {
        int vista = tabella.convertRowIndexToView(indiceModello);
        if (vista >= 0) {
            tabella.setRowSelectionInterval(vista, vista);
        }
    }

    private static AbstractAction azione(Runnable cosa) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                cosa.run();
            }
        };
    }

    private void aggiornaStato() {
        int quanti = elenco.size();
        String conteggio = quanti == 1 ? "1 contatto" : quanti + " contatti";
        stato.setText(conteggio + "  \u00b7  " + dati.descrizioneBreve());
    }

    /**
     * Tabella che, se vuota, dice cosa fare invece di restare bianca.
     */
    private static final class TabellaConVuoto extends JTable {

        private static final long serialVersionUID = 1L;

        TabellaConVuoto(ModelloTabellaPersone modello) {
            super(modello);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() > 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Stile.TESTO_TENUE);
            g2.setFont(getFont());
            String testo = "Nessun contatto. Premi Nuovo per aggiungerne uno.";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(testo)) / 2;
            int y = getHeight() / 3;
            g2.drawString(testo, Math.max(x, 8), y);
            g2.dispose();
        }
    }
}
