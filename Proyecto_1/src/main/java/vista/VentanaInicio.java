package vista;

import audio.Sonidos;
import modelo.juego.Partida;
import modelo.juego.Mapa;
import modelo.componentes.Nucleo;
import persistencia.Archivo;

/** Menú principal. Sus controles se editan desde Design. */
public class VentanaInicio extends javax.swing.JFrame {
    public VentanaInicio() {
        initComponents();

        configurarFondo();
        Sonidos.configurarSonidosBTN(this);
        setLocationRelativeTo(null);
    }

    /** Escala el icono elegido en Design conservando proporción y cobertura. */
    private void configurarFondo() {
        // Captura la imagen seleccionada en Design; el Icon escala sin crear bitmaps al redimensionar.
        if (lblFondo.getIcon() instanceof javax.swing.ImageIcon imagen) {
            lblFondo.setIcon(new javax.swing.Icon() {
                @Override public int getIconWidth() { return Math.max(1, lblFondo.getWidth()); }
                @Override public int getIconHeight() { return Math.max(1, lblFondo.getHeight()); }
                @Override public void paintIcon(java.awt.Component componente, java.awt.Graphics graphics, int x, int y) {
                    int ancho = lblFondo.getWidth(), alto = lblFondo.getHeight();
                    if (ancho <= 0 || alto <= 0 || imagen.getIconWidth() <= 0 || imagen.getIconHeight() <= 0) return;
                    // Usa la mayor escala: cubre toda la ventana y recorta, sin deformar ni dejar franjas.
                    double escala = Math.max((double) ancho / imagen.getIconWidth(),
                            (double) alto / imagen.getIconHeight());
                    int dibujadoAncho = (int) Math.ceil(imagen.getIconWidth() * escala);
                    int dibujadoAlto = (int) Math.ceil(imagen.getIconHeight() * escala);
                    java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                    try {
                        g.clipRect(0, 0, ancho, alto);
                        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        g.drawImage(imagen.getImage(), (ancho - dibujadoAncho) / 2,
                                (alto - dibujadoAlto) / 2, dibujadoAncho, dibujadoAlto, componente);
                    } finally { g.dispose(); }
                }
            });
        }
    }

    private void abrirJuego(Partida partida) {
        VentanaJuego juego = new VentanaJuego();
        // La misión ya existe: conserva los enemigos y el estado inicial.
        juego.setPartida(partida, true);
        juego.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent evento) {
                setVisible(true);
            }
        });
        juego.setVisible(true);
        setVisible(false);
    }

    private void btnNuevaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaActionPerformed
        crearPartida();
    }//GEN-LAST:event_btnNuevaActionPerformed

    private void btnCargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCargarActionPerformed
        cargarPartida();
    }//GEN-LAST:event_btnCargarActionPerformed

    private void btnAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdminActionPerformed
        abrirAdministrador();
    }//GEN-LAST:event_btnAdminActionPerformed

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        dispose();
    }//GEN-LAST:event_btnSalirActionPerformed

    // Operaciones de la ventana. Se pueden invocar sin fabricar eventos de Swing.
    /** Solicita el nombre y prepara la misión antes de abrir el juego. */
    private void crearPartida() {
        String nombre = javax.swing.JOptionPane.showInputDialog(this, "Nombre del comandante:");
        if (nombre == null) return; // Cancelar no crea una partida.
        nombre = nombre.strip();
        if (nombre.isBlank() || nombre.length() > 40 || nombre.chars().anyMatch(Character::isISOControl)) {
            javax.swing.JOptionPane.showMessageDialog(this, "Ingrese un nombre de 1 a 40 caracteres, sin saltos de línea.");
            return;
        }
        try {
            Mapa mapa = new Mapa();
            // Mantiene la vida inicial configurada para el juego.
            mapa.colocar(new Nucleo("Núcleo", 2000), 12, 12);
            Partida partida = new Partida(nombre, mapa);
            partida.prepararMision(new Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json"));
            abrirJuego(partida);
        } catch (IllegalStateException | IllegalArgumentException | java.io.UncheckedIOException e) {
            // Un catálogo ausente, inválido o sin enemigos habilitados conserva abierto el menú.
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(),
                    "No se pudo crear la partida", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Abre los slots y continúa con la partida cargada. */
    private void cargarPartida() {
        VentanaSlots slots = new VentanaSlots(this, null);
        slots.setVisible(true);
        if (slots.getPartidaCargada() != null) abrirJuego(slots.getPartidaCargada());
    }

    /** Usa el flujo de acceso del administrador. */
    private void abrirAdministrador() {
        new VentanaAdmin().mostrar();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        btnNueva = new javax.swing.JButton();
        btnCargar = new javax.swing.JButton();
        btnAdmin = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        lblFondo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Colonia Marte - Inicio");

        lblTitulo.setFont(new java.awt.Font("Magneto", 1, 48)); // NOI18N
        lblTitulo.setForeground(new java.awt.Color(226, 232, 240));
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("MARS WARS");

        btnNueva.setBackground(new java.awt.Color(0, 0, 51));
        btnNueva.setFont(new java.awt.Font("Century Gothic", 1, 24)); // NOI18N
        btnNueva.setForeground(new java.awt.Color(255, 255, 255));
        btnNueva.setText("Nueva partida");
        btnNueva.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnNueva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevaActionPerformed(evt);
            }
        });

        btnCargar.setBackground(new java.awt.Color(0, 0, 51));
        btnCargar.setFont(new java.awt.Font("Century Gothic", 1, 24)); // NOI18N
        btnCargar.setForeground(new java.awt.Color(255, 255, 255));
        btnCargar.setText("Cargar partida");
        btnCargar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnCargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCargarActionPerformed(evt);
            }
        });

        btnAdmin.setBackground(new java.awt.Color(0, 0, 51));
        btnAdmin.setFont(new java.awt.Font("Century Gothic", 1, 24)); // NOI18N
        btnAdmin.setForeground(new java.awt.Color(255, 255, 255));
        btnAdmin.setText("Administrador");
        btnAdmin.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAdminActionPerformed(evt);
            }
        });

        btnSalir.setBackground(new java.awt.Color(0, 0, 51));
        btnSalir.setFont(new java.awt.Font("Century Gothic", 1, 24)); // NOI18N
        btnSalir.setForeground(new java.awt.Color(255, 255, 255));
        btnSalir.setText("Salir");
        btnSalir.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        lblFondo.setBackground(new java.awt.Color(15, 23, 42));
        lblFondo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/fondos/fondoMenu.png"))); // NOI18N
        lblFondo.setOpaque(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 84, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCargar, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAdmin, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 83, Short.MAX_VALUE))
            .addComponent(lblFondo, 0, 600, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 57, Short.MAX_VALUE)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 40, 40)
                .addComponent(btnNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 15, 15)
                .addComponent(btnCargar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 15, 15)
                .addComponent(btnAdmin, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 15, 15)
                .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 58, Short.MAX_VALUE))
            .addComponent(lblFondo, 0, 440, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdmin;
    private javax.swing.JButton btnCargar;
    private javax.swing.JButton btnNueva;
    private javax.swing.JButton btnSalir;
    private javax.swing.JLabel lblFondo;
    private javax.swing.JLabel lblTitulo;
    // End of variables declaration//GEN-END:variables
}
