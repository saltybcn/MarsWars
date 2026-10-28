package audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;
import modelo.atacantes.Atacante;
import modelo.componentes.Combate;
import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.defensas.Defensa;

public final class Sonidos {
    public enum Evento {
        ATAQUE_ATACANTE("ataque_atacante.wav"),
        ATAQUE_DEFENSA("ataque_defensa.wav"),
        MUERTE_ATACANTE("muerte_atacante.wav"),
        MUERTE_DEFENSA("muerte_defensa.wav"),
        DESTRUCCION_NUCLEO("destruccion_nucleo.wav"),
        CLICK_BOTON("click_boton.wav"),
        VICTORIA("victoria.wav"),
        DERROTA("derrota.wav"),
        COLOCAR("colocar.wav");
        private final String archivo;
        Evento(String archivo) { this.archivo = archivo; }
        public String getRuta() { return "sonidos/" + archivo; }
    }

    private static final ThreadPoolExecutor trabajador = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32), tarea -> {
                Thread hilo = new Thread(tarea, "Efectos-sonoros"); hilo.setDaemon(true); return hilo;
            }, new ThreadPoolExecutor.DiscardPolicy());
    private static final Map<String, List<Clip>> clips = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, List<Clip>> anterior) {
            if (size() <= 12) return false;
            for (Clip clip : anterior.getValue()) clip.close();
            return true;
        }
    };
    // Un fallo no silencia la ruta hasta reiniciar: permite recuperarse tras añadir el archivo o el dispositivo.
    private static final Map<String, Long> fallidos = new HashMap<>();
    private static final Map<String, Long> ultimoEvento = new HashMap<>();

    private Sonidos() { }

    // Comprueba el contenido del sonido antes de guardarlo en el catálogo.
    public static void validar(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) throw new IOException("Ruta de sonido vacía");
        try (InputStream archivo = abrir(ruta)) {
            if (archivo == null) throw new IOException("No se encontró el sonido: " + ruta);
            byte[] contenido = archivo.readAllBytes();
            try (AudioInputStream entrada = AudioSystem.getAudioInputStream(new ByteArrayInputStream(contenido))) {
                AudioFormat formato = entrada.getFormat();
                if ((formato.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                        && formato.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED)
                        || formato.getChannels() < 1 || formato.getChannels() > 2
                        || !AudioFileFormat.Type.WAVE.equals(AudioSystem.getAudioFileFormat(
                                new ByteArrayInputStream(contenido)).getType()))
                    throw new IOException("Seleccione un archivo con .WAV");
            }
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("El archivo no tiene extension .WAV " + ruta, e);
        }
    }

    // Metodos que reproducen el sonido dependiendo del evento
    public static void ataque(Combate unidad) {
        if (unidad.isAtaqueSilenciado()) return;
        reproducir(unidad instanceof Atacante ? Evento.ATAQUE_ATACANTE : Evento.ATAQUE_DEFENSA,
                unidad.getSonidoAtacando());
    }

    public static void destruccion(Componente unidad) {
        if (unidad instanceof Combate combate && combate.isDestruccionSilenciada()) return;
        String personalizado = unidad instanceof Combate combate ? combate.getSonidoDestruido() : null;
        if (unidad instanceof Nucleo) reproducir(Evento.DESTRUCCION_NUCLEO, null);
        else if (unidad instanceof Atacante) reproducir(Evento.MUERTE_ATACANTE, personalizado);
        else if (unidad instanceof Defensa) reproducir(Evento.MUERTE_DEFENSA, personalizado);
    }

    public static void reproducir(Evento evento, String personalizado) {
        String ruta = personalizado == null ? "" : personalizado.strip();
        // Dos unidades con sonidos distintos no deben silenciarse entre sí.
        String clave = evento.name() + "|" + ruta;
        synchronized (ultimoEvento) {
            long ahora = System.nanoTime(); Long anterior = ultimoEvento.get(clave);
            if (anterior != null && ahora - anterior < 40_000_000L) return;
            ultimoEvento.put(clave, ahora);
        }
        trabajador.execute(() -> {
            if (ruta.isEmpty() || !reproducirArchivo(ruta, 70))
                reproducirArchivo(evento.getRuta(), 70);
        }); // Abrir WAV o pedir un mezclador nunca bloquea la acción de la unidad.
    }

    private static boolean reproducirArchivo(String ruta, int volumen) {
        Long reintento = fallidos.get(ruta);
        if (reintento != null && System.nanoTime() - reintento < 0) return false;
        try {
            List<Clip> grupo = clips.get(ruta);
            if (grupo == null) { grupo = new ArrayList<>(); clips.put(ruta, grupo); }
            Clip clip = null;
            for (Clip candidato : grupo) if (!candidato.isRunning()) { clip = candidato; break; }
            if (clip == null && grupo.size() >= 2) return true; // El efecto ya está sonando; limita voces por archivo.
            if (clip == null) {
                try (InputStream bytes = abrir(ruta)) {
                    if (bytes == null) throw new FileNotFoundException("No se encontró el sonido: " + ruta);
                    try (AudioInputStream entrada = AudioSystem.getAudioInputStream(new BufferedInputStream(bytes))) {
                        // Algunos mezcladores no abren directamente PCM de 24000 Hz u otras frecuencias.
                        // Convierte en memoria a PCM de 16 bits/44100 Hz sin alterar el WAV del usuario.
                        AudioFormat formato = new AudioFormat(44100, 16, entrada.getFormat().getChannels(), true, false);
                        try (AudioInputStream reproducible = AudioSystem.getAudioInputStream(formato, entrada)) {
                            clip = AudioSystem.getClip();
                            try { clip.open(reproducible); grupo.add(clip); }
                            catch (Exception e) { clip.close(); throw e; }
                        }
                    }
                }
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ganancia = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float decibelios = (float) (20 * Math.log10(Math.min(100, volumen) / 100.0));
                ganancia.setValue(Math.max(ganancia.getMinimum(), Math.min(ganancia.getMaximum(), decibelios)));
            }
            clip.stop(); clip.setFramePosition(0); clip.start();
            fallidos.remove(ruta);
            return true;
        } catch (Exception e) {
            // Cierra el grupo fallido para que el siguiente intento pueda pedir una línea nueva.
            List<Clip> grupo = clips.remove(ruta);
            if (grupo != null) for (Clip clip : grupo) clip.close();
            if (fallidos.put(ruta, System.nanoTime() + 1_000_000_000L) == null)
                System.err.println("Efecto omitido; se usará el genérico si existe: " + ruta + " (" + e.getMessage() + ")");
            return false; // Un archivo ausente, incompatible o un equipo sin audio no detiene el juego.
        }
    }

    // Abre el sonido
    private static InputStream abrir(String ruta) throws IOException {
        Path archivo = Path.of(ruta);
        if (Files.isRegularFile(archivo)) return Files.newInputStream(archivo);
        String recurso = ruta.replace('\\', '/').replaceFirst("^/", "");
        if (recurso.startsWith("src/main/resources/")) recurso = recurso.substring(19);
        // Admite también las rutas que pudieron quedar en configuraciones anteriores al renombrado.
        recurso = switch (recurso) {
            case "sonidos/muerte-defensa.wav" -> "sonidos/muerte_defensa.wav";
            case "sonidos/nucleo-destruir.wav" -> "sonidos/destruccion_nucleo.wav";
            default -> recurso;
        };
        return Sonidos.class.getResourceAsStream("/" + recurso);
    }
    public static void configurarSonidosBTN(java.awt.Container contenedor) { // Metodo para las ventanas, inserta el sonido de los botones en todos los que existen
    // Se ejecuta después de crear los controles de la ventana.
    for (java.awt.Component componente : contenedor.getComponents()) {
        if (componente instanceof javax.swing.JButton boton) {
            boton.addActionListener(e ->
                audio.Sonidos.reproducir(
                    audio.Sonidos.Evento.CLICK_BOTON, null));
        }

        // Busca también dentro de paneles y otros contenedores.
        if (componente instanceof java.awt.Container hijo) {
            configurarSonidosBTN(hijo);
        }
    }
}

}
