package rubrica.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * Icone disegnate qui, senza png. Il jar resta uno solo.
 * Disegno su una griglia 20x20; sono vettoriali, restano nitide anche su schermi retina.
 */
public final class Icone {

    private static final Color VERDE = new Color(34, 139, 84);
    private static final Color BLU = new Color(37, 99, 170);
    private static final Color ROSSO = new Color(196, 52, 52);
    private static final Color GRIGIO = new Color(100, 106, 115);

    private Icone() {
    }

    public static Icon nuovo() {
        return new Icona("nuovo", 20);
    }

    public static Icon modifica() {
        return new Icona("modifica", 20);
    }

    public static Icon elimina() {
        return new Icona("elimina", 20);
    }

    public static Icon salva() {
        return new Icona("salva", 20);
    }

    public static Icon annulla() {
        return new Icona("annulla", 20);
    }

    public static Icon login() {
        return new Icona("login", 20);
    }

    /** Per l'intestazione delle finestre. */
    public static Icon persona() {
        return new Icona("persona", 32);
    }

    /**
     * Logo: rubrica blu con la sagoma di una persona. Griglia 24x24.
     */
    static void paintLogo(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BLU);
        g2.fillRoundRect(3, 1, 18, 22, 5, 5);
        g2.setColor(Color.WHITE);
        g2.fillOval(9, 5, 6, 6);
        g2.fillArc(6, 12, 12, 12, 0, 180);
        g2.setColor(new Color(20, 70, 130));
        g2.fillRect(3, 4, 2, 3);
        g2.fillRect(3, 10, 2, 3);
        g2.fillRect(3, 16, 2, 3);
    }

    private static final class Icona implements Icon {

        private final String tipo;
        private final int lato;

        private Icona(String tipo, int lato) {
            this.tipo = tipo;
            this.lato = lato;
        }

        public int getIconWidth() {
            return lato;
        }

        public int getIconHeight() {
            return lato;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(lato / 20.0, lato / 20.0);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            boolean attiva = c == null || c.isEnabled();

            if ("nuovo".equals(tipo)) {
                g2.setColor(colore(VERDE, attiva));
                g2.drawOval(2, 2, 16, 16);
                g2.drawLine(10, 6, 10, 14);
                g2.drawLine(6, 10, 14, 10);
            } else if ("modifica".equals(tipo)) {
                // matita
                g2.setColor(colore(BLU, attiva));
                g2.drawLine(4, 16, 5, 12);
                g2.drawLine(5, 12, 13, 4);
                g2.drawLine(13, 4, 16, 7);
                g2.drawLine(16, 7, 8, 15);
                g2.drawLine(8, 15, 4, 16);
                g2.drawLine(11, 6, 14, 9);
            } else if ("elimina".equals(tipo)) {
                // cestino
                g2.setColor(colore(ROSSO, attiva));
                g2.drawLine(3, 5, 17, 5);
                g2.drawLine(8, 5, 8, 3);
                g2.drawLine(8, 3, 12, 3);
                g2.drawLine(12, 3, 12, 5);
                g2.drawRoundRect(5, 5, 10, 12, 3, 3);
                g2.drawLine(8, 8, 8, 14);
                g2.drawLine(12, 8, 12, 14);
            } else if ("salva".equals(tipo)) {
                g2.setColor(colore(VERDE, attiva));
                g2.drawOval(2, 2, 16, 16);
                g2.drawLine(6, 10, 9, 13);
                g2.drawLine(9, 13, 14, 7);
            } else if ("annulla".equals(tipo)) {
                g2.setColor(colore(GRIGIO, attiva));
                g2.drawOval(2, 2, 16, 16);
                g2.drawLine(7, 7, 13, 13);
                g2.drawLine(13, 7, 7, 13);
            } else if ("login".equals(tipo)) {
                // freccia che entra in una porta
                g2.setColor(colore(BLU, attiva));
                g2.drawLine(11, 3, 16, 3);
                g2.drawLine(16, 3, 16, 17);
                g2.drawLine(16, 17, 11, 17);
                g2.drawLine(3, 10, 12, 10);
                g2.drawLine(9, 7, 12, 10);
                g2.drawLine(12, 10, 9, 13);
            } else if ("persona".equals(tipo)) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillOval(0, 0, 20, 20);
                g2.setColor(Color.WHITE);
                g2.fillOval(7, 4, 6, 6);
                g2.fillArc(4, 11, 12, 12, 0, 180);
            }

            g2.dispose();
        }

        private Color colore(Color base, boolean attiva) {
            if (attiva) {
                return base;
            }
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
        }
    }
}
