import audio.Sonidos;
import configuracion.*;
import persistencia.Archivo;
import modelo.atacantes.Acechador;
import modelo.componentes.*;
import modelo.defensas.Contacto;
import modelo.juego.Mapa;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Comprueba archivos reales y rutas de reproducción sin depender de una tarjeta de sonido. */
public class PruebasAudioPersonalizado {
    static void comprobar(boolean c, String m) { if (!c) throw new AssertionError(m); }
    @SuppressWarnings("unchecked")
    static <T> T campo(String nombre) throws Exception {
        Field f=Sonidos.class.getDeclaredField(nombre);f.setAccessible(true);return (T)f.get(null);
    }
    static void esperar() throws Exception {
        PruebasAudioPersonalizado.<ThreadPoolExecutor>campo("trabajador").submit(()->{}).get(3,TimeUnit.SECONDS);
    }
    static Clip salidaSimulada(AtomicInteger veces) {
        return (Clip)Proxy.newProxyInstance(Clip.class.getClassLoader(),new Class<?>[]{Clip.class},(obj,metodo,args)->{
            if (metodo.getName().equals("start")) veces.incrementAndGet();
            Class<?> tipo=metodo.getReturnType();
            if (tipo==boolean.class) return false;
            if (tipo==int.class) return 0;
            if (tipo==long.class) return 0L;
            if (tipo==float.class) return 0f;
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        // Los nombres deben coincidir con Evento, también fuera del directorio del proyecto.
        for (String archivo : new String[]{"ataque_atacante.wav","ataque_defensa.wav","muerte_atacante.wav",
                "muerte_defensa.wav","destruccion_nucleo.wav"}) {
            Sonidos.validar("src/main/resources/sonidos/"+archivo);
            try (AudioInputStream original=AudioSystem.getAudioInputStream(Sonidos.class.getResource("/sonidos/"+archivo))) {
                AudioFormat destino=new AudioFormat(44100,16,original.getFormat().getChannels(),true,false);
                try (AudioInputStream convertido=AudioSystem.getAudioInputStream(destino,original)) {
                    comprobar(convertido.readAllBytes().length>0,"PCM reproducible: "+archivo);
                }
            }
        }
        Sonidos.validar("src/main/resources/sonidos/muerte-defensa.wav");
        Sonidos.validar("src/main/resources/sonidos/nucleo-destruir.wav");
        AudioFormat formato=new AudioFormat(8000,16,1,true,false);byte[] pcm=new byte[1600];
        try(AudioInputStream entrada=new AudioInputStream(new ByteArrayInputStream(pcm),formato,pcm.length/2)) {
            AudioSystem.write(entrada,AudioFileFormat.Type.WAVE,Path.of("contacto.wav").toFile());
        }
        Configuracion config=new Configuracion("Contacto propio","Contacto",100,5,1,1,1,1);
        config.setSonidoAtacando("contacto.wav");config.setRutaIdle("nucleo/nucleo.png");
        Catalogo catalogo=new Catalogo();catalogo.agregar(config);new Archivo().guardarCatalogo(catalogo,"catalogo.json");
        comprobar(Path.of(config.getSonidoAtacando()).getParent().equals(Path.of(config.getRutaIdle()).getParent()),
                "Imagen y sonido personalizado separados");
        Files.delete(Path.of("contacto.wav")); // El selector original ya no debe ser necesario.
        Sonidos.validar(config.getSonidoAtacando());
        Mapa mapa=new Mapa();Contacto defensa=(Contacto)config.crearUnidad();
        Acechador enemigo=new Acechador("Objetivo",100,1,1000,1,500);Nucleo nucleo=new Nucleo("Núcleo",100);
        mapa.colocar(defensa,0,0);mapa.colocar(enemigo,0,1);mapa.colocar(nucleo,12,12);
        AtomicInteger ataque=new AtomicInteger(),muerte=new AtomicInteger(),fin=new AtomicInteger(),respaldo=new AtomicInteger();
        Map<String,List<Clip>> clips=campo("clips");
        clips.put(config.getSonidoAtacando(),List.of(salidaSimulada(ataque)));
        // El genérico se reutiliza desde el classpath; ya no existe una copia por configuración.
        clips.put("sonidos/muerte_defensa.wav",List.of(salidaSimulada(muerte)));
        clips.put("sonidos/destruccion_nucleo.wav",List.of(salidaSimulada(fin)));
        clips.put("sonidos/ataque_defensa.wav",List.of(salidaSimulada(respaldo)));
        defensa.aplicarGolpe(enemigo);defensa.recibirDmg(100);nucleo.recibirDmg(100);esperar();
        comprobar(ataque.get()==1 && muerte.get()==1 && fin.get()==1,"No reproduce ataque propio/muerte de defensa/núcleo");
        comprobar(respaldo.get()==0,"Un ataque personalizado también reproduce el genérico");
        Map<String,Long> fallidos=campo("fallidos");
        String ruta=config.getSonidoAtacando();fallidos.put(ruta,System.nanoTime()+1_000_000_000L);
        Map<String,Long> eventos=campo("ultimoEvento");eventos.clear();
        Sonidos.reproducir(Sonidos.Evento.ATAQUE_DEFENSA,ruta);esperar();
        comprobar(respaldo.get()==1,"No usa respaldo mientras el personalizado falla");
        fallidos.put(ruta,System.nanoTime()-1);eventos.clear();
        Sonidos.reproducir(Sonidos.Evento.ATAQUE_DEFENSA,ruta);esperar();
        comprobar(ataque.get()==2 && !fallidos.containsKey(ruta),"Un error silencia el personalizado permanentemente");
        comprobar(respaldo.get()==1,"Se reproduce el genérico después de recuperar el personalizado");
        System.out.println("OK cinco WAV reales, PCM, prefijo de recursos, Contacto personalizado portable, muertes y recuperación tras fallo.");
    }
}
