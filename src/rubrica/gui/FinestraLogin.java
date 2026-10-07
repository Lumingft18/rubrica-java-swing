package rubrica.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import rubrica.ElencoPersone;
import rubrica.archivio.GestoreDati;

/**
 * All'avvio si vede solo questa. La rubrica si apre dopo un login giusto.
 */
public class FinestraLogin extends JFrame {

    private static final long serialVersionUID = 1L;

    private final GestoreDati dati;
    private final JTextField campoUtente = new JTextField(18);
    private final JPasswordField campoPassword = new JPasswordField(18);
    private final JLabel modo = Stile.tenue("Preparo i dati...");
    private final JButton bottoneLogin = new JButton("Login");
    private JButton bottoneBarra;
    private boolean accessoEseguito;
    private boolean occupata = true;

    /**
     * La finestra si apre subito. prepara() (che col database può aspettare qualche secondo)
     * gira in un SwingWorker: intanto Login è spento.
     */
    public FinestraLogin(GestoreDati dati) {
        super("Accesso");
        this.dati = dati;

        JToolBar barra = Barre.nuova();
        bottoneBarra = Barre.aggiungi(barra, "Login", Icone.login(), e -> entra());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 8, 20));
        aggiungiRiga(form, 0, "Utente", campoUtente);
        aggiungiRiga(form, 1, "Password", campoPassword);

        JLabel hint = Stile.tenue("Utente di prova: admin / admin");
        GridBagConstraints hintGbc = new GridBagConstraints();
        hintGbc.gridx = 1;
        hintGbc.gridy = 2;
        hintGbc.insets = new Insets(10, 8, 0, 8);
        hintGbc.anchor = GridBagConstraints.WEST;
        form.add(hint, hintGbc);

        GridBagConstraints modoGbc = new GridBagConstraints();
        modoGbc.gridx = 1;
        modoGbc.gridy = 3;
        modoGbc.insets = new Insets(2, 8, 4, 8);
        modoGbc.anchor = GridBagConstraints.WEST;
        form.add(modo, modoGbc);

        bottoneLogin.addActionListener(e -> entra());
        campoPassword.addActionListener(e -> entra());

        JPanel sud = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        sud.setBorder(Stile.bordoSopra());
        sud.add(bottoneLogin);

        JPanel nord = new JPanel(new BorderLayout());
        nord.add(Stile.intestazione("Rubrica", "Accedi per vedere i contatti"), BorderLayout.NORTH);
        nord.add(barra, BorderLayout.SOUTH);

        getContentPane().add(nord, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(sud, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(bottoneLogin);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                // dispose() dopo il login non deve chiudere tutto
                if (!accessoEseguito) {
                    System.exit(0);
                }
            }
        });

        setIconImages(Stile.iconeFinestra());
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                campoUtente.requestFocusInWindow();
            }
        });
        impostaOccupata(true);
        new Preparazione().execute();
    }

    private void impostaOccupata(boolean si) {
        occupata = si;
        bottoneLogin.setEnabled(!si);
        bottoneBarra.setEnabled(!si);
        setCursor(si ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void avvisa(String testo) {
        JOptionPane.showMessageDialog(this, testo, "Attenzione", JOptionPane.WARNING_MESSAGE);
    }

    private void aggiungiRiga(JPanel form, int riga, String etichetta, JTextField campo) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
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

    private void entra() {
        if (occupata) {
            return;
        }
        String username = campoUtente.getText().trim();
        char[] caratteri = campoPassword.getPassword();
        String password = new String(caratteri);
        Arrays.fill(caratteri, '\0');

        if (username.length() == 0 || password.length() == 0) {
            avvisa("Inserisci utente e password.");
            return;
        }

        impostaOccupata(true);
        new Accesso(username, password).execute();
    }

    /** prepara() lontano dalla grafica. */
    private final class Preparazione extends SwingWorker<String, Void> {

        protected String doInBackground() {
            return dati.prepara();
        }

        protected void done() {
            String avviso;
            try {
                avviso = get();
            } catch (Exception e) {
                avviso = "Errore all'avvio.\n" + e.getMessage();
            }
            modo.setText(dati.usaDatabase() ? "Salvataggio: database" : "Salvataggio: file");
            impostaOccupata(false);
            if (avviso != null && avviso.length() > 0) {
                avvisa(avviso);
            }
        }
    }

    /** Login e caricamento dei contatti lontano dalla grafica: col database possono aspettare la rete. */
    private final class Accesso extends SwingWorker<ElencoPersone, Void> {

        private final String username;
        private final String password;

        Accesso(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected ElencoPersone doInBackground() throws IOException {
            if (!dati.login(username, password)) {
                return null;
            }
            ElencoPersone elenco = new ElencoPersone();
            elenco.carica(dati.caricaPersone());
            return elenco;
        }

        protected void done() {
            impostaOccupata(false);
            ElencoPersone elenco;
            try {
                elenco = get();
            } catch (ExecutionException e) {
                JOptionPane.showMessageDialog(
                        FinestraLogin.this,
                        "Errore durante l'accesso.\n" + e.getCause().getMessage(),
                        "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (elenco == null) {
                JOptionPane.showMessageDialog(FinestraLogin.this, "Login errato", "Accesso", JOptionPane.ERROR_MESSAGE);
                campoPassword.setText("");
                campoPassword.requestFocusInWindow();
                return;
            }
            accessoEseguito = true;
            dispose();
            FinestraPrincipale finestra = new FinestraPrincipale(dati, elenco, username);
            finestra.setVisible(true);
            String avviso = dati.avvisoCaricamento();
            if (avviso != null) {
                JOptionPane.showMessageDialog(finestra, avviso, "Attenzione", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
