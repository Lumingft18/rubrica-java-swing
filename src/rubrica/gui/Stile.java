package rubrica.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Colori e pezzi grafici in comune fra le tre finestre.
 * Solo Swing base, niente librerie.
 */
final class Stile {

    static final Color BLU = new Color(37, 99, 170);
    static final Color BLU_CHIARO = new Color(212, 228, 247);
    static final Color RIGA_ALTERNA = new Color(246, 248, 251);
    static final Color BORDO = new Color(218, 222, 228);
    static final Color TESTO_TENUE = new Color(110, 116, 125);

    private Stile() {
    }

    /**
     * Fascia in alto con titolo e sottotitolo.
     */
    static JPanel intestazione(String titolo, String sottotitolo) {
        JLabel grande = new JLabel(titolo, Icone.persona(), SwingConstants.LEFT);
        grande.setIconTextGap(12);
        grande.setForeground(Color.WHITE);
        grande.setFont(grande.getFont().deriveFont(Font.BOLD, grande.getFont().getSize2D() + 5f));

        JLabel piccolo = new JLabel(sottotitolo);
        piccolo.setForeground(new Color(220, 232, 246));
        piccolo.setBorder(new EmptyBorder(2, 44, 0, 0));

        JPanel pannello = new JPanel(new BorderLayout());
        pannello.setBackground(BLU);
        pannello.setBorder(new EmptyBorder(14, 18, 14, 18));
        pannello.add(grande, BorderLayout.CENTER);
        pannello.add(piccolo, BorderLayout.SOUTH);
        return pannello;
    }

    static JLabel tenue(String testo) {
        JLabel etichetta = new JLabel(testo);
        etichetta.setForeground(TESTO_TENUE);
        etichetta.setFont(etichetta.getFont().deriveFont(etichetta.getFont().getSize2D() - 1f));
        return etichetta;
    }

    /**
     * Righe alternate, margine nelle celle, niente griglia verticale.
     */
    static void tabella(JTable tabella) {
        tabella.setRowHeight(28);
        tabella.setShowVerticalLines(false);
        tabella.setShowHorizontalLines(true);
        tabella.setGridColor(new Color(236, 238, 242));
        tabella.setIntercellSpacing(new Dimension(0, 1));
        tabella.setSelectionBackground(BLU_CHIARO);
        tabella.setSelectionForeground(Color.BLACK);
        tabella.setDefaultRenderer(Object.class, new RendererCella());

        JTableHeader testata = tabella.getTableHeader();
        testata.setReorderingAllowed(false);
        final TableCellRenderer originale = testata.getDefaultRenderer();
        testata.setDefaultRenderer((t, valore, sel, focus, riga, colonna) -> {
            Component c = originale.getTableCellRendererComponent(t, valore, sel, focus, riga, colonna);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setBorder(BorderFactory.createCompoundBorder(l.getBorder(), new EmptyBorder(2, 4, 2, 4)));
            }
            return c;
        });
    }

    /**
     * Icona della finestra, nelle misure che il sistema chiede.
     */
    static List<Image> iconeFinestra() {
        List<Image> immagini = new ArrayList<Image>();
        int[] misure = { 16, 32, 64 };
        for (int i = 0; i < misure.length; i++) {
            int lato = misure[i];
            BufferedImage img = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.scale(lato / 24.0, lato / 24.0);
            Icone.paintLogo(g);
            g.dispose();
            immagini.add(img);
        }
        return immagini;
    }

    static Border bordoSopra() {
        return BorderFactory.createMatteBorder(1, 0, 0, 0, BORDO);
    }

    private static final class RendererCella extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public Component getTableCellRendererComponent(JTable tabella, Object valore,
                boolean selezionata, boolean focus, int riga, int colonna) {
            super.getTableCellRendererComponent(tabella, valore, selezionata, false, riga, colonna);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (!selezionata) {
                setBackground(riga % 2 == 0 ? Color.WHITE : RIGA_ALTERNA);
                setForeground(Color.BLACK);
            }
            return this;
        }
    }
}
