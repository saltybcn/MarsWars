package Main;
// MARS WARS
// Proyecto 1 de POO
import javax.swing.SwingUtilities;
import vista.VentanaInicio;

public class Proyecto_1 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaInicio().setVisible(true));
    }
}
