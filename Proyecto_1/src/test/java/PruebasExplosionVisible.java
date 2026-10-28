import modelo.componentes.*;
import modelo.defensas.*;
import modelo.atacantes.*;
import modelo.juego.Mapa;
import vista.PanelMapa;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;

/** La detonación se ve brevemente sin mantener la mina viva, ocupada o con un hilo activo. */
public class PruebasExplosionVisible {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        for(boolean mina:new boolean[]{true,false}) {
            PanelMapa[] panel={null};JButton[] casilla={null};
            SwingUtilities.invokeAndWait(()->{
                Mapa mapa=new Mapa();
                Combate explosivo=mina?new Impacto("Mina",100,10,500,2,1):new Demoledor("Demo",100,10,500,1,2);
                Combate blanco=mina?new Acechador("Objetivo",100,1,1000,1,500):new Contacto("Objetivo",100,1,1000,1);
                BufferedImage sprite=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
                var g=sprite.createGraphics();g.setColor(java.awt.Color.ORANGE);g.fillRect(4,4,56,56);g.dispose();
                explosivo.setImgAtq(new ImageIcon(sprite));
                mapa.colocar(explosivo,5,5);mapa.colocar(blanco,5,6);
                panel[0]=new PanelMapa();panel[0].setMapa(mapa);
                casilla[0]=(JButton)panel[0].getComponent(5*25+5);
                explosivo.atacar(List.of(blanco));panel[0].actualizar();
                comprobar(explosivo.getVida()==0&&mapa.obtener(5,5)==null,"La animación retrasa la muerte/retirada");
                comprobar(blanco.getVida()==90,"La animación retrasa o duplica el daño");
                comprobar(casilla[0].getIcon()!=null,"No muestra el sprite al detonar");
            });
            Thread.sleep(60);
            SwingUtilities.invokeAndWait(()->{panel[0].actualizar();comprobar(casilla[0].getIcon()!=null,"Explosión demasiado corta");});
            Thread.sleep(150);
            SwingUtilities.invokeAndWait(()->{panel[0].actualizar();comprobar(casilla[0].getIcon()==null,"Explosión permanente");});
        }
        System.out.println("OK Impacto y Demoledor: imagen breve, daño y retirada inmediatos, limpieza del efecto.");
    }
}
