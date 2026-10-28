import modelo.componentes.*;
import modelo.juego.Mapa;
import vista.PanelMapa;
import javax.swing.*;
import java.awt.image.BufferedImage;
/** Verifica el sprite final sin modificar la vida ni la imagen base del núcleo. */
public class PruebasNucleoDestruido {
    private static BufferedImage pintar(JButton celda) {
        celda.setSize(68,68);
        BufferedImage imagen=new BufferedImage(68,68,BufferedImage.TYPE_INT_ARGB);
        var g=imagen.createGraphics();celda.getIcon().paintIcon(celda,g,0,0);g.dispose();return imagen;
    }
    public static void main(String[] args)throws Exception {
        ImageIcon destruido=Componente.cargarImagen("nucleo/nucleo_destruido.png");
        if(destruido==null)throw new AssertionError("Falta recurso del núcleo destruido");
        SwingUtilities.invokeAndWait(()->{
            Nucleo nucleo=new Nucleo("Núcleo",100);
            ImageIcon base=Componente.cargarImagen("nucleo/nucleo.png");nucleo.setImgIdle(base);
            Mapa mapa=new Mapa();mapa.colocar(nucleo,12,12);
            PanelMapa panel=new PanelMapa();panel.setMapa(mapa);
            JButton celda=(JButton)panel.getComponent(312);
            BufferedImage vivo=pintar(celda);
            nucleo.setVida(0);panel.actualizar();BufferedImage muerto=pintar(celda);
            boolean diferente=false,transparente=false;
            for(int y=0;y<68;y++)for(int x=0;x<68;x++){
                diferente |= vivo.getRGB(x,y)!=muerto.getRGB(x,y);
                transparente |= (muerto.getRGB(x,y)>>>24)==0;
            }
            if(!diferente||!transparente)throw new AssertionError("Sprite destruido sin cambio o sin transparencia");
            if(nucleo.getImgIdle()!=base||mapa.obtener(12,12)!=nucleo)throw new AssertionError("La vista modificó el modelo");
            nucleo.setVida(100);panel.actualizar();BufferedImage restaurado=pintar(celda);
            for(int y=0;y<68;y++)for(int x=0;x<68;x++)
                if(restaurado.getRGB(x,y)!=vivo.getRGB(x,y))throw new AssertionError("No restauró el sprite al repetir");
        });
        System.out.println("OK núcleo destruido: recurso, transparencia, cambio de imagen y restauración sin mutar el modelo.");
    }
}
