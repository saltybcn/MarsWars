import configuracion.*;
import modelo.atacantes.*;
import modelo.componentes.*;
import modelo.defensas.*;
import modelo.juego.*;

/** Casos del PDF que no dependen de ventanas ni de valores aleatorios exactos. */
public class PruebasRequisitosPDF {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) throws Exception {
        Mapa mapa = new Mapa();
        Nucleo nucleo = new Nucleo("Núcleo", 500);
        comprobar(mapa.colocar(nucleo, 12, 12), "Núcleo central");
        comprobar(nucleo.getMapa() == mapa, "El núcleo también conoce su mapa");
        comprobar(!mapa.mover(nucleo, 12, 13), "Núcleo fijo");
        Partida partida = new Partida("Comandante PDF", mapa);
        comprobar(partida.getEscuadron().getCapacidadTotal() == 20, "Capacidad inicial 20");
        Contacto defensa = new Contacto("Defensa", 200, 20, 1000, 2);
        partida.getEscuadron().agregar(defensa); mapa.colocar(defensa, 3, 4);
        Catalogo catalogo = new Catalogo();
        catalogo.agregar(new Configuracion("Acechador", "Acechador", 100, 10, 1, 1, 4, 1));
        partida.setMisionActual(10);
        partida.prepararMision(catalogo);
        int costoEnemigos = 0;
        for (Componente[] fila : mapa.getMat()) for (Componente c : fila) {
            if (c instanceof Atacante atacante) {
                int[] p = c.getPos();
                comprobar(Math.abs(p[0] - 12) > 6 || Math.abs(p[1] - 12) > 6, "Aparición fuera del área protegida del núcleo");
                costoEnemigos += atacante.getCosto();
                comprobar(atacante.getHistorico().getMejoras().size() == 9, "Crecimiento hasta misión 10");
            }
        }
        comprobar(costoEnemigos == 20, "Ejército usa el presupuesto total, no el remanente del escuadrón");
        partida.getBatalla().setResultado("VICTORIA"); partida.avanzar();
        comprobar(partida.getMisionActual() == 11, "Permite generar misión 11");
        comprobar(partida.getEscuadron().getCapacidadTotal() == 25, "Premio de capacidad tras victoria");
        Defensa conservada = partida.getEscuadron().getDefensas().get(0);
        comprobar(conservada.getVidaMax() >= 210 && conservada.getVidaMax() <= 240, "Mejora de vida 5–20%");
        comprobar(conservada.getDmg() >= 21 && conservada.getDmg() <= 24, "Mejora independiente del daño");
        partida.getBatalla().setResultado("DERROTA"); partida.avanzar();
        comprobar(partida.getMisionActual() == 12 && partida.getEscuadron().getCapacidadTotal() == 30,
                "Avanzar tras perder también aumenta la capacidad");
        int vida = partida.getEscuadron().getDefensas().get(0).getVidaMax();
        partida.repetir();
        comprobar(partida.getEscuadron().getDefensas().get(0).getVidaMax() == vida, "Repetir no vuelve a mejorar");

        // La derrota impide turnos adicionales aunque el monitor todavía no haya despertado.
        mapa = new Mapa(); nucleo = new Nucleo("Núcleo", 1); mapa.colocar(nucleo, 12, 12);
        Acechador enemigo = new Acechador("Enemigo", 100, 10, 500, 1, 500);
        mapa.colocar(enemigo, 12, 13);
        Batalla batalla = new Batalla(mapa);
        try {
            batalla.iniciar();
            long limite = System.nanoTime() + 2_000_000_000L;
            while (batalla.isEnCurso() && System.nanoTime() < limite) Thread.sleep(10);
            comprobar("DERROTA".equals(batalla.getResultado()) && !batalla.isEnCurso(), "Derrota automática por núcleo");
            comprobar(nucleo.getVida() == 0, "Vida final cero sin valores negativos");
            comprobar(nucleo.getHistorico().getRecibido().get(0).getNumGolpes() == 1, "Historial del golpe final");
        } finally { batalla.finalizar(); }
        System.out.println("OK PDF: núcleo, capacidad, perímetro, crecimiento, misión 11, derrota y repetición.");
    }
}
