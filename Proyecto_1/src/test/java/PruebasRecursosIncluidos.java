import configuracion.*;
import modelo.componentes.Componente;
import persistencia.Archivo;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/** Ejecute desde Proyecto_1. Inspección de recursos del catálogo entregado, sin modificarlo. */
public class PruebasRecursosIncluidos {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    static BufferedImage pintar(ImageIcon icono) {
        BufferedImage imagen=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        var g=imagen.createGraphics();icono.paintIcon(null,g,0,0);g.dispose();return imagen;
    }
    public static void main(String[] args) throws Exception {
        Catalogo catalogo=new Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json");
        comprobar(catalogo.getConfigs().size()==11,"Once configuraciones de ejemplo");

        for(Configuracion c:catalogo.getConfigs()) {
            String tipo=switch(c.getTipo()) {
                case "AlcanceMedio"->"alcance_medio";
                case "AtaqueMultiple"->"ataque_multiple";
                default->c.getTipo().toLowerCase(java.util.Locale.ROOT);
            };
            var importada=Componente.cargarImagen(c.getRutaIdle());
            comprobar(importada!=null && importada.getIconWidth()==64,"Miniatura importada: "+tipo);
            comprobar(c.crearUnidad().getImgIdle()!=null,"La fábrica conserva la imagen: "+tipo);
            Path carpeta=Path.of(c.getRutaIdle()).getParent();
            for(String ruta:new String[]{c.getRutaAtaque(),c.getRutaMovimiento(),c.getSonidoAtacando(),c.getSonidoDestruido()}) {
                if(ruta==null||ruta.isBlank())continue;
                if (ruta.startsWith("sonidos/")) {
                    audio.Sonidos.validar(ruta); // Los genéricos globales no se duplican por unidad.
                    continue;
                }
                comprobar(carpeta.equals(Path.of(ruta).getParent()),"Recursos separados: "+tipo);
                comprobar(Files.isRegularFile(Path.of(ruta)),"Recurso ausente: "+ruta);
                if(ruta.endsWith(".wav")) audio.Sonidos.validar(ruta);
            }
        }
        System.out.println("OK 11 imágenes actuales, fábrica, carpetas y todos los sonidos publicados.");
    }
}
