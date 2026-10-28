import configuracion.*;
import modelo.atacantes.Atacante;
import modelo.componentes.*;
import modelo.defensas.Contacto;
import modelo.juego.*;
import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Repite transiciones reales; comprueba que no sobrevivan hilos del escenario anterior. */
public class PruebasFinCombates {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    private static Thread obtenerHilo(Object objeto, Class<?> clase, String atributo) throws Exception {
        Field campo = clase.getDeclaredField(atributo); campo.setAccessible(true);
        return (Thread) campo.get(objeto);
    }
    public static void main(String[] args) throws Exception {
        Mapa mapa = new Mapa(); mapa.colocar(new Nucleo("Núcleo", 10000), 12, 12);
        Partida partida = new Partida("Prueba de ciclos", mapa);
        Contacto defensa = new Contacto("Defensa", 100000, 0, 5000, 1);
        mapa.colocar(defensa, 11, 12); partida.getEscuadron().agregar(defensa);
        Catalogo catalogo = new Catalogo();
        catalogo.agregar(new Configuracion("Enemigo", "Acechador", 100000, 1, 5, 1, 20, 1));
        partida.prepararMision(catalogo);
        ArrayList<Thread> todos = new ArrayList<>();
        long mayorCierre = 0;
        try {
            for (int ciclo = 0; ciclo < 24; ciclo++) {
                Batalla batalla = partida.getBatalla();
                batalla.iniciar();
                ArrayList<Thread> anteriores = new ArrayList<>();
                synchronized (partida.getMapa()) {
                    for (Componente c : batalla.getParticipantes()) if (c instanceof Combate combate) {
                        Thread hilo = obtenerHilo(combate, Combate.class, "hilo");
                        if (hilo != null) anteriores.add(hilo);
                    }
                    Thread monitor = obtenerHilo(batalla, Batalla.class, "comprobador");
                    comprobar(monitor != null, "Monitor creado"); anteriores.add(monitor);
                }
                comprobar(anteriores.size() >= 3, "Hilos independientes de enemigo, defensa y monitor");
                todos.addAll(anteriores);
                int capacidadAnterior = partida.getEscuadron().getCapacidadTotal();
                if (ciclo % 4 == 0) {
                    partida.getMapa().getNucleo().setVida(4321);
                    for (Componente c : batalla.getParticipantes())
                        if (c instanceof Atacante) c.recibirDmg(c.getVida());
                    batalla.comprobarResultado();
                    comprobar("VICTORIA".equals(batalla.getResultado()), "Victoria real");
                    partida.avanzar();
                    comprobar(partida.getMapa().getNucleo().getVida() == 4321, "Mantiene daño tras ganar");
                } else if (ciclo % 4 == 1) {
                    partida.getMapa().getNucleo().recibirDmg(10000);
                    batalla.comprobarResultado();
                    comprobar("DERROTA".equals(batalla.getResultado()), "Derrota real");
                    partida.avanzar();
                    comprobar(partida.getMapa().getNucleo().getVida() == 10000, "Restaura el núcleo destruido al avanzar");
                } else if (ciclo % 4 == 2) {
                    batalla.pausar(); batalla.finalizar();
                } else {
                    partida.repetir(); // Reiniciar durante combate también detiene el escenario anterior.
                }
                if (ciclo % 4 <= 1)
                    comprobar(partida.getEscuadron().getCapacidadTotal() == capacidadAnterior + 5,
                            "Aumenta capacidad después de cualquiera de los dos resultados");
                long inicio = System.nanoTime();
                // Nunca espere un hilo manteniendo el bloqueo del mapa: necesitaría liberarlo para terminar.
                long limite = inicio + 2_000_000_000L;
                for (Thread hilo : anteriores) {
                    long restante = limite - System.nanoTime();
                    if (restante > 0) hilo.join(Math.max(1, restante / 1_000_000L));
                    comprobar(!hilo.isAlive(), "Hilo sobreviviente: " + hilo.getName() + " " + hilo.getState());
                }
                mayorCierre = Math.max(mayorCierre, System.nanoTime() - inicio);
                partida.repetir();
                comprobar(partida.getMapa().getNucleo().getVida() == 10000, "Reiniciar cura completamente");
                comprobar(!partida.getBatalla().isEnCurso(), "Reiniciar no arranca hilos");
                comprobar(ManagementFactory.getThreadMXBean().findDeadlockedThreads() == null, "Sin bloqueos mutuos");
            }
            comprobar(todos.stream().noneMatch(Thread::isAlive), "No se acumulan hilos de combates anteriores");
            System.out.printf("OK 24 combates/transiciones, %d hilos terminados, máximo cierre observado %.2f ms; núcleo/capacidad correctos.%n",
                    todos.size(), mayorCierre / 1e6);
        } finally { partida.getBatalla().finalizar(); }
    }
}
