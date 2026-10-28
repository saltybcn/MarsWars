package vista;

import java.awt.*;
import javax.swing.JPanel;

/** Fondo translúcido; sus componentes conservan su propia opacidad y se editan en Design. */
public class PanelTranslucido extends JPanel {
    private static final long serialVersionUID = 1L;
    private float opacidad = 0.75f;

    public PanelTranslucido() {
        setOpaque(false); // Swing debe repintar el tablero que permanece debajo.
        // Captura también los clics y la rueda sobre espacios vacíos de la capa.
        addMouseListener(new java.awt.event.MouseAdapter() { });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });
        addMouseWheelListener(evento -> evento.consume());
    }

    public float getOpacidad() { return opacidad; }

    public void setOpacidad(float opacidad) {
        if (!Float.isFinite(opacidad) || opacidad < 0 || opacidad > 1)
            throw new IllegalArgumentException("Opacidad entre 0 y 1");
        this.opacidad = opacidad;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setComposite(AlphaComposite.SrcOver.derive(opacidad));
            Color fondo = getBackground();
            g.setColor(new Color(fondo.getRed(), fondo.getGreen(), fondo.getBlue()));
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally { g.dispose(); }
    }
}
