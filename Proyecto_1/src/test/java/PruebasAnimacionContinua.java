import modelo.componentes.*;
import modelo.defensas.Contacto;
import modelo.atacantes.Acechador;
import modelo.juego.Mapa;
import vista.PanelMapa;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/** Un atacante rápido debe alternar sus imágenes aunque no reciba daño. */
public class PruebasAnimacionContinua {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    static ImageIcon imagen(Color color){BufferedImage b=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=b.createGraphics();g.setColor(color);g.fillRect(0,0,64,64);g.dispose();return new ImageIcon(b);}
    public static void main(String[] args)throws Exception{
        Mapa mapa=new Mapa();mapa.colocar(new Nucleo("Núcleo",10000),12,12);
        Contacto unidad=new Contacto("Ráfaga",10000,1,1,1);
        Acechador objetivo=new Acechador("Objetivo inmóvil",100000,0,1000,1,1000);
        unidad.setImgIdle(imagen(Color.BLUE));unidad.setImgAtq(imagen(Color.ORANGE));
        mapa.colocar(unidad,8,8);mapa.colocar(objetivo,8,9);
        PanelMapa[] panel={null};AtomicInteger bases=new AtomicInteger(),ataques=new AtomicInteger();
        SwingUtilities.invokeAndWait(()->{panel[0]=new PanelMapa();panel[0].setMapa(mapa);panel[0].setBotonesActivos(false);});
        try{
            unidad.start();
            for(int i=0;i<24;i++){
                Thread.sleep(50); // El muestreo tiene el mismo ritmo que VentanaJuego.
                SwingUtilities.invokeAndWait(()->{
                    panel[0].actualizar();JButton b=(JButton)panel[0].getComponent(8*25+8);b.setSize(32,32);
                    BufferedImage salida=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var g=salida.createGraphics();b.getDisabledIcon().paintIcon(b,g,0,0);g.dispose();
                    int pixel=salida.getRGB(16,16);if(pixel==Color.BLUE.getRGB())bases.incrementAndGet();else if(pixel==Color.ORANGE.getRGB())ataques.incrementAndGet();
                    comprobar(b.getBackground().equals(((JButton)panel[0].getComponent(0)).getBackground()),"Mantiene el color de bando pese a tener imagen");
                    comprobar(b.getIcon().getIconWidth()==31,"No aprovecha el tamaño de la casilla");
                });
            }
            comprobar(bases.get()>=3&&ataques.get()>=3,"Pose clavada: base="+bases+", ataque="+ataques);
            comprobar(objetivo.getVida()<99980,"El pulso visual frenó el daño real");
            comprobar(unidad.getVida()==10000&&unidad.getUltimoDmg()==0,"La prueba depende de recibir un golpe");
            unidad.pausar();boolean pose=unidad.isAtaqueVisible();Thread.sleep(180);comprobar(pose==unidad.isAtaqueVisible(),"La pausa consume la pose");
        }finally{unidad.detener();}
        System.out.println("OK animación rápida sin recibir daño, daño independiente, icono deshabilitado, fondo neutro, tamaño y pausa.");
    }
}
