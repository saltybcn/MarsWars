import audio.Sonidos;
import com.google.gson.Gson;
import configuracion.Configuracion;
import modelo.componentes.Combate;
import javax.sound.sampled.Clip;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Silenciar afecta tanto el WAV propio como el fallback y sobrevive al guardado. */
public class PruebasSilencioUnidades {
    public static void main(String[] args) throws Exception {
        AtomicInteger emisiones = new AtomicInteger();
        Map<String, List<Clip>> clips = PruebasAudioPersonalizado.campo("clips");
        for (String ruta : new String[]{"propio-ataque.wav", "propio-muerte.wav",
                "sonidos/ataque_atacante.wav", "sonidos/ataque_defensa.wav",
                "sonidos/muerte_atacante.wav", "sonidos/muerte_defensa.wav"}) {
            clips.put(ruta, List.of(PruebasAudioPersonalizado.salidaSimulada(emisiones)));
        }
        Gson gson = new Gson();
        for (String tipo : new String[]{"Contacto", "AlcanceMedio", "AtaqueMultiple", "Impacto", "Dron",
                "Barrera", "Acechador", "Escupidor", "Enjambre", "Demoledor", "Volador"}) {
            Configuracion config = new Configuracion("Prueba", tipo, 100, 1, 1, 1, 1, 1);
            config.setMaxObjetivos(2);
            if ("Barrera".equals(tipo)) {
                config.setDmg(0);
                config.setAlcance(0);
                config.setFrecuenciaAtaque(0);
            }
            config.setSonidoAtacando("propio-ataque.wav");
            config.setSonidoDestruido("propio-muerte.wav");
            config.setAtaqueSilenciado(true);
            config.setDestruccionSilenciada(true);
            Configuracion leida = gson.fromJson(gson.toJson(config), Configuracion.class);
            Combate unidad = leida.crearUnidad();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) { salida.writeObject(unidad); }
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                unidad = (Combate) entrada.readObject();
            }
            if ((!unidad.isAtaqueSilenciado() || !unidad.isDestruccionSilenciada())) throw new AssertionError("Pierde silencio: " + tipo);
            for (boolean ataqueSilenciado : new boolean[]{false, true}) {
                for (boolean destruccionSilenciada : new boolean[]{false, true}) {
                    unidad.setAtaqueSilenciado(ataqueSilenciado);
                    unidad.setDestruccionSilenciada(destruccionSilenciada);
                    for (String ruta : new String[]{"propio-ataque.wav", ""}) {
                        unidad.setSonidoAtacando(ruta);
                        unidad.setSonidoDestruido(ruta.isEmpty() ? "" : "propio-muerte.wav");
                        PruebasAudioPersonalizado.<Map<String, Long>>campo("ultimoEvento").clear();
                        emisiones.set(0);
                        Sonidos.ataque(unidad);
                        PruebasAudioPersonalizado.esperar();
                        if (emisiones.get() != (ataqueSilenciado ? 0 : 1))
                            throw new AssertionError("Ataque no es independiente: " + tipo);
                        emisiones.set(0);
                        Sonidos.destruccion(unidad);
                        PruebasAudioPersonalizado.esperar();
                        if (emisiones.get() != (destruccionSilenciada ? 0 : 1))
                            throw new AssertionError("Destrucción no es independiente: " + tipo);
                    }
                }
            }
        }
        Configuracion anterior = gson.fromJson("{}", Configuracion.class);
        if ((anterior.isAtaqueSilenciado() || anterior.isDestruccionSilenciada())) throw new AssertionError("Silencia catálogos anteriores");
        System.out.println("OK 11 tipos: cuatro combinaciones de ataque/muerte independientes, audio propio/genérico, JSON y serialización.");
    }
}
