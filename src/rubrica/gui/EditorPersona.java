package rubrica.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import rubrica.Persona;
import rubrica.Validatore;

/**
 * Finestra editor-persona. Campi in riga: etichetta e valore.
 * Salva chiude e restituisce la persona. Annulla chiude e non restituisce nulla.
 */
public class EditorPersona extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField campoNome = new JTextField(24);
    private final JTextField campoCognome = new JTextField(24);
    private final JTextField campoIndirizzo = new JTextField(24);
    private final JTextField campoTelefono = new JTextField(24);
    private final JTextField campoEta = new JTextField(24);

    private final int idOriginale;
    private Persona personaSalvata;

    public EditorPersona(Frame padre, Persona persona) {
        super(padre, "Editor persona", true);
        this.idOriginale = persona == null ? 0 : persona.getId();

        JToolBar barra = Barre.nuova();
        Barre.aggiungi(barra, "Salva", Icone.salva(), e -> eseguiSalva());
        Barre.aggiungi(barra, "Annulla", Icone.annulla(), e -> eseguiAnnulla());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 16, 6, 20));
        aggiungiRiga(form, 0, "Nome *", campoNome);
        aggiungiRiga(form, 1, "Cognome *", campoCognome);
        aggiungiRiga(form, 2, "Indirizzo", campoIndirizzo);
        aggiungiRiga(form, 3, "Telefono", campoTelefono);
        aggiungiRiga(form, 4, "Età", campoEta);

        GridBagConstraints notaGbc = new GridBagConstraints();
        notaGbc.gridx = 1;
        notaGbc.gridy = 5;
        notaGbc.insets = new Insets(6, 8, 0, 8);
        notaGbc.anchor = GridBagConstraints.WEST;
        form.add(Stile.tenue("* obbligatori"), notaGbc);

        if (persona != null) {
            campoNome.setText(persona.getNome());
            campoCognome.setText(persona.getCognome());
            campoIndirizzo.setText(persona.getIndirizzo());
            campoTelefono.setText(persona.getTelefono());
            campoEta.setText(Integer.toString(persona.getEta()));
        }

        JButton bottoneSalva = new JButton("Salva");
        bottoneSalva.addActionListener(e -> eseguiSalva());
        JButton bottoneAnnulla = new JButton("Annulla");
        bottoneAnnulla.addActionListener(e -> eseguiAnnulla());

        JPanel sud = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        sud.setBorder(Stile.bordoSopra());
        sud.add(bottoneSalva);
        sud.add(bottoneAnnulla);

        JPanel nord = new JPanel(new BorderLayout());
        nord.add(Stile.intestazione(
                persona == null ? "Nuovo contatto" : "Modifica contatto",
                persona == null ? "Compila i dati e premi Salva" : persona.nomeCompleto()),
                BorderLayout.NORTH);
        nord.add(barra, BorderLayout.SOUTH);

        getContentPane().add(nord, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(sud, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(bottoneSalva);
        // Esc come Annulla
        getRootPane().registerKeyboardAction(e -> eseguiAnnulla(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(padre);

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                campoNome.requestFocusInWindow();
            }
        });
    }

    public Persona getPersonaSalvata() {
        return personaSalvata;
    }

    private void aggiungiRiga(JPanel form, int riga, String etichetta, JTextField campo) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = riga;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(etichetta, SwingConstants.RIGHT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(campo, gbc);
    }

    private void eseguiSalva() {
        String errore = Validatore.errore(
                campoNome.getText(),
                campoCognome.getText(),
                campoIndirizzo.getText(),
                campoTelefono.getText(),
                campoEta.getText());
        if (errore != null) {
            JOptionPane.showMessageDialog(this, errore, "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }
        personaSalvata = Validatore.crea(
                campoNome.getText(),
                campoCognome.getText(),
                campoIndirizzo.getText(),
                campoTelefono.getText(),
                campoEta.getText());
        personaSalvata.setId(idOriginale);
        dispose();
    }

    private void eseguiAnnulla() {
        personaSalvata = null;
        dispose();
    }
}
