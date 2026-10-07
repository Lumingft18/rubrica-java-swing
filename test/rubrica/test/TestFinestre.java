package rubrica.test;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import rubrica.*;
import rubrica.archivio.GestoreDati;
import rubrica.gui.*;

@EnabledIfEnvironmentVariable(named = "RUBRICA_TEST_GUI", matches = "true")
@Timeout(20)
class TestFinestre {
    @TempDir Path temp;
    private ElencoPersone elenco;
    private FinestraPrincipale finestra;

    @AfterEach void chiudiFinestre() throws Exception {
        edt(() -> {
            for (Window w : Window.getWindows()) {
                for (WindowListener listener : w.getWindowListeners()) w.removeWindowListener(listener);
                w.dispose();
            }
            return null;
        });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUBRICA_TEST_AVVIO", matches = "true")
    void mainApreSoloLoginEUsaCartellaJar() throws Exception {
        RubricaMain.main(new String[0]);
        attendi(() -> finestraAperta(FinestraLogin.class) != null);
        FinestraLogin login = edt(() -> finestraAperta(FinestraLogin.class));
        attendi(() -> bottone(login, "Login", false).isEnabled());
        assertNull(edt(() -> finestraAperta(FinestraPrincipale.class)));
        Path attesa = java.nio.file.Paths.get(System.getenv("RUBRICA_TEST_CARTELLA"));
        assertTrue(java.nio.file.Files.exists(attesa.resolve("utenti.txt")));
        edt(() -> {
            List<JTextField> campi = componenti(login, JTextField.class);
            campi.get(0).setText("admin");
            campi.get(1).setText("admin");
            return null;
        });
        click(login, "Login", false);
        attendi(() -> finestraAperta(FinestraPrincipale.class) != null && !login.isShowing());
    }

    @Test void editorSalvaMantieneIdEZeri() throws Exception {
        edt(() -> {
            Persona prima = persona("René", "Ferretti", "001");
            prima.setId(42);
            EditorPersona editor = new EditorPersona(null, prima);
            List<JTextField> campi = componenti(editor, JTextField.class);
            assertEquals(5, campi.size());
            assertEquals("René", campi.get(0).getText());
            assertEquals("50", campi.get(4).getText());
            campi.get(3).setText("0000123");
            bottone(editor, "Salva", false).doClick();
            assertEquals(42, editor.getPersonaSalvata().getId());
            assertEquals("0000123", editor.getPersonaSalvata().getTelefono());
            assertEquals("001", prima.getTelefono());
            assertFalse(editor.isDisplayable());
            return null;
        });
    }

    @Test void editorErrorePoiAnnullaConEsc() throws Exception {
        EditorPersona editor = edt(() -> new EditorPersona(null, null));
        edt(() -> {
            for (JTextField campo : componenti(editor, JTextField.class)) assertEquals("", campo.getText());
            return null;
        });
        click(editor, "Salva", false);
        JOptionPane errore = attendiMessaggio();
        assertEquals("Nome e cognome sono obbligatori.", edt(() -> errore.getMessage()));
        chiudiMessaggio(errore, JOptionPane.OK_OPTION);
        edt(() -> {
            KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
            Object key = editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(esc);
            editor.getRootPane().getActionMap().get(key).actionPerformed(new ActionEvent(editor, 0, ""));
            assertNull(editor.getPersonaSalvata());
            return null;
        });
    }

    @Test void nuovoModificaAnnullaERiapre() throws Exception {
        prepara(false);
        aggiungi("René", "Ferretti", "001", false);
        edt(() -> { assertEquals("001", tabella().getValueAt(0, 2)); return null; });
        click(finestra, "Modifica", false);
        EditorPersona editor = attendiEditor();
        edt(() -> {
            List<JTextField> campi = componenti(editor, JTextField.class);
            assertEquals("René", campi.get(0).getText());
            assertEquals("via Roma", campi.get(2).getText());
            campi.get(3).setText("999");
            bottone(editor, "Annulla", false).doClick();
            return null;
        });
        attendi(() -> !editor.isShowing());
        assertEquals("001", elenco.get(0).getTelefono());
        GestoreDati riaperto = new GestoreDati(temp.toFile());
        assertEquals(elenco.get(0).aRiga(), riaperto.caricaPersone().get(0).aRiga());
    }

    @Test void eliminaInMezzoModificaUltimaConTabellaOrdinata() throws Exception {
        prepara(false);
        aggiungi("René", "Ferretti", "001", false);
        aggiungi("Duccio", "Patane", "002", false);
        aggiungi("Arianna", "Dell’Arti", "003", false);
        edt(() -> { tabella().getRowSorter().toggleSortOrder(0); seleziona(1); return null; });
        click(finestra, "Elimina", false);
        JOptionPane conferma = attendiMessaggio();
        assertEquals("Eliminare la persona Duccio Patane?", edt(() -> conferma.getMessage()));
        chiudiMessaggio(conferma, JOptionPane.NO_OPTION);
        assertEquals(3, elenco.size());
        click(finestra, "Elimina", false);
        chiudiMessaggio(attendiMessaggio(), JOptionPane.YES_OPTION);
        attendi(() -> elenco.size() == 2);
        edt(() -> { seleziona(1); return null; });
        SwingUtilities.invokeLater(() -> tabella().getActionMap().get("modifica")
                .actionPerformed(new ActionEvent(tabella(), 0, "")));
        EditorPersona editor = attendiEditor();
        edt(() -> {
            List<JTextField> campi = componenti(editor, JTextField.class);
            assertEquals("Arianna", campi.get(0).getText());
            campi.get(3).setText("004");
            bottone(editor, "Salva", false).doClick();
            return null;
        });
        attendi(() -> "004".equals(elenco.get(1).getTelefono()));
        assertEquals("001", elenco.get(0).getTelefono());
        assertEquals("004", new GestoreDati(temp.toFile()).caricaPersone().get(1).getTelefono());
    }

    @Test void senzaSelezioneModificaEdEliminaAvvisano() throws Exception {
        prepara(false);
        for (String azione : new String[] {"Modifica", "Elimina"}) {
            click(finestra, azione, false);
            JOptionPane messaggio = attendiMessaggio();
            assertTrue(edt(() -> messaggio.getMessage().toString()).contains("selezionare una persona"));
            chiudiMessaggio(messaggio, JOptionPane.OK_OPTION);
        }
        assertEquals(0, elenco.size());
    }

    @Test void toolbarDoppioClickETastoElimina() throws Exception {
        prepara(false);
        aggiungi("René", "Ferretti", "001", true);
        SwingUtilities.invokeLater(() -> tabella().dispatchEvent(new MouseEvent(tabella(),
                MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 2, false)));
        EditorPersona editor = attendiEditor();
        edt(() -> { bottone(editor, "Annulla", true).doClick(); return null; });
        attendi(() -> !editor.isShowing());
        SwingUtilities.invokeLater(() -> tabella().getActionMap().get("elimina")
                .actionPerformed(new ActionEvent(tabella(), 0, "")));
        chiudiMessaggio(attendiMessaggio(), JOptionPane.YES_OPTION);
        attendi(() -> elenco.size() == 0);
        assertTrue(new GestoreDati(temp.toFile()).caricaPersone().isEmpty());
    }

    @Test void erroreSalvataggioRipristinaNuovoModificaElimina() throws Exception {
        prepara(true);
        aggiungi("Duccio", "Patane", "002", false);
        chiudiMessaggio(attendiMessaggio(), JOptionPane.OK_OPTION);
        attendi(() -> elenco.size() == 1);
        edt(() -> { seleziona(0); return null; });
        click(finestra, "Modifica", false);
        EditorPersona editor = attendiEditor();
        edt(() -> {
            componenti(editor, JTextField.class).get(3).setText("999");
            bottone(editor, "Salva", false).doClick();
            return null;
        });
        chiudiMessaggio(attendiMessaggio(), JOptionPane.OK_OPTION);
        attendi(() -> "001".equals(elenco.get(0).getTelefono()));
        edt(() -> { seleziona(0); return null; });
        click(finestra, "Elimina", false);
        chiudiMessaggio(attendiMessaggio(), JOptionPane.YES_OPTION);
        chiudiMessaggio(attendiMessaggio(), JOptionPane.OK_OPTION);
        attendi(() -> elenco.size() == 1);
        assertEquals("René", elenco.get(0).getNome());
        assertEquals("001", new GestoreDati(temp.toFile()).caricaPersone().get(0).getTelefono());
    }

    @Test void loginVuotoErratoPoiCorretto() throws Exception {
        FinestraLogin login = edt(() -> {
            FinestraLogin f = new FinestraLogin(new GestoreDati(temp.toFile()));
            f.setVisible(true);
            return f;
        });
        attendi(() -> bottone(login, "Login", false).isEnabled());
        click(login, "Login", false);
        chiudiMessaggio(attendiMessaggio(), JOptionPane.OK_OPTION);
        edt(() -> {
            List<JTextField> campi = componenti(login, JTextField.class);
            campi.get(0).setText("admin");
            campi.get(1).setText("errata");
            return null;
        });
        click(login, "Login", false);
        JOptionPane errore = attendiMessaggio();
        assertEquals("Login errato", edt(() -> errore.getMessage()));
        chiudiMessaggio(errore, JOptionPane.OK_OPTION);
        edt(() -> {
            assertEquals("", componenti(login, JTextField.class).get(1).getText());
            assertTrue(login.isShowing());
            componenti(login, JTextField.class).get(1).setText("admin");
            return null;
        });
        click(login, "Login", true);
        attendi(() -> !login.isShowing() && finestraAperta(FinestraPrincipale.class) != null);
        assertTrue(edt(() -> finestraAperta(FinestraPrincipale.class).getTitle()).contains("admin"));
    }

    private void prepara(boolean errore) throws Exception {
        GestoreDati dati = new GestoreDati(temp.toFile());
        dati.prepara();
        elenco = new ElencoPersone();
        if (errore) {
            elenco.aggiungi(persona("René", "Ferretti", "001"));
            dati.salvaPersone(elenco.getPersone());
            dati = new GestoreDati(temp.toFile()) {
                public void salvaPersone(java.util.Vector<Persona> persone) throws IOException {
                    throw new IOException("Disco non disponibile");
                }
            };
        }
        final GestoreDati archivio = dati;
        finestra = edt(() -> {
            FinestraPrincipale f = new FinestraPrincipale(archivio, elenco, "admin");
            f.setVisible(true);
            return f;
        });
    }
    private void aggiungi(String nome, String cognome, String telefono, boolean barra) throws Exception {
        int prima = elenco.size();
        click(finestra, "Nuovo", barra);
        EditorPersona editor = attendiEditor();
        edt(() -> {
            List<JTextField> campi = componenti(editor, JTextField.class);
            String[] valori = {nome, cognome, "via Roma", telefono, "50"};
            for (int i = 0; i < valori.length; i++) campi.get(i).setText(valori[i]);
            bottone(editor, "Salva", barra).doClick();
            return null;
        });
        attendi(() -> !editor.isShowing() && elenco.size() == prima + 1);
    }
    private JTable tabella() { return componenti(finestra, JTable.class).get(0); }
    private void seleziona(int indice) {
        int vista = tabella().convertRowIndexToView(indice);
        tabella().setRowSelectionInterval(vista, vista);
    }
    private Persona persona(String nome, String cognome, String telefono) {
        return new Persona(nome, cognome, "via Roma", telefono, 50);
    }
    private void click(Container parent, String testo, boolean barra) {
        SwingUtilities.invokeLater(() -> bottone(parent, testo, barra).doClick());
    }
    private JButton bottone(Container parent, String testo, boolean barra) {
        return componenti(parent, JButton.class).stream().filter(b ->
                (testo.equals(b.getText()) || testo.equals(b.getToolTipText()))
                && (b.getParent() instanceof JToolBar) == barra).findFirst().get();
    }
    private EditorPersona attendiEditor() throws Exception {
        attendi(() -> finestraAperta(EditorPersona.class) != null);
        return edt(() -> finestraAperta(EditorPersona.class));
    }
    private JOptionPane attendiMessaggio() throws Exception {
        attendi(() -> messaggio() != null);
        return edt(() -> messaggio());
    }
    private JOptionPane messaggio() {
        for (Window w : Window.getWindows()) if (w.isShowing()) {
            List<JOptionPane> panes = componenti(w, JOptionPane.class);
            if (!panes.isEmpty()) return panes.get(0);
        }
        return null;
    }
    private void chiudiMessaggio(JOptionPane pane, int risposta) throws Exception {
        edt(() -> { pane.setValue(risposta); return null; });
        attendi(() -> !pane.isShowing());
        edt(() -> null);
    }
    private <T extends Window> T finestraAperta(Class<T> tipo) {
        for (Window w : Window.getWindows()) if (tipo.isInstance(w) && w.isShowing()) return tipo.cast(w);
        return null;
    }
    private <T> List<T> componenti(Container root, Class<T> tipo) {
        List<T> trovati = new ArrayList<T>();
        if (tipo.isInstance(root)) trovati.add(tipo.cast(root));
        for (Component c : root.getComponents()) {
            if (c instanceof Container) trovati.addAll(componenti((Container)c, tipo));
            else if (tipo.isInstance(c)) trovati.add(tipo.cast(c));
        }
        return trovati;
    }
    private void attendi(Callable<Boolean> condizione) throws Exception {
        long fine = System.nanoTime() + 10000000000L;
        while (!edt(condizione)) {
            if (System.nanoTime() > fine) fail("Tempo scaduto aspettando la finestra");
            Thread.sleep(20);
        }
    }
    private <T> T edt(Callable<T> azione) throws Exception {
        FutureTask<T> task = new FutureTask<T>(azione);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
