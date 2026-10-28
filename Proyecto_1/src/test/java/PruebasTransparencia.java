import vista.*;
import modelo.componentes.Nucleo;
import modelo.juego.Mapa;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/** Verifica mezcla del fondo, hijos opacos, captura de puntero y tablero visible sin clics. */
public class PruebasTransparencia {
    static void verificar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JLayeredPane capas=new JLayeredPane();capas.setSize(120,120);
            JPanel base=new JPanel();base.setBackground(Color.RED);base.setBounds(0,0,120,120);
            PanelTranslucido superior=new PanelTranslucido();superior.setBackground(Color.BLUE);
            superior.setLayout(null);superior.setBounds(0,0,120,120);
            JPanel hijo=new JPanel();hijo.setBackground(Color.GREEN);hijo.setBounds(40,40,20,20);superior.add(hijo);
            capas.add(base,Integer.valueOf(0));capas.add(superior,Integer.valueOf(100));
            BufferedImage imagen=new BufferedImage(120,120,BufferedImage.TYPE_INT_ARGB);
            var g=imagen.createGraphics();capas.paint(g);g.dispose();
            Color mezcla=new Color(imagen.getRGB(10,10),true);
            verificar(Math.abs(mezcla.getRed()-64)<=1 && Math.abs(mezcla.getBlue()-191)<=1,
                    "No mezcla 75% del fondo con 25% del tablero");
            verificar(imagen.getRGB(45,45)==Color.GREEN.getRGB(),"La opacidad afecta a los hijos");
            verificar(SwingUtilities.getDeepestComponentAt(capas,5,5)==superior,"Los clics atraviesan la capa");
            AtomicInteger clics=new AtomicInteger();PanelMapa mapaVisual=new PanelMapa(c->clics.incrementAndGet());
            Mapa mapa=new Mapa();mapa.colocar(new Nucleo("Núcleo",100),12,12);mapaVisual.setMapa(mapa);
            JButton boton=(JButton)mapaVisual.getComponent(312);verificar(boton.getIcon()!=null,"Sin sprite");
            // Simule el estado visual que deja una celda seleccionada o bajo el puntero.
            boton.getModel().setSelected(true); boton.getModel().setRollover(true);
            boton.getModel().setArmed(true); boton.getModel().setPressed(true);
            mapaVisual.setBotonesActivos(false);
            verificar(!boton.getModel().isSelected() && !boton.getModel().isRollover()
                    && !boton.getModel().isArmed() && !boton.getModel().isPressed(), "Conserva el gesto del clic detrás de la capa");
            verificar(!boton.isFocusPainted() && !boton.isFocusable(), "La celda bloqueada conserva el foco visual");
            verificar(clics.get()==0, "Soltar la celda al bloquear dispara una acción");
            verificar(boton.isVisible() && !boton.isEnabled(),"Oculta las unidades al bloquear");
            verificar(boton.getDisabledIcon()==boton.getIcon(),"Decolora el sprite bloqueado");
            boton.doClick();verificar(clics.get()==0,"El tablero deshabilitado recibe clics");
            mapaVisual.setBotonesActivos(true);boton.doClick();verificar(clics.get()==1,"No recupera los clics");
            boolean rechazo=false;try{superior.setOpacidad(Float.NaN);}catch(IllegalArgumentException e){rechazo=true;}
            verificar(rechazo,"Acepta opacidad inválida");
        });
        System.out.println("OK transparencia 75%, controles opacos, puntero capturado, sprites visibles y clics bloqueados/restaurados.");
    }
}
