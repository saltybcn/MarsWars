package vista;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.componentes.Combate;
import modelo.defensas.Defensa;
import modelo.juego.Mapa; 

/** Representación visual; no contiene reglas de combate. */
public class PanelMapa extends JPanel {
    private final JButton[][] celdas = new JButton[25][25];
    private Mapa mapa;
    private final Componente[][] anteriores = new Componente[25][25];
    private final long[][] finEfecto = new long[25][25];
    private final ImageIcon nucleoDestruido = Componente.cargarImagen("nucleo/nucleo_destruido.png");

    // Una versión roja por sprite; reutiliza la imagen al recibir más golpes.
    private final java.util.Map<ImageIcon, ImageIcon> imagenesDano = new java.util.WeakHashMap<>();

    private final Consumer<Componente> seleccion;
    // Carga el fondo completo: cargarImagen genera miniaturas de 64 px para las unidades.
    private final ImageIcon fondo = PanelMapa.class.getResource("/fondos/fondoMapa.png") == null
            ? null : new ImageIcon(PanelMapa.class.getResource("/fondos/fondoMapa.png"));
    // Constructor sin argumentos para el diseñador y la paleta de NetBeans. 
    public PanelMapa() { this(componente -> { }); }

    public PanelMapa(Consumer<Componente> seleccion) {
        this(seleccion, null);
    }

