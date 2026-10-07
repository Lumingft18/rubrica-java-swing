package rubrica.gui;

import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

final class Barre {

    private Barre() {
    }

    static JToolBar nuova() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.setRollover(true);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Stile.BORDO),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return barra;
    }

    /**
     * Bottone piatto: il bordo si vede solo col mouse sopra.
     */
    static JButton aggiungi(JToolBar barra, String testo, Icon icona, ActionListener azione) {
        final JButton bottone = new JButton(testo, icona);
        bottone.setFocusable(false);
        bottone.setVerticalTextPosition(SwingConstants.BOTTOM);
        bottone.setHorizontalTextPosition(SwingConstants.CENTER);
        bottone.setIconTextGap(3);
        bottone.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        bottone.setContentAreaFilled(false);
        bottone.setBorderPainted(false);
        bottone.getModel().addChangeListener(e -> {
            bottone.setOpaque(bottone.getModel().isRollover());
            bottone.setBackground(bottone.getModel().isPressed() ? Stile.BORDO : Stile.BLU_CHIARO);
            bottone.repaint();
        });
        bottone.addActionListener(azione);
        barra.add(bottone);
        return bottone;
    }
}
