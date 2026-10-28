import java.util.ArrayList;
import java.util.List;
import modelo.atacantes.Atacante;
import modelo.atacantes.Volador;
import modelo.componentes.Combate;
import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.defensas.Barrera;
import modelo.defensas.Contacto;
import modelo.defensas.Dron;
import modelo.juego.Mapa;

/** Pruebas sin interfaz gráfica: ejecutar main con las clases del proyecto. */
public class PruebasRefactorCombate {
    static class Explorador extends Atacante {
        List<Componente> recibidos;
        int llamadas;
        Explorador() { setVida(100); setAlcance(1); setDmg(10); setVelAtq(1000); }
        Componente buscar() { return scan(); }
        void turno() { actuar(); }
        void paso(Componente objetivo) { avanzarHacia(objetivo); }
        @Override public void atacar(List<Componente> objetivos) {
            recibidos = new ArrayList<>(objetivos);
            llamadas++;
            super.atacar(objetivos); // Solo registra lo recibido, no implementa ataque múltiple
            objetivos.clear(); // La copia no debe alterar detectados
        }
    }
    static class Vuelo extends Volador {
        Vuelo(int alcance) { super("Volador", 100, 10, 1000, alcance, 1); setVelMov(200); }
        void turno() { actuar(); }
        long espera() { return esperaMovimiento; }
        void terminarSalto() { esperaMovimiento = 0; }
    }
    static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    static Contacto defensa(Mapa mapa, int fila, int col) {
        Contacto d = new Contacto("Defensa", 100, 1, 1000, 1);
        comprobar(mapa.colocar(d, fila, col), "Colocación defensa");
        return d;
    }
    static Barrera muro(Mapa mapa, int fila, int col) {
        Barrera b = new Barrera("Muro", 100, 1);
        comprobar(mapa.colocar(b, fila, col), "Colocación muro");
        return b;
    }
    public static void main(String[] args) throws Exception {
        Mapa mapa = new Mapa();
        Explorador a = new Explorador(); a.setAlcance(2); mapa.colocar(a, 5, 5);
        Contacto d1 = defensa(mapa, 5, 6), d2 = defensa(mapa, 5, 7);
        a.turno();
        comprobar(a.recibidos.size() == 2, "actuar entrega todos los detectados en alcance");
        comprobar(a.getDetectados().size() == 2, "La copia del ataque no altera detectados");
        comprobar(d1.getVida() == 90 && d2.getVida() == 100, "El ataque común solo golpea uno");
        a.turno(); comprobar(a.llamadas == 1, "Respeta el intervalo de ataque");
        mapa.retirar(5, 6); a.buscar();
        comprobar(a.getDetectados().size() == 1 && a.getDetectados().get(0) == d2, "Retira detectados ausentes");
        System.out.println("OK lista completa, copia, ataque común e intervalo");

        mapa = new Mapa(); a = new Explorador(); mapa.colocar(a, 5, 2);
        Contacto objetivo = defensa(mapa, 5, 8);
        for (int f = 3; f <= 7; f++) { Explorador aliado = new Explorador(); mapa.colocar(aliado, f, 4); }
        comprobar(a.buscar() == objetivo && a.getDetectados().isEmpty(), "Persigue sin incluir enemigos lejanos en detectados");
        int pasos = 0;
        while (a.getDetectados().isEmpty() && pasos++ < 30) {
            a.paso(a.buscar()); a.buscar();
        }
        comprobar(pasos < 30 && a.getDetectados().contains(objetivo), "Rodea aliados y después detecta");
        System.out.println("OK persecución y rodeo de aliados");

        mapa = new Mapa(); a = new Explorador(); mapa.colocar(a, 10, 12);
        Nucleo n = new Nucleo(); n.setVida(100); mapa.colocar(n, 12, 12);
        Barrera b = muro(mapa, 11, 12);
        muro(mapa, 11, 11); muro(mapa, 11, 13);
        for (int c = 11; c <= 13; c++) muro(mapa, 13, c);
        muro(mapa, 12, 11); muro(mapa, 12, 13);
        defensa(mapa, 5, 20);
        Componente bloqueoElegido = a.buscar();
        comprobar(bloqueoElegido instanceof Barrera, "El cerco también se detecta en contacto");
        a.turno(); comprobar(bloqueoElegido.getVida() == 90 && a.recibidos.get(0) == bloqueoElegido,
                "El ataque recibe y daña una barrera del cerco");
        a.turno(); comprobar(bloqueoElegido.getVida() == 90, "Intervalo al romper muros");

        for (int alcance : new int[]{1, 2}) {
            mapa = new Mapa(); Vuelo v = new Vuelo(alcance); mapa.colocar(v, 0, 2);
            Contacto d = defensa(mapa, 0, 0);
            Barrera b1 = muro(mapa, 0, 1), b2 = muro(mapa, 1, 0), b3 = muro(mapa, 1, 1);
            v.turno();
            comprobar(alcance == 2 ? d.getVida() == 90 && b1.getVida() + b2.getVida() + b3.getVida() == 300
                    : d.getVida() == 100 && b1.getVida() + b2.getVida() + b3.getVida() == 300,
                    "Vuelo ataca sobre muros si tiene alcance; de lo contrario espera sin romperlos");
        }
        mapa = new Mapa(); Vuelo v = new Vuelo(1); mapa.colocar(v, 5, 2);
        defensa(mapa, 5, 7); Barrera b1 = muro(mapa, 5, 3), b2 = muro(mapa, 5, 4);
        v.turno();
        comprobar(v.getPos()[1] == 5 && v.espera() == 600_000_000L, "Salto de tres casillas cobra tres intervalos");
        comprobar(mapa.obtener(5, 3) == b1 && mapa.obtener(5, 4) == b2, "No sobrescribe obstáculos");
        Contacto cercano = defensa(mapa, 5, 6); v.turno();
        comprobar(cercano.getVida() == 100, "No ataca durante el salto");
        v.terminarSalto(); v.turno(); comprobar(cercano.getVida() == 90, "Ataca después del salto");
        System.out.println("OK overrides aéreos, obstáculos y tiempos");

        mapa = new Mapa(); Dron dron = new Dron("Dron", 100, 10, 1000, 1, 1); mapa.colocar(dron, 0, 0);
        a = new Explorador(); mapa.colocar(a, 0, 5);
        comprobar(dron.scan() == a && dron.getDetectados().isEmpty(), "Dron persigue fuera de alcance");
        mapa.mover(a, 0, 1);
        comprobar(dron.scan() == a && dron.getDetectados().size() == 1, "Dron detecta en alcance");
        a.setAereo(true); comprobar(dron.scan() == null && dron.getDetectados().isEmpty(), "Filtro aéreo limpia detectados");
        dron.setAtacaAereos(true); comprobar(dron.scan() == a, "Recupera enemigo aéreo válido");

        mapa = new Mapa(); Contacto fija = defensa(mapa, 5, 5);
        Explorador primero = new Explorador(); mapa.colocar(primero, 5, 6); fija.scan();
        Explorador segundo = new Explorador(); mapa.colocar(segundo, 4, 5);
        comprobar(fija.scan() == primero && fija.getDetectados().size() == 2, "Defensa conserva FIFO");
        primero.recibirDmg(100); comprobar(fija.scan() == segundo, "Limpia detectado muerto");
        mapa.mover(segundo, 0, 0); comprobar(fija.scan() == null, "Defensa fija no persigue");
        System.out.println("OK Dron, filtro aéreo y FIFO de defensa fija");
        comprobar(java.lang.reflect.Modifier.isAbstract(Combate.class.getDeclaredMethod("scan").getModifiers()), "scan abstracto");
        System.out.println("Todas las pruebas de refactorización pasaron.");
    }
}
