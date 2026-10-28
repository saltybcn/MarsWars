import configuracion.*;
import persistencia.Archivo;
import modelo.componentes.*;
import java.util.*;
import java.nio.file.*;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/** Ejecute desde Proyecto_1: las poses deben existir, ser distintas y viajar con su unidad. */
public class PruebasPosesCatalogo {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    static void validar(ImageIcon imagen,String ruta) throws Exception {
        comprobar(imagen!=null&&imagen.getIconWidth()==64&&imagen.getIconHeight()==64,"Miniatura de estado inválida: "+ruta);
        BufferedImage png=javax.imageio.ImageIO.read(Path.of(ruta).toFile());
        comprobar(png.getColorModel().hasAlpha(),"Sin transparencia: "+ruta);
        boolean transparente=false,visible=false;
        for(int y=0;y<64;y++)for(int x=0;x<64;x++){
            int alfa=png.getRGB(x,y)>>>24;transparente|=alfa==0;visible|=alfa>0;
        }
        comprobar(transparente&&visible,"Fondo opaco o sprite vacío: "+ruta);
    }
    public static void main(String[] args) throws Exception {
        Catalogo catalogo=new Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json");
        Set<String> moviles=Set.of("Acechador","Escupidor","Enjambre","Demoledor","Volador","Dron");
        int ataques=0,movimientos=0;
        for(Configuracion config:catalogo.getConfigs()){
            Combate unidad=config.crearUnidad();validar(unidad.getImgIdle(),config.getRutaIdle());
            Path carpeta=Path.of(config.getRutaIdle()).getParent();
            if(!config.getTipo().equals("Barrera")){
                validar(unidad.getImgAtq(),config.getRutaAtaque());
                comprobar(!Arrays.equals(Files.readAllBytes(Path.of(config.getRutaIdle())),Files.readAllBytes(Path.of(config.getRutaAtaque()))),
                        "Ataque igual a base: "+config.getTipo());
                comprobar(carpeta.equals(Path.of(config.getRutaAtaque()).getParent()),"Ataque en otra carpeta");ataques++;
            }
            if(moviles.contains(config.getTipo())){
                validar(unidad.getImgMov(),config.getRutaMovimiento());
                comprobar(!config.getRutaMovimiento().equals(config.getRutaIdle())&&!config.getRutaMovimiento().equals(config.getRutaAtaque()),
                        "Movimiento repetido: "+config.getTipo());
                comprobar(carpeta.equals(Path.of(config.getRutaMovimiento()).getParent()),"Movimiento en otra carpeta");movimientos++;
            }
        }
        comprobar(ataques==10&&movimientos==6,"Faltan estados de animación");
        System.out.println("OK 10 ataques y 6 movimientos diferentes, PNG 64x64 transparentes, fábrica y carpetas por unidad.");
    }
}
