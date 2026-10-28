import configuracion.*;
import modelo.componentes.*;
import modelo.defensas.Dron;
import modelo.atacantes.Acechador;
import modelo.juego.Mapa;
import persistencia.Archivo;
import vista.PanelMapa;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;

/** Ejecute desde Proyecto_1, con src/main/resources incluido en el classpath. */
public class PruebasSprites {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) throws Exception {
        Catalogo catalogo = new Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json");
        verificar(catalogo.getConfigs().size() == 11, "Faltan tipos del catálogo");
        Mapa mapa = new Mapa();
        int columna = 0;
        for (Configuracion config : catalogo.getConfigs()) {
            verificar(config.validar(), config.getTipo());
            Combate unidad = config.crearUnidad();
            verificar(unidad.getImgIdle() != null, "Imagen: " + config.getTipo());
            verificar(unidad.getImgIdle().getIconWidth() <= 64, "Miniatura demasiado grande");
            verificar(config.getTipo().equals("Barrera") || unidad.getImgAtq() != null, "Falta imagen de ataque");
            verificar(config.getTipo().equals("Barrera") || unidad.getVelAtq()
                    == Math.round(config.getFrecuenciaAtaque() * 1000), "Conversión a ms");
            mapa.colocar(unidad, 0, columna++);
        }
        mapa.colocar(new Nucleo("Núcleo", 1000), 12, 12);
        Dron dron = new Dron("Dron", 100, 1, 1000, 3, 1, 500);
        var plantillaDron = catalogo.getConfigs().stream().filter(c -> "Dron".equals(c.getTipo())).findFirst().orElseThrow().crearUnidad();
        dron.setImgIdle(plantillaDron.getImgIdle());
        dron.setImgAtq(plantillaDron.getImgAtq());
        Acechador objetivo = new Acechador("Objetivo", 100, 1, 1000, 1, 500);
        mapa.colocar(dron, 10, 10); mapa.colocar(objetivo, 10, 9);
        dron.aplicarGolpe(objetivo);
        verificar(dron.isMirandoIzquierda() && dron.isAtaqueVisible(), "Ataque izquierdo");
        mapa.mover(objetivo, 10, 11); dron.aplicarGolpe(objetivo);
        verificar(!dron.isMirandoIzquierda(), "Ataque derecho");
        mapa.mover(objetivo, 9, 10); dron.aplicarGolpe(objetivo);
        verificar(!dron.isMirandoIzquierda(), "Misma columna conserva orientación");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) { salida.writeObject(mapa); }
        Mapa copia;
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copia = (Mapa) entrada.readObject();
        }
        verificar(copia.obtener(0, 0).getImgIdle() != null, "Se perdió la imagen al cargar");
        verificar(((Combate)copia.obtener(10, 10)).isAtaqueVisible(), "Se perdió la fase visual al guardar");
        verificar(Componente.cargarImagen("no-existe.png") == null, "Ruta inválida");
        verificar(Componente.cargarImagen("nucleo/nucleo.png") != null, "Recurso del núcleo");
        SwingUtilities.invokeAndWait(() -> {
            PanelMapa panel = new PanelMapa(c -> {}); panel.setMapa(mapa);
            panel.setSize(750, 750); panel.doLayout(); panel.actualizar();
            for (int i = 0; i < 11; i++) verificar(((JButton) panel.getComponent(i)).getIcon() != null,
                    "Casilla sin sprite: " + i);
            verificar(((JButton) panel.getComponent(12 * 25 + 12)).getIcon() != null, "Núcleo sin sprite");
            // Comprobar los píxeles del Icon: el reflejo debe ser horizontal, no vertical.
            JButton celda = (JButton) panel.getComponent(10 * 25 + 10);
            celda.setSize(65, 65); // Margen actual de 1 px: sprite de 64 px sin redondeo del escalado.
            BufferedImage derecha = new BufferedImage(65, 65, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = derecha.createGraphics(); celda.getIcon().paintIcon(celda, g, 0, 0); g.dispose();
            mapa.mover(objetivo, 10, 9); dron.aplicarGolpe(objetivo); panel.actualizar();
            BufferedImage izquierda = new BufferedImage(65, 65, BufferedImage.TYPE_INT_ARGB);
            g = izquierda.createGraphics(); celda.getIcon().paintIcon(celda, g, 0, 0); g.dispose();
            for (int y = 0; y < 65; y++) for (int x = 0; x < 65; x++)
                verificar(derecha.getRGB(x, y) == izquierda.getRGB(64 - x, y), "Reflejo incorrecto");
            panel.doLayout();
            BufferedImage captura = new BufferedImage(750, 750, BufferedImage.TYPE_INT_RGB);
            g = captura.createGraphics(); panel.paint(g); g.dispose();
            try { javax.imageio.ImageIO.write(captura, "png", new File(System.getProperty("java.io.tmpdir"), "matriz-sprites.png")); }
            catch (IOException e) { throw new RuntimeException(e); }
            panel.setSize(1000, 800); panel.doLayout(); panel.actualizar();
            verificar(celda.getIcon().getIconWidth() == celda.getWidth() - 1, "No se adapta al tamaño");
        });
        System.out.println("OK: 11 tipos, 12 sprites, segundos/ms, serialización y reflejo horizontal.");
    }
}
