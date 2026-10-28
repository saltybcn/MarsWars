package vista;

import configuracion.Auth;
import java.util.Arrays;
import javax.swing.*;

/** Acceso, cambio y recuperación editables desde un mismo formulario de NetBeans. */
public final class AccesoAdmin extends JDialog {
    private Auth autenticacion;
    private Auth autorizada;
    private String modo;

    private AccesoAdmin(java.awt.Window propietario, Auth auth) {
        super(propietario, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        autenticacion = auth == null ? new Auth() : auth;
        initComponents();

        getRootPane().setDefaultButton(btnAceptar);
        cambiarModo(auth == null ? "CREAR" : "ENTRAR");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { limpiarClaves(); }
        });
        setLocationRelativeTo(propietario);
    }

    public static Auth solicitar(java.awt.Component propietario) {
        try {
            java.awt.Window ventana = propietario instanceof java.awt.Window w ? w
                    : propietario == null ? null : SwingUtilities.getWindowAncestor(propietario);
            AccesoAdmin acceso = new AccesoAdmin(ventana, Auth.cargar());
            acceso.setVisible(true); // Es modal: solo devuelve acceso después de validar o cancelar.
            return acceso.autorizada;
        } catch (java.io.IOException | IllegalStateException e) {
            JOptionPane.showMessageDialog(propietario, e.getMessage(), "Acceso no disponible", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void cambiarModo(String nuevo) {
        modo = nuevo;
        limpiarClaves();
        txtUsuario.setText(autenticacion.getUsuario() == null ? "" : autenticacion.getUsuario());
        boolean entrar = "ENTRAR".equals(modo);
        boolean cambiar = "CAMBIAR".equals(modo);
        boolean recuperar = "RECUPERAR".equals(modo);
        lblTitulo.setText(entrar ? "ACCESO DE ADMINISTRADOR" : cambiar ? "CAMBIAR ACCESO"
                : recuperar ? "RECUPERAR ACCESO" : "CREAR ADMINISTRADOR");
        setTitle(lblTitulo.getText());
        lblMensaje.setText(entrar ? "Ingrese su usuario y contraseña." : recuperar
                ? "Use el código entregado al configurar el acceso." : "La nueva contraseña debe tener al menos 4 caracteres.");
        lblActual.setText(cambiar ? "Contraseña actual" : "Contraseña");
        lblActual.setVisible(entrar || cambiar); pwdActual.setVisible(entrar || cambiar);
        lblNueva.setVisible(!entrar); pwdNueva.setVisible(!entrar);
        lblConfirmar.setVisible(!entrar); pwdConfirmar.setVisible(!entrar);
        lblCodigo.setVisible(recuperar); pwdCodigo.setVisible(recuperar);
        btnCambiar.setVisible(entrar); btnRecuperar.setVisible(entrar);
        
        btnAceptar.setText(entrar ? "Entrar" : "Guardar acceso");
        
        btnVolver.setText(entrar || "CREAR".equals(modo) ? "Cancelar" : "Volver");
        pnlFormulario.revalidate(); pnlFormulario.repaint();
    }

    private void limpiarClaves() {
        pwdActual.setText(""); pwdNueva.setText(""); pwdConfirmar.setText(""); pwdCodigo.setText("");
    }

    private void btnAceptarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAceptarActionPerformed
        procesarAcceso();
    }//GEN-LAST:event_btnAceptarActionPerformed

    private void btnVolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVolverActionPerformed
        volverAlAcceso();
    }//GEN-LAST:event_btnVolverActionPerformed

    private void btnCambiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCambiarActionPerformed
        cambiarModo("CAMBIAR");
    }//GEN-LAST:event_btnCambiarActionPerformed

    private void btnRecuperarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRecuperarActionPerformed
        cambiarModo("RECUPERAR");
    }//GEN-LAST:event_btnRecuperarActionPerformed

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> solicitar(null));
    }

    // Operaciones de la ventana. Se pueden invocar sin fabricar eventos de Swing.
    /** Verifica el modo y las credenciales antes de autorizar o guardar el acceso. */
    private void procesarAcceso() {
        char[] actual = pwdActual.getPassword(), nueva = pwdNueva.getPassword();
        char[] confirmar = pwdConfirmar.getPassword(), codigo = pwdCodigo.getPassword();
        try {
            if ("ENTRAR".equals(modo)) {
                if (!autenticacion.validar(txtUsuario.getText(), actual)) {
                    lblMensaje.setText("Usuario o contraseña incorrectos."); return;
                }
                autorizada = autenticacion; dispose(); return;
            }
            // Cambiar exige la clave vigente; recuperar exige el código, nunca solo el nombre.
            if ("CAMBIAR".equals(modo) && !autenticacion.validar(autenticacion.getUsuario(), actual)) {
                lblMensaje.setText("Contraseña actual incorrecta."); return;
            }
            if ("RECUPERAR".equals(modo) && !autenticacion.recuperar(codigo)) {
                lblMensaje.setText("Código de recuperación incorrecto."); return;
            }
            if (!Arrays.equals(nueva, confirmar)) {
                lblMensaje.setText("Las contraseñas nuevas no coinciden."); return;
            }
            String recuperacion = autenticacion.configurar(txtUsuario.getText(), nueva);
            JTextArea texto = new JTextArea("Guarde este código para recuperar el acceso.\n" + recuperacion, 5, 48);
            texto.setEditable(false); texto.setLineWrap(true); texto.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Código de recuperación", JOptionPane.INFORMATION_MESSAGE);
            cambiarModo("ENTRAR");
            lblMensaje.setText("Acceso guardado. Ya puede iniciar sesión.");
        } catch (IllegalArgumentException | java.io.IOException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo guardar el acceso", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(actual, '\0'); Arrays.fill(nueva, '\0'); Arrays.fill(confirmar, '\0'); Arrays.fill(codigo, '\0');
        }
    }

    /** Cancela o regresa al inicio de sesión según el modo actual. */
    private void volverAlAcceso() {
        if ("ENTRAR".equals(modo) || "CREAR".equals(modo)) dispose();
        else cambiarModo("ENTRAR");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlFormulario = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        lblMensaje = new javax.swing.JLabel();
        lblUsuario = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        lblActual = new javax.swing.JLabel();
        pwdActual = new javax.swing.JPasswordField();
        lblNueva = new javax.swing.JLabel();
        pwdNueva = new javax.swing.JPasswordField();
        lblConfirmar = new javax.swing.JLabel();
        pwdConfirmar = new javax.swing.JPasswordField();
        lblCodigo = new javax.swing.JLabel();
        pwdCodigo = new javax.swing.JPasswordField();
        btnAceptar = new javax.swing.JButton();
        btnVolver = new javax.swing.JButton();
        btnCambiar = new javax.swing.JButton();
        btnRecuperar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Administrador");
        setMinimumSize(new java.awt.Dimension(520, 560));
        setPreferredSize(new java.awt.Dimension(640, 620));

        lblTitulo.setText("ACCESO DE ADMINISTRADOR");

        lblMensaje.setText("Ingrese su usuario y contraseña.");

        lblUsuario.setText("Usuario");

        lblActual.setText("Contraseña");

        lblNueva.setText("Nueva contraseña");

        lblConfirmar.setText("Confirmar contraseña");

        lblCodigo.setText("Código de recuperación");

        btnAceptar.setText("Entrar");
        
        btnAceptar.addActionListener(this::btnAceptarActionPerformed);

        btnVolver.setText("Cancelar");
        
        btnVolver.addActionListener(this::btnVolverActionPerformed);

        btnCambiar.setText("Cambiar acceso");
        
        btnCambiar.addActionListener(this::btnCambiarActionPerformed);

        btnRecuperar.setText("Olvidé mi contraseña");
        
        btnRecuperar.addActionListener(this::btnRecuperarActionPerformed);

        javax.swing.GroupLayout pnlFormularioLayout = new javax.swing.GroupLayout(pnlFormulario);
        pnlFormulario.setLayout(pnlFormularioLayout);
        pnlFormularioLayout.setHorizontalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormularioLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitulo, 0, 412, Short.MAX_VALUE)
                    .addComponent(lblMensaje, 0, 412, Short.MAX_VALUE)
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(lblUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, 12)
                        .addComponent(txtUsuario, 0, 250, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(lblActual, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, 12)
                        .addComponent(pwdActual, 0, 250, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(lblNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, 12)
                        .addComponent(pwdNueva, 0, 250, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(lblConfirmar, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, 12)
                        .addComponent(pwdConfirmar, 0, 250, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 12, 12)
                        .addComponent(pwdCodigo, 0, 250, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(btnAceptar, 0, 200, Short.MAX_VALUE)
                        .addGap(0, 12, 12)
                        .addComponent(btnVolver, 0, 200, Short.MAX_VALUE))
                    .addGroup(pnlFormularioLayout.createSequentialGroup()
                        .addComponent(btnCambiar, 0, 200, Short.MAX_VALUE)
                        .addGap(0, 12, 12)
                        .addComponent(btnRecuperar, 0, 200, Short.MAX_VALUE)))
                .addGap(0, 0, 0))
        );
        pnlFormularioLayout.setVerticalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormularioLayout.createSequentialGroup()
                .addGap(0, 20, 20)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 8, 8)
                .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 16, 16)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 8, 8)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblActual, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pwdActual, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 8, 8)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pwdNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 8, 8)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblConfirmar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pwdConfirmar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 8, 8)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pwdCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAceptar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 8, 8)
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCambiar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRecuperar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(pnlFormulario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(pnlFormulario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAceptar;
    private javax.swing.JButton btnCambiar;
    private javax.swing.JButton btnRecuperar;
    private javax.swing.JButton btnVolver;
    private javax.swing.JLabel lblActual;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblConfirmar;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblNueva;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JPasswordField pwdActual;
    private javax.swing.JPasswordField pwdCodigo;
    private javax.swing.JPasswordField pwdConfirmar;
    private javax.swing.JPasswordField pwdNueva;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}

