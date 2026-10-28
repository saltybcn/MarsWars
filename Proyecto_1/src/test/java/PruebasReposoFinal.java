import modelo.componentes.*;
import modelo.atacantes.Acechador;
import modelo.defensas.Contacto;
import modelo.juego.*;
import vista.PanelMapa;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

/** Pausa congela una pose; terminar vuelve a base a quienes siguen vivos. */
public class PruebasReposoFinal {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    static void estado(Combate unidad,String nombre,Object valor)throws Exception {
        Field f=Combate.class.getDeclaredField(nombre);f.setAccessible(true);f.set(unidad,valor);
    }
    static ImageIcon color(Color color){
        BufferedImage b=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=b.createGraphics();
        g.setColor(color);g.fillRect(0,0,64,64);g.dispose();return new ImageIcon(b);
    }
    public static void main(String[] args)throws Exception {
        for(boolean derrota:new boolean[]{false,true}) {
            Mapa mapa=new Mapa();Nucleo nucleo=new Nucleo("Núcleo",1000);mapa.colocar(nucleo,12,12);
            Contacto defensa=new Contacto("Defensa",1000,0,100000,1);
            Acechador enemigo=new Acechador("Enemigo",1000,0,100000,1,100000);
            mapa.colocar(defensa,10,10);mapa.colocar(enemigo,0,0);
            defensa.setImgIdle(color(Color.BLUE));defensa.setImgAtq(color(Color.ORANGE));defensa.setImgMov(color(Color.GREEN));
            Batalla batalla=new Batalla(mapa);
            try {
                batalla.iniciar();batalla.pausar();
                for(Componente c:batalla.getParticipantes())if(c instanceof Combate unidad){
                    estado(unidad,"pasoAlterno",true);estado(unidad,"esperaVisual",1_000_000_000L);
                }
                batalla.pausar();comprobar(defensa.isPasoAlterno()&&defensa.isAtaqueVisible(),"Pausa borra la pose");
                synchronized(mapa){
                    if(derrota)nucleo.recibirDmg(1000);else enemigo.recibirDmg(1000);
                    batalla.reanudar();batalla.comprobarResultado();
                    comprobar((derrota?"DERROTA":"VICTORIA").equals(batalla.getResultado()),"Resultado incorrecto");
                    for(Componente c:batalla.getParticipantes())if(c instanceof Combate unidad&&c.getVida()>0)
                        comprobar(!unidad.isPasoAlterno()&&!unidad.isAtaqueVisible(),"Termina en segundo sprite: "+unidad.getNombre());
                }
                SwingUtilities.invokeAndWait(()->{
                    PanelMapa panel=new PanelMapa();panel.setMapa(mapa);JButton celda=(JButton)panel.getComponent(10*25+10);celda.setSize(68,68);
                    BufferedImage b=new BufferedImage(68,68,BufferedImage.TYPE_INT_ARGB);var g=b.createGraphics();celda.getIcon().paintIcon(celda,g,0,0);g.dispose();
                    comprobar(b.getRGB(32,32)==Color.BLUE.getRGB(),"El tablero conserva una pose de ataque/movimiento");
                });
            }finally{batalla.finalizar();}
        }
        System.out.println("OK victoria/derrota: unidades vivas y tablero vuelven a base; pausa conserva la pose.");
    }
}