    public PanelMapa(Consumer<Componente> seleccion, VentanaJuego ventana) {
        this.seleccion = seleccion;
        initComponents();
        removeAll(); // La celda de Design es una plantilla; el mapa necesita 625 casillas reales.
        for (int fila = 0; fila < 25; fila++) {
            for (int col = 0; col < 25; col++) {
                final int f = fila, c = col;
                JButton boton = new JButton();
                // Copia el estilo editado en btnModelo desde Design.
                boton.setMargin(btnModelo.getMargin());
                boton.setFont(btnModelo.getFont());
                boton.setForeground(btnModelo.getForeground());
                boton.setBorder(btnModelo.getBorder());
                boton.setOpaque(btnModelo.isOpaque());
                boton.setContentAreaFilled(btnModelo.isContentAreaFilled());
                boton.setFocusPainted(btnModelo.isFocusPainted());
                // El foco se solicita al seleccionar con clic izquierdo, no al retirar.
                boton.setRequestFocusEnabled(false);
                boton.addActionListener(e -> {
                    // Mantiene el marco en la casilla seleccionada hasta cambiar la selección.
                    boton.requestFocusInWindow();
                    seleccionarCasilla(f, c, ventana);
                });
                boton.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mousePressed(java.awt.event.MouseEvent e) {
                        // Un clic derecho solo retira; no dispara la colocación del clic izquierdo.
                        if (boton.isEnabled() && SwingUtilities.isRightMouseButton(e)) {
                            // Desarma antes de soltar para no generar un clic izquierdo accidental.
                            ButtonModel modelo = boton.getModel();
                            modelo.setArmed(false);
                            modelo.setPressed(false);
                            modelo.setRollover(false);
                            modelo.setSelected(false);
                            // Retirar la casilla seleccionada también elimina su marco de foco.
                            if (boton.isFocusOwner()) {
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                            }
                            if (ventana != null) ventana.retirarCasilla(f, c);
                        }
                    }
                });
                celdas[fila][col] = boton;
                add(boton);
            }
        }
        actualizar();
    }
    @Override
    // Dibujo del fondo en el panel
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // Si falta la imagen, conserva el color de fondo del panel.
        if (fondo == null) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            // Mantiene los bordes del pixel art al cambiar el tamaño.
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g.drawImage(fondo.getImage(),
                    0, 0, getWidth(), getHeight(), this);
        } finally {
            g.dispose();
        }
    }
    private void seleccionarCasilla(int fila, int columna, VentanaJuego ventana) {
        seleccion.accept(mapa == null ? null : mapa.obtener(fila, columna));
        // Comunica también las casillas vacías para permitir colocar una defensa.
        if (ventana != null) ventana.seleccionarCasilla(fila, columna);
    }

    public void setBotonesActivos(boolean estado) {
        for (JButton[] fila : celdas) {
            for (JButton boton : fila) {
                boton.setDisabledIcon(boton.getIcon()); // Bloquea clics sin ocultar ni decolorar la unidad.
                if (!estado) {
                    // Quita el gesto del último clic antes de cubrir la casilla con la capa.
                    javax.swing.ButtonModel modelo = boton.getModel();
                    modelo.setArmed(false); // Desarma antes de soltar para no disparar un ataque/clic.
                    modelo.setPressed(false);
                    modelo.setRollover(false);
                    modelo.setSelected(false);
                }
                boton.setFocusPainted(estado && btnModelo.isFocusPainted());
                boton.setFocusable(estado && btnModelo.isFocusable());
                boton.setEnabled(estado);
            }
        }
    }
 
    public void setMapa(Mapa mapa) {
        if (this.mapa != mapa) for (int f = 0; f < 25; f++) {
            java.util.Arrays.fill(anteriores[f], null); java.util.Arrays.fill(finEfecto[f], 0);
        }
        this.mapa = mapa; actualizar();
    }

    public void actualizar() {
        Componente[][] datos = mapa == null ? null : mapa.getMat();
        for (int f = 0; f < 25; f++) {
            for (int c = 0; c < 25; c++) {
                Componente unidad = datos == null ? null : datos[f][c];
                // La explosión retira la mina/demolición del modelo de inmediato; conserva solo su imagen.
                // 150 ms permiten verla durante tres refrescos de 50 ms, sin detener el combate.
                if (unidad == null && anteriores[f][c] instanceof Combate anterior
                        && anterior.getVida() <= 0 && anterior.isAtaqueVisible()) {
                    if (finEfecto[f][c] == 0) finEfecto[f][c] = System.nanoTime() + 150_000_000L;
                    if (System.nanoTime() < finEfecto[f][c]) unidad = anterior;
                    else anteriores[f][c] = null;
                } else { anteriores[f][c] = unidad; finEfecto[f][c] = 0; }
                JButton boton = celdas[f][c];
                boton.setIcon(null);
                boton.setText("");
                boton.setForeground(btnModelo.getForeground()); // Restablece también el fallback de texto.
                boton.setBackground(btnModelo.getBackground());
                boton.setOpaque(btnModelo.isOpaque());
                boton.setContentAreaFilled(btnModelo.isContentAreaFilled());
                String detalle = "Fila " + f + ", columna " + c;
                if (unidad != null) {
                    String nombre = unidad.getNombre();
                    if (nombre == null || nombre.isEmpty()) nombre = unidad.getClass().getSimpleName();
                    ImageIcon imagen = unidad.getImgIdle();
                    String accion = "";
                    boolean izquierda = false;
                    if (unidad instanceof Combate combate) {
                        izquierda = combate.isMirandoIzquierda();
                        if (combate.isAtaqueVisible()) {
                            accion = "_ataque"; imagen = combate.getImgAtq();
                        } else if (combate.isPasoAlterno()) {
                            accion = "_movimiento"; imagen = combate.getImgMov();
                        }
                    }
                    if (imagen == null) imagen = unidad.getImgIdle();
                    // El núcleo conserva su casilla y muestra los restos durante el desenlace.
                    if (unidad instanceof Nucleo && unidad.getVida() <= 0 && nucleoDestruido != null)
                        imagen = nucleoDestruido;
                    // Destello de daño: un solo pulso rojo durante 120 ms, sin detener la animación actual.
                    long ultimoGolpe = unidad.getUltimoDmg();
                    long transcurrido = System.nanoTime() - ultimoGolpe;
                    boolean rojo = ultimoGolpe != 0 && transcurrido >= 0
                            && transcurrido < 120_000_000L;
                    if (rojo && imagen != null) imagen = imagenDano(imagen);
                    if (rojo) boton.setForeground(Color.RED);
                    if (imagen == null) boton.setText(nombre.substring(0, 1).toUpperCase());
                    else {
                        // El Icon pinta al tamaño real de la casilla; no crea bitmaps en cada refresco.
                        final ImageIcon sprite = imagen;
                        final boolean reflejar = izquierda;
                        boton.setIcon(new Icon() {
                            public int getIconWidth() { return Math.max(1, boton.getWidth() - 1); }
                            public int getIconHeight() { return Math.max(1, boton.getHeight() - 1); }
                            public void paintIcon(Component componente, Graphics graphics, int x, int y) {
                                Graphics2D g = (Graphics2D) graphics.create();
                                try {
                                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                                    double escala = Math.min((double) getIconWidth() / sprite.getIconWidth(),
                                            (double) getIconHeight() / sprite.getIconHeight());
                                    int ancho = Math.max(1, (int) (sprite.getIconWidth() * escala));
                                    int alto = Math.max(1, (int) (sprite.getIconHeight() * escala));
                                    int inicioX = (boton.getWidth() - ancho) / 2;
                                    int inicioY = (boton.getHeight() - alto) / 2;
                                    if (reflejar) {
                                        g.translate(boton.getWidth(), 0);
                                        g.scale(-1, 1); // Refleja el dibujo, no la posición de la unidad.
                                    }
                                    g.drawImage(sprite.getImage(), inicioX, inicioY, ancho, alto, componente);
                                } finally { g.dispose(); }
                            }
                        });
                    }
                    // El color por bando solo respalda una unidad sin imagen; con sprite conserva el tablero.
                    if (imagen == null) {
                        // Sin sprite conserva la identificación por color sobre el terreno.
                        boton.setOpaque(true);
                        boton.setContentAreaFilled(true);
                        if (unidad instanceof Nucleo) boton.setBackground(new Color(255, 220, 115));
                        else if (unidad instanceof Defensa) boton.setBackground(new Color(173, 216, 230));
                        else boton.setBackground(new Color(255, 99, 71));
                    }
                    detalle += " — " + nombre + " — Vida: " + unidad.getVida();
                }
                boton.setDisabledIcon(boton.getIcon());
                boton.setToolTipText(detalle);
            }
        }
    }
    /** Tiñe solamente los píxeles visibles; conserva la transparencia del sprite. */
    private ImageIcon imagenDano(ImageIcon original) {
        return imagenesDano.computeIfAbsent(original, imagen -> {
            java.awt.image.BufferedImage roja = new java.awt.image.BufferedImage(
                    imagen.getIconWidth(), imagen.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = roja.createGraphics();
            try {
                g.drawImage(imagen.getImage(), 0, 0, null);
                // SRC_ATOP limita el rojo al dibujo y mantiene intacto su canal alfa.
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.65f));
                g.setColor(Color.RED);
                g.fillRect(0, 0, roja.getWidth(), roja.getHeight());
            } finally { g.dispose(); }
            return new ImageIcon(roja);
        });
    }

    /** Panel y celda modelo editables en NetBeans Design. */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnModelo = new javax.swing.JButton();

        setBackground(new java.awt.Color(64, 64, 64));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(750, 750));
        setLayout(new java.awt.GridLayout(25, 25, 1, 1));

        btnModelo.setBackground(new java.awt.Color(153, 121, 108));
        btnModelo.setFont(new java.awt.Font("SansSerif", 1, 10)); // NOI18N
        btnModelo.setForeground(new java.awt.Color(0, 0, 0));
        btnModelo.setText("M");
        btnModelo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 51, 51)));
        btnModelo.setContentAreaFilled(false);
        btnModelo.setMargin(new java.awt.Insets(0, 0, 0, 0));
        add(btnModelo);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnModelo;
    // End of variables declaration//GEN-END:variables

}

