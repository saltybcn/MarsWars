import audio.Sonidos;
import configuracion.Catalogo;
import configuracion.Configuracion;
import modelo.componentes.Nucleo;
import modelo.defensas.Contacto;
import modelo.juego.Mapa;
import modelo.juego.Partida;
import persistencia.Archivo;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.*;

/** Se ejecuta en una carpeta temporal; conserva guardados y originales externos. */
public class PruebasLimpiezaSonidos {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void crearWav(Path destino, int muestras) throws Exception {
        byte[] pcm = new byte[muestras * 2];
        AudioFormat formato = new AudioFormat(8000, 16, 1, true, false);
        try (AudioInputStream entrada = new AudioInputStream(new ByteArrayInputStream(pcm), formato, muestras)) {
            AudioSystem.write(entrada, AudioFileFormat.Type.WAVE, destino.toFile());
        }
    }

    public static void main(String[] args) throws Exception {
        Archivo archivo = new Archivo();
        Catalogo catalogo = new Catalogo();
        Configuracion config = new Configuracion("Contacto ágil 🚀", "Contacto", 100, 1, 1, 1, 1, 1);
        catalogo.agregar(config);

        // Los campos vacíos no crean copias del fallback, ni siquiera la carpeta de unidad.
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(config.getSonidoAtacando() == null && config.getSonidoDestruido() == null,
                "Rellena sonidos vacíos con copias genéricas");
        comprobar(!Files.exists(Path.of("recursos/unidades")), "Importa recursos sin personalización");

        config.setSonidoAtacando("sonidos/ataque_defensa.wav");
        // También migra un genérico antiguo renombrado, comparando el contenido real.
        try (var entrada = Sonidos.class.getResourceAsStream("/sonidos/muerte_defensa.wav")) {
            Files.copy(entrada, Path.of("generico_antiguo.wav"));
        }
        config.setSonidoDestruido("generico_antiguo.wav");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar("sonidos/ataque_defensa.wav".equals(config.getSonidoAtacando())
                && "sonidos/muerte_defensa.wav".equals(config.getSonidoDestruido()), "No reutiliza los genéricos globales");
        comprobar(!Files.exists(Path.of("recursos/unidades")), "Duplica los WAV predeterminados");
        for (Sonidos.Evento evento : Sonidos.Evento.values()) {
            config.setSonidoAtacando(evento.getRuta());
            archivo.guardarCatalogo(catalogo, "catalogo.json");
            comprobar(evento.getRuta().equals(config.getSonidoAtacando()), "Copia un genérico de otro evento");
        }
        comprobar(!Files.exists(Path.of("recursos/unidades")), "Importa un sonido global de menú o resultado");

        // Un WAV externo con el mismo nombre que el genérico sigue siendo personalizado.
        Files.createDirectories(Path.of("sonidos"));
        Path primero = Path.of("sonidos/ataque_defensa.wav"), segundo = Path.of("segundo.wav");
        crearWav(primero, 400); crearWav(segundo, 900);
        config.setSonidoAtacando(primero.toString()); config.setSonidoDestruido(primero.toString());
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        Path importado = Path.of(config.getSonidoAtacando());
        comprobar(importado.startsWith(Path.of("recursos/unidades")), "Confunde un archivo externo con el genérico");
        comprobar(config.getSonidoAtacando().equals(config.getSonidoDestruido()), "Duplica un WAV entre eventos");
        config.setSonidoAtacando(segundo.toString());
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(Files.exists(importado), "Elimina el sonido que todavía usa destrucción");
        config.setSonidoDestruido("");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(!Files.exists(importado), "Conserva un sonido sustituido sin referencias");

        // La única referencia antigua queda dentro del byte[] para repetir, no en el catálogo del slot.
        Path protegido = Path.of(config.getSonidoAtacando());
        Mapa mapa = new Mapa(); mapa.colocar(new Nucleo("Núcleo", 100), 12, 12);
        Contacto defensa = (Contacto) config.crearUnidad();
        mapa.colocar(defensa, 11, 12);
        Partida partida = new Partida("Prueba de audio", mapa);
        partida.getEscuadron().agregar(defensa);
        Catalogo enemigos = new Catalogo();
        enemigos.agregar(new Configuracion("Enemigo", "Acechador", 100, 1, 1, 1, 1, 1));
        partida.prepararMision(enemigos);
        mapa.retirar(11, 12); partida.getEscuadron().quitar(defensa);
        archivo.guardarPartida(partida, null);
        config.setSonidoAtacando(primero.toString());
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(Files.exists(protegido), "Rompe el sonido contenido en la copia de reinicio");
        Partida restaurada = archivo.cargarPartida("partidas/save0.dat"); restaurada.repetir();
        Contacto recuperada = (Contacto) restaurada.getMapa().obtener(11, 12);
        Sonidos.validar(recuperada.getSonidoAtacando());

        Archivo.eliminarPartida("partidas/save0.dat");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(!Files.exists(protegido), "No limpia después de eliminar el último slot que lo usa");

        Path actual = Path.of(config.getSonidoAtacando());
        byte[] anterior = Files.readAllBytes(Path.of("catalogo.json"));
        Files.writeString(Path.of("invalido.wav"), "no es audio");
        config.setSonidoAtacando("invalido.wav");
        try {
            archivo.guardarCatalogo(catalogo, "catalogo.json");
            throw new AssertionError("Admite un sonido inválido");
        } catch (java.io.UncheckedIOException esperado) { }
        comprobar(Files.exists(actual) && java.util.Arrays.equals(anterior, Files.readAllBytes(Path.of("catalogo.json"))),
                "Un guardado fallido borra el recurso o modifica el catálogo anterior");
        config.setSonidoAtacando(actual.toString());
        catalogo.eliminar(config.getId()); archivo.guardarCatalogo(catalogo, "catalogo.json");
        comprobar(!Files.exists(actual), "No limpia sonidos de una configuración eliminada");
        comprobar(Files.exists(primero) && Files.exists(segundo) && Files.exists(Path.of("generico_antiguo.wav")),
                "Borra archivos originales externos");
        System.out.println("OK genéricos compartidos, personalizados, reemplazo, borrado, UTF modificado, copia de reinicio y fallo seguro.");
    }
}
