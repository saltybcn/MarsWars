package vista;

import audio.Sonidos;
import configuracion.Auth;
import configuracion.Catalogo;
import configuracion.Configuracion;
import persistencia.Archivo;

public class VentanaAdmin extends javax.swing.JFrame {
    private static final long serialVersionUID = 1L;
    private Catalogo catalogo = new Catalogo();
    private Auth autenticacion;
    private static final String RUTA_CATALOGO = "src/main/java/persistencia/catalogo.json";
    private boolean cargaFallida; // Impide sobrescribir un archivo que no se pudo recuperar.
    private String idSeleccionado; // La edición conserva la identidad de la plantilla

    public VentanaAdmin() {
        initComponents();

        Sonidos.configurarSonidosBTN(this);
        // Desplaza el formulario sin comprimir los campos ni alterar su diseño.
        scrollCrear.getVerticalScrollBar().setUnitIncrement(16);
        scrollCrear.getHorizontalScrollBar().setUnitIncrement(16);
        scrollCrear.getViewport().setBackground(pnlCrear.getBackground());
        // Los spinners guardan numeros
        spinnerVida.setModel(new javax.swing.SpinnerNumberModel(100, 1, Integer.MAX_VALUE, 1));
        spinnerDmg.setModel(new javax.swing.SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        spinnerAlcance.setModel(new javax.swing.SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        spinnerCapacidad.setModel(new javax.swing.SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        spinnerMision.setModel(new javax.swing.SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        spinnerIntervalo.setModel(new javax.swing.SpinnerNumberModel(1.0, 0.0, Integer.MAX_VALUE / 1000.0, 0.1));
        spinnerMovimiento.setModel(new javax.swing.SpinnerNumberModel(0.5, 0.001, Integer.MAX_VALUE / 1000.0, 0.1));
        spinnerMaxObjetivos.setModel(new javax.swing.SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        // El editor se crea de nuevo al cambiar el modelo del spinner.
        for (javax.swing.JSpinner spinner : new javax.swing.JSpinner[]{spinnerVida, spinnerDmg,
                spinnerAlcance, spinnerCapacidad, spinnerMision, spinnerIntervalo, spinnerMaxObjetivos, spinnerMovimiento}) {
            javax.swing.JFormattedTextField editor = ((javax.swing.JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            // Evita que Nimbus pinta un fondo claro al deshabilitar el editor.
            editor.setUI(new javax.swing.plaf.basic.BasicFormattedTextFieldUI());
            editor.setOpaque(true);
            editor.setBackground(new java.awt.Color(24, 33, 48));
            editor.setForeground(new java.awt.Color(226, 232, 240));
            editor.setDisabledTextColor(new java.awt.Color(156, 163, 175));
            editor.setCaretColor(java.awt.Color.WHITE);
            editor.setSelectionColor(new java.awt.Color(55, 65, 81));
            editor.setSelectedTextColor(java.awt.Color.WHITE);
        }
        for (javax.swing.JTextField campo : new javax.swing.JTextField[]{txtNombre, txtImgIdle, txtImgAtq, txtImgMov, txtSonidoAtacando, txtSonidoDestruido}) {
            campo.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
            campo.setOpaque(true);
        }
        comboTipo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        comboSub.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        // Incluye alternar estado para conservar el mismo fondo al deshabilitar los botones.
        for (javax.swing.JButton boton : new javax.swing.JButton[]{btnCrear, btnNuevo, btnEliminar, btnActivar, btnImgIdle, btnImgAtq, btnImgMov, btnSonidoAtacando, btnSonidoDestruido}) {
            boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            boton.setOpaque(true);
        }
        javax.swing.DefaultListCellRenderer opciones = new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista,
                    Object valor, int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                setForeground(new java.awt.Color(226, 232, 240));
                setBackground(seleccionado ? new java.awt.Color(55, 65, 81) : new java.awt.Color(24, 33, 48));
                return this;
            }
        };
        comboTipo.setRenderer(opciones);
        comboSub.setRenderer(opciones);
        tablaCatalogo.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(javax.swing.JTable tabla,
                    Object valor, boolean seleccionado, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
                setForeground(new java.awt.Color(226, 232, 240));
                setBackground(new java.awt.Color(24, 33, 48));
                setFont(tabla.getTableHeader().getFont());
                return this;
            }
        });
        scrollCatalogo.setColumnHeaderView(tablaCatalogo.getTableHeader());
        scrollCatalogo.getViewport().setBackground(tablaCatalogo.getBackground());
        tablaCatalogo.setFillsViewportHeight(true);
        tablaCatalogo.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaCatalogo.getSelectionModel().addListSelectionListener(this::tablaCatalogoSeleccionada);
        txtSonidoAtacando.setToolTipText("WAV PCM opcional. Deje vacío para utilizar el efecto genérico.");
        txtSonidoDestruido.setToolTipText("WAV PCM opcional. Deje vacío para utilizar el efecto genérico.");
        actualizarSubtipos(); // Carga las defensas al abrir la ventana
        try {
            setCatalogo(new Archivo().cargarCatalogo(RUTA_CATALOGO));
        } catch (java.io.UncheckedIOException | IllegalArgumentException e) {
            cargaFallida = true;
            btnCrear.setEnabled(false);
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No se pudo cargar el catálogo. El archivo se conserva sin cambios.\n"
                    + e.getMessage() + "\nCorrige el archivo y vuelve a abrir la ventana.",
                    "Error al cargar", javax.swing.JOptionPane.ERROR_MESSAGE);
            actualizarCatalogo();
        }
        pack(); // Recalcula con las filas de recursos limitadas y los textos finales.
        // Mantiene la ventana inicial dentro de la pantalla; el scroll permite llegar al resto.
        java.awt.Rectangle area = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(getWidth(), area.width), Math.min(getHeight(), area.height));
        setLocationRelativeTo(null);
    }

    public void mostrar() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(this::mostrar); return;
        }
        if (autenticacion == null) autenticacion = AccesoAdmin.solicitar(this);
        if (autenticacion != null) setVisible(true);
        else dispose();
    }

    public void actualizarCatalogo() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(this::actualizarCatalogo);
            return;
        }
        javax.swing.table.DefaultTableModel datos = new javax.swing.table.DefaultTableModel(
                new String[]{"Nombre", "Tipo", "Vida", "Daño", "Alcance", "Intervalo (s)",
                    "Capacidad", "Misión", "Máx. objetivos", "Ataca aéreos", "Estado", "ID"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
            @Override public Class<?> getColumnClass(int columna) {
                if (columna == 5) return Double.class;
                if (columna == 9) return Boolean.class;
                return columna >= 2 && columna <= 8 ? Integer.class : String.class;
            }
        };
        for (Configuracion config : catalogo.getConfigs()) {
            datos.addRow(new Object[]{config.getNombre(), config.getTipo(), config.getVida(),
                config.getDmg(), config.getAlcance(), config.getFrecuenciaAtaque(),
                config.getCostoCapacidad(), config.getMisionMinima(), config.getMaxObjetivos(),
                config.isAtacaAereos(), config.isActivo() ? "Activa" : "Inactiva", config.getId()});
        }
        tablaCatalogo.setModel(datos);
        // El ID permanece en el modelo aunque el usuario ordene las columnas o filas.
        tablaCatalogo.removeColumn(tablaCatalogo.getColumnModel().getColumn(11));
        javax.swing.table.DefaultTableCellRenderer celdas = new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(javax.swing.JTable tabla,
                    Object valor, boolean seleccionado, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
                setForeground(tabla.getForeground());
                setBackground(seleccionado ? tabla.getSelectionBackground() : tabla.getBackground());
                if (valor instanceof Boolean) setText(Boolean.TRUE.equals(valor) ? "Sí" : "No");
                return this;
            }
        };
        for (int columna = 0; columna < tablaCatalogo.getColumnCount(); columna++) {
            tablaCatalogo.getColumnModel().getColumn(columna).setCellRenderer(celdas);
        }
        tablaCatalogo.setAutoCreateRowSorter(true);
        tablaCatalogo.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        for (int columna = 0; columna < tablaCatalogo.getColumnCount(); columna++) {
            tablaCatalogo.getColumnModel().getColumn(columna).setPreferredWidth(columna < 2 ? 140 : 110);
        }
    }

    public Catalogo getCatalogo() { return catalogo; }

    public void setCatalogo(Catalogo catalogo) {
        this.catalogo = java.util.Objects.requireNonNull(catalogo);
        nuevaConfiguracion();
        actualizarCatalogo();
    }

    public Auth getAutenticacion() { return autenticacion; }

    public void setAutenticacion(Auth autenticacion) {
        this.autenticacion = autenticacion;
    }

    // Operaciones de la ventana. Se pueden invocar sin fabricar eventos de Swing.
    /** Ajusta los tipos concretos a la categoría seleccionada. */
    private void actualizarSubtipos() {
        String[] tipos = "Atacante".equals(comboTipo.getSelectedItem())
                ? new String[]{"Acechador", "Escupidor", "Enjambre", "Demoledor", "Volador"}
                : new String[]{"Contacto", "AlcanceMedio", "AtaqueMultiple", "Impacto", "Dron", "Barrera"};
        comboSub.setModel(new javax.swing.DefaultComboBoxModel<>(tipos));
        comboSub.setEnabled(true);
        actualizarCamposPorTipo();
    }

    /** Aplica las restricciones de edición propias del tipo concreto. */
    private void actualizarCamposPorTipo() {
        String tipo = (String) comboSub.getSelectedItem();
        boolean barrera = "Barrera".equals(tipo);
        boolean contacto = "Contacto".equals(tipo) || "Acechador".equals(tipo);
        // Las restricciones visibles coinciden con Configuracion.validar().
        spinnerAlcance.setEnabled(!barrera && !contacto);
        if (barrera || contacto) spinnerAlcance.setValue(barrera ? 0 : 1);
        else if (((Number) spinnerAlcance.getValue()).intValue() == 0) spinnerAlcance.setValue(1);
        spinnerDmg.setEnabled(!barrera);
        spinnerIntervalo.setEnabled(!barrera);
        checkAtacaAereos.setEnabled(!barrera);
        if (barrera) {
            spinnerDmg.setValue(0);
            spinnerIntervalo.setValue(0.0);
            checkAtacaAereos.setSelected(false);
        } else if (((Number) spinnerIntervalo.getValue()).doubleValue() == 0) {
            spinnerIntervalo.setValue(1.0);
        }
        spinnerMaxObjetivos.setEnabled("AtaqueMultiple".equals(tipo) || "Enjambre".equals(tipo));
        txtImgMov.setEnabled("Atacante".equals(comboTipo.getSelectedItem()) || "Dron".equals(tipo));
        spinnerMovimiento.setEnabled(txtImgMov.isEnabled());
        txtImgAtq.setEnabled(!barrera);
        btnImgAtq.setEnabled(txtImgAtq.isEnabled());
        btnImgMov.setEnabled(txtImgMov.isEnabled());
        txtSonidoAtacando.setEnabled(!barrera);
        btnSonidoAtacando.setEnabled(!barrera);
    }

    /** Valida los campos antes de modificar y persistir el catálogo. */
    private void guardarConfiguracion() {
        if (cargaFallida) return;
        try {
            // Confirma el texto del editor antes de leer el valor del spinner.
            for (javax.swing.JSpinner spinner : new javax.swing.JSpinner[]{spinnerVida, spinnerDmg,
                    spinnerAlcance, spinnerCapacidad, spinnerMision, spinnerIntervalo, spinnerMaxObjetivos, spinnerMovimiento}) {
                if (spinner.isEnabled()) spinner.commitEdit();
            }
            // También comprueba rutas que se escribieron o cargaron sin usar el selector.
            for (javax.swing.JTextField campo : new javax.swing.JTextField[]{txtImgIdle, txtImgAtq, txtImgMov}) {
                String ruta = campo.getText().strip();
                if (campo.isEnabled() && !ruta.isEmpty()) {
                    // Acepta también los recursos del JAR, además de las copias y los archivos externos.
                    if (modelo.componentes.Componente.cargarImagen(ruta) == null) {
                        javax.swing.JOptionPane.showMessageDialog(this,
                                "No se puede cargar la imagen: " + ruta, "Imagen inválida",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            // Una ruta vacía conserva el efecto genérico; comprueba también las rutas escritas a mano.
            for (javax.swing.JTextField campo : new javax.swing.JTextField[]{txtSonidoAtacando, txtSonidoDestruido}) {
                if (campo.isEnabled() && !campo.getText().isBlank()) audio.Sonidos.validar(campo.getText().strip());
            }
            Configuracion config = new Configuracion(txtNombre.getText().strip(),
                    (String) comboSub.getSelectedItem(),
                    ((Number) spinnerVida.getValue()).intValue(),
                    ((Number) spinnerDmg.getValue()).intValue(),
                    ((Number) spinnerIntervalo.getValue()).doubleValue(),
                    ((Number) spinnerAlcance.getValue()).intValue(),
                    ((Number) spinnerCapacidad.getValue()).intValue(),
                    ((Number) spinnerMision.getValue()).intValue());
            config.setIntervaloMovimiento(spinnerMovimiento.isEnabled() ? ((Number) spinnerMovimiento.getValue()).doubleValue() : 0.5);
            config.setMaxObjetivos(spinnerMaxObjetivos.isEnabled()
                    ? ((Number) spinnerMaxObjetivos.getValue()).intValue() : 0);
            config.setAtacaAereos(checkAtacaAereos.isEnabled() && checkAtacaAereos.isSelected());
            config.setRutaIdle(txtImgIdle.getText().strip());
            config.setRutaAtaque(txtImgAtq.isEnabled() ? txtImgAtq.getText().strip() : "");
            config.setRutaMovimiento(txtImgMov.isEnabled() ? txtImgMov.getText().strip() : "");
            config.setSonidoAtacando(txtSonidoAtacando.isEnabled() ? txtSonidoAtacando.getText().strip() : "");
            config.setSonidoDestruido(txtSonidoDestruido.getText().strip());
            config.setAtaqueSilenciado(!checkSonidoAtacando.isSelected());
            config.setDestruccionSilenciada(!checkSonidoDestruido.isSelected());
            if (!config.validar()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Revisá el nombre, tipo, vida, daño, intervalo, alcance, capacidad, misión y máximo de objetivos.",
                        "Configuración inválida", javax.swing.JOptionPane.WARNING_MESSAGE);
                return; // No modifica el catálogo cuando hay datos inválidos
            }
            if (idSeleccionado == null) {
                catalogo.agregar(config);
            } else {
                // Se valida una copia; un error no altera el objeto ya guardado.
                Configuracion anterior = catalogo.consultar(idSeleccionado);
                if (anterior == null) throw new IllegalArgumentException("La configuración ya no existe");
                config.setId(idSeleccionado);
                config.setActivo(anterior.isActivo());
                catalogo.modificar(config);
            }
            nuevaConfiguracion();
            actualizarCatalogo();
            new Archivo().guardarCatalogo(catalogo, RUTA_CATALOGO);
            javax.swing.JOptionPane.showMessageDialog(this, "Configuración guardada en el catálogo.");
        } catch (java.io.UncheckedIOException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "El cambio está en memoria, pero no se pudo guardar en un archivo.\n" + e.getMessage(),
                    "Error al guardar", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (java.text.ParseException | IllegalArgumentException | java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No se pudo guardar la configuración. Revise los valores, las imágenes y los WAV.\n" + e.getMessage(),
                    "Datos inválidos", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Limpia la selección y restablece los valores de creación. */
    private void nuevaConfiguracion() {
        idSeleccionado = null;
        lblCrear.setText("CREAR UNIDAD");
        tablaCatalogo.clearSelection();
        btnCrear.setText("CREAR");
        btnEliminar.setEnabled(false);
        btnActivar.setEnabled(false);
        txtNombre.setText("");
        spinnerMovimiento.setValue(0.5);
        txtImgIdle.setText("");
        txtImgAtq.setText("");
        txtImgMov.setText("");
        txtSonidoAtacando.setText("");
        txtSonidoDestruido.setText("");
        checkSonidoAtacando.setSelected(true);
        checkSonidoDestruido.setSelected(true);
        comboTipo.setSelectedItem("Defensa");
        comboSub.setSelectedItem("Contacto");
        spinnerVida.setValue(100);
        spinnerDmg.setValue(10);
        spinnerAlcance.setValue(1);
        spinnerCapacidad.setValue(1);
        spinnerMision.setValue(1);
        spinnerIntervalo.setValue(1.0);
        spinnerMaxObjetivos.setValue(1);
        checkAtacaAereos.setSelected(false);
    }

    /** Confirma la eliminación antes de modificar el catálogo. */
    private void eliminarConfiguracion() {
        if (cargaFallida || idSeleccionado == null) return;
        int respuesta = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Eliminar esta configuración del catálogo?", "Eliminar configuración",
                javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        if (respuesta != javax.swing.JOptionPane.YES_OPTION) return;
        catalogo.eliminar(idSeleccionado);
        nuevaConfiguracion();
        actualizarCatalogo();
        btnEliminar.setEnabled(false);
        btnActivar.setEnabled(false);
        try {
            new Archivo().guardarCatalogo(catalogo, RUTA_CATALOGO);
        } catch (java.io.UncheckedIOException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Se eliminó de memoria, pero no se pudo actualizar el JSON.\n" + e.getMessage(),
                    "Error al guardar", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Cambia el estado de la configuración seleccionada y refresque la tabla. */
    private void alternarEstadoConfiguracion() {
        if (cargaFallida || idSeleccionado == null) return;
        catalogo.alternarActivo(idSeleccionado);
        if(catalogo.consultar(idSeleccionado).isActivo()) btnActivar.setText("DESACTIVAR");
        else btnActivar.setText("ACTIVAR");
        actualizarCatalogo();
    }

    /** Código administrado por el diseñador de NetBeans. Editar desde Design. */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlDividido = new javax.swing.JSplitPane();
        scrollCrear = new javax.swing.JScrollPane();
        pnlCrear = new javax.swing.JPanel();
        lblCrear = new javax.swing.JLabel();
        pnlConfig = new javax.swing.JPanel();
        pnlNombre = new javax.swing.JPanel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        pnlTipo = new javax.swing.JPanel();
        lblTipo = new javax.swing.JLabel();
        comboTipo = new javax.swing.JComboBox<>();
        comboSub = new javax.swing.JComboBox<>();
        pnlVida = new javax.swing.JPanel();
        lblVida = new javax.swing.JLabel();
        spinnerVida = new javax.swing.JSpinner();
        pnlDmg = new javax.swing.JPanel();
        lblDmg = new javax.swing.JLabel();
        spinnerDmg = new javax.swing.JSpinner();
        pnlMovimiento = new javax.swing.JPanel();
        lblMovimiento = new javax.swing.JLabel();
        spinnerMovimiento = new javax.swing.JSpinner();
        pnlVelAtq = new javax.swing.JPanel();
        lblIntervalo = new javax.swing.JLabel();
        spinnerIntervalo = new javax.swing.JSpinner();
        pnlAlcance = new javax.swing.JPanel();
        lblAlcance = new javax.swing.JLabel();
        spinnerAlcance = new javax.swing.JSpinner();
        pnlMaxObjetivos = new javax.swing.JPanel();
        lblMaxObjetivos = new javax.swing.JLabel();
        spinnerMaxObjetivos = new javax.swing.JSpinner();
        pnlCosto = new javax.swing.JPanel();
        lblCosto = new javax.swing.JLabel();
        spinnerCapacidad = new javax.swing.JSpinner();
        pnlMision = new javax.swing.JPanel();
        lblMision = new javax.swing.JLabel();
        spinnerMision = new javax.swing.JSpinner();
        pnlAtacaAereos = new javax.swing.JPanel();
        lblAtacaAereos = new javax.swing.JLabel();
        checkAtacaAereos = new javax.swing.JCheckBox();
        pnlImgIdle = new javax.swing.JPanel();
        lblImgIdle = new javax.swing.JLabel();
        txtImgIdle = new javax.swing.JTextField();
        btnImgIdle = new javax.swing.JButton();
        pnlImgAtq = new javax.swing.JPanel();
        lblImgAtq = new javax.swing.JLabel();
        txtImgAtq = new javax.swing.JTextField();
        btnImgAtq = new javax.swing.JButton();
        pnlImgMov = new javax.swing.JPanel();
        lblImgMov = new javax.swing.JLabel();
        txtImgMov = new javax.swing.JTextField();
        btnImgMov = new javax.swing.JButton();
        pnlSonidoAtacando = new javax.swing.JPanel();
        lblSonidoAtacando = new javax.swing.JLabel();
        txtSonidoAtacando = new javax.swing.JTextField();
        btnSonidoAtacando = new javax.swing.JButton();
        checkSonidoAtacando = new javax.swing.JCheckBox();
        pnlSonidoDestruido = new javax.swing.JPanel();
        lblSonidoDestruido = new javax.swing.JLabel();
        txtSonidoDestruido = new javax.swing.JTextField();
        btnSonidoDestruido = new javax.swing.JButton();
        checkSonidoDestruido = new javax.swing.JCheckBox();
        btnCrear = new javax.swing.JButton();
        pnlVer = new javax.swing.JPanel();
        scrollCatalogo = new javax.swing.JScrollPane();
        tablaCatalogo = new javax.swing.JTable();
        pnlAcciones = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnActivar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Colonia Marte - Administrador");
        setResizable(true);

        pnlDividido.setBackground(new java.awt.Color(0, 102, 102));
        pnlDividido.setDividerLocation(900);
        pnlDividido.setResizeWeight(0.65);

        scrollCrear.setBackground(new java.awt.Color(17, 24, 39));
        scrollCrear.setMinimumSize(new java.awt.Dimension(250, 200));
        scrollCrear.setPreferredSize(new java.awt.Dimension(900, 700));
        scrollCrear.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollCrear.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        pnlCrear.setBackground(new java.awt.Color(17, 24, 39));
        pnlCrear.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnlCrear.setMinimumSize(new java.awt.Dimension(500, 0));

        lblCrear.setFont(new java.awt.Font("CaskaydiaCove NF", 1, 48)); // NOI18N
        lblCrear.setForeground(new java.awt.Color(226, 232, 240));
        lblCrear.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCrear.setText("CREAR UNIDAD");

        pnlConfig.setBackground(new java.awt.Color(17, 24, 39));
        pnlConfig.setLayout(new java.awt.GridLayout(0, 1));

        pnlNombre.setBackground(new java.awt.Color(17, 24, 39));
        pnlNombre.setLayout(new java.awt.GridLayout(1, 0));

        lblNombre.setBackground(new java.awt.Color(17, 24, 39));
        lblNombre.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblNombre.setForeground(new java.awt.Color(226, 232, 240));
        lblNombre.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblNombre.setText("Nombre");
        pnlNombre.add(lblNombre);

        txtNombre.setForeground(new java.awt.Color(226, 232, 240));
        txtNombre.setBackground(new java.awt.Color(24, 33, 48));
        txtNombre.setCaretColor(new java.awt.Color(226, 232, 240));
        txtNombre.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtNombre.setSelectionColor(new java.awt.Color(55, 65, 81));
        txtNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNombreActionPerformed(evt);
            }
        });
        pnlNombre.add(txtNombre);

        pnlConfig.add(pnlNombre);

        pnlTipo.setBackground(new java.awt.Color(17, 24, 39));
        pnlTipo.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        lblTipo.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblTipo.setForeground(new java.awt.Color(226, 232, 240));
        lblTipo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTipo.setText("Tipo");
        pnlTipo.add(lblTipo);

        comboTipo.setBackground(new java.awt.Color(24, 33, 48));
        comboTipo.setForeground(new java.awt.Color(0, 0, 0));
        comboTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Defensa", "Atacante" }));
        comboTipo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        comboTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTipoActionPerformed(evt);
            }
        });
        pnlTipo.add(comboTipo);

        comboSub.setBackground(new java.awt.Color(24, 33, 48));
        comboSub.setForeground(new java.awt.Color(0, 0, 0));
        comboSub.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        comboSub.setEnabled(false);
        comboSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboSubActionPerformed(evt);
            }
        });
        pnlTipo.add(comboSub);

        pnlConfig.add(pnlTipo);

        pnlVida.setBackground(new java.awt.Color(17, 24, 39));
        pnlVida.setAlignmentX(1.0F);
        pnlVida.setLayout(new java.awt.GridLayout(1, 0));

        lblVida.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblVida.setForeground(new java.awt.Color(226, 232, 240));
        lblVida.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblVida.setText("Vida");
        pnlVida.add(lblVida);
        pnlVida.add(spinnerVida);

        pnlConfig.add(pnlVida);

        pnlDmg.setBackground(new java.awt.Color(17, 24, 39));
        pnlDmg.setAlignmentX(1.0F);
        pnlDmg.setLayout(new java.awt.GridLayout(1, 0));

        lblDmg.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblDmg.setForeground(new java.awt.Color(226, 232, 240));
        lblDmg.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblDmg.setText("Daño");
        pnlDmg.add(lblDmg);
        pnlDmg.add(spinnerDmg);

        pnlConfig.add(pnlDmg);

        pnlVelAtq.setBackground(new java.awt.Color(17, 24, 39));
        pnlVelAtq.setAlignmentX(1.0F);
        pnlVelAtq.setLayout(new java.awt.GridLayout(1, 0));

        lblIntervalo.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblIntervalo.setForeground(new java.awt.Color(226, 232, 240));
        lblIntervalo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblIntervalo.setText("Intervalo entre ataques (segundos)");
        pnlVelAtq.add(lblIntervalo);
        pnlVelAtq.add(spinnerIntervalo);

        pnlConfig.add(pnlVelAtq);

        pnlMovimiento.setBackground(new java.awt.Color(17, 24, 39));
        pnlMovimiento.setAlignmentX(1.0F);
        pnlMovimiento.setLayout(new java.awt.GridLayout(1, 0));

        lblMovimiento.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblMovimiento.setForeground(new java.awt.Color(226, 232, 240));
        lblMovimiento.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblMovimiento.setText("Intervalo entre pasos (segundos)");
        pnlMovimiento.add(lblMovimiento);
        pnlMovimiento.add(spinnerMovimiento);

        pnlConfig.add(pnlMovimiento);

        pnlAlcance.setBackground(new java.awt.Color(17, 24, 39));
        pnlAlcance.setAlignmentX(1.0F);
        pnlAlcance.setLayout(new java.awt.GridLayout(1, 0));

        lblAlcance.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblAlcance.setForeground(new java.awt.Color(226, 232, 240));
        lblAlcance.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblAlcance.setText("Alcance");
        pnlAlcance.add(lblAlcance);

        spinnerAlcance.setForeground(new java.awt.Color(226, 232, 240));
        spinnerAlcance.setBackground(new java.awt.Color(24, 33, 48));
        pnlAlcance.add(spinnerAlcance);

        pnlConfig.add(pnlAlcance);

        pnlMaxObjetivos.setBackground(new java.awt.Color(17, 24, 39));
        pnlMaxObjetivos.setAlignmentX(1.0F);
        pnlMaxObjetivos.setLayout(new java.awt.GridLayout(1, 0));

        lblMaxObjetivos.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblMaxObjetivos.setForeground(new java.awt.Color(226, 232, 240));
        lblMaxObjetivos.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblMaxObjetivos.setText("Máximo de objetivos");
        pnlMaxObjetivos.add(lblMaxObjetivos);

        spinnerMaxObjetivos.setForeground(new java.awt.Color(226, 232, 240));
        spinnerMaxObjetivos.setBackground(new java.awt.Color(24, 33, 48));
        pnlMaxObjetivos.add(spinnerMaxObjetivos);

        pnlConfig.add(pnlMaxObjetivos);

        pnlCosto.setBackground(new java.awt.Color(17, 24, 39));
        pnlCosto.setAlignmentX(1.0F);
        pnlCosto.setLayout(new java.awt.GridLayout(1, 0));

        lblCosto.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblCosto.setForeground(new java.awt.Color(226, 232, 240));
        lblCosto.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblCosto.setText("Costo de capacidad");
        pnlCosto.add(lblCosto);

        spinnerCapacidad.setForeground(new java.awt.Color(226, 232, 240));
        spinnerCapacidad.setBackground(new java.awt.Color(24, 33, 48));
        pnlCosto.add(spinnerCapacidad);

        pnlConfig.add(pnlCosto);

        pnlMision.setBackground(new java.awt.Color(17, 24, 39));
        pnlMision.setAlignmentX(1.0F);
        pnlMision.setLayout(new java.awt.GridLayout(1, 0));

        lblMision.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblMision.setForeground(new java.awt.Color(226, 232, 240));
        lblMision.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblMision.setText("Mision de obtencion");
        pnlMision.add(lblMision);

        spinnerMision.setForeground(new java.awt.Color(226, 232, 240));
        spinnerMision.setBackground(new java.awt.Color(24, 33, 48));
        pnlMision.add(spinnerMision);

        pnlConfig.add(pnlMision);

        pnlAtacaAereos.setBackground(new java.awt.Color(17, 24, 39));
        pnlAtacaAereos.setLayout(new java.awt.GridLayout(1, 0));

        lblAtacaAereos.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblAtacaAereos.setForeground(new java.awt.Color(226, 232, 240));
        lblAtacaAereos.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblAtacaAereos.setText("Puede atacar aéreos");
        pnlAtacaAereos.add(lblAtacaAereos);

        checkAtacaAereos.setBackground(new java.awt.Color(24, 33, 48));
        checkAtacaAereos.setForeground(new java.awt.Color(226, 232, 240));
        pnlAtacaAereos.add(checkAtacaAereos);

        pnlConfig.add(pnlAtacaAereos);

        pnlImgIdle.setBackground(new java.awt.Color(17, 24, 39));
        pnlImgIdle.setLayout(new java.awt.GridLayout());

        lblImgIdle.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblImgIdle.setForeground(new java.awt.Color(226, 232, 240));
        lblImgIdle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblImgIdle.setText("Imagen");
        lblImgIdle.setToolTipText("Ruta de la imagen");
        pnlImgIdle.add(lblImgIdle);

        txtImgIdle.setBackground(new java.awt.Color(24, 33, 48));
        txtImgIdle.setColumns(16);
        txtImgIdle.setForeground(new java.awt.Color(226, 232, 240));
        txtImgIdle.setCaretColor(new java.awt.Color(226, 232, 240));
        txtImgIdle.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtImgIdle.setSelectionColor(new java.awt.Color(55, 65, 81));
        pnlImgIdle.add(txtImgIdle);

        btnImgIdle.setBackground(new java.awt.Color(24, 33, 48));
        btnImgIdle.setForeground(new java.awt.Color(226, 232, 240));
        btnImgIdle.setText("Buscar…");
        btnImgIdle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImgIdleActionPerformed(evt);
            }
        });
        pnlImgIdle.add(btnImgIdle);

        pnlConfig.add(pnlImgIdle);

        pnlImgAtq.setBackground(new java.awt.Color(17, 24, 39));
        pnlImgAtq.setLayout(new java.awt.GridLayout());

        lblImgAtq.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblImgAtq.setForeground(new java.awt.Color(226, 232, 240));
        lblImgAtq.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblImgAtq.setText("Imagen de ataque");
        lblImgAtq.setToolTipText("Ruta de la imagen de ataque");
        pnlImgAtq.add(lblImgAtq);

        txtImgAtq.setBackground(new java.awt.Color(24, 33, 48));
        txtImgAtq.setColumns(16);
        txtImgAtq.setForeground(new java.awt.Color(226, 232, 240));
        txtImgAtq.setCaretColor(new java.awt.Color(226, 232, 240));
        txtImgAtq.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtImgAtq.setSelectionColor(new java.awt.Color(55, 65, 81));
        pnlImgAtq.add(txtImgAtq);

        btnImgAtq.setBackground(new java.awt.Color(24, 33, 48));
        btnImgAtq.setForeground(new java.awt.Color(226, 232, 240));
        btnImgAtq.setText("Buscar…");
        btnImgAtq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImgAtqActionPerformed(evt);
            }
        });
        pnlImgAtq.add(btnImgAtq);

        pnlConfig.add(pnlImgAtq);

        pnlImgMov.setBackground(new java.awt.Color(17, 24, 39));
        pnlImgMov.setLayout(new java.awt.GridLayout());

        lblImgMov.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lblImgMov.setForeground(new java.awt.Color(226, 232, 240));
        lblImgMov.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblImgMov.setText("Imagen de movimiento");
        lblImgMov.setToolTipText("Ruta de la imagen de movimiento");
        pnlImgMov.add(lblImgMov);

        txtImgMov.setBackground(new java.awt.Color(24, 33, 48));
        txtImgMov.setColumns(16);
        txtImgMov.setForeground(new java.awt.Color(226, 232, 240));
        txtImgMov.setCaretColor(new java.awt.Color(226, 232, 240));
        txtImgMov.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtImgMov.setEnabled(false);
        txtImgMov.setSelectionColor(new java.awt.Color(55, 65, 81));
        pnlImgMov.add(txtImgMov);

        btnImgMov.setBackground(new java.awt.Color(24, 33, 48));
        btnImgMov.setForeground(new java.awt.Color(226, 232, 240));
        btnImgMov.setText("Buscar…");
        btnImgMov.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImgMovActionPerformed(evt);
            }
        });
        pnlImgMov.add(btnImgMov);

        pnlConfig.add(pnlImgMov);

        pnlSonidoAtacando.setBackground(new java.awt.Color(17, 24, 39));
        pnlSonidoAtacando.setLayout(new java.awt.GridLayout());

        lblSonidoAtacando.setFont(new java.awt.Font("sansserif", 0, 13)); // NOI18N
        lblSonidoAtacando.setForeground(new java.awt.Color(226, 232, 240));
        lblSonidoAtacando.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSonidoAtacando.setText("Sonido de ataque");
        pnlSonidoAtacando.add(lblSonidoAtacando);

        txtSonidoAtacando.setBackground(new java.awt.Color(24, 33, 48));
        txtSonidoAtacando.setColumns(16);
        txtSonidoAtacando.setForeground(new java.awt.Color(226, 232, 240));
        txtSonidoAtacando.setCaretColor(new java.awt.Color(226, 232, 240));
        txtSonidoAtacando.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtSonidoAtacando.setSelectionColor(new java.awt.Color(55, 65, 81));
        pnlSonidoAtacando.add(txtSonidoAtacando);

        btnSonidoAtacando.setBackground(new java.awt.Color(24, 33, 48));
        btnSonidoAtacando.setForeground(new java.awt.Color(226, 232, 240));
        btnSonidoAtacando.setText("Buscar…");
        btnSonidoAtacando.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSonidoAtacandoActionPerformed(evt);
            }
        });
        pnlSonidoAtacando.add(btnSonidoAtacando);

        checkSonidoAtacando.setBackground(new java.awt.Color(17, 24, 39));
        checkSonidoAtacando.setForeground(new java.awt.Color(226, 232, 240));
        checkSonidoAtacando.setSelected(true);
        checkSonidoAtacando.setText("Activado");
        checkSonidoAtacando.setToolTipText("Desmarque para silenciar solo este sonido, incluido su efecto genérico.");
        pnlSonidoAtacando.add(checkSonidoAtacando);

        pnlConfig.add(pnlSonidoAtacando);

        pnlSonidoDestruido.setBackground(new java.awt.Color(17, 24, 39));
        pnlSonidoDestruido.setLayout(new java.awt.GridLayout());

        lblSonidoDestruido.setFont(new java.awt.Font("sansserif", 0, 13)); // NOI18N
        lblSonidoDestruido.setForeground(new java.awt.Color(226, 232, 240));
        lblSonidoDestruido.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSonidoDestruido.setText("Sonido al destruirse");
        pnlSonidoDestruido.add(lblSonidoDestruido);

        txtSonidoDestruido.setBackground(new java.awt.Color(24, 33, 48));
        txtSonidoDestruido.setColumns(16);
        txtSonidoDestruido.setForeground(new java.awt.Color(226, 232, 240));
        txtSonidoDestruido.setCaretColor(new java.awt.Color(226, 232, 240));
        txtSonidoDestruido.setDisabledTextColor(new java.awt.Color(156, 163, 175));
        txtSonidoDestruido.setSelectionColor(new java.awt.Color(55, 65, 81));
        pnlSonidoDestruido.add(txtSonidoDestruido);

        btnSonidoDestruido.setBackground(new java.awt.Color(24, 33, 48));
        btnSonidoDestruido.setForeground(new java.awt.Color(226, 232, 240));
        btnSonidoDestruido.setText("Buscar…");
        btnSonidoDestruido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSonidoDestruidoActionPerformed(evt);
            }
        });
        pnlSonidoDestruido.add(btnSonidoDestruido);

        checkSonidoDestruido.setBackground(new java.awt.Color(17, 24, 39));
        checkSonidoDestruido.setForeground(new java.awt.Color(226, 232, 240));
        checkSonidoDestruido.setSelected(true);
        checkSonidoDestruido.setText("Activado");
        checkSonidoDestruido.setToolTipText("Desmarque para silenciar solo este sonido, incluido su efecto genérico.");
        pnlSonidoDestruido.add(checkSonidoDestruido);

        pnlConfig.add(pnlSonidoDestruido);

        btnCrear.setBackground(new java.awt.Color(24, 33, 48));
        btnCrear.setFont(new java.awt.Font("CaskaydiaCove NF", 1, 36)); // NOI18N
        btnCrear.setForeground(new java.awt.Color(226, 232, 240));
        btnCrear.setText("CREAR");
        btnCrear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlCrearLayout = new javax.swing.GroupLayout(pnlCrear);
        pnlCrear.setLayout(pnlCrearLayout);
        pnlCrearLayout.setHorizontalGroup(
            pnlCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblCrear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(pnlCrearLayout.createSequentialGroup()
                .addGap(0, 65, Short.MAX_VALUE)
                .addGroup(pnlCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCrear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(71, Short.MAX_VALUE))
        );
        pnlCrearLayout.setVerticalGroup(
            pnlCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCrearLayout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(lblCrear, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(91, 91, 91)
                .addComponent(pnlConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 477, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 85, Short.MAX_VALUE)
                .addComponent(btnCrear)
                .addGap(44, 44, 44))
        );

        scrollCrear.setViewportView(pnlCrear);
        pnlDividido.setLeftComponent(scrollCrear);

        pnlVer.setBackground(new java.awt.Color(24, 33, 48));
        pnlVer.setLayout(new java.awt.BorderLayout());

        tablaCatalogo.setBackground(new java.awt.Color(0, 51, 51));
        tablaCatalogo.setRowHeight(28);
        tablaCatalogo.setForeground(new java.awt.Color(226, 232, 240));
        tablaCatalogo.setSelectionForeground(new java.awt.Color(226, 232, 240));
        tablaCatalogo.setSelectionBackground(new java.awt.Color(55, 65, 81));
        scrollCatalogo.setViewportView(tablaCatalogo);

        pnlVer.add(scrollCatalogo, java.awt.BorderLayout.CENTER);

        pnlAcciones.setOpaque(false);
        pnlAcciones.setLayout(new java.awt.GridLayout(1, 3, 8, 0));

        btnNuevo.setBackground(new java.awt.Color(24, 33, 48));
        btnNuevo.setForeground(new java.awt.Color(226, 232, 240));
        btnNuevo.setText("NUEVO");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnNuevo);

        btnEliminar.setBackground(new java.awt.Color(24, 33, 48));
        btnEliminar.setForeground(new java.awt.Color(226, 232, 240));
        btnEliminar.setText("ELIMINAR");
        btnEliminar.setEnabled(false);
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnEliminar);

        btnActivar.setBackground(new java.awt.Color(24, 33, 48));
        btnActivar.setForeground(new java.awt.Color(226, 232, 240));
        btnActivar.setText("DESACTIVAR");
        btnActivar.setEnabled(false);
        btnActivar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnActivarMouseClicked(evt);
            }
        });
        pnlAcciones.add(btnActivar);

        pnlVer.add(pnlAcciones, java.awt.BorderLayout.PAGE_START);

        pnlDividido.setRightComponent(pnlVer);

        getContentPane().add(pnlDividido, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNombreActionPerformed
        // Los datos se validan al guardar la configuración.
    }//GEN-LAST:event_txtNombreActionPerformed

    private void comboTipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTipoActionPerformed
        actualizarSubtipos();
    }//GEN-LAST:event_comboTipoActionPerformed

    private void comboSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSubActionPerformed
        actualizarCamposPorTipo();
    }//GEN-LAST:event_comboSubActionPerformed

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCrearActionPerformed
        guardarConfiguracion();
    }//GEN-LAST:event_btnCrearActionPerformed

    private void seleccionarRecurso(javax.swing.JTextField destino, boolean sonido) {
        javax.swing.JFileChooser selector = new javax.swing.JFileChooser();
        selector.setDialogTitle(sonido ? "Seleccionar sonido WAV PCM" : "Seleccionar imagen");
        selector.setFileFilter(sonido
                ? new javax.swing.filechooser.FileNameExtensionFilter("Sonido WAV PCM", "wav")
                : new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "png", "jpg", "jpeg", "gif", "bmp"));
        String ruta = destino.getText().strip();
        if (!ruta.isEmpty()) selector.setSelectedFile(new java.io.File(ruta));
        if (selector.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File archivo = selector.getSelectedFile();
        try {
            if (!archivo.isFile() || !archivo.canRead()) throw new java.io.IOException("Archivo no legible");
            if (sonido) audio.Sonidos.validar(archivo.getPath());
            else if (javax.imageio.ImageIO.read(archivo) == null) throw new java.io.IOException("Imagen inválida");
            destino.setText(archivo.getAbsolutePath()); // Cancelar o fallar mantiene la selección anterior.
        } catch (java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Recurso inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnSonidoAtacandoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSonidoAtacandoActionPerformed
        seleccionarRecurso(txtSonidoAtacando, true);
    }//GEN-LAST:event_btnSonidoAtacandoActionPerformed

    private void btnSonidoDestruidoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSonidoDestruidoActionPerformed
        seleccionarRecurso(txtSonidoDestruido, true);
    }//GEN-LAST:event_btnSonidoDestruidoActionPerformed

    private void btnImgIdleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImgIdleActionPerformed
        seleccionarRecurso(txtImgIdle, false);
    }//GEN-LAST:event_btnImgIdleActionPerformed

    private void btnImgAtqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImgAtqActionPerformed
        seleccionarRecurso(txtImgAtq, false);
    }//GEN-LAST:event_btnImgAtqActionPerformed

    private void btnImgMovActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImgMovActionPerformed
        seleccionarRecurso(txtImgMov, false);
    }//GEN-LAST:event_btnImgMovActionPerformed

    private void tablaCatalogoSeleccionada(javax.swing.event.ListSelectionEvent evento) {
        if (!evento.getValueIsAdjusting()) cargarConfiguracionSeleccionada();
    }

    /** Carga por ID la configuración elegida, incluso después de ordenar la tabla. */
    private void cargarConfiguracionSeleccionada() {
        if (tablaCatalogo.getSelectedRow() < 0) return;
        // La fila visible cambia al ordenar: el ID se obtiene de la fila del modelo.
        int fila = tablaCatalogo.convertRowIndexToModel(tablaCatalogo.getSelectedRow());
        String id = (String) tablaCatalogo.getModel().getValueAt(fila, 11);
        Configuracion config = catalogo.consultar(id);
        if (config == null) return;
        boolean atacante = java.util.Set.of("Acechador", "Escupidor", "Enjambre", "Demoledor", "Volador").contains(config.getTipo());
        comboTipo.setSelectedItem(atacante ? "Atacante" : "Defensa");
        comboSub.setSelectedItem(config.getTipo()); // Primero ajusta las restricciones, luego carga los valores
        txtNombre.setText(config.getNombre());
        spinnerVida.setValue(config.getVida());
        spinnerDmg.setValue(config.getDmg());
        spinnerAlcance.setValue(config.getAlcance());
        spinnerCapacidad.setValue(config.getCostoCapacidad());
        spinnerMision.setValue(config.getMisionMinima());
        spinnerIntervalo.setValue(config.getFrecuenciaAtaque());
        spinnerMovimiento.setValue(config.getIntervaloMovimiento());
        spinnerMaxObjetivos.setValue(Math.max(1, config.getMaxObjetivos()));
        checkAtacaAereos.setSelected(config.isAtacaAereos());
        txtImgIdle.setText(config.getRutaIdle());
        txtImgAtq.setText(config.getRutaAtaque());
        txtImgMov.setText(config.getRutaMovimiento());
        txtSonidoAtacando.setText(config.getSonidoAtacando());
        txtSonidoDestruido.setText(config.getSonidoDestruido());
        checkSonidoAtacando.setSelected(!config.isAtaqueSilenciado());
        checkSonidoDestruido.setSelected(!config.isDestruccionSilenciada());
        idSeleccionado = id;
        lblCrear.setText("EDITAR UNIDAD");
        // Cambios del gui
        btnActivar.setEnabled(true);
        if(config.isActivo()) btnActivar.setText("DESACTIVAR");
        else btnActivar.setText("ACTIVAR");
        btnCrear.setText("GUARDAR");
        btnEliminar.setEnabled(true);
    }

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        nuevaConfiguracion();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarConfiguracion();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnActivarMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnActivarMouseClicked
        alternarEstadoConfiguracion();
    }//GEN-LAST:event_btnActivarMouseClicked

    /** Permite previsualizar esta ventana con Run File en NetBeans. */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new VentanaAdmin().mostrar());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActivar;
    private javax.swing.JButton btnCrear;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnImgAtq;
    private javax.swing.JButton btnImgIdle;
    private javax.swing.JButton btnImgMov;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnSonidoAtacando;
    private javax.swing.JButton btnSonidoDestruido;
    private javax.swing.JCheckBox checkAtacaAereos;
    private javax.swing.JCheckBox checkSonidoAtacando;
    private javax.swing.JCheckBox checkSonidoDestruido;
    private javax.swing.JComboBox<String> comboSub;
    private javax.swing.JComboBox<String> comboTipo;
    private javax.swing.JLabel lblAlcance;
    private javax.swing.JLabel lblAtacaAereos;
    private javax.swing.JLabel lblCosto;
    private javax.swing.JLabel lblCrear;
    private javax.swing.JLabel lblDmg;
    private javax.swing.JLabel lblImgAtq;
    private javax.swing.JLabel lblImgIdle;
    private javax.swing.JLabel lblImgMov;
    private javax.swing.JLabel lblIntervalo;
    private javax.swing.JLabel lblMaxObjetivos;
    private javax.swing.JLabel lblMision;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblSonidoAtacando;
    private javax.swing.JLabel lblSonidoDestruido;
    private javax.swing.JLabel lblTipo;
    private javax.swing.JLabel lblVida;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlAlcance;
    private javax.swing.JPanel pnlAtacaAereos;
    private javax.swing.JPanel pnlConfig;
    private javax.swing.JPanel pnlCosto;
    private javax.swing.JPanel pnlCrear;
    private javax.swing.JSplitPane pnlDividido;
    private javax.swing.JPanel pnlDmg;
    private javax.swing.JPanel pnlImgAtq;
    private javax.swing.JPanel pnlImgIdle;
    private javax.swing.JPanel pnlImgMov;
    private javax.swing.JPanel pnlMaxObjetivos;
    private javax.swing.JPanel pnlMision;
    private javax.swing.JPanel pnlNombre;
    private javax.swing.JPanel pnlSonidoAtacando;
    private javax.swing.JPanel pnlSonidoDestruido;
    private javax.swing.JPanel pnlTipo;
    private javax.swing.JPanel pnlMovimiento;
    private javax.swing.JLabel lblMovimiento;
    private javax.swing.JSpinner spinnerMovimiento;
    private javax.swing.JPanel pnlVelAtq;
    private javax.swing.JPanel pnlVer;
    private javax.swing.JPanel pnlVida;
    private javax.swing.JScrollPane scrollCatalogo;
    private javax.swing.JScrollPane scrollCrear;
    private javax.swing.JSpinner spinnerAlcance;
    private javax.swing.JSpinner spinnerCapacidad;
    private javax.swing.JSpinner spinnerDmg;
    private javax.swing.JSpinner spinnerIntervalo;
    private javax.swing.JSpinner spinnerMaxObjetivos;
    private javax.swing.JSpinner spinnerMision;
    private javax.swing.JSpinner spinnerVida;
    private javax.swing.JTable tablaCatalogo;
    private javax.swing.JTextField txtImgAtq;
    private javax.swing.JTextField txtImgIdle;
    private javax.swing.JTextField txtImgMov;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtSonidoAtacando;
    private javax.swing.JTextField txtSonidoDestruido;
    // End of variables declaration//GEN-END:variables
}




