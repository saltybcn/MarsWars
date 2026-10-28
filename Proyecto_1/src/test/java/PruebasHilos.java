import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import modelo.atacantes.Atacante;
import modelo.componentes.Combate;

/** Comprueba el ciclo de vida del hilo que administra cada combatiente. */
public class PruebasHilos {
    static class Contador extends Atacante {
        final AtomicInteger acciones = new AtomicInteger();
        Contador() { super("Contador", 100, 1, 1000, 1, 1); }
        @Override protected void actuar() { acciones.incrementAndGet(); }
    }
    static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    static Thread hiloDe(Combate unidad) throws Exception {
        Field campo = Combate.class.getDeclaredField("hilo");
        campo.setAccessible(true);
        synchronized (unidad) { return (Thread) campo.get(unidad); }
    }
    static void detener(Combate unidad) throws Exception {
        Thread hilo = hiloDe(unidad);
        unidad.detener();
        if (hilo != null) { hilo.join(1000); comprobar(!hilo.isAlive(), "Hilo detenido"); }
    }
    static void esperarAccion(Contador unidad, int anteriores) throws InterruptedException {
        long limite = System.nanoTime() + 1_000_000_000L;
        while (unidad.acciones.get() == anteriores && System.nanoTime() < limite) Thread.sleep(5);
        comprobar(unidad.acciones.get() > anteriores, "El hilo ejecuta acciones");
    }
    public static void main(String[] args) throws Exception {
        Contador unidad = new Contador();
        CopyOnWriteArrayList<Thread> observados = new CopyOnWriteArrayList<>();
        ArrayList<Thread> llamadas = new ArrayList<>();
        try {
            for (int i = 0; i < 12; i++) {
                Thread llamada = new Thread(() -> {
                    unidad.start();
                    try { observados.add(hiloDe(unidad)); }
                    catch (Exception e) { throw new AssertionError(e); }
                });
                llamadas.add(llamada); llamada.start();
            }
            for (Thread llamada : llamadas) llamada.join(1000);
            comprobar(observados.size() == 12, "Todas las llamadas terminaron");
            Thread primero = hiloDe(unidad);
            for (Thread observado : observados) comprobar(observado == primero, "start concurrente no duplica hilo");
            esperarAccion(unidad, 0);
            unidad.pausar(); Thread.sleep(50);
            int antes = unidad.acciones.get();
            unidad.start(); Thread.sleep(80);
            comprobar(hiloDe(unidad) == primero && unidad.acciones.get() == antes, "start no reemplaza ni reanuda un hilo pausado");
            unidad.reanudar(); esperarAccion(unidad, antes);
            unidad.pausar(); detener(unidad);
            antes = unidad.acciones.get();
            unidad.start(); esperarAccion(unidad, antes);
            comprobar(hiloDe(unidad) != primero, "Reinicio crea un Thread nuevo");
            System.out.println("OK inicio concurrente, pausa, reanudación, detención y reinicio");

            // Los archivos conservan el estado de la unidad, no el Thread.
            unidad.pausar(); Thread.sleep(50);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) { salida.writeObject(unidad); }
            Contador cargada;
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                cargada = (Contador) entrada.readObject();
            }
            comprobar(hiloDe(cargada) == null, "Thread transient al cargar");
            try {
                antes = cargada.acciones.get(); cargada.start(); esperarAccion(cargada, antes);
                comprobar(hiloDe(cargada) != primero, "Unidad cargada crea su propio hilo");
            } finally { detener(cargada); }
            System.out.println("OK serialización y nueva ejecución después de cargar");
        } finally { detener(unidad); }

        Contador muerta = new Contador(); muerta.recibirDmg(100); muerta.start();
        comprobar(hiloDe(muerta) == null, "No inicia unidades destruidas");
        Contador manual = new Contador();
        try { manual.run(); throw new AssertionError("run no debe ejecutarse manualmente"); }
        catch (IllegalStateException esperado) { }
        System.out.println("OK unidad destruida y protección del punto de entrada");
    }
}
