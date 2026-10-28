import java.io.IOException;
import java.nio.file.*;
import persistencia.Archivo;

/** Ejecute en una carpeta temporal vacía; nunca utiliza los guardados del jugador. */
public class PruebasEliminarSlots {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    private static void rechazar(String ruta) throws Exception {
        try {
            Archivo.eliminarPartida(ruta);
            throw new AssertionError("Debió rechazar la ruta: " + ruta);
        } catch (IOException esperada) { }
    }
    public static void main(String[] args) throws Exception {
        comprobar(!Files.exists(Path.of("partidas")), "Use una carpeta temporal vacía.");
        Files.createDirectory(Path.of("partidas"));
        Files.writeString(Path.of("partidas/save0.dat"), "Cabecera dañada");
        modelo.juego.Mapa mapa = new modelo.juego.Mapa();
        mapa.colocar(new modelo.componentes.Nucleo("Núcleo", 100), 12, 12);
        Archivo archivo = new Archivo();
        archivo.guardarPartida(new modelo.juego.Partida("Prueba slots", mapa), "partidas/save1.dat");
        byte[] valido = Files.readAllBytes(Path.of("partidas/save1.dat"));
        comprobar(archivo.cargarPartida("partidas/save1.dat").getNombreComandante().equals("Prueba slots"),
                "El segundo slot es una partida válida y cargable");
        Files.writeString(Path.of("catalogo.json"), "No borrar");
        rechazar(null); rechazar(""); rechazar("catalogo.json");
        rechazar("partidas/../catalogo.json"); rechazar("partidas/save99.dat");
        Files.createDirectory(Path.of("partidas/save2.dat"));
        rechazar("partidas/save2.dat");
        // Un slot ilegible también debe poder retirarse sin deserializarlo.
        Archivo.eliminarPartida("partidas/save0.dat");
        comprobar(!Files.exists(Path.of("partidas/save0.dat")), "Elimina el elegido");
        comprobar(java.util.Arrays.equals(valido, Files.readAllBytes(Path.of("partidas/save1.dat"))), "Conserva otros slots");
        comprobar(Files.readString(Path.of("catalogo.json")).equals("No borrar"), "Conserva catálogo");
        comprobar(Archivo.listarPartidas().size() == 1, "El listado se actualiza");
        rechazar("partidas/save0.dat"); // Archivo retirado por otra ventana o proceso.
        Archivo.eliminarPartida(Path.of("partidas/save1.dat").toAbsolutePath().toString());
        comprobar(Archivo.listarPartidas().isEmpty(), "Permite eliminar el último slot");
        System.out.println("OK eliminación de slots válidos y dañados, rutas inválidas, aislamiento y último guardado.");
    }
}
