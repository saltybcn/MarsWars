import configuracion.Catalogo;
import configuracion.Configuracion;
import java.util.HashMap;
import java.util.Map;
import modelo.atacantes.Atacante;
import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.defensas.Barrera;
import modelo.defensas.Contacto;
import modelo.juego.Mapa;
import modelo.juego.Partida;

/** Ejecute este main para revisar las misiones sin abrir ventanas ni iniciar hilos. */
public class PruebasPartida {
    public static void main(String[] args) {
        Mapa mapa = new Mapa();
        Nucleo nucleo = new Nucleo("Núcleo de prueba", 100);
        comprobar(mapa.colocar(nucleo, 12, 12), "Colocación del núcleo");
        Partida partida = new Partida(mapa);
        Contacto defensa = new Contacto("Defensa de prueba", 80, 1, 1000, 2);
        partida.getEscuadron().agregar(defensa);
        mapa.colocar(defensa, 11, 12);
        partida.getEscuadron().setCapacidadTotal(5);
        Catalogo catalogo = new Catalogo();
        catalogo.agregar(new Configuracion("Disponible", "Acechador", 50, 1, 1, 1, 3, 1));
        Configuracion inactiva = new Configuracion("Inactiva", "Volador", 50, 1, 1, 2, 1, 1);
        inactiva.setActivo(false);
        catalogo.agregar(inactiva);
        catalogo.agregar(new Configuracion("Futura", "Volador", 50, 1, 1, 2, 1, 99));
        rechaza(partida::avanzar, "No permite avanzar sin resultado");
        rechaza(partida::repetir, "No permite repetir sin copia");
        partida.prepararMision(catalogo);
        Map<String, String> inicial = escenario(partida.getMapa());
        int atacantes = 0;
        for (Componente[] fila : mapa.getMat()) {
            for (Componente unidad : fila) {
                if (unidad instanceof Atacante) {
                    atacantes++;
                    int[] pos = unidad.getPos();
                    comprobar(Math.max(Math.abs(pos[0] - 12), Math.abs(pos[1] - 12)) > 6,
                            "Respeta la zona protegida");
                    comprobar("Disponible".equals(unidad.getNombre()), "Filtra plantillas");
                    comprobar(unidad.getMapa() == mapa, "Referencia al mapa");
                }
            }
        }
        comprobar(atacantes == 1, "Termina con capacidad sobrante insuficiente");
        comprobar(!partida.getBatalla().isEnCurso(), "Preparar no inicia hilos");
        defensa.recibirDmg(20);
        mapa.retirar(11, 12);
        nucleo.recibirDmg(100);
        partida.repetir();
        comprobar(escenario(partida.getMapa()).equals(inicial), "Repite posiciones, IDs y vidas");
        comprobar(partida.getMapa() != mapa, "Copia independiente");
        comprobar(partida.getMapa().obtener(11, 12) == partida.getEscuadron().getDefensas().get(0),
                "Mapa y escuadrón comparten la misma defensa restaurada");
        comprobar(partida.getMapa().obtener(11, 12).getMapa() == partida.getMapa(), "Referencia restaurada");
        partida.getMapa().obtener(11, 12).recibirDmg(1);
        partida.repetir();
        comprobar(escenario(partida.getMapa()).equals(inicial), "La copia permite varios reintentos");

        // Se simula cada resultado para probar la transición, no el combate ya probado aparte.
        for (String resultado : new String[]{"VICTORIA", "DERROTA"}) {
            int vidaNucleo = "VICTORIA".equals(resultado) ? 41 : 0;
            partida.getMapa().getNucleo().setVida(vidaNucleo);
            int capacidadAnterior = partida.getEscuadron().getCapacidadTotal();
            partida.getBatalla().setResultado(resultado);
            int anterior = partida.getMisionActual();
            partida.avanzar();
            comprobar(partida.getMisionActual() == anterior + 1, "Avanza después de " + resultado);
            comprobar(partida.getMapa().getNucleo().getVida() == (vidaNucleo == 0 ? 100 : vidaNucleo), "Conserva daño o restaura núcleo destruido");
            comprobar(partida.getEscuadron().getCapacidadTotal() == capacidadAnterior + 5, "Capacidad tras ambos resultados");
            comprobar("PENDIENTE".equals(partida.getBatalla().getResultado()), "Batalla nueva");
            comprobar(!partida.getBatalla().isEnCurso(), "Avanzar no inicia hilos");
            Map<String, String> nueva = escenario(partida.getMapa());
            comprobar(!nueva.equals(inicial), "Crea enemigos nuevos");
            partida.repetir();
            nueva.put(partida.getMapa().getNucleo().getId(), "12:12:100");
            comprobar(escenario(partida.getMapa()).equals(nueva), "Repite la nueva misión y cura el núcleo");
        }
        // Comprueba que las defensas acumularon una mejora por avance y que repetir no añadió otra.
        Contacto mejorada = (Contacto) partida.getEscuadron().getDefensas().get(0);
        comprobar(mejorada.getHistorico().getMejoras().size() == 2, "Dos avances, dos mejoras");
        int vidaEsperada = 80;
        for (modelo.registros.Mejora mejora : mejorada.getHistorico().getMejoras()) {
            comprobar(mejora.getPorcentajeVida() >= 0.05 && mejora.getPorcentajeVida() <= 0.20,
                    "Porcentaje de vida válido");
            comprobar(mejora.getPorcentajeDmg() >= 0.05 && mejora.getPorcentajeDmg() <= 0.20,
                    "Porcentaje de daño válido");
            vidaEsperada = (int) Math.round(vidaEsperada * (1 + mejora.getPorcentajeVida()));
        }
        comprobar(mejorada.getVidaMax() == vidaEsperada, "Crecimiento compuesto de defensa");
        for (Componente[] fila : partida.getMapa().getMat()) for (Componente unidad : fila) {
            if (unidad instanceof Atacante atacante) {
                comprobar(atacante.getHistorico().getMejoras().size() == 2, "Enemigos con crecimiento acumulado");
                int vidaEnemigo = 50;
                for (modelo.registros.Mejora mejora : atacante.getHistorico().getMejoras())
                    vidaEnemigo = (int) Math.round(vidaEnemigo * (1 + mejora.getPorcentajeVida()));
                comprobar(atacante.getVidaMax() == vidaEnemigo, "Crecimiento compuesto enemigo");
            }
        }
        Contacto destruida = new Contacto("Destruida", 100, 20, 1000, 1);
        destruida.setVida(0);
        destruida.mejorar(0.10, 0.20);
        comprobar(destruida.getVida() == 0 && destruida.getVidaMax() == 110 && destruida.getDmg() == 24,
                "Mejorar no resucita una defensa");
        Map<String, String> antes = escenario(partida.getMapa());
        partida.getBatalla().setEnCurso(true);
        partida.prepararMision(catalogo);
        rechaza(partida::avanzar, "No avanza durante combate");
        rechaza(partida::guardarEstadoInicial, "No copia durante combate");
        comprobar(escenario(partida.getMapa()).equals(antes), "No prepara durante combate");
        partida.getBatalla().setEnCurso(false);
        partida.getBatalla().setResultado("DERROTA");
        int mision = partida.getMisionActual();
        catalogo.getConfigs().get(0).setActivo(false);
        rechaza(partida::avanzar, "Sin plantillas habilitadas");
        comprobar(partida.getMisionActual() == mision, "No incrementa si falla");
        comprobar(partida.getEscuadron().getDefensas().get(0).getHistorico().getMejoras().size() == 2,
                "Una preparación rechazada no aplica mejoras");
        comprobar(escenario(partida.getMapa()).equals(antes), "No limpia si falla");
        partida.repetir();
        comprobar(escenario(partida.getMapa()).equals(antes), "Conserva la copia tras fallo");

        Catalogo caro = new Catalogo();
        caro.agregar(new Configuracion("Caro", "Acechador", 50, 1, 1, 1, 99, 1));
        rechaza(() -> partida.prepararMision(caro), "Ninguna unidad cabe");
        partida.getEscuadron().setCapacidadTotal(0);
        rechaza(() -> partida.prepararMision(caro), "Capacidad cero");
        partida.getEscuadron().setCapacidadTotal(100);
        // Llena las casillas externas para verificar que no se quede buscando una libre.
        partida.getMapa().limpiar();
        for (int f = 0; f < 25; f++) for (int c = 0; c < 25; c++) {
            if (Math.max(Math.abs(f - 12), Math.abs(c - 12)) > 6)
                partida.getMapa().colocar(new Barrera("Muro", 10, 1), f, c);
        }
        rechaza(() -> partida.prepararMision(caro), "Mapa exterior lleno");
        System.out.println("PruebasPartida: todas las comprobaciones pasaron");
    }

    private static Map<String, String> escenario(Mapa mapa) {
        Map<String, String> resultado = new HashMap<>();
        for (int f = 0; f < 25; f++) for (int c = 0; c < 25; c++) {
            Componente unidad = mapa.obtener(f, c);
            if (unidad != null) resultado.put(unidad.getId(), f + ":" + c + ":" + unidad.getVida());
        }
        return resultado;
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void rechaza(Runnable accion, String mensaje) {
        try { accion.run(); }
        catch (IllegalStateException esperado) { return; }
        throw new AssertionError(mensaje);
    }
}
