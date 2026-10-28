package vista;

import audio.Sonidos;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.ArrayList;
import modelo.juego.Partida;
import persistencia.Archivo;

/** El mismo formulario sirve para guardar o cargar; no deserializa partidas al listar. */
public class VentanaSlots extends javax.swing.JDialog {
    private final Partida paraGuardar;
    private Partida partidaCargada;
    private final ArrayList<String> rutas = new ArrayList<>();

    public VentanaSlots(java.awt.Frame propietario, Partida paraGuardar) {
        super(propietario, true);
        this.paraGuardar = paraGuardar;
        initComponents();

        Sonidos.configurarSonidosBTN(this);
        setTitle(paraGuardar == null ? "Cargar partida" : "Guardar partida");
        
        btnAceptar.setText(paraGuardar == null ? "Cargar" : "Guardar");
        tablaSlots.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaSlots.getTableHeader().setReorderingAllowed(false);
        setLocationRelativeTo(propietario);
        tablaSlots.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaSlots.getSelectedRow();
            // Nuevo Guardado es una acción, no un archivo que pueda eliminarse.
            btnEliminar.setEnabled(tablaSlots.isEnabled() && fila >= 0
                    && rutas.get(tablaSlots.convertRowIndexToModel(fila)) != null);
        });
        actualizarSlots();
    }

    public Partida getPartidaCargada() { return partidaCargada; }

    private void actualizarSlots() {
        javax.swing.table.DefaultTableModel datos = (javax.swing.table.DefaultTableModel) tablaSlots.getModel();
        tablaSlots.clearSelection();
        btnEliminar.setEnabled(false);
        datos.setRowCount(0);
        rutas.clear();
        if (paraGuardar != null) {
            rutas.add(null);
            datos.addRow(new Object[]{"Nuevo Guardado", "—", "—", "Nuevo"});
        }
        try {
            for (String ruta : Archivo.listarPartidas()) {
                rutas.add(ruta);
                // Lee solo la cabecera; una partida dañada no impide listar las demás.
                try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ruta))) {
                    String nombre = entrada.readUTF();
                    int mision = entrada.readInt();
                    datos.addRow(new Object[]{new java.io.File(ruta).getName(), nombre, mision, "Disponible"});
                } catch (IOException e) {
                    datos.addRow(new Object[]{new java.io.File(ruta).getName(), "Cabecera ilegible", "—", "Dañado"});
                }
            }
            lblIndicacion.setText(rutas.isEmpty() ? "No hay partidas guardadas." : "Seleccione un slot.");
            btnAceptar.setEnabled(!rutas.isEmpty());
        } catch (IOException e) {
            lblIndicacion.setText("No se pudo leer la carpeta de partidas.");
            btnAceptar.setEnabled(false);
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void btnAceptarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAceptarActionPerformed
        procesarSlotSeleccionado();
    }//GEN-LAST:event_btnAceptarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelarActionPerformed

    // Operaciones de la ventana. Se pueden invocar sin fabricar eventos de Swing.
    /** Valida la selección y confirma antes de iniciar la operación de archivos. */
    private void procesarSlotSeleccionado() {
        int fila = tablaSlots.getSelectedRow();
        if (fila < 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Seleccione un slot primero.");
            return;
        }
        // El orden visual cambia al ordenar columnas; convierte el índice al modelo.
        String ruta = rutas.get(tablaSlots.convertRowIndexToModel(fila));
        if (paraGuardar != null && ruta != null
                && javax.swing.JOptionPane.showConfirmDialog(this,
                    "¿Sobrescribir este guardado? Se reemplazará su contenido anterior.",
                    "Confirmar sobrescritura", javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE) != javax.swing.JOptionPane.YES_OPTION) return;
        btnAceptar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnCancelar.setEnabled(false);
        tablaSlots.setEnabled(false);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        lblIndicacion.setText(paraGuardar == null ? "Cargando..." : "Guardando...");
        // La lectura y escritura no bloquean el hilo de Swing.
        new javax.swing.SwingWorker<Partida, Void>() {
            @Override protected Partida doInBackground() throws Exception {
                Archivo archivo = new Archivo();
                if (paraGuardar == null) return archivo.cargarPartida(ruta);
                archivo.guardarPartida(paraGuardar, ruta);
                return null;
            }
            @Override protected void done() {
                try {
                    partidaCargada = get();
                    if (paraGuardar != null) javax.swing.JOptionPane.showMessageDialog(VentanaSlots.this,
                            ruta == null ? "Nuevo guardado creado correctamente." : "Partida sobrescrita correctamente.");
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    javax.swing.JOptionPane.showMessageDialog(VentanaSlots.this,
                            "No se pudo completar la operación: " + e.getCause().getMessage());
                } finally {
                    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                    btnAceptar.setEnabled(true);
                    btnCancelar.setEnabled(true);
                    tablaSlots.setEnabled(true);
                    int seleccion = tablaSlots.getSelectedRow();
                    btnEliminar.setEnabled(seleccion >= 0
                            && rutas.get(tablaSlots.convertRowIndexToModel(seleccion)) != null);
                    lblIndicacion.setText("Seleccione un slot.");
                }
            }
        }.execute();
    }

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarSlotSeleccionado();
    }//GEN-LAST:event_btnEliminarActionPerformed

    /** Confirma el archivo seleccionado y lo borra fuera del hilo de Swing. */
    private void eliminarSlotSeleccionado() {
        int fila = tablaSlots.getSelectedRow();
        if (fila < 0 || !tablaSlots.isEnabled()) return;
        String ruta = rutas.get(tablaSlots.convertRowIndexToModel(fila));
        if (ruta == null) return;
        if (javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + new java.io.File(ruta).getName() + "? Esta acción no se puede deshacer.",
                "Eliminar guardado", javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE) != javax.swing.JOptionPane.YES_OPTION) return;
        btnAceptar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnCancelar.setEnabled(false);
        tablaSlots.setEnabled(false);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        lblIndicacion.setText("Eliminando...");
        new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                Archivo.eliminarPartida(ruta);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    javax.swing.JOptionPane.showMessageDialog(VentanaSlots.this, "Guardado eliminado correctamente.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    javax.swing.JOptionPane.showMessageDialog(VentanaSlots.this,
                            "No se pudo eliminar el guardado: " + e.getCause().getMessage());
                } finally {
                    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                    btnCancelar.setEnabled(true);
                    tablaSlots.setEnabled(true);
                    // Vuelve a listar incluso si otro proceso había retirado el archivo.
                    actualizarSlots();
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblIndicacion = new javax.swing.JLabel();
        scrollSlots = new javax.swing.JScrollPane();
        tablaSlots = new javax.swing.JTable();
        pnlAcciones = new javax.swing.JPanel();
        btnAceptar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Partidas guardadas");
        setModal(true);
        setPreferredSize(new java.awt.Dimension(650, 380));

        lblIndicacion.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblIndicacion.setText("Seleccione un slot.");
        getContentPane().add(lblIndicacion, java.awt.BorderLayout.NORTH);

        tablaSlots.setRowHeight(30);
        tablaSlots.setAutoCreateRowSorter(true);
        tablaSlots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Slot", "Comandante", "Misión", "Estado"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scrollSlots.setViewportView(tablaSlots);

        getContentPane().add(scrollSlots, java.awt.BorderLayout.CENTER);

        
        btnAceptar.setText("Aceptar");
        btnAceptar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAceptarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnAceptar);

        
        btnEliminar.setText("Eliminar");
        btnEliminar.setEnabled(false);
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnEliminar);

        
        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnCancelar);

        getContentPane().add(pnlAcciones, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAceptar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JLabel lblIndicacion;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JScrollPane scrollSlots;
    private javax.swing.JTable tablaSlots;
    // End of variables declaration//GEN-END:variables
}
