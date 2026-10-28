import configuracion.Auth;
import modelo.atacantes.*;
import modelo.defensas.*;
import modelo.componentes.*;
import modelo.juego.*;
import vista.PanelDetalle;
import java.nio.file.*;

/** Ejecute desde un directorio temporal vacío; crea src/main/java/persistencia/admin.json. */
public class PruebasEntrega {
    static void verificar(boolean c, String mensaje) { if (!c) throw new AssertionError(mensaje); }
    static class Explorador extends Acechador {
        Explorador() { super("Explorador", 100, 2, 300, 1, 100); }
        void turno() { actuar(); }
        void listo() { esperaMovimiento = 0; }
    }
    public static void main(String[] args) throws Exception {
        verificar(!Files.exists(Path.of("src/main/java/persistencia/admin.json")) && !Files.exists(Path.of("datos/admin.json")), "Use una carpeta temporal vacía");
        verificar(Auth.cargar() == null, "Primer uso");
        Auth auth = new Auth(); String codigo = auth.configurar("admin", "clave-primera".toCharArray());
        String archivo = Files.readString(Path.of("src/main/java/persistencia/admin.json"));
        verificar(!archivo.contains("clave-primera") && !archivo.contains(codigo), "No guardar secretos legibles");
        auth = Auth.cargar(); verificar(auth.validar("admin", "clave-primera"), "Carga de credenciales");
        verificar(!auth.validar("admin", "incorrecta") && !auth.recuperar("incorrecto".toCharArray()), "Rechazo de acceso");
        verificar(auth.recuperar(codigo.toCharArray()), "Código recupera");
        String nuevo = auth.configurar("nuevo", "clave-segunda".toCharArray());
        verificar(!auth.recuperar(codigo.toCharArray()) && auth.recuperar(nuevo.toCharArray()), "Rotación del código");
        verificar(!auth.validar("admin", "clave-primera") && Auth.cargar().validar("nuevo", "clave-segunda"), "Cambio persistente");
        Files.writeString(Path.of("src/main/java/persistencia/admin.json"), "{}"); boolean rechazo = false;
        try { Auth.cargar(); } catch (java.io.IOException e) { rechazo = true; }
        verificar(rechazo, "Archivo corrupto no abre ni reinicia acceso");
        Mapa mapa = new Mapa(); Demoledor demo = new Demoledor("Demo", 100, 30, 500, 1, 3);
        Contacto lejano = new Contacto("Lejano", 100, 1, 500, 1), cerca = new Contacto("Cerca", 100, 1, 500, 1);
        mapa.colocar(demo, 5, 5); mapa.colocar(lejano, 5, 7);
        demo.atacar(java.util.List.of(lejano)); verificar(demo.getVida() == 100 && lejano.getVida() == 100, "No explota a distancia");
        mapa.colocar(cerca, 5, 6); demo.atacar(java.util.List.of(cerca));
        verificar(demo.getVida() == 0 && lejano.getVida() == 70 && cerca.getVida() == 70, "Contacto detona toda el área");
        verificar(java.util.Arrays.equals(demo.getPosicionFinal(), new int[]{5,5}) && mapa.obtener(5,5) == null, "Posición final conservada");
        String detalle = PanelDetalle.describir(demo);
        verificar(detalle.contains("Lejano") && detalle.contains("Cerca") && detalle.contains("[5, 5]"), "Detalle de destruido e historial");
        mapa = new Mapa(); Explorador e = new Explorador(); mapa.colocar(e, 1, 1);
        mapa.colocar(new Contacto("Objetivo", 1000, 1, 1000, 1), 1, 8);
        e.turno(); verificar(e.isPasoAlterno(), "Primer paso muestra sprite 2");
        e.listo(); e.turno(); verificar(!e.isPasoAlterno(), "Segundo paso muestra sprite 1");
        // El reloj de ataque real debe volver a Idle antes del siguiente golpe.
        Contacto d = new Contacto("Atacante", 1000, 1, 400, 1);
        Acechador a = new Acechador("Blanco", 1000, 0, 1000, 1, 1000);
        mapa = new Mapa(); mapa.colocar(d, 4, 4); mapa.colocar(a, 4, 5);
        try {
            d.start(); long limite = System.nanoTime() + 1_000_000_000L;
            while (!d.isAtaqueVisible() && System.nanoTime() < limite) Thread.sleep(5);
            verificar(d.isAtaqueVisible(), "Gesto de ataque iniciado");
            d.pausar(); Thread.sleep(250); verificar(d.isAtaqueVisible(), "Pausa congela animación");
            d.reanudar(); Thread.sleep(260); verificar(!d.isAtaqueVisible(), "Vuelve a Idle en el intervalo");
        } finally { d.detener(); }
        System.out.println("OK autenticación, recuperación, Demoledor, posición final, historial y fases de animación.");
    }
}
