import audio.Sonidos;
import configuracion.*;
import persistencia.Archivo;
import modelo.componentes.*;
import modelo.atacantes.Acechador;
import modelo.defensas.Contacto;
import modelo.juego.Mapa;
import javax.sound.sampled.*;
import java.nio.file.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Una detonación real emite su WAV propio aunque el daño sea cero o la pose siga activa. */
public class PruebasAudioExplosivos {
    static Path audioAtaque; // Argumento opcional para verificar también un WAV real del catálogo.
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    @SuppressWarnings("unchecked") static <T>T campo(String n)throws Exception{Field f=Sonidos.class.getDeclaredField(n);f.setAccessible(true);return (T)f.get(null);}
    static Clip salida(AtomicInteger contador){return (Clip)Proxy.newProxyInstance(Clip.class.getClassLoader(),new Class<?>[]{Clip.class},(o,m,a)->{
        if(m.getName().equals("start"))contador.incrementAndGet();Class<?> t=m.getReturnType();
        if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;if(t==float.class)return 0f;return null;
    });}
    static void wav(Path ruta,int muestras)throws Exception{
        AudioFormat formato=new AudioFormat(8000,16,1,true,false);byte[] pcm=new byte[muestras*2];
        try(AudioInputStream entrada=new AudioInputStream(new ByteArrayInputStream(pcm),formato,muestras)){AudioSystem.write(entrada,AudioFileFormat.Type.WAVE,ruta.toFile());}
    }
    static void caso(String tipo,int dmg,boolean poseActiva)throws Exception{
        Path ataque=Path.of("ataque.wav"),muerte=Path.of("muerte.wav");
        if(audioAtaque==null)wav(ataque,800);
        else Files.copy(audioAtaque,ataque,StandardCopyOption.REPLACE_EXISTING);
        wav(muerte,1600);
        Configuracion c=new Configuracion(tipo+" "+dmg+" "+poseActiva,tipo,100,dmg,1,2,1,1);
        c.setSonidoAtacando(ataque.toAbsolutePath().toString());c.setSonidoDestruido(muerte.toAbsolutePath().toString());
        Catalogo catalogo=new Catalogo();catalogo.agregar(c);Archivo archivo=new Archivo();archivo.guardarCatalogo(catalogo,"catalogo.json");
        Files.delete(ataque);Files.delete(muerte);Configuracion leida=archivo.cargarCatalogo("catalogo.json").consultar(c.getId());Combate explosivo=leida.crearUnidad();
        Sonidos.validar(explosivo.getSonidoAtacando());Sonidos.validar(explosivo.getSonidoDestruido());
        AtomicInteger ataques=new AtomicInteger(),muertes=new AtomicInteger();Map<String,List<Clip>> clips=campo("clips");
        clips.put(explosivo.getSonidoAtacando(),List.of(salida(ataques)));clips.put(explosivo.getSonidoDestruido(),List.of(salida(muertes)));
        Mapa mapa=new Mapa();mapa.colocar(new Nucleo("Núcleo",1000),12,12);mapa.colocar(explosivo,5,5);
        ArrayList<Componente> objetivos=new ArrayList<>();
        for(int[] p:new int[][]{{5,6},{6,5},{6,6}}){Combate blanco=tipo.equals("Impacto")?new Acechador("Blanco",100,0,1000,1,500):new Contacto("Blanco",100,0,1000,1);mapa.colocar(blanco,p[0],p[1]);objetivos.add(blanco);}
        if(poseActiva){Field f=Combate.class.getDeclaredField("esperaVisual");f.setAccessible(true);f.setLong(explosivo,500_000_000L);}
        explosivo.atacar(objetivos);
        PruebasAudioExplosivos.<ThreadPoolExecutor>campo("trabajador").submit(()->{}).get(3,TimeUnit.SECONDS);
        comprobar(ataques.get()==1,tipo+": no emite un único ataque personalizado; dmg="+dmg+", pose="+poseActiva+", emisiones="+ataques.get());
        comprobar(muertes.get()==1,"Falta el sonido de destrucción activado de "+tipo);
        comprobar(explosivo.getVida()==0&&mapa.obtener(5,5)==null,"Se retrasó la retirada de "+tipo);
        for(Componente blanco:objetivos)comprobar(blanco.getVida()==100-dmg,"Cambió el daño de área");
        // Destruirlo desde fuera sí conserva su sonido de muerte personalizado.
        PruebasAudioExplosivos.<Map<String,Long>>campo("ultimoEvento").clear();
        Combate eliminado=leida.crearUnidad();
        eliminado.recibirDmg(eliminado.getVida());
        PruebasAudioExplosivos.<ThreadPoolExecutor>campo("trabajador").submit(()->{}).get(3,TimeUnit.SECONDS);
        comprobar(muertes.get()==2,"Una muerte por daño externo quedó silenciada: "+tipo);
    }
    public static void main(String[]args)throws Exception{
        if(args.length>0){
            audioAtaque=Path.of(args[0]).toAbsolutePath();Sonidos.validar(audioAtaque.toString());
            try(AudioInputStream original=AudioSystem.getAudioInputStream(audioAtaque.toFile())){
                AudioFormat formato=new AudioFormat(44100,16,original.getFormat().getChannels(),true,false);
                try(AudioInputStream convertido=AudioSystem.getAudioInputStream(formato,original)){
                    comprobar(convertido.readAllBytes().length>0,"El WAV real no se convierte al formato de reproducción");
                }
            }
        }
        for(String tipo:new String[]{"Impacto","Demoledor"}){caso(tipo,10,false);caso(tipo,10,true);caso(tipo,0,false);}
        System.out.println("OK Impacto/Demoledor: ataque y destrucción independientes también al detonar.");
    }
}
