import configuracion.Catalogo;
import configuracion.Configuracion;
import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.juego.Mapa;
import persistencia.Archivo;
import vista.PanelMapa;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Ejecute en una carpeta temporal: las pruebas crean recursos/unidades y un JSON de prueba. */
public class PruebasImagenesDano {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    private static BufferedImage pintar(JButton celda) {
        celda.setSize(20, 20);
        BufferedImage imagen = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        celda.getIcon().paintIcon(celda, g, 0, 0);
        g.dispose();
        return imagen;
    }
    public static void main(String[] args) throws Exception {
        BufferedImage sprite = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        sprite.setRGB(1, 1, 0xff00ff00);
        sprite.setRGB(2, 1, 0x8000ff00);
        Nucleo nucleo = new Nucleo("Prueba", 100);
        nucleo.setImgIdle(new ImageIcon(sprite));
        Mapa mapa = new Mapa(); mapa.colocar(nucleo, 12, 12);
        PanelMapa[] panel = new PanelMapa[1];
        BufferedImage[] normal = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new PanelMapa(); panel[0].setMapa(mapa);
            normal[0] = pintar((JButton) panel[0].getComponent(312));
            verificar(nucleo.recibirDmg(1) == 1, "Daño aplicado");
            panel[0].actualizar();
            BufferedImage roja = pintar((JButton) panel[0].getComponent(312));
            boolean huboRojo = false;
            for (int y=0; y<20; y++) for (int x=0; x<20; x++) {
                int antes = normal[0].getRGB(x,y), despues = roja.getRGB(x,y);
                verificar((antes >>> 24) == (despues >>> 24), "Se perdió transparencia");
                if (((despues >>> 16) & 255) > ((antes >>> 16) & 255)) huboRojo = true;
            }
            verificar(huboRojo, "El sprite no se tiñó de rojo");
        });
        long primerGolpe = nucleo.getUltimoDmg();
        Thread.sleep(5);
        nucleo.recibirDmg(1);
        verificar(nucleo.getUltimoDmg() > primerGolpe, "El segundo golpe no renueva el efecto");
        long ultimo = nucleo.getUltimoDmg();
        nucleo.recibirDmg(0); nucleo.recibirDmg(-1);
        verificar(nucleo.getUltimoDmg() == ultimo, "Un golpe sin daño activó el efecto");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(nucleo); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Nucleo cargado = (Nucleo) in.readObject();
            verificar(cargado.getUltimoDmg() == 0, "El destello se guardó en la partida");
            verificar(cargado.getImgIdle() != null, "Se perdió el sprite guardado");
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                var instante = Componente.class.getDeclaredField("ultimoDmg"); instante.setAccessible(true);
                // Entre 60 y 120 ms debe seguir rojo: un pulso continuo, sin apagarse y encenderse.
                instante.setLong(nucleo, System.nanoTime() - 80_000_000L);
                panel[0].actualizar();
                BufferedImage durante = pintar((JButton) panel[0].getComponent(312));
                verificar(durante.getRGB(8, 8) != normal[0].getRGB(8, 8), "El destello se interrumpió");
                instante.setLong(nucleo, System.nanoTime() - 160_000_000L);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> {
            panel[0].actualizar(); BufferedImage fin = pintar((JButton) panel[0].getComponent(312));
            for (int y=0;y<20;y++) for(int x=0;x<20;x++)
                verificar(fin.getRGB(x,y)==normal[0].getRGB(x,y), "El destello no terminó");
        });
        Path original = Path.of("externa.png"); ImageIO.write(sprite, "png", original.toFile());
        Catalogo catalogo = new Catalogo();
        Configuracion config = new Configuracion("Prueba", "Contacto", 100, 5, 1, 1, 1, 1);
        config.setRutaIdle(original.toAbsolutePath().toString());
        config.setRutaAtaque(original.toAbsolutePath().toString());
        catalogo.agregar(config);
        Archivo archivo = new Archivo(); archivo.guardarCatalogo(catalogo, "catalogo.json");
        String ruta = config.getRutaIdle();
        verificar(!Path.of(ruta).isAbsolute() && ruta.startsWith("recursos/unidades/"), "Ruta no portable");
        verificar(ruta.equals(config.getRutaAtaque()), "Duplicó el mismo sprite");
        verificar(java.util.Arrays.equals(Files.readAllBytes(original), Files.readAllBytes(Path.of(ruta))), "Copia alterada");
        Path renombrada = Path.of("otra-extension.jpg"); Files.copy(original, renombrada);
        config.setRutaAtaque(renombrada.toAbsolutePath().toString());
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        verificar(config.getRutaAtaque().equals(ruta), "Duplicó contenido con otra extensión");
        Files.delete(renombrada);
        Files.delete(original);
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        verificar(archivo.cargarCatalogo("catalogo.json").getConfigs().get(0).crearUnidad().getImgIdle()!=null,
                "La unidad depende del original borrado");
        try (var archivos = Files.list(Path.of(ruta).getParent())) {
            verificar(archivos.filter(p -> p.toString().endsWith(".png")).count()==1, "Duplicó imágenes al volver a guardar");
        }
        String jsonAntes = Files.readString(Path.of("catalogo.json"));
        Path invalida = Path.of("invalida.png"); Files.writeString(invalida, "no es una imagen");
        config.setRutaIdle(invalida.toString());
        try { archivo.guardarCatalogo(catalogo, "catalogo.json"); throw new AssertionError("Aceptó imagen inválida"); }
        catch (UncheckedIOException esperado) { }
        verificar(Files.readString(Path.of("catalogo.json")).equals(jsonAntes), "Dañó el JSON al fallar");
        verificar(config.getRutaIdle().equals(invalida.toString()), "Cambió rutas antes de guardar");
        Path segunda = Path.of("segunda.png"); ImageIO.write(sprite, "png", segunda.toFile());
        config.setRutaIdle(segunda.toAbsolutePath().toString()); config.setRutaAtaque("");
        Files.writeString(Path.of("bloqueado"), "Este archivo impide crear el directorio");
        String anterior = config.getRutaIdle();
        try { archivo.guardarCatalogo(catalogo, "bloqueado/catalogo.json"); throw new AssertionError("Guardó en destino inválido"); }
        catch (UncheckedIOException esperado) { }
        verificar(config.getRutaIdle().equals(anterior), "Cambió rutas tras fallo de escritura");
        config.setRutaIdle("nucleo/nucleo.png");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        verificar(config.getRutaIdle().startsWith("recursos/unidades/")
                && Files.isRegularFile(Path.of(config.getRutaIdle())), "No importó el recurso incluido");
        verificar(config.crearUnidad().getImgIdle()!=null, "Se perdió el recurso incluido");
        config.setRutaIdle("imagen-ausente.png"); config.setRutaAtaque("");
        archivo.guardarCatalogo(catalogo, "catalogo.json");
        verificar(archivo.cargarCatalogo("catalogo.json").getConfigs().get(0).isActivo(), "Desactivó el fallback");
        System.out.println("OK: rojo, transparencia, segundo golpe, expiración, serialización, copia portable, deduplicación y fallo seguro.");
    }
}
