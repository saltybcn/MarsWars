import configuracion.*;
import modelo.componentes.*;
import modelo.atacantes.Atacante;
import modelo.defensas.Dron;
import modelo.juego.*;
import persistencia.Archivo;
import com.google.gson.Gson;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Se ejecuta en una carpeta temporal y no altera el catálogo entregado. */
public class PruebasVelocidadYLimpieza {
    static void comprobar(boolean valor, String mensaje) { if (!valor) throw new AssertionError(mensaje); }
    static void imagen(String nombre, int color) throws Exception {
        BufferedImage dibujo = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        dibujo.setRGB(0, 0, color); ImageIO.write(dibujo, "png", Path.of(nombre).toFile());
    }
    public static void main(String[] args) throws Exception {
        for (String tipo : new String[]{"Acechador", "Escupidor", "Enjambre", "Demoledor", "Volador", "Dron"}) {
            Configuracion c = new Configuracion("Prueba", tipo, 100, 1, 1, 1, 1, 1);
            c.setMaxObjetivos(2); c.setIntervaloMovimiento(0.275);
            Configuracion leida = new Gson().fromJson(new Gson().toJson(c), Configuracion.class);
            Combate unidad = leida.crearUnidad();
            int intervalo = unidad instanceof Atacante a ? a.getVelMov() : ((Dron) unidad).getVelMov();
            comprobar(intervalo == 275, "Conversión de movimiento: " + tipo);
            for (double invalido : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 3e9}) {
                c.setIntervaloMovimiento(invalido); comprobar(!c.validar(), "Admite intervalo inválido: " + tipo);
            }
        }
        comprobar(new Gson().fromJson("{}", Configuracion.class).getIntervaloMovimiento() == 0.5,
                "El catálogo antiguo pierde el intervalo de 500 ms");
        imagen("roja.png", 0xffff0000); imagen("azul.png", 0xff0000ff);
        Configuracion c = new Configuracion("Prueba", "Acechador", 100, 1, 1, 1, 1, 1);
        c.setRutaIdle("roja.png"); c.setRutaAtaque("roja.png");
        Catalogo catalogo = new Catalogo(); catalogo.agregar(c); Archivo archivo = new Archivo();
        archivo.guardarCatalogo(catalogo, "catalogo.json"); Path roja = Path.of(c.getRutaIdle());
        c.setRutaIdle("azul.png"); archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(Files.exists(roja), "Borra una imagen que todavía usa la pose de ataque");
        c.setRutaAtaque(c.getRutaIdle()); archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(!Files.exists(roja), "Conserva una imagen sustituida sin referencias");
        Path azul = Path.of(c.getRutaIdle());
        Mapa mapa = new Mapa(); mapa.colocar(new Nucleo("Núcleo", 100), 12, 12);
        Partida partida = new Partida("Prueba", mapa); partida.prepararMision(catalogo);
        archivo.guardarPartida(partida, null);
        c.setRutaIdle("roja.png"); c.setRutaAtaque("roja.png");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(Files.exists(azul), "Borra un recurso usado por las plantillas de un guardado");
        Partida recuperada = archivo.cargarPartida("partidas/save0.dat");
        comprobar(recuperada.getCatalogo().getConfigs().get(0).crearUnidad().getImgIdle() != null,
                "La partida recuperada pierde imágenes al generar unidades");
        Archivo.eliminarPartida("partidas/save0.dat"); archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(!Files.exists(azul), "No limpia cuando desaparece la última referencia del guardado");
        comprobar(Files.exists(Path.of("roja.png")) && Files.exists(Path.of("azul.png")), "Borra los originales externos");
        Path actual = Path.of(c.getRutaIdle()); Files.writeString(Path.of("bloqueado"), "archivo");
        c.setRutaIdle("azul.png"); c.setRutaAtaque("azul.png");
        try { archivo.guardarCatalogo(catalogo, "bloqueado/catalogo.json"); throw new AssertionError("Guardado inválido aceptado"); }
        catch (java.io.UncheckedIOException esperado) { }
        comprobar(Files.exists(actual), "Un guardado fallido elimina recursos del catálogo anterior");
        System.out.println("OK movimiento de seis tipos, JSON anterior, limpieza, poses compartidas, guardados y fallo de escritura.");
    }
}
