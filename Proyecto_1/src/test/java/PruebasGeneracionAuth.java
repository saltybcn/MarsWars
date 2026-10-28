import configuracion.*;
import modelo.atacantes.Atacante;
import modelo.componentes.*;
import modelo.defensas.Barrera;
import modelo.juego.*;
import java.nio.file.*;

/** Ejecute en una carpeta temporal vacía; verifica esquinas y migración de credenciales. */
public class PruebasGeneracionAuth {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) throws Exception {
        Mapa mapa = new Mapa(); mapa.colocar(new Nucleo("Núcleo", 1000), 12, 12);
        for (int f=0;f<25;f++) for (int c=0;c<25;c++) {
            boolean exterior = Math.abs(f-12)>6 || Math.abs(c-12)>6;
            // Deje solo dos huecos exteriores junto a defensas en esquinas opuestas.
            if (exterior && !(f==0 && c==1) && !(f==24 && c==23)) {
                Barrera barrera = new Barrera(); barrera.setVida(100);
                verificar(mapa.colocar(barrera,f,c), "Colocación del escenario");
            }
        }
        Partida partida = new Partida("Esquinas",mapa); partida.getEscuadron().setCapacidadTotal(2);
        Catalogo catalogo = new Catalogo();
        catalogo.agregar(new Configuracion("Enemigo","Acechador",100,1,1,1,1,1));
        partida.prepararMision(catalogo);
        verificar(mapa.obtener(0,1) instanceof Atacante && mapa.obtener(24,23) instanceof Atacante,
                "Las defensas de las esquinas excluyen indebidamente casillas vecinas");
        verificar(mapa.obtener(0,0) instanceof Barrera && mapa.obtener(24,24) instanceof Barrera,
                "Los atacantes reemplazaron casillas ocupadas");
        for (int f=6;f<=18;f++) for (int c=6;c<=18;c++)
            verificar(!(mapa.obtener(f,c) instanceof Atacante), "Apareció dentro del radio 6 del núcleo");
        Path nueva=Path.of("src/main/java/persistencia/admin.json"), anterior=Path.of("datos/admin.json");
        verificar(Auth.cargar()==null,"Primer uso sin credenciales");
        Auth auth=new Auth(); String codigo=auth.configurar("admin","clave-prueba".toCharArray());
        verificar(Files.isRegularFile(nueva) && !Files.exists(anterior),"Ruta de persistencia incorrecta");
        byte[] contenido=Files.readAllBytes(nueva);
        Files.createDirectories(anterior.getParent()); Files.move(nueva,anterior);
        Auth migrada=Auth.cargar();
        verificar(migrada.validar("admin","clave-prueba") && migrada.recuperar(codigo.toCharArray()),
                "La migración cambió el acceso o el código");
        verificar(Files.isRegularFile(nueva) && !Files.exists(anterior)
                && java.util.Arrays.equals(contenido,Files.readAllBytes(nueva)),"No migró el archivo íntegro");
        Files.writeString(anterior,"{}");
        verificar(Auth.cargar().validar("admin","clave-prueba"),"El archivo antiguo reemplazó al actual");
        Files.delete(nueva);
        try {Auth.cargar();throw new AssertionError("Aceptó credenciales antiguas dañadas");}
        catch(java.io.IOException esperado) { }
        verificar(Files.exists(anterior) && !Files.exists(nueva),"Movió unas credenciales inválidas");
        System.out.println("OK: aparición junto a esquinas, ocupación, radio del núcleo, ruta auth y migración segura.");
    }
}
