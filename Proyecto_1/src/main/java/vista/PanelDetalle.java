package vista;

import modelo.componentes.*;
import modelo.registros.*;
import javax.swing.*;

/** Detalle vivo integrado en el inventario y disponible también para participantes destruidos. */
public class PanelDetalle extends JPanel {
    private Componente componente;

    public PanelDetalle() {
        initComponents();
        mostrar(null);
    }

    public void mostrar(Componente componente) {
        boolean cambio = this.componente != componente;
        this.componente = componente;
        String contenido;
        if (componente == null) contenido = "Seleccione una unidad del mapa o un participante de los resultados para consultar su historial.";
        else {
            Object monitor = componente.getMapa() == null ? componente : componente.getMapa();
            synchronized (monitor) { contenido = describir(componente); }
        }
        // Conserva el scroll al refrescar la vida y los registros de la misma unidad.
        if (cambio || !contenido.equals(texto.getText())) {
            int posicion = cambio ? 0 : texto.getCaretPosition();
            texto.setText(contenido); texto.setCaretPosition(Math.min(posicion, contenido.length()));
        }
    }

    public static String describir(Componente c) {
        StringBuilder s = new StringBuilder(c.getNombre()).append("\nID: ").append(c.getId())
                .append("\nTipo: ").append(c.getClass().getSimpleName())
                .append("\nVida inicial: ").append(c.getVidaInicial())
                .append("\nVida actual/final: ").append(c.getVida()).append(" / ").append(c.getVidaMax())
                .append("\nÚltima posición: ").append(java.util.Arrays.toString(c.getPosicionFinal()));
        if (c instanceof Combate combate) s.append("\nDaño: ").append(combate.getDmg())
                .append("\nIntervalo de ataque: ").append(combate.getVelAtq() / 1000.0).append(" s")
                .append("\nNivel: ").append(combate.getLvl()).append(" | Misión de aparición: ").append(combate.getMision())
                .append("\nAlcance/radio: ").append(combate.getAlcance());
        s.append("\n\nOBJETIVOS ATACADOS\n");
        for (RegistroDmg r : c.getHistorico().getInfligido()) s.append(r.getNombreComp()).append(" [")
                .append(r.getIdComp()).append("] — golpes: ").append(r.getNumGolpes()).append(" | daño: ").append(r.getDmg()).append('\n');
        s.append("\nATAQUES RECIBIDOS\n");
        for (RegistroDmg r : c.getHistorico().getRecibido()) s.append(r.getNombreComp()).append(" [")
                .append(r.getIdComp()).append("] — golpes: ").append(r.getNumGolpes()).append(" | daño: ").append(r.getDmg()).append('\n');
        s.append("\nMEJORAS\n");
        for (Mejora m : c.getHistorico().getMejoras()) s.append("Misión ").append(m.getMision())
                .append(" | vida +").append(Math.round(m.getPorcentajeVida() * 100)).append("% | daño +")
                .append(Math.round(m.getPorcentajeDmg() * 100)).append("%\n");
        return s.toString();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        scroll = new javax.swing.JScrollPane();
        texto = new javax.swing.JTextArea();
        titulo = new javax.swing.JLabel();
        setLayout(new java.awt.BorderLayout(0, 10));
        setOpaque(false);
        titulo.setText("DETALLE DE UNIDAD");
        titulo.setForeground(new java.awt.Color(94, 234, 212));
        add(titulo, java.awt.BorderLayout.NORTH);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setRows(18);
        texto.setColumns(22);
        texto.setFont(new java.awt.Font("SansSerif", 0, 12));
        texto.setBackground(new java.awt.Color(24, 33, 48));
        texto.setForeground(new java.awt.Color(226, 232, 240));
        texto.setCaretColor(new java.awt.Color(226, 232, 240));
        texto.setMargin(new java.awt.Insets(10, 10, 10, 10));
        scroll.setViewportView(texto);
        add(scroll, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTextArea texto;
    private javax.swing.JLabel titulo;
    // End of variables declaration//GEN-END:variables
}
