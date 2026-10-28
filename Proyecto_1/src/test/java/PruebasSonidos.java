import audio.Sonidos;
import modelo.componentes.*;
import modelo.atacantes.*;
import modelo.defensas.*;
import modelo.juego.Mapa;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Pruebas de audio por unidad. Ejecute en una carpeta temporal sin necesidad de mezclador real. */
public class PruebasSonidos {
    static void verificar(boolean c,String m){if(!c)throw new AssertionError(m);}
    @SuppressWarnings("unchecked")
    static Map<String,Long> eventos()throws Exception {
        Field f=Sonidos.class.getDeclaredField("ultimoEvento");f.setAccessible(true);return (Map<String,Long>)f.get(null);
    }
    static void esperar()throws Exception {
        Field f=Sonidos.class.getDeclaredField("trabajador");f.setAccessible(true);
        ((ThreadPoolExecutor)f.get(null)).submit(()->{}).get(3,TimeUnit.SECONDS);
    }
    public static void main(String[] args)throws Exception {
        AudioFormat formato=new AudioFormat(8000,16,1,true,false);byte[] pcm=new byte[1600];
        Path valido=Path.of("valido.wav");
        try(AudioInputStream e=new AudioInputStream(new ByteArrayInputStream(pcm),formato,pcm.length/2)){
            AudioSystem.write(e,AudioFileFormat.Type.WAVE,valido.toFile());
        }
        Sonidos.validar(valido.toString());
        Files.writeString(Path.of("invalido.wav"),"no es WAV");
        boolean rechazo=false;try{Sonidos.validar("invalido.wav");}catch(IOException e){rechazo=true;}
        verificar(rechazo,"Acepta audio inválido");
        rechazo=false;try{Sonidos.validar("ausente.wav");}catch(IOException e){rechazo=true;}
        verificar(rechazo,"Acepta archivo ausente al editar");
        Mapa mapa=new Mapa();Acechador a=new Acechador("A",100,5,1000,1,500);
        Acechador b=new Acechador("B",100,5,1000,1,500);Contacto d=new Contacto("D",100,5,1000,1);
        Nucleo nucleo=new Nucleo("Núcleo",100);
        a.setSonidoAtacando("ataque_A_ausente.wav");a.setSonidoDestruido("muerte_A_ausente.wav");
        b.setSonidoAtacando("ataque_B_ausente.wav");
        d.setSonidoAtacando("ataque_D_ausente.wav");d.setSonidoDestruido("muerte_D_ausente.wav");
        mapa.colocar(a,0,0);mapa.colocar(b,0,2);mapa.colocar(d,0,1);mapa.colocar(nucleo,12,12);
        Map<String,Long> eventos=eventos();
        a.aplicarGolpe(d);b.aplicarGolpe(d);
        verificar(eventos.containsKey("ATAQUE_ATACANTE|ataque_A_ausente.wav")
                && eventos.containsKey("ATAQUE_ATACANTE|ataque_B_ausente.wav"),"Silencia dos personalizaciones distintas");
        long ronda=eventos.get("ATAQUE_ATACANTE|ataque_A_ausente.wav");Thread.sleep(50);
        // El filtro agrupa ráfagas de 40 ms; una pose de ataque activa no silencia el siguiente ataque.
        a.aplicarGolpe(d);verificar(eventos.get("ATAQUE_ATACANTE|ataque_A_ausente.wav")>ronda,"La pose visual silencia otro ataque");
        d.aplicarGolpe(a);verificar(eventos.containsKey("ATAQUE_DEFENSA|ataque_D_ausente.wav"),"Falta ataque personalizado de defensa");
        d.recibirDmg(d.getVida());verificar(eventos.containsKey("MUERTE_DEFENSA|muerte_D_ausente.wav"),"Falta muerte personalizada");
        long muerte=eventos.get("MUERTE_DEFENSA|muerte_D_ausente.wav");Thread.sleep(50);d.recibirDmg(100);
        verificar(eventos.get("MUERTE_DEFENSA|muerte_D_ausente.wav")==muerte,"Repite muerte ya registrada");
        a.recibirDmg(a.getVida());verificar(eventos.containsKey("MUERTE_ATACANTE|muerte_A_ausente.wav"),"Falta muerte del atacante");
        nucleo.recibirDmg(100);verificar(eventos.containsKey("DESTRUCCION_NUCLEO|"),"Falta efecto del núcleo");
        esperar();
        Field campo=Sonidos.class.getDeclaredField("fallidos");campo.setAccessible(true);Map<?,?> ausentes=(Map<?,?>)campo.get(null);
        // Un dispositivo real puede reproducir el fallback correctamente y no registrarlo como fallido.
        // PruebasAudioPersonalizado comprueba esa reproducción con salida simulada.
        verificar(ausentes.containsKey("ataque_A_ausente.wav"), "No registra el personalizado ausente");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream salida=new ObjectOutputStream(bytes)){salida.writeObject(a);}
        try(ObjectInputStream entrada=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
            Combate copia=(Combate)entrada.readObject();
            verificar(copia.getSonidoAtacando().equals(a.getSonidoAtacando())
                    && copia.getSonidoDestruido().equals(a.getSonidoDestruido()),"No persiste las rutas de la unidad");
        }
        verificar(!Files.exists(Path.of("src/main/java/persistencia/sonidos.json")),"Conserva preferencias globales innecesarias");
        System.out.println("OK WAV, sonidos por unidad, cinco eventos, ataques independientes de la pose y serialización.");
    }
}
