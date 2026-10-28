import configuracion.*;
import persistencia.Archivo;
import java.io.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import modelo.componentes.Combate;
import modelo.componentes.Nucleo;
import modelo.juego.Mapa;
import modelo.juego.Partida;

/** Imágenes y WAV personalizados comparten una carpeta segura por configuración. */
public class PruebasRecursosUnidad {
    static void verificar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        Archivo archivo=new Archivo();Catalogo catalogo=new Catalogo();
        Path imagen=Path.of("externa.png"),wav=Path.of("externo.wav");
        ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB),"png",imagen.toFile());
        byte[] pcm=new byte[1600];AudioFormat formato=new AudioFormat(8000,16,1,true,false);
        try(AudioInputStream e=new AudioInputStream(new ByteArrayInputStream(pcm),formato,pcm.length/2)){
            AudioSystem.write(e,AudioFileFormat.Type.WAVE,wav.toFile());
        }
        Configuracion a=new Configuracion("Dron de prueba","Dron",100,5,1,3,1,1);
        a.setRutaIdle(imagen.toAbsolutePath().toString());a.setRutaAtaque(a.getRutaIdle());a.setRutaMovimiento(a.getRutaIdle());
        a.setSonidoAtacando(wav.toAbsolutePath().toString());a.setSonidoDestruido(a.getSonidoAtacando());
        catalogo.agregar(a);archivo.guardarCatalogo(catalogo,"catalogo.json");
        Path carpeta=Path.of(a.getRutaIdle()).getParent();
        verificar(carpeta.startsWith(Path.of("recursos/unidades")) && carpeta.getFileName().toString().startsWith(a.getNombre()),"No usa el nombre de unidad");
        verificar(Path.of(a.getSonidoAtacando()).getParent().equals(carpeta)
                && Path.of(a.getSonidoDestruido()).getParent().equals(carpeta),"Separa sonidos e imágenes");
        try(var f=Files.list(carpeta)){verificar(f.count()==2,"Duplica recursos reutilizados en varios estados");}
        Files.delete(imagen);Files.delete(wav);
        Configuracion cargada=archivo.cargarCatalogo("catalogo.json").consultar(a.getId());Combate unidad=cargada.crearUnidad();
        verificar(unidad.getImgIdle()!=null && unidad.getSonidoAtacando().equals(a.getSonidoAtacando())
                && unidad.getSonidoDestruido().equals(a.getSonidoDestruido()),"Depende de los originales o no aplica los sonidos");
        Configuracion homonima=new Configuracion(a.getNombre(),"Dron",100,5,1,3,1,1);
        homonima.setRutaIdle(a.getRutaIdle());homonima.setSonidoAtacando(a.getSonidoAtacando());catalogo.agregar(homonima);
        archivo.guardarCatalogo(catalogo,"catalogo.json");
        verificar(!Path.of(homonima.getRutaIdle()).getParent().equals(carpeta),"Mezcla plantillas homónimas");
        // Una partida real mantiene las referencias anteriores al renombrar la plantilla.
        Mapa mapa = new Mapa(); mapa.colocar(new Nucleo("Núcleo", 100), 12, 12);
        Configuracion enemigo = new Configuracion("Enemigo", "Acechador", 100, 1, 1, 1, 1, 1);
        catalogo.agregar(enemigo);
        Partida partida = new Partida("Prueba", mapa); partida.prepararMision(catalogo);
        archivo.guardarPartida(partida, null);
        a.setNombre("Unidad renombrada");archivo.guardarCatalogo(catalogo,"catalogo.json");
        verificar(!Path.of(a.getRutaIdle()).getParent().equals(carpeta) && Files.isRegularFile(Path.of(unidad.getSonidoAtacando())),
                "Renombrar rompe los sonidos de una partida existente");
        byte[] anterior=Files.readAllBytes(Path.of("catalogo.json"));
        Files.writeString(Path.of("corrupto.wav"),"no es un sonido");a.setSonidoDestruido("corrupto.wav");
        String rutaAnterior=a.getSonidoAtacando();boolean rechazo=false;
        try{archivo.guardarCatalogo(catalogo,"catalogo.json");}catch(UncheckedIOException e){rechazo=true;}
        verificar(rechazo && java.util.Arrays.equals(anterior,Files.readAllBytes(Path.of("catalogo.json")))
                && a.getSonidoAtacando().equals(rutaAnterior),"Modifica el JSON o las rutas pese al fallo");
        a.setSonidoDestruido("");a.setNombre("../../Núcleo:*? peligroso");archivo.guardarCatalogo(catalogo,"catalogo.json");
        verificar(Path.of(a.getRutaIdle()).toAbsolutePath().normalize().startsWith(Path.of("recursos/unidades").toAbsolutePath()),
                "El nombre permite salir de la carpeta de recursos");
        Files.writeString(Path.of("antiguo.json"),"[{\"id\":\"viejo\",\"nombre\":\"Viejo\",\"tipo\":\"Contacto\",\"vida\":100,\"dmg\":1,\"frecuenciaAtaque\":1,\"alcance\":1,\"costoCapacidad\":1,\"misionMinima\":1,\"activo\":true}]");
        Combate antigua=archivo.cargarCatalogo("antiguo.json").getConfigs().get(0).crearUnidad();
        verificar(antigua.getSonidoAtacando()==null && antigua.getSonidoDestruido()==null,"Rompe catálogos anteriores");
        System.out.println("OK carpeta por unidad, homónimos, imágenes/WAV, renombrar, copia segura y compatibilidad.");
    }
}
