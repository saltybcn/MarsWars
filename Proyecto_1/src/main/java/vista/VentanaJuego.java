package vista;

import audio.Sonidos;
import configuracion.Catalogo;
import modelo.componentes.Componente;
import modelo.juego.Batalla;
import modelo.juego.Partida;
import modelo.registros.RegistroDmg;

public class VentanaJuego extends javax.swing.JFrame {
    private static final long serialVersionUID = 1L;
    private Partida partida;
    private Catalogo catalogo;
    private boolean misionPreparada;
    private final java.util.ArrayList<configuracion.Configuracion> opcionesInventario = new java.util.ArrayList<>();
    private PanelMapa panelMapa;
    private javax.swing.Timer refresco;
    private Componente componenteSeleccionado;
    private final java.util.ArrayList<Componente> filasResultados = new java.util.ArrayList<>();
    private transient long mostrarResumenDesde; // Instante para dejar visible el desenlace antes del resumen.
    private transient boolean resultadosMostrados; // El timer no debe reconstruir el resumen varias veces

public VentanaJuego() {
    initComponents();

    Sonidos.configurarSonidosBTN(this);
    detalle.setToolTipText("Consulte el detalle completo debajo del inventario.");
    tablaResultados.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    tablaResultados.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) mostrarParticipanteSeleccionado();
    });
    tablaResultados.setToolTipText("Seleccione un participante para consultar su historial en el panel lateral.");
    // Oculta la pantalla de resultados
    jResultados.setVisible(false);
    btnFinalizarCampana.setVisible(false);
    pnlPausa.setVisible(false);
    lblFondoPausa.setOpaque(false); // La etiqueta de fondo no debe tapar la transparencia.
    jLayeredPane1.setLayer(pnlPausa, javax.swing.JLayeredPane.MODAL_LAYER);
    // Hace que las celdas de la tabla esten centradas luego
    javax.swing.table.DefaultTableCellRenderer centrado = new javax.swing.table.DefaultTableCellRenderer() {
        @Override public java.awt.Component getTableCellRendererComponent(javax.swing.JTable tabla,
                Object valor, boolean seleccionada, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            setOpaque(seleccionada); // Solo la fila seleccionada lleva un fondo sólido.
            return this;
        }
    };
    centrado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    for(int x=0;x<7;x++){
       tablaResultados.getColumnModel().getColumn(x).setCellRenderer(centrado);}
                     
    jLayeredPane1.setLayer(jResultados, javax.swing.JLayeredPane.PALETTE_LAYER);
    tablaResultados.getTableHeader().setBackground(new java.awt.Color(35, 46, 64));
    tablaResultados.getTableHeader().setForeground(new java.awt.Color(226, 232, 240));
    tablaResultados.getTableHeader().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
    tablaResultados.getTableHeader().setReorderingAllowed(false);
    scrollResultados.setColumnHeaderView(tablaResultados.getTableHeader());
    tablaResultados.setFillsViewportHeight(true);
    tablaResultados.setOpaque(false);
    scrollResultados.setOpaque(false);
    scrollResultados.getViewport().setOpaque(false);
    scrollResultados.getViewport().setBackground(tablaResultados.getBackground());
    int[] anchos = {180, 130, 95, 85, 105, 105, 75};
    for (int i = 0; i < anchos.length; i++) tablaResultados.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
    javax.swing.table.DefaultTableCellRenderer numeros = new javax.swing.table.DefaultTableCellRenderer();
    numeros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    tablaResultados.setDefaultRenderer(Long.class, numeros);
    // Cambia el layout del contenedor para que el mapa llene su espacio.
    panelContenedorMapa.setLayout(new java.awt.BorderLayout());
    panelMapa = new PanelMapa(this::mostrarDetalle, this);
    // El mapa pertenece al scroll y el scroll pertenece al contenedor.
    javax.swing.JScrollPane scrollMapa = new javax.swing.JScrollPane(panelMapa);
    // Las coordenadas siguen disponibles al señalar una casilla y en el detalle de la unidad.
    panelContenedorMapa.add(scrollMapa, java.awt.BorderLayout.CENTER);
    detalle.setText("Seleccione una casilla para ver su contenido.");
    refresco = new javax.swing.Timer(50, e -> actualizarMapa());
    addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowOpened(java.awt.event.WindowEvent e) {
            if (!resultadosMostrados) refresco.start();
        }
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            solicitarSalida();
        }
        @Override
        public void windowClosed(java.awt.event.WindowEvent e) {
            if (partida != null && partida.getBatalla() != null && partida.getBatalla().isEnCurso()) {
                partida.getBatalla().finalizar();
            }
            refresco.stop();
        }
    });
    // El catálogo se carga una vez; los errores se muestran sin sustituir el archivo.
    try {
        setCatalogo(new persistencia.Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json"));
    } catch (RuntimeException e) {
        javax.swing.JOptionPane.showMessageDialog(this, "No se pudo cargar el catálogo: " + e.getMessage());
    }
    // Recalcula el tamaño después de añadir los componentes.
    pack();
    setLocationRelativeTo(null);
}

    public void mostrar() {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            setVisible(true);
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    public void actualizarMapa() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(this::actualizarMapa);
            return;
        }
        actualizarControles();
        if (resultadosMostrados || pnlPausa.isVisible()) return; // Mantiene el resumen final aunque quede un evento del timer pendiente
        panelMapa.setMapa(partida == null ? null : partida.getMapa());
        actualizarDetalle();
        if (partida == null || partida.getBatalla() == null) return;
        Batalla batalla = partida.getBatalla();
        // Lee el resultado después de comprobar la bandera compartida del combate.
        if (!batalla.isEnCurso()) mostrarResultados(batalla.getResultado());
    }

    public void mostrarDetalle(Componente componente) {
        if (jResultados.isVisible() || pnlPausa.isVisible()) return; // También bloquea acciones disparadas desde el teclado
        componenteSeleccionado = componente;
        actualizarDetalle();
    }
    
    private void actualizarDetalle() {
        panelDetalle.mostrar(componenteSeleccionado);
        if (componenteSeleccionado == null) {
            detalle.setText("Seleccione una casilla para ver su contenido.");
            return;
        }
        if (componenteSeleccionado.getVida() <= 0) {
            detalle.setText("La unidad fue destruida. Su historial permanece en el panel lateral.");
            return;
        }
        int[] pos = componenteSeleccionado.getPos();
        if (pos == null || pos[0] < 0 || pos[1] < 0) {
            detalle.setText("La unidad ya no está en el mapa.");
            componenteSeleccionado = null;
            return;
        }
        detalle.setText(
                componenteSeleccionado.getNombre()
                + " | "
                + componenteSeleccionado.getClass().getSimpleName()
                + " | Vida: "
                + componenteSeleccionado.getVida()
                + "/"
                + componenteSeleccionado.getVidaMax()
                + " | Posición: ["
                + pos[0]
                + ", "
                + pos[1]
                + "]"
        );
}
    public void mostrarResultados(String resultado) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(() -> mostrarResultados(resultado));
            return;
        }
        if (partida == null || partida.getBatalla() == null || resultadosMostrados
                || partida.getBatalla().isEnCurso()) return;
        if (!"VICTORIA".equals(resultado) && !"DERROTA".equals(resultado)
                && !"CANCELADA".equals(resultado) && !"ERROR".equals(resultado)) return;

        // Deja ver el último golpe con el combate ya detenido. El Timer sigue repintando el mapa.
        // No usa sleep aquí: bloquearía los repintados y los clics de Swing.
        if ("VICTORIA".equals(resultado) || "DERROTA".equals(resultado)) {
            if (mostrarResumenDesde == 0) {
                mostrarResumenDesde = System.nanoTime() + 1_500_000_000L; // 1,5 segundos.
                return;
            }
            if (System.nanoTime() - mostrarResumenDesde < 0) return;
        }

        // Cabecera según el resultado
        java.awt.Color acento;
        switch (resultado) {
            case "VICTORIA":
                lblResultado.setText("VICTORIA");
                lblDescripcion.setText("La colonia resistió.");
                acento = new java.awt.Color(94, 234, 212);
                break;
            case "DERROTA":
                lblResultado.setText("DERROTA");
                lblDescripcion.setText("El núcleo fue destruido.");
                acento = new java.awt.Color(251, 146, 132);
                break;
            default:
                lblResultado.setText("ERROR".equals(resultado) ? "BATALLA INTERRUMPIDA" : "BATALLA CANCELADA");
                lblDescripcion.setText("El combate terminó antes de tiempo");
                acento = new java.awt.Color(253, 230, 138);
        }
        // Permite terminar también tras perder la décima, para facilitar la revisión.
        boolean finCampana = partida.getMisionActual() == 10
                && ("VICTORIA".equals(resultado) || "DERROTA".equals(resultado));
        btnFinalizarCampana.setVisible(finCampana);
        btnAvanzar.setText(finCampana ? "Generar misión 11" : "Avanzar");
        if (finCampana) {
            if ("VICTORIA".equals(resultado)) lblResultado.setText("CAMPAÑA COMPLETADA");
            lblDescripcion.setText("Misión 10 finalizada. Puede terminar la campaña o generar nuevas misiones.");
        }
        lblResultado.setForeground(acento);
        lblResumen.setForeground(acento);
        String comandante = partida.getNombreComandante();
        lblMision.setText("MISIÓN " + partida.getMisionActual() + "  ·  "
                + (comandante == null || comandante.isBlank() ? "Comandante" : comandante));

        // Estadísticas de todos los participantes, incluidos los destruidos
        javax.swing.table.DefaultTableModel datos = (javax.swing.table.DefaultTableModel) tablaResultados.getModel();
        datos.setRowCount(0);
        filasResultados.clear();
        int defensasVivas = 0, totalDefensas = 0, atacantesDestruidos = 0, totalAtacantes = 0;
        Batalla batalla = partida.getBatalla();
        synchronized (partida.getMapa()) {
            for (Componente componente : batalla.getParticipantes()) {
                if (componente == null) continue;
                int[] pos = componente.getPos();
                boolean colocado = partida.getMapa().obtener(pos[0], pos[1]) == componente;
                boolean vivo = componente.getVida() > 0;
                if (componente instanceof modelo.defensas.Defensa && !(componente instanceof modelo.defensas.Barrera)) {
                    totalDefensas++;
                    if (vivo && colocado) defensasVivas++;
                }
                if (componente instanceof modelo.atacantes.Atacante) {
                    totalAtacantes++;
                    if (!vivo) atacantesDestruidos++;
                }
                long infligido = 0, recibido = 0, golpes = 0;
                for (RegistroDmg registro : componente.getHistorico().getInfligido()) {
                    infligido += registro.getDmg();
                    golpes += registro.getNumGolpes();
                }
                for (RegistroDmg registro : componente.getHistorico().getRecibido()) recibido += registro.getDmg();
                filasResultados.add(componente);
                String nombre = componente.getNombre();
                datos.addRow(new Object[]{nombre == null || nombre.isBlank() ? "Sin nombre" : nombre,
                    componente.getClass().getSimpleName(), !vivo ? "Destruido" : colocado ? "En pie" : "Retirado",
                    componente.getVida() + " / " + componente.getVidaMax(), infligido, recibido, golpes});
                
            }
            modelo.componentes.Nucleo nucleo = partida.getMapa().getNucleo();
            String vidaNucleo = nucleo == null ? "Sin núcleo" : nucleo.getVida() + " / " + nucleo.getVidaMax();
            lblResumen.setText("Núcleo: " + vidaNucleo + "    |    Defensas en pie: " + defensasVivas + " / "
                    + totalDefensas + "    |    Enemigos destruidos: " + atacantesDestruidos + " / " + totalAtacantes);
        }

        // La capa de resultados bloquea al mapa del juego
        resultadosMostrados = true;
        // Reproduce el resultado una sola vez, después de mostrar el último golpe.
        if ("VICTORIA".equals(resultado)) {
            Sonidos.reproducir(Sonidos.Evento.VICTORIA, null);
        } else if ("DERROTA".equals(resultado)) {
            Sonidos.reproducir(Sonidos.Evento.DERROTA, null);
        }
        pnlPausa.setVisible(false);
        panelContenedorMapa.setVisible(true);
        detalle.setText("COLONIA MARTE · RESULTADOS DE MISIÓN");
        refresco.stop();
        panelMapa.setBotonesActivos(false);
        jResultados.setVisible(true);
        jLayeredPane1.moveToFront(jResultados);
        panelDetalle.mostrar(componenteSeleccionado);
        actualizarControles();
        btnAvanzar.requestFocusInWindow();
        btnAvanzar.setVisible(true);
        btnReiniciar.setVisible(true);
        getContentPane().revalidate();
        getContentPane().repaint();
        jLayeredPane1.revalidate();
        jLayeredPane1.repaint();
    }

    public Partida getPartida() { return partida; }

    public void setPartida(Partida partida) {
        setPartida(partida, false);
    }

    public void setPartida(Partida partida, boolean conservarEscenario) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(() -> setPartida(partida, conservarEscenario));
            return;
        }
        if (this.partida != partida) {
            misionPreparada = conservarEscenario;
            if (this.partida != null && this.partida.getBatalla() != null && this.partida.getBatalla().isEnCurso()) {
                this.partida.getBatalla().finalizar();
            }
        }
        // Avanzar y repetir modifican la misma Partida; también requieren cerrar los resultados.
        resultadosMostrados = false;
        mostrarResumenDesde = 0; // Repetir, avanzar o cargar inicia su propio desenlace.
        componenteSeleccionado = null;
        jResultados.setVisible(false);
        panelMapa.setBotonesActivos(true);
        this.partida = partida;
        if (conservarEscenario && partida != null && partida.getCatalogo() != null)
            catalogo = partida.getCatalogo(); // Una carga conserva las plantillas de esa partida.
        pnlPausa.setVisible(partida != null && partida.getBatalla().isPausada());
        panelMapa.setBotonesActivos(!pnlPausa.isVisible());
        panelContenedorMapa.setVisible(true);
        if (partida != null && !misionPreparada && catalogo != null
                && !partida.getBatalla().isEnCurso()
                && "PENDIENTE".equals(partida.getBatalla().getResultado())) {
            try {
                // Muestra los enemigos antes de permitir la colocación de las defensas.
                partida.prepararMision(catalogo);
                misionPreparada = true;
            } catch (IllegalStateException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
        actualizarInventario();
        actualizarMapa();
        if (isShowing() && !resultadosMostrados) refresco.start();
    }

    public Catalogo getCatalogo() { return catalogo; }

    public void setCatalogo(Catalogo catalogo) {
        this.catalogo = java.util.Objects.requireNonNull(catalogo);
        // También admite recibir el catálogo después de asignar la partida.
        if (partida != null && !misionPreparada) setPartida(partida);
        else actualizarInventario();
    }
    
    private void actualizarInventario() {
        opcionesInventario.clear();
        comboInventario.removeAllItems();
        opcionesInventario.add(null);
        comboInventario.addItem("Seleccione una tropa");
        if (partida != null && !partida.getBatalla().isIniciada()
                && "PENDIENTE".equals(partida.getBatalla().getResultado())) {
            // Sin reservas, las defensas destruidas dejan de ocupar capacidad en la preparación.
            for (modelo.defensas.Defensa defensa : partida.getEscuadron().getDefensas()) {
                if (defensa.getVida() <= 0) partida.getEscuadron().quitar(defensa);
            }
        }
        if (catalogo != null) for (configuracion.Configuracion config : catalogo.getConfigs()) {
            if (config.isActivo() && config.validar()
                    && (partida == null || config.getMisionMinima() <= partida.getMisionActual())
                    && java.util.List.of("Contacto", "AlcanceMedio", "AtaqueMultiple", "Impacto", "Dron", "Barrera").contains(config.getTipo())) {
                opcionesInventario.add(config);
                comboInventario.addItem(config.getNombre() + " · " + config.getCostoCapacidad() + " espacios");
            }
        }
        actualizarControles();
    }

    private void actualizarControles() {
        // Conserva la geometría del tablero detrás de pausa y resultados; el lateral permite consultar el historial.
        boolean mostrarInventario = true; // Conserva el ancho del tablero detrás de las capas.
        pnlControlesInventario.setVisible(!resultadosMostrados); // En resultados permanece solo el detalle lateral.
        if (pnlInventario.isVisible() != mostrarInventario) {
            pnlInventario.setVisible(mostrarInventario);
            getContentPane().revalidate();
        }
        boolean preparar = partida != null && misionPreparada && !resultadosMostrados
                && !partida.getBatalla().isIniciada() && !partida.getBatalla().isPausada()
                && "PENDIENTE".equals(partida.getBatalla().getResultado());
        boolean permitePausa = partida != null && !resultadosMostrados
                && "PENDIENTE".equals(partida.getBatalla().getResultado());
        // Conserva el botón durante el desenlace; ya no hay un combate que pueda pausarse.
        btnPausa.setVisible(partida != null && !resultadosMostrados);
        btnPausa.setEnabled(permitePausa && !pnlPausa.isVisible());
        comboInventario.setEnabled(preparar);
        // Durante el desenlace permite observar el mapa sin reiniciar antes de ver el resumen.
        btnReiniciarEC.setEnabled(!preparar && !pnlPausa.isVisible() && partida != null
                && "PENDIENTE".equals(partida.getBatalla().getResultado()));
        btnIniciar.setEnabled(preparar && catalogo != null);
        if (partida != null) {
            lblCapacidad.setText("Capacidad: " + partida.getEscuadron().capacidadUtilizada()
                    + " / " + partida.getEscuadron().getCapacidadTotal()
                    + " · Libres: " + partida.getEscuadron().capacidadRestante());
            lblMision.setText("MISIÓN " + partida.getMisionActual() + " · " + partida.getNombreComandante());
        }
    }

    void seleccionarCasilla(int fila, int columna) {
        if (!comboInventario.isEnabled() || partida == null) return;
        int indice = comboInventario.getSelectedIndex();
        if (indice <= 0 || partida.getMapa().obtener(fila, columna) != null) return;

        modelo.defensas.Defensa defensa = (modelo.defensas.Defensa) opcionesInventario.get(indice).crearUnidad();
        defensa.setMision(partida.getMisionActual());
        if (!partida.getEscuadron().agregar(defensa)) {
            javax.swing.JOptionPane.showMessageDialog(this, "No queda capacidad suficiente.");
            return;
        }
        if (!partida.getMapa().colocar(defensa, fila, columna)) {
            partida.getEscuadron().quitar(defensa); // Una colocación fallida no consume capacidad.
            return;
        }
        // Una colocación fallida no debe reproducir el sonido.
        Sonidos.reproducir(Sonidos.Evento.COLOCAR, null);
        componenteSeleccionado = defensa;
        actualizarMapa();
    }

    void retirarCasilla(int fila, int columna) {
        if (partida == null || !comboInventario.isEnabled()) return;
        Componente componente = partida.getMapa().obtener(fila, columna);
        // Ignora casillas vacías, enemigos y núcleo; solo retira defensas durante la preparación.
        if (!(componente instanceof modelo.defensas.Defensa defensa)) return;
        partida.getMapa().retirar(fila, columna);
        partida.getEscuadron().quitar(defensa); // Libera capacidad y elimina la unidad con sus mejoras.
        // El sonido corresponde a una retirada válida, no al clic sobre cualquier casilla.
        Sonidos.reproducir(Sonidos.Evento.MUERTE_DEFENSA, "muerte_defensa.wav");
        if (componenteSeleccionado == defensa) componenteSeleccionado = null;
        actualizarMapa();
    }

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        iniciarMision();
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void btnPausaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPausaActionPerformed
        pausarPartida();
    }//GEN-LAST:event_btnPausaActionPerformed

    private void btnContinuarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnContinuarActionPerformed
        continuarPartida();
    }//GEN-LAST:event_btnContinuarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarPartida();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnSalirPausaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirPausaActionPerformed
        solicitarSalida();
    }//GEN-LAST:event_btnSalirPausaActionPerformed

    private void solicitarSalida() {
        if (partida == null) { dispose(); return; }
        Batalla batalla = partida.getBatalla();
        boolean estabaPausada = batalla.isPausada();
        batalla.pausar(); // El diálogo no debe dejar que el combate siga avanzando detrás.
        int respuesta = javax.swing.JOptionPane.showConfirmDialog(this,
                "La partida no se va a guardar. ¿Desea salir y perder el progreso no guardado?",
                "Salir de la partida", javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (respuesta == javax.swing.JOptionPane.YES_OPTION) {
            batalla.finalizar();
            refresco.stop();
            dispose();
        } else if (!estabaPausada) {
            batalla.reanudar(); // Cancelar conserva el estado previo del menú.
        }
        if (isDisplayable()) actualizarMapa();
    }

    
    
    /** Consulta el participante mediante el índice del modelo, no el orden visual. */
    private void mostrarParticipanteSeleccionado() {
        int fila = tablaResultados.getSelectedRow();
        if (fila >= 0 && fila < tablaResultados.getRowCount()) {
            int indice = tablaResultados.convertRowIndexToModel(fila);
            if (indice < filasResultados.size()) {
                componenteSeleccionado = filasResultados.get(indice);
                actualizarDetalle(); // El mismo panel permite consultar participantes destruidos.
            }
        }
    }

    // Operaciones de la ventana. Se pueden invocar sin fabricar eventos de Swing.
    /** Conserva la colocación para repetirla antes de iniciar los hilos. */
    private void iniciarMision() {
        if (!btnIniciar.isEnabled()) return;
        try {
            // Incluye la colocación final; los reintentos no vuelven a generar enemigos.
            partida.guardarEstadoInicial();
            partida.getBatalla().iniciar();
            actualizarMapa();
        } catch (IllegalStateException e) {
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    /** Pausa el modelo antes de mostrar el menú y bloquear el mapa. */
    private void pausarPartida() {
        if (!btnPausa.isEnabled() || partida == null) return;
        partida.getBatalla().pausar();
        // Si el último ataque terminó la batalla antes del bloqueo, muestra resultados.
        if (!partida.getBatalla().isPausada()) {
            actualizarMapa();
            return;
        }
        refresco.stop(); // Evita reconstruir celdas detrás del panel durante la pausa.
        panelMapa.setBotonesActivos(false);
        panelContenedorMapa.setVisible(true);
        pnlPausa.setVisible(true);
        jLayeredPane1.moveToFront(pnlPausa);
        actualizarControles();
        jLayeredPane1.revalidate();
        jLayeredPane1.repaint();
        btnContinuar.requestFocusInWindow();
    }

    // Reanuda  y restablece la vista de la misión. 
    private void continuarPartida() {
        if (partida == null) return;
        partida.getBatalla().reanudar();
        pnlPausa.setVisible(false);
        panelContenedorMapa.setVisible(true);
        panelMapa.setBotonesActivos(true);
        jLayeredPane1.revalidate();
        jLayeredPane1.repaint();
        refresco.start();
        actualizarMapa();
    }

    // Abre los slots únicamente con la partida pausada.
    private void guardarPartida() {
        if (partida == null || !partida.getBatalla().isPausada()) return;
        new VentanaSlots(this, partida).setVisible(true);
        // Cerrar la tabla no reanuda: el usuario decide cuándo continuar.
    }

    // Prepara la siguiente misión y actualiza la misma ventana
    private void avanzarMision() {
        try {
            partida.avanzar();
            misionPreparada = true; // avanzar ya generó los enemigos de esta misión.
            setPartida(partida); // Oculta resultados, habilita el mapa y refresca.
        } catch (IllegalStateException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    /** Permite finalizar tras victoria o derrota en la décima misión. */
    private void finalizarCampana() {
        if (partida == null || partida.getMisionActual() != 10 || partida.getBatalla() == null) return;
        String resultado = partida.getBatalla().getResultado();
        if (!"VICTORIA".equals(resultado) && !"DERROTA".equals(resultado)) return;
        partida.getBatalla().finalizar();
        dispose(); // El menú principal reaparece mediante su windowClosed.
    }

    /** Recupera el escenario inicial sin generar enemigos nuevos. */
    private void repetirMision() {
        try {
            partida.repetir();
            misionPreparada = true; // Conserva los enemigos y posiciones de la copia.
            setPartida(partida); // Oculta resultados, habilita el mapa y refresca.
        } catch (IllegalStateException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    /** Código administrado por el diseñador de NetBeans. Editar desde Design. */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane() {
            // Repinta desde las capas para que el foco de una celda no se dibuje sobre el menú.
            @Override protected boolean isPaintingOrigin() { return true; }
            @Override public boolean isOptimizedDrawingEnabled() { return false; }
        };
        jResultados = new vista.PanelTranslucido();
        pnlResumenResultados = new javax.swing.JPanel();
        lblDescripcion = new javax.swing.JLabel();
        lblResumen = new javax.swing.JLabel();
        lblResultado = new javax.swing.JLabel();
        scrollResultados = new javax.swing.JScrollPane();
        tablaResultados = new javax.swing.JTable();
        pnlAccionesResultados = new javax.swing.JPanel();
        lblAyudaResultados = new javax.swing.JLabel();
        pnlBotonesResultados = new javax.swing.JPanel();
        btnReiniciar = new javax.swing.JButton();
        btnAvanzar = new javax.swing.JButton();
        btnFinalizarCampana = new javax.swing.JButton();
        panelContenedorMapa = new javax.swing.JPanel();
        pnlPausa = new vista.PanelTranslucido();
        lblPausa = new javax.swing.JLabel();
        btnContinuar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnSalirPausa = new javax.swing.JButton();
        lblFondoPausa = new javax.swing.JLabel();
        pnlInfo = new javax.swing.JPanel();
        detalle = new javax.swing.JLabel();
        lblMision = new javax.swing.JLabel();
        btnPausa = new javax.swing.JButton();
        pnlInventario = new javax.swing.JPanel();
        pnlControlesInventario = new javax.swing.JPanel();
        lblInventario = new javax.swing.JLabel();
        comboInventario = new javax.swing.JComboBox();
        lblCapacidad = new javax.swing.JLabel();
        lblAyudaInventario = new javax.swing.JLabel();
        btnIniciar = new javax.swing.JButton();
        btnReiniciarEC = new javax.swing.JButton();
        panelDetalle = new vista.PanelDetalle();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Colonia Marte - Juego");

        jLayeredPane1.setPreferredSize(new java.awt.Dimension(760, 700));

        jResultados.setBackground(new java.awt.Color(17, 24, 39));
        jResultados.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 24, 28));
        jResultados.setLayout(new java.awt.BorderLayout());

        pnlResumenResultados.setOpaque(false);

        lblDescripcion.setFont(new java.awt.Font("Century Gothic", 1, 14)); // NOI18N
        lblDescripcion.setForeground(new java.awt.Color(203, 213, 225));
        lblDescripcion.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDescripcion.setText("La colonia resistió.");

        lblResumen.setFont(new java.awt.Font("SansSerif", 0, 13)); // NOI18N
        lblResumen.setForeground(new java.awt.Color(94, 234, 212));
        lblResumen.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblResumen.setText("Núcleo: —    |    Defensas en pie: —    |    Enemigos destruidos: —");

        lblResultado.setFont(new java.awt.Font("Magneto", 1, 36)); // NOI18N
        lblResultado.setForeground(new java.awt.Color(94, 234, 212));
        lblResultado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblResultado.setText("VICTORIA");

        javax.swing.GroupLayout pnlResumenResultadosLayout = new javax.swing.GroupLayout(pnlResumenResultados);
        pnlResumenResultados.setLayout(pnlResumenResultadosLayout);
        pnlResumenResultadosLayout.setHorizontalGroup(
            pnlResumenResultadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblResumen, 0, 704, Short.MAX_VALUE)
            .addGroup(pnlResumenResultadosLayout.createSequentialGroup()
                .addGroup(pnlResumenResultadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlResumenResultadosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblDescripcion, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(lblResultado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlResumenResultadosLayout.setVerticalGroup(
            pnlResumenResultadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlResumenResultadosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblResultado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblResumen)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblDescripcion)
                .addContainerGap())
        );

        jResultados.add(pnlResumenResultados, java.awt.BorderLayout.NORTH);

        scrollResultados.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tablaResultados.setAutoCreateRowSorter(true);
        tablaResultados.setBackground(new java.awt.Color(24, 33, 48));
        tablaResultados.setFont(new java.awt.Font("SansSerif", 0, 13)); // NOI18N
        tablaResultados.setForeground(new java.awt.Color(226, 232, 240));
        tablaResultados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Unidad", "Tipo", "Estado", "Vida", "Daño infligido", "Daño recibido", "Golpes"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaResultados.setRowHeight(32);
        tablaResultados.setSelectionBackground(new java.awt.Color(55, 65, 81));
        tablaResultados.setSelectionForeground(new java.awt.Color(255, 255, 255));
        scrollResultados.setViewportView(tablaResultados);

        jResultados.add(scrollResultados, java.awt.BorderLayout.CENTER);

        pnlAccionesResultados.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 0, 0, 0));
        pnlAccionesResultados.setOpaque(false);
        pnlAccionesResultados.setLayout(new java.awt.BorderLayout(0, 12));

        lblAyudaResultados.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        lblAyudaResultados.setForeground(new java.awt.Color(148, 163, 184));
        lblAyudaResultados.setText("Ordene las estadísticas haciendo clic en el encabezado de una columna.");
        pnlAccionesResultados.add(lblAyudaResultados, java.awt.BorderLayout.NORTH);

        pnlBotonesResultados.setOpaque(false);
        pnlBotonesResultados.setLayout(new java.awt.GridLayout(1, 2, 12, 0));

        btnReiniciar.setBackground(new java.awt.Color(94, 234, 212));
        btnReiniciar.setFont(new java.awt.Font("Century Gothic", 1, 18)); // NOI18N
        btnReiniciar.setForeground(new java.awt.Color(0, 0, 0));
        btnReiniciar.setText("Reiniciar Mision");
        btnReiniciar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnReiniciar.setPreferredSize(new java.awt.Dimension(150, 42));
        btnReiniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarActionPerformed(evt);
            }
        });
        pnlBotonesResultados.add(btnReiniciar);

        btnAvanzar.setBackground(new java.awt.Color(94, 234, 212));
        btnAvanzar.setFont(new java.awt.Font("Century Gothic", 1, 18)); // NOI18N
        btnAvanzar.setForeground(new java.awt.Color(15, 23, 42));
        btnAvanzar.setText("Avanzar");
        btnAvanzar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnAvanzar.setPreferredSize(new java.awt.Dimension(150, 42));
        btnAvanzar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAvanzarActionPerformed(evt);
            }
        });
        pnlBotonesResultados.add(btnAvanzar);

        btnFinalizarCampana.setBackground(new java.awt.Color(94, 234, 212));
        btnFinalizarCampana.setFont(new java.awt.Font("Century Gothic", 1, 18)); // NOI18N
        btnFinalizarCampana.setForeground(new java.awt.Color(15, 23, 42));
        btnFinalizarCampana.setText("Finalizar campaña");
        btnFinalizarCampana.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnFinalizarCampana.setPreferredSize(new java.awt.Dimension(170, 42));
        btnFinalizarCampana.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizarCampanaActionPerformed(evt);
            }
        });
        pnlBotonesResultados.add(btnFinalizarCampana);

        pnlAccionesResultados.add(pnlBotonesResultados, java.awt.BorderLayout.SOUTH);

        jResultados.add(pnlAccionesResultados, java.awt.BorderLayout.SOUTH);

        panelContenedorMapa.setBackground(new java.awt.Color(36, 32, 29));
        panelContenedorMapa.setLayout(new java.awt.BorderLayout());

        pnlPausa.setBackground(new java.awt.Color(17, 24, 39));

        lblPausa.setForeground(new java.awt.Color(226, 232, 240));
        lblPausa.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPausa.setText("PARTIDA EN PAUSA");

        btnContinuar.setBackground(new java.awt.Color(94, 234, 212));
        btnContinuar.setForeground(new java.awt.Color(15, 23, 42));
        btnContinuar.setText("Continuar");
        btnContinuar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnContinuarActionPerformed(evt);
            }
        });

        btnGuardar.setBackground(new java.awt.Color(94, 234, 212));
        btnGuardar.setForeground(new java.awt.Color(15, 23, 42));
        btnGuardar.setText("Guardar partida");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnSalirPausa.setBackground(new java.awt.Color(94, 234, 212));
        btnSalirPausa.setForeground(new java.awt.Color(15, 23, 42));
        btnSalirPausa.setText("Salir");
        btnSalirPausa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirPausaActionPerformed(evt);
            }
        });

        lblFondoPausa.setBackground(new java.awt.Color(17, 24, 39));

        javax.swing.GroupLayout pnlPausaLayout = new javax.swing.GroupLayout(pnlPausa);
        pnlPausa.setLayout(pnlPausaLayout);
        pnlPausaLayout.setHorizontalGroup(
            pnlPausaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPausaLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(pnlPausaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lblPausa, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnContinuar, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSalirPausa, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(lblFondoPausa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlPausaLayout.setVerticalGroup(
            pnlPausaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPausaLayout.createSequentialGroup()
                .addGap(0, 201, Short.MAX_VALUE)
                .addComponent(lblPausa, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 40, 40)
                .addComponent(btnContinuar, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 25, 25)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 25, 25)
                .addComponent(btnSalirPausa, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 199, Short.MAX_VALUE))
            .addComponent(lblFondoPausa, 0, 700, Short.MAX_VALUE)
        );

        jLayeredPane1.setLayer(jResultados, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(panelContenedorMapa, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(pnlPausa, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jResultados, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelContenedorMapa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnlPausa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jResultados, 0, 700, Short.MAX_VALUE)
            .addComponent(panelContenedorMapa, 0, 700, Short.MAX_VALUE)
            .addComponent(pnlPausa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        getContentPane().add(jLayeredPane1, java.awt.BorderLayout.CENTER);

        pnlInfo.setBackground(new java.awt.Color(15, 23, 42));
        pnlInfo.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 20, 12, 20));
        pnlInfo.setLayout(new java.awt.BorderLayout());

        detalle.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        detalle.setForeground(new java.awt.Color(203, 213, 225));
        detalle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        detalle.setText("Seleccione una casilla para ver su contenido.");
        pnlInfo.add(detalle, java.awt.BorderLayout.CENTER);

        lblMision.setForeground(new java.awt.Color(148, 163, 184));
        lblMision.setText("MISIÓN 1 · Comandante");
        lblMision.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        pnlInfo.add(lblMision, java.awt.BorderLayout.LINE_END);

        btnPausa.setText("Pausa");
        btnPausa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPausaActionPerformed(evt);
            }
        });
        pnlInfo.add(btnPausa, java.awt.BorderLayout.WEST);

        getContentPane().add(pnlInfo, java.awt.BorderLayout.NORTH);

        pnlInventario.setBackground(new java.awt.Color(17, 24, 39));
        pnlInventario.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 12, 16, 12));
        pnlInventario.setPreferredSize(new java.awt.Dimension(280, 400));
        pnlInventario.setLayout(new java.awt.BorderLayout(0, 12));

        pnlControlesInventario.setOpaque(false);
        pnlControlesInventario.setLayout(new java.awt.GridLayout(6, 1, 0, 10));

        lblInventario.setForeground(new java.awt.Color(94, 234, 212));
        lblInventario.setText("DEFENSAS");
        pnlControlesInventario.add(lblInventario);
        pnlControlesInventario.add(comboInventario);

        lblCapacidad.setForeground(new java.awt.Color(226, 232, 240));
        lblCapacidad.setText("Capacidad: —");
        pnlControlesInventario.add(lblCapacidad);

        lblAyudaInventario.setForeground(new java.awt.Color(203, 213, 225));
        lblAyudaInventario.setText("<html>Clic izquierdo: colocar.<br>Clic derecho: retirar y liberar capacidad.<br>Al retirar pierde las mejoras.</html>");
        pnlControlesInventario.add(lblAyudaInventario);

        btnIniciar.setText("Iniciar misión");
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });
        pnlControlesInventario.add(btnIniciar);

        btnReiniciarEC.setText("Reiniciar misión");
        btnReiniciarEC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarECActionPerformed(evt);
            }
        });
        pnlControlesInventario.add(btnReiniciarEC);

        pnlInventario.add(pnlControlesInventario, java.awt.BorderLayout.NORTH);
        pnlInventario.add(panelDetalle, java.awt.BorderLayout.CENTER);

        getContentPane().add(pnlInventario, java.awt.BorderLayout.EAST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAvanzarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAvanzarActionPerformed
        avanzarMision();
    }//GEN-LAST:event_btnAvanzarActionPerformed

    private void btnFinalizarCampanaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarCampanaActionPerformed
        finalizarCampana();
    }//GEN-LAST:event_btnFinalizarCampanaActionPerformed

    private void btnReiniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReiniciarActionPerformed
        repetirMision();
    }//GEN-LAST:event_btnReiniciarActionPerformed

    private void btnReiniciarECActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReiniciarECActionPerformed
        repetirMision(); // Este boton es cuando la partida esta en curso, el mismo metodo funciona
    }//GEN-LAST:event_btnReiniciarECActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAvanzar;
    private javax.swing.JButton btnContinuar;
    private javax.swing.JButton btnFinalizarCampana;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnPausa;
    private javax.swing.JButton btnReiniciar;
    private javax.swing.JButton btnReiniciarEC;
    private javax.swing.JButton btnSalirPausa;
    private javax.swing.JComboBox comboInventario;
    private javax.swing.JLabel detalle;
    private javax.swing.JLayeredPane jLayeredPane1;
    private vista.PanelTranslucido jResultados;
    private javax.swing.JLabel lblAyudaInventario;
    private javax.swing.JLabel lblAyudaResultados;
    private javax.swing.JLabel lblCapacidad;
    private javax.swing.JLabel lblDescripcion;
    private javax.swing.JLabel lblFondoPausa;
    private javax.swing.JLabel lblInventario;
    private javax.swing.JLabel lblMision;
    private javax.swing.JLabel lblPausa;
    private javax.swing.JLabel lblResultado;
    private javax.swing.JLabel lblResumen;
    private javax.swing.JPanel panelContenedorMapa;
    private vista.PanelDetalle panelDetalle;
    private javax.swing.JPanel pnlAccionesResultados;
    private javax.swing.JPanel pnlBotonesResultados;
    private javax.swing.JPanel pnlControlesInventario;
    private javax.swing.JPanel pnlInfo;
    private javax.swing.JPanel pnlInventario;
    private vista.PanelTranslucido pnlPausa;
    private javax.swing.JPanel pnlResumenResultados;
    private javax.swing.JScrollPane scrollResultados;
    private javax.swing.JTable tablaResultados;
    // End of variables declaration//GEN-END:variables
}

