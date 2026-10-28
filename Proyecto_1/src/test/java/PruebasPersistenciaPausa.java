import java.io.*;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;
import modelo.componentes.*;
import modelo.atacantes.Atacante;
import modelo.defensas.Contacto;
import modelo.juego.*;
import configuracion.*;
import persistencia.Archivo;

/** Ejecute en una carpeta temporal: no utiliza los guardados del jugador. */
public class PruebasPersistenciaPausa {
    static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    static Object campo(Object objeto, Class<?> clase, String nombre) throws Exception {
        Field f = clase.getDeclaredField(nombre); f.setAccessible(true); return f.get(objeto);
    }
    static class CatalogoFallido extends Catalogo {
        Object fallo = new Object(); // Provoca un fallo real después de escribir la cabecera.
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of("partidas"))) throw new IllegalStateException("Use una carpeta temporal vacía.");
        Archivo archivo = new Archivo();
        Mapa mapa = new Mapa();
        Nucleo nucleo = new Nucleo("Núcleo", 10000);
        mapa.colocar(nucleo, 12, 12);
        Catalogo catalogo = new Catalogo();
        catalogo.agregar(new Configuracion("Enemigo", "Acechador", 1000, 1, 1, 1, 20, 1));
        Partida partida = new Partida("Comandante prueba", mapa);
        partida.prepararMision(catalogo);
        Map<String, String> posiciones = new HashMap<>();
        Atacante atacante = null;
        for (Componente[] fila : mapa.getMat()) for (Componente c : fila) {
            if (c instanceof Atacante a) { posiciones.put(c.getId(), Arrays.toString(c.getPos())); atacante = a; }
        }
        verificar(atacante != null, "Enemigos preparados");
        Contacto defensa = new Contacto("Colocada después", 10000, 1, 200, 1);
        mapa.colocar(defensa, 11, 11);partida.getEscuadron().agregar(defensa);
        partida.guardarEstadoInicial(); // Reintento y guardado conservan la colocación; solo repetir elimina el daño posterior.
        Batalla batalla = partida.getBatalla();
        try {
            batalla.iniciar(); Thread.sleep(80); batalla.pausar();
            long espera = (Long) campo(atacante, Combate.class, "esperaMovimiento");
            Thread hilo = (Thread) campo(atacante, Combate.class, "hilo");
            Thread.sleep(200);
            verificar((Long) campo(atacante, Combate.class, "esperaMovimiento") == espera, "Pausa congela intervalo");
            nucleo.setVida(5000); // Simule daño posterior a la preparación.
            archivo.guardarPartida(partida, null);
            verificar(nucleo.getVida() == 5000 && mapa.obtener(11, 11) == defensa, "Guardar no modifica la partida actual");
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("partidas/save0.dat"))) {
                verificar(in.readUTF().equals("Comandante prueba") && in.readInt() == 1, "Cabecera legible sin cargar");
            }
            Partida cargada = archivo.cargarPartida("partidas/save0.dat");
            verificar(!cargada.getBatalla().isEnCurso() && cargada.getBatalla().isIniciada()
                    && cargada.getBatalla().isPausada(), "Carga pausada y sin hilos");
            verificar(cargada.getMapa().getNucleo().getVida() == 5000, "Conserva daño del combate");
            verificar(cargada.getMapa().obtener(11, 11) == cargada.getEscuadron().getDefensas().get(0), "Conserva referencias compartidas");
            verificar(!cargada.getBatalla().getParticipantes().isEmpty(), "Conserva participantes");
            for (Componente[] fila : cargada.getMapa().getMat()) for (Componente c : fila) {
                if (c instanceof Atacante a) {
                    verificar(Arrays.equals(a.getPos(), atacante.getPos()), "Posición actual exacta");
                    verificar(campo(a, Combate.class, "hilo") == null, "Sin hilos al cargar");
                    verificar((Long)campo(a, Combate.class, "esperaMovimiento") == espera, "Conserva intervalo restante");
                }
            }
            cargada.repetir();
            verificar(cargada.getMapa().obtener(11, 11) != null && cargada.getMapa().getNucleo().getVida() == 10000,
                    "Repetir conserva defensas y elimina daño del combate");
            // Un fallo de serialización después de la cabecera no debe reemplazar el archivo anterior.
            byte[] anterior = Files.readAllBytes(Path.of("partidas/save0.dat"));
            Field campoCatalogo = Partida.class.getDeclaredField("catalogo"); campoCatalogo.setAccessible(true);
            campoCatalogo.set(partida, new CatalogoFallido());
            boolean rechazo = false;
            try { archivo.guardarPartida(partida, "partidas/save0.dat"); }
            catch (NotSerializableException e) { rechazo = true; }
            verificar(rechazo && Arrays.equals(anterior, Files.readAllBytes(Path.of("partidas/save0.dat"))), "Fallo conserva slot anterior");
            campoCatalogo.set(partida, catalogo);
            try (var archivos = Files.list(Path.of("partidas"))) {
                verificar(archivos.noneMatch(p -> p.toString().endsWith(".tmp")), "Limpia temporales");
            }
            archivo.guardarPartida(partida, null);
            verificar(Archivo.listarPartidas().size() == 2, "Permite varios slots del mismo comandante");
            Partida segundoSlot = archivo.cargarPartida("partidas/save1.dat");
            verificar(segundoSlot.getNombreComandante().equals(partida.getNombreComandante()),
                    "El segundo slot conserva el nombre del comandante");
            verificar(Arrays.equals(anterior, Files.readAllBytes(Path.of("partidas/save0.dat"))),
                    "Crear otro slot no sobrescribe el primero");
            batalla.reanudar();
            verificar(campo(atacante, Combate.class, "hilo") == hilo, "Continuar reutiliza hilo");
            rechazo = false;
            try { archivo.guardarPartida(partida, "partidas/save0.dat"); }
            catch (IOException e) { rechazo = true; }
            verificar(rechazo, "No guarda combate activo");
            batalla.pausar();nucleo.setVida(0);batalla.comprobarResultado();
            verificar("PENDIENTE".equals(batalla.getResultado()), "Monitor respeta pausa");
            batalla.reanudar();batalla.comprobarResultado();
            verificar("DERROTA".equals(batalla.getResultado()), "Resuelve al continuar");
            hilo.join(1000);verificar(!hilo.isAlive(), "Finalizar detiene hilos");
            System.out.println("OK pausa, carga exacta, posiciones, intervalos, varios slots por comandante y escritura sin pérdida");
        } finally { batalla.finalizar(); }
    }
}
