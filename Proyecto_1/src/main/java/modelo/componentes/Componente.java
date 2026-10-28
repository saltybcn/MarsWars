package modelo.componentes;

import modelo.registros.Historial;
import java.io.Serializable;
import javax.swing.ImageIcon;
import modelo.juego.Mapa;

public abstract class Componente implements Serializable {
    private static final long serialVersionUID = 1L;
    // ID nueva de 16 caracteres; los archivos existentes conservan sus identificadores.
    private String id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private String nombre;
    private ImageIcon imgIdle;
    // Miniaturas compartidas, fuera del estado serializado; limita el consumo a 64 recursos recientes.
    private static final java.util.Map<String, ImageIcon> imagenesCargadas =
            new java.util.LinkedHashMap<>(64, 0.75f, true);
    private volatile int[] pos = new int[]{-1, -1};
    private int[] posicionFinal = new int[]{-1, -1}; // Última casilla, incluso después de morir.
    private int vidaMax;
    private int vidaInicial;
    private volatile int vida;
    // Es un efecto breve de esta ejecución; no debe reaparecer al cargar una partida.
    private transient volatile long ultimoDmg;
    private Historial historico = new Historial();
    private boolean aereo = false;
    private volatile Mapa mapa;
    protected Componente() {
    }

    protected Componente(String nombre, int vida) {
        if (nombre == null || nombre.isBlank() || vida <= 0)
            throw new IllegalArgumentException("Nombre y vida inicial inválidos"); // Rechaza datos inválidos antes de crear el componente.
        this.nombre = nombre;
        this.vidaInicial = vida;
        this.vidaMax = vida;
        this.vida = vida;
    }
    
    public int recibirDmg(int dmg) {
        if (dmg < 0) return 0;
        int aplicado;
        boolean destruido;
        synchronized (this) {
            aplicado = Math.min(vida, dmg);
            vida -= aplicado;
            // Registra cada impacto real, aunque no provenga de aplicarGolpe (por ejemplo, una explosión).
            // Reiniciar el instante permite renovar el destello sin crear hilos por golpe.
            if (aplicado > 0) ultimoDmg = System.nanoTime();
            destruido = aplicado > 0 && vida == 0; // Solo la transición a cero emite la muerte, no golpes posteriores.
        }
        // Evita mantener el bloqueo de la unidad mientras pedimos el del mapa:
        // mover/actuar utilizan primero mapa y luego la unidad.
        if (destruido) audio.Sonidos.destruccion(this); // Solo encola; nunca abre archivos bajo el bloqueo del mapa.
        Mapa actual = mapa;
        // Si se queda sin vida, se retira del mapa
        if (vida <= 0 && !(this instanceof Nucleo) && actual != null) {
            synchronized (actual) {
                int[] posicion = getPos();
                if (actual.obtener(posicion[0], posicion[1]) == this) {
                    actual.retirar(posicion[0], posicion[1]);
                }
            }
        }
        return aplicado;
    }

    public long getUltimoDmg() { return ultimoDmg; }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public static synchronized ImageIcon cargarImagen(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        // Primero admite archivos del selector; los recursos incluidos funcionan también desde el JAR.
        String recurso = ruta.replace('\\', '/');
        if (recurso.startsWith("src/main/resources/")) recurso = recurso.substring(19);
        try {
            java.io.File archivo = new java.io.File(ruta);
            java.awt.image.BufferedImage original;
            String clave;
            if (archivo.isFile()) {
                java.nio.file.attribute.BasicFileAttributes datos = java.nio.file.Files.readAttributes(
                        archivo.toPath(), java.nio.file.attribute.BasicFileAttributes.class);
                // Si el archivo cambia, no reutiliza una imagen vieja del editor.
                clave = archivo.toPath().toAbsolutePath().normalize() + "|" + datos.lastModifiedTime()
                        + "|" + datos.size() + "|" + recurso;
                ImageIcon guardada = imagenesCargadas.get(clave);
                if (guardada != null) return guardada;
                original = javax.imageio.ImageIO.read(archivo);
            } else {
                java.net.URL url = Componente.class.getResource("/" + recurso.replaceFirst("^/", ""));
                if (url == null) return null; // No recuerda ausencias: el usuario puede añadir el archivo después.
                clave = url.toExternalForm() + "|" + recurso;
                ImageIcon guardada = imagenesCargadas.get(clave);
                if (guardada != null) return guardada;
                original = javax.imageio.ImageIO.read(url);
            }
            if (original == null) return null;
            // Guarda una miniatura, no los millones de píxeles del original en cada partida.
            int ancho = Math.max(1, (int) Math.round(64.0 * original.getWidth()
                    / Math.max(original.getWidth(), original.getHeight())));
            int alto = Math.max(1, (int) Math.round(64.0 * original.getHeight()
                    / Math.max(original.getWidth(), original.getHeight())));
            java.awt.image.BufferedImage miniatura = new java.awt.image.BufferedImage(
                    ancho, alto, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = miniatura.createGraphics();
            try {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(original, 0, 0, ancho, alto, null);
            } finally { g.dispose(); }
            ImageIcon imagen = new ImageIcon(miniatura);
            imagenesCargadas.put(clave, imagen);
            if (imagenesCargadas.size() > 64) {
                // Retira solo la referencia de caché; las unidades que la usan conservan su imagen.
                String antigua = imagenesCargadas.keySet().iterator().next();
                imagenesCargadas.remove(antigua);
            }
            return imagen;
        } catch (java.io.IOException | SecurityException ex) {
            // Una imagen ausente no debe impedir crear ni recuperar una unidad.
            System.err.println("No se pudo cargar la imagen: " + ruta);
            return null;
        }
    }

    public ImageIcon getImgIdle() { return imgIdle; }

    public void setImgIdle(ImageIcon imgIdle) {
        this.imgIdle = imgIdle;
    }

    public int[] getPos() { return pos.clone(); }

    public void setPos(int[] pos) {
        if (pos == null || pos.length != 2) throw new IllegalArgumentException("Posición inválida");
        if (pos[0] >= 0 && pos[1] >= 0) posicionFinal = pos.clone();
        this.pos = pos.clone();
    }

    public int[] getPosicionFinal() {
        return posicionFinal == null ? getPos() : posicionFinal.clone();
    }

    public int getVidaMax() { return vidaMax; }

    public void setVidaMax(int vidaMax) {
        if(vidaMax < 0){
            System.out.println("Error setVidaMax: Vida negativa");
            return;
        }
        this.vidaMax = vidaMax;
    }

    public int getVidaInicial() { return vidaInicial; }

    public void setVidaInicial(int vidaInicial) {
        if (vidaInicial < 0){
            System.out.println("Error setVidaInicial: Vida negativa");
            return;
        }
        this.vidaInicial = vidaInicial;
    }

    public int getVida() { return vida; }

    public void setVida(int vida) {
        if (vida < 0){
            System.out.println("Error setVida: Vida negativa");
            return;
        }
        this.vida = vida;
    }

    public Historial getHistorico() { return historico; }
     
    public boolean esAereo() {return aereo;}
    public void setAereo(boolean aereo) { this.aereo = aereo; }
    
    public Mapa getMapa() { return mapa; }

    public void setMapa(Mapa mapa) {
        this.mapa = mapa;
    }
}

