package persistencia; 
import com.google.gson.Gson; //Gson es una libreria de google para serializar y deserializar objetos en un json
import com.google.gson.GsonBuilder;
import configuracion.Catalogo;
import configuracion.Configuracion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import modelo.juego.Partida;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class Archivo implements Serializable {
    private static final long serialVersionUID = 1L;

    public Archivo() {
    }

    public void guardarPartida(Partida partida, String ruta) throws IOException {
        java.util.Objects.requireNonNull(partida, "Partida requerida");
        // Una ruta vacía solicita un slot nuevo; una ruta concreta reemplaza ese slot.
        Path carpeta = Path.of("partidas");
        Files.createDirectories(carpeta);
        boolean nuevo = ruta == null || ruta.isBlank();
        Path destino;
        if (nuevo) {
            int numero = 0;
            do { destino = carpeta.resolve("save" + numero++ + ".dat"); }
            while (Files.exists(destino));
        } else destino = Path.of(ruta);
        destino = destino.toAbsolutePath();
        Files.createDirectories(destino.getParent());
        Path temporal = Files.createTempFile(destino.getParent(), "partida-", ".tmp");
        try {
            synchronized (partida.getMapa()) {
                if (partida.getBatalla().isEnCurso() && !partida.getBatalla().isPausada()) {
                    throw new IOException("Pause la batalla antes de guardar.");
                }
                // Termina y cierra el temporal antes de tocar el guardado anterior.
                try (ObjectOutputStream salida = new ObjectOutputStream(Files.newOutputStream(temporal))) {
                    salida.writeUTF(partida.getNombreComandante());
                    salida.writeInt(partida.getMisionActual());
                    salida.writeObject(partida);
                }
            }
            if (nuevo) {
                // Sin REPLACE_EXISTING: si otra escritura ocupó el slot, conserva ese archivo.
                Files.move(temporal, destino);
            } else {
                // El temporal está en la misma carpeta para permitir el reemplazo atómico.
                // Si el sistema no lo permite, informa el fallo y conserva el guardado anterior.
                Files.move(temporal, destino, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public Partida cargarPartida(String ruta) throws IOException, ClassNotFoundException {
        // deserializa el objeto
        try(ObjectInputStream archivo = new ObjectInputStream(new FileInputStream(ruta))){
            // Consume la cabecera en el mismo orden de escritura antes de leer el objeto.
            archivo.readUTF();
            archivo.readInt();
            Object obj = archivo.readObject();
            if(!(obj instanceof Partida)){ // Comprueba que el archivo recien leido sea una partida
                throw new IOException("Slot de partida inválido");
            }
            Partida partida = (Partida) obj;
            if (partida.getMapa() == null || partida.getBatalla() == null
                    || partida.getEscuadron() == null || partida.getNombreComandante() == null
                    || partida.getNombreComandante().isBlank() || partida.getMisionActual() < 1
                    || partida.getBatalla().getMapa() != partida.getMapa()) {
                throw new IOException("La partida guardada está incompleta.");
            }
            return partida; // readObject deja la batalla pausada y sin hilos; la GUI decide cuándo reanudar.
        }
    }

    /** Elimina únicamente un slot del directorio de partidas, sin cargar su contenido. */
    public static void eliminarPartida(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) throw new IOException("Seleccione un guardado existente.");
        Path carpeta = Path.of("partidas").toAbsolutePath().normalize();
        Path destino = Path.of(ruta).toAbsolutePath().normalize();
        // No permite que una ruta equivocada borre el catálogo u otro archivo del proyecto.
        if (!carpeta.equals(destino.getParent())
                || !destino.getFileName().toString().matches("save\\d+\\.dat")
                || !Files.isRegularFile(destino, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El slot no existe o no es un archivo de partida válido.");
        }
        Files.delete(destino); // Un error se informa a la ventana; no se anuncia un borrado fallido.
    }

    public static ArrayList<String> listarPartidas() throws IOException {
        // devuelve una lista con las partidas encontradas
        // le queda pendiente al programa obtener la info de esas partidas mediante los archivos
        File carpeta = new File("partidas");
        ArrayList<String> partidas = new ArrayList<>();

        // Antes del primer guardado, la carpeta puede no existir.
        if (!carpeta.exists()) {
            return partidas;
        }

        File[] archivos = carpeta.listFiles(
                archivo -> archivo.isFile()
                        && archivo.getName().matches("save\\d+\\.dat")
        );

        // listFiles devuelve null si no puede leer el directorio.
        if (archivos == null) {
            throw new IOException("No se pudo leer la carpeta de partidas.");
        }

        for (File archivo : archivos) {
            partidas.add(archivo.getPath());
        }

        partidas.sort(String::compareTo);
        return partidas;
    }

    public void guardarCatalogo(Catalogo catalogo, String ruta) {
        java.util.Objects.requireNonNull(catalogo);
        // Valida todo antes de tocar el archivo que ya estaba guardado.
        Catalogo validado = new Catalogo();
        for (Configuracion config : catalogo.getConfigs()) 
            validado.agregar(config);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // Prepara copias: si falla la escritura, conserva las rutas originales del catálogo en memoria.
        ArrayList<Configuracion> portables = new ArrayList<>();
        Path destino = Path.of(ruta).toAbsolutePath();
        Path temporal = null;
        try {
            for (Configuracion config : validado.getConfigs()) {
                Configuracion copia = gson.fromJson(gson.toJson(config), Configuracion.class);
                copia.setRutaIdle(copiarRecurso(config.getRutaIdle(), config, false));
                copia.setRutaAtaque(copiarRecurso(config.getRutaAtaque(), config, false));
                copia.setRutaMovimiento(copiarRecurso(config.getRutaMovimiento(), config, false));
                // Un campo vacío usa el genérico global; solo importa los sonidos personalizados.
                copia.setSonidoAtacando(copiarRecurso(config.getSonidoAtacando(), config, true));
                copia.setSonidoDestruido(copiarRecurso(config.getSonidoDestruido(), config, true));
                portables.add(copia);
            }
            Files.createDirectories(destino.getParent());
            temporal = Files.createTempFile(destino.getParent(), "catalogo-", ".tmp");
            Files.writeString(temporal, gson.toJson(portables), StandardCharsets.UTF_8);
            // Reemplaza únicamente después de escribir el JSON completo.
            try {
                Files.move(temporal, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // Actualiza las rutas solo después de guardar correctamente el JSON.
            for (Configuracion copia : portables) {
                Configuracion original = catalogo.consultar(copia.getId());
                original.setRutaIdle(copia.getRutaIdle());
                original.setRutaAtaque(copia.getRutaAtaque());
                original.setRutaMovimiento(copia.getRutaMovimiento());
                original.setSonidoAtacando(copia.getSonidoAtacando());
                original.setSonidoDestruido(copia.getSonidoDestruido());
            }
            limpiarImagenes(catalogo);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("No se pudo guardar el catálogo: " + destino, e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) { temporal.toFile().deleteOnExit(); }
            }
        }
    }

    /** Limpia imágenes y WAV importados sin referencias después de guardar el catálogo. */
    private void limpiarImagenes(Catalogo catalogo) {
        Path carpeta = Path.of("recursos", "unidades").toAbsolutePath().normalize();
        if (!Files.isDirectory(carpeta) || Files.isSymbolicLink(carpeta)) return;
        java.util.Set<Path> utilizadas = new java.util.HashSet<>();
        for (Configuracion config : catalogo.getConfigs()) {
            for (String ruta : new String[]{config.getRutaIdle(), config.getRutaAtaque(), config.getRutaMovimiento(),
                    config.getSonidoAtacando(), config.getSonidoDestruido()}) {
                if (ruta != null && !ruta.isBlank()) utilizadas.add(Path.of(ruta).toAbsolutePath().normalize());
            }
        }
        try (var archivos = Files.walk(carpeta)) {
            var importados = archivos.filter(p -> Files.isRegularFile(p, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.(png|jpg|jpeg|gif|bmp|wav)"))
                    .toList();
            // Las rutas también aparecen dentro del byte[] del reintento y en unidades ya retiradas.
            // Busca las rutas completas sin reconstruir imágenes ni unidades de cada guardado.
            for (String ruta : listarPartidas()) {
                String guardado = Files.readString(Path.of(ruta), StandardCharsets.ISO_8859_1);
                for (Path recurso : importados) {
                    String relativa = Path.of("").toAbsolutePath().relativize(recurso).toString().replace('\\', '/');
                    // La serialización usa UTF modificado; writeUTF conserva nombres con tildes o emojis.
                    // ISO-8859-1 representa cada byte sin alterarlo y permite buscar también en copias anidadas.
                    for (String referencia : new String[]{relativa, relativa.replace('/', '\\')}) {
                        var memoria = new java.io.ByteArrayOutputStream();
                        try (var salida = new java.io.DataOutputStream(memoria)) { salida.writeUTF(referencia); }
                        String codificada = memoria.toString(StandardCharsets.ISO_8859_1).substring(2);
                        if (guardado.contains(codificada)) utilizadas.add(recurso.toAbsolutePath().normalize());
                    }
                }
            }
            // Solo borra archivos administrados: nunca modifica los originales externos ni sonidos globales.
            for (Path recurso : importados) {
                if (!utilizadas.contains(recurso.toAbsolutePath().normalize())) Files.delete(recurso);
            }
        } catch (IOException e) {
            // Un fallo de limpieza no convierte un catálogo ya guardado en un guardado fallido.
            System.err.println("Catálogo guardado; no se pudo completar la limpieza de recursos: " + e.getMessage());
        }
    }

    /** Importa imágenes y WAV en la misma carpeta de la configuración; nunca mueve el original. */
    private String copiarRecurso(String ruta, Configuracion config, boolean sonido) throws IOException {
        if (ruta == null || ruta.isBlank()) return ruta;
        String recurso = ruta.replace('\\', '/');
        if (recurso.startsWith("src/main/resources/")) recurso = recurso.substring(19);
        Path origen = Path.of(ruta);
        // Los predeterminados permanecen en el classpath, sin copias por unidad.
        var genericos = audio.Sonidos.Evento.values();
        if (sonido && !Files.isRegularFile(origen)) {
            for (var generico : genericos) {
                if (generico.getRuta().equals(recurso.replaceFirst("^/", ""))) return generico.getRuta();
            }
        }
        byte[] contenido;
        if (Files.isRegularFile(origen)) contenido = Files.readAllBytes(origen);
        else try (java.io.InputStream entrada = Archivo.class.getResourceAsStream("/" + recurso)) {
            if (entrada == null) return ruta; // Conserva el fallback de rutas antiguas ausentes.
            contenido = entrada.readAllBytes();
        }
        if (sonido) {
            // Reconoce también los genéricos copiados por versiones anteriores, aunque tengan otro nombre.
            for (var generico : genericos) {
                try (java.io.InputStream entrada = Archivo.class.getResourceAsStream("/" + generico.getRuta())) {
                    // Mantiene el efecto elegido, incluso si se configuró un genérico de otro tipo.
                    if (entrada != null && java.util.Arrays.equals(contenido, entrada.readAllBytes())) return generico.getRuta();
                }
            }
            audio.Sonidos.validar(ruta);
        }
        else if (javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(contenido)) == null)
            throw new IOException("No se puede leer la imagen: " + ruta);
        String huella;
        try {
            huella = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
        // Usa el formato real, no la extensión escrita por el usuario: el mismo contenido se reutiliza.
        String extension;
        if (sonido) extension = ".wav";
        else try (javax.imageio.stream.ImageInputStream entrada = javax.imageio.ImageIO.createImageInputStream(
                new java.io.ByteArrayInputStream(contenido))) {
            var lectores = javax.imageio.ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new IOException("Formato de imagen no reconocido: " + ruta);
            javax.imageio.ImageReader lector = lectores.next();
            try { extension = "." + lector.getFormatName().toLowerCase(java.util.Locale.ROOT); }
            finally { lector.dispose(); }
        }
        String nombre = java.text.Normalizer.normalize(config.getNombre().strip(), java.text.Normalizer.Form.NFKC)
                .replaceAll("[^\\p{L}\\p{N} ._-]", "_").replaceAll("[. ]+$", "");
        nombre = nombre.codePoints().limit(40).collect(StringBuilder::new,
                StringBuilder::appendCodePoint, StringBuilder::append).toString();
        if (nombre.isBlank()) nombre = "Unidad";
        String identidad;
        try {
            identidad = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(config.getId().getBytes(StandardCharsets.UTF_8))).substring(0, 12);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        // El sufijo separa plantillas homónimas. Al renombrar se copian los recursos sin romper partidas viejas.
        Path carpeta = Path.of("recursos", "unidades", nombre + "-" + identidad);
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(huella + extension);
        // El contenido determina el nombre: evita colisiones con otros archivos llamados igual.
        if (!Files.exists(destino) || !java.util.Arrays.equals(Files.readAllBytes(destino), contenido)) {
            Path temporal = Files.createTempFile(carpeta, "recurso-", ".tmp");
            try {
                Files.write(temporal, contenido);
                try {
                    Files.move(temporal, destino, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(temporal, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporal); }
        }
        return destino.toString().replace('\\', '/');
    }

    public Catalogo cargarCatalogo(String ruta) {
        try {
            // Strictness evita aceptar comentarios o sintaxis que no es JSON.
            Gson gson = new GsonBuilder().setStrictness(com.google.gson.Strictness.STRICT).create();
            String json = Files.readString(java.nio.file.Path.of(ruta), StandardCharsets.UTF_8);
            Configuracion[] configs = gson.fromJson(json, Configuracion[].class);
            if (configs == null) throw new IllegalArgumentException("El catálogo está vacío o contiene null");
            Catalogo cargado = new Catalogo();
            for (Configuracion config : configs) {
                if (config == null) throw new IllegalArgumentException("Hay una configuración nula");
                cargado.agregar(config); // Valida atributos e IDs duplicados
            }
            return cargado;
        } catch (java.nio.file.NoSuchFileException e) {
            return new Catalogo(); // Primera ejecución: todavía no existe el archivo
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("No se pudo leer el catálogo: " + ruta, e);
        } catch (com.google.gson.JsonParseException e) {
            throw new IllegalArgumentException("El archivo no contiene un JSON válido para el catálogo", e);
        }
    }
}
