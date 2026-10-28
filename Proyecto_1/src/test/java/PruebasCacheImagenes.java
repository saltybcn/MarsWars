import modelo.componentes.Componente;
import configuracion.Configuracion;
import javax.swing.ImageIcon;
import java.nio.file.*;
import java.awt.image.BufferedImage;

/** Ejecute en un directorio temporal; comprueba reutilización e invalidación de miniaturas. */
public class PruebasCacheImagenes {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) throws Exception {
        Path archivo = Path.of("sprite.png");
        BufferedImage imagen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        imagen.setRGB(0, 0, java.awt.Color.RED.getRGB());
        javax.imageio.ImageIO.write(imagen, "png", archivo.toFile());
        ImageIcon primera = Componente.cargarImagen(archivo.toString());
        comprobar(primera != null && primera == Componente.cargarImagen(archivo.toString()), "Reutiliza la miniatura");
        Configuracion config = new Configuracion("Unidad", "Acechador", 100, 1, 1, 1, 1, 1);
        config.setRutaIdle(archivo.toString()); config.setRutaAtaque(archivo.toString()); config.setRutaMovimiento(archivo.toString());
        var unidad1 = config.crearUnidad(); var unidad2 = config.crearUnidad();
        comprobar(unidad1.getImgIdle() == unidad2.getImgIdle() && unidad1.getImgAtq() == unidad2.getImgMov(),
                "Comparte imágenes entre unidades, sin compartir vida ni historial");
        comprobar(unidad1 != unidad2 && unidad1.getHistorico() != unidad2.getHistorico(), "Unidades independientes");
        long anterior = Files.getLastModifiedTime(archivo).toMillis();
        imagen.setRGB(0, 0, java.awt.Color.BLUE.getRGB());
        javax.imageio.ImageIO.write(imagen, "png", archivo.toFile());
        Files.setLastModifiedTime(archivo, java.nio.file.attribute.FileTime.fromMillis(anterior + 2000));
        ImageIcon modificada = Componente.cargarImagen(archivo.toString());
        comprobar(modificada != primera, "Recarga el archivo modificado");
        Files.delete(archivo);
        comprobar(Componente.cargarImagen(archivo.toString()) == null, "Eliminar archivo conserva el fallback");
        for (int i = 0; i < 70; i++) {
            Path recurso = Path.of("imagen-" + i + ".png");
            javax.imageio.ImageIO.write(imagen, "png", recurso.toFile());
            comprobar(Componente.cargarImagen(recurso.toString()) != null, "Carga recurso " + i);
        }
        var campo = Componente.class.getDeclaredField("imagenesCargadas"); campo.setAccessible(true);
        comprobar(((java.util.Map<?, ?>) campo.get(null)).size() <= 64, "Caché acotada");
        System.out.println("OK reutilización entre unidades, archivo modificado/ausente y límite de caché.");
    }
}
