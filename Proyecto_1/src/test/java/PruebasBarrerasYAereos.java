import configuracion.*;
import modelo.atacantes.*;
import modelo.componentes.*;
import modelo.defensas.*;
import modelo.juego.*;
import persistencia.Archivo;

/** Regresiones: las barreras reciben daño y el permiso antiaéreo depende de la configuración. */
public class PruebasBarrerasYAereos {
    static class Grupo extends Enjambre {
        Grupo(){super("Enjambre",100,10,1000,2,1,3);}
        void turno(){actuar();}
    }
    static class Vuelo extends Volador {
        Vuelo(){super("Volador",100,10,1000,2,1);}
        void turno(){actuar();}
    }
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args){
        Mapa mapa = new Mapa(); Grupo grupo = new Grupo();
        Barrera izquierda = new Barrera("Izquierda",100,1), derecha = new Barrera("Derecha",100,1);
        Contacto defensa = new Contacto("Defensa",100,1,1000,1);
        mapa.colocar(grupo,10,12); mapa.colocar(izquierda,10,11); mapa.colocar(derecha,10,13); mapa.colocar(defensa,9,12);
        grupo.turno();
        comprobar(izquierda.getVida()==90 && derecha.getVida()==90 && defensa.getVida()==90,"Enjambre golpea defensas y barreras en la misma ronda");
        grupo.turno();comprobar(izquierda.getVida()==90,"No repite golpes sin esperar intervalo");
        comprobar(!mapa.mover(grupo,10,11),"La barrera sigue bloqueando su casilla");
        mapa = new Mapa(); Vuelo volador = new Vuelo();
        Barrera muro = new Barrera("Muro",100,1); mapa.colocar(volador,5,5); mapa.colocar(muro,5,6); volador.turno();
        comprobar(muro.getVida()==100 && volador.getDetectados().isEmpty(),"Volador ignora barreras terrestres");
        Dron dron = new Dron("Dron terrestre",100,10,1000,3,1); mapa.colocar(dron,4,5);
        comprobar(dron.scan()==null,"Dron sin autorización no detecta aéreos");
        dron.atacar(volador);comprobar(volador.getVida()==100,"Dron sin autorización no daña aéreos");
        for(String ruta:new String[]{"src/main/java/persistencia/catalogo.json","src/main/java/persistencia/catalogo_pruebas.json"}) {
            Catalogo catalogo=new Archivo().cargarCatalogo(ruta);
            for(Configuracion c:catalogo.getConfigs())if("Dron".equals(c.getTipo()))
                comprobar(!c.isAtacaAereos() && !c.crearUnidad().isAtacaAereos(),"Catálogo de drones terrestres sin permiso antiaéreo");
        }
        System.out.println("OK Enjambre/barreras, intervalo, ocupación, Volador y Dron sin autorización aérea.");
    }
}
