import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import modelo.atacantes.Acechador;
import modelo.componentes.Componente;
import modelo.componentes.Combate;
import modelo.componentes.Nucleo;
import modelo.defensas.Barrera;
import modelo.defensas.Contacto;
import modelo.defensas.Dron;
import modelo.juego.Mapa;

/** Prueba ejecutable sin dependencias: java -ea PruebasCombate. */
public class PruebasCombate {
    static class Explorador extends Acechador {
        final List<Long> pasos = new CopyOnWriteArrayList<>();
        final List<Long> golpes = new CopyOnWriteArrayList<>();
        Explorador() { super("Prueba", 1000, 1, 220, 1, 60); }
        Componente elegir() { return scan(); }
        void pasoDePrueba() {
            synchronized (getMapa()) {
                Componente objetivo = scan();
                if (objetivo == null) return;
                int[] pos = getPos(), destino = objetivo.getPos();
                if (Math.max(Math.abs(pos[0] - destino[0]),
                        Math.abs(pos[1] - destino[1])) <= getAlcance()) atacar(objetivo);
                else avanzarHacia(objetivo);
            }
        }
        @Override protected void avanzarHacia(Componente objetivo) {
            int[] antes = getPos();
            super.avanzarHacia(objetivo);
            int[] despues = getPos();
            if (antes[0] != despues[0] || antes[1] != despues[1])
                pasos.add(System.nanoTime());
        }
        @Override public void atacar(Componente objetivo) {
            super.atacar(objetivo);
            golpes.add(System.nanoTime());
        }
    }

    static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) throws Exception {
        prioridades();
        fifoYAereos();
        tiemposYPausa();
        dronYRetirada();
        rutasYMuros();
        System.out.println("OK: prioridades, FIFO, tiempos, pausa, dron, retirada y rutas.");
    }

    static void prioridades() {
        Mapa mapa = new Mapa();
        Explorador atacante = new Explorador();
        Nucleo nucleo = new Nucleo("Núcleo", 100);
        Barrera barrera = new Barrera("Barrera", 100, 1);
        Contacto defensa = new Contacto("Defensa", 100, 1, 1000, 1);
        mapa.colocar(atacante, 10, 10);
        mapa.colocar(nucleo, 12, 12);
        mapa.colocar(barrera, 10, 12);
        mapa.colocar(defensa, 12, 10);
        comprobar(atacante.elegir() == nucleo, "Núcleo gana el empate");
        nucleo.setVida(0);
        comprobar(atacante.elegir() == defensa, "Defensa gana a barrera");
        Barrera cercana = new Barrera("Cercana", 100, 1);
        mapa.colocar(cercana, 10, 11);
        comprobar(atacante.elegir() == cercana, "El muro cercano también es un objetivo terrestre válido");
        Explorador borde = new Explorador();
        Mapa esquina = new Mapa();
        esquina.colocar(borde, 0, 0);
        esquina.colocar(new Contacto("Lejana", 10, 0, 1000, 1), 24, 24);
        comprobar(borde.elegir() != null, "Búsqueda alcanza esquina contraria");
    }

    static void fifoYAereos() {
        Mapa mapa = new Mapa();
        Contacto defensa = new Contacto("Defensa", 100, 1, 1000, 1);
        Explorador primero = new Explorador(), segundo = new Explorador();
        mapa.colocar(defensa, 5, 5);
        mapa.colocar(primero, 6, 6);
        comprobar(defensa.scan() == primero, "Primer detectado");
        mapa.colocar(segundo, 4, 4);
        comprobar(defensa.scan() == primero, "FIFO conserva el primero");
        primero.setAereo(true);
        comprobar(defensa.scan() == segundo, "No bloquearse con un aéreo inválido");
    }

    static void tiemposYPausa() throws Exception {
        Mapa mapa = new Mapa();
        Explorador atacante = new Explorador();
        mapa.colocar(atacante, 0, 0);
        mapa.colocar(new Contacto("Objetivo", 1000, 0, 1000, 1), 0, 5);
        atacante.start();
        Thread hilo = hiloDe(atacante);
        try {
            long limite = System.nanoTime() + 3_000_000_000L;
            while (atacante.golpes.size() < 3 && System.nanoTime() < limite)
                Thread.sleep(10);
            comprobar(atacante.pasos.size() == 4, "Se detiene en alcance");
            comprobar(atacante.golpes.size() >= 3, "Ataca al llegar");
            for (int i = 1; i < atacante.pasos.size(); i++)
                comprobar(atacante.pasos.get(i) - atacante.pasos.get(i - 1)
                        >= 55_000_000L, "Intervalo de movimiento");
            for (int i = 1; i < atacante.golpes.size(); i++)
                comprobar(atacante.golpes.get(i) - atacante.golpes.get(i - 1)
                        >= 215_000_000L, "Intervalo de ataque");
            comprobar(atacante.pasos.get(3) - atacante.pasos.get(0)
                    < 600_000_000L, "Movimiento independiente de velAtq");
            atacante.pausar();
            Thread.sleep(60);
            int golpes = atacante.golpes.size();
            Thread.sleep(300);
            comprobar(atacante.golpes.size() == golpes, "Pausa");
            atacante.reanudar();
            long reanudaHasta = System.nanoTime() + 1_000_000_000L;
            while (atacante.golpes.size() == golpes && System.nanoTime() < reanudaHasta) Thread.sleep(10);
            comprobar(atacante.golpes.size() > golpes, "Reanuda");
            atacante.pausar();
        } finally {
            atacante.detener();
            hilo.join(1000);
            comprobar(!hilo.isAlive(), "Detener despierta una unidad pausada");
        }
        int golpes = atacante.golpes.size();
        atacante.start(); // Ahora una unidad detenida puede crear una nueva ejecución.
        Thread reiniciado = hiloDe(atacante);
        try {
            long limite = System.nanoTime() + 1_000_000_000L;
            while (atacante.golpes.size() == golpes && System.nanoTime() < limite) Thread.sleep(10);
            comprobar(reiniciado != hilo && atacante.golpes.size() > golpes, "start crea otro hilo al reiniciar");
        } finally {
            atacante.detener();
            reiniciado.join(1000);
            comprobar(!reiniciado.isAlive(), "Reinicio detenido");
        }
    }

    static Thread hiloDe(Combate unidad) throws Exception {
        java.lang.reflect.Field campo = Combate.class.getDeclaredField("hilo");
        campo.setAccessible(true);
        synchronized (unidad) { return (Thread) campo.get(unidad); }
    }

    static void dronYRetirada() throws Exception {
        Mapa mapa = new Mapa();
        Dron dron = new Dron("Dron", 100, 2000, 100, 1, 1, 30);
        Explorador objetivo = new Explorador();
        mapa.colocar(dron, 0, 0);
        mapa.colocar(objetivo, 0, 3);
        comprobar(dron.scan() == objetivo, "Dron busca fuera de alcance");
        dron.start();
        Thread hilo = hiloDe(dron);
        try {
            long limite = System.nanoTime() + 2_000_000_000L;
            while (objetivo.getVida() > 0 && System.nanoTime() < limite)
                Thread.sleep(10);
            synchronized (mapa) {
                comprobar(objetivo.getVida() == 0, "Daño limitado a la vida");
                comprobar(mapa.obtener(0, 3) == null, "Muerto retirado");
            }
            comprobar(!objetivo.isActivo(), "Unidad destruida detenida");
            comprobar(dron.getHistorico().getInfligido().size() == 1, "Historial");
            new Barrera("Sin mapa", 5, 1).recibirDmg(10);
        } finally {
            dron.detener();
            hilo.join(1000);
            comprobar(!hilo.isAlive(), "Dron detenido");
        }
    }

    static void rutasYMuros() {
        // Las barreras son defensas válidas; la más cercana recibe daño antes de continuar.
        Mapa mapa = new Mapa();
        Explorador atacante = new Explorador();
        Nucleo nucleo = new Nucleo("Núcleo", 100);
        Barrera cercana = new Barrera("Muro lateral", 20, 1);
        mapa.colocar(atacante, 11, 15);
        mapa.colocar(nucleo, 12, 12);
        mapa.colocar(cercana, 12, 15);
        mapa.colocar(new Contacto("Defensa lejana", 100, 0, 1000, 1), 11, 11);
        comprobar(atacante.elegir() == cercana, "La barrera más cercana también es una defensa atacable");
        for (int i = 0; i < 80; i++) atacante.pasoDePrueba();
        comprobar(nucleo.getVida() < 100 && cercana.getVida() == 0,
                "Destruir el objetivo cercano y continuar al núcleo");

        // Un grupo de aliados obliga a alejarse temporalmente para encontrar la salida.
        Mapa desvio = new Mapa();
        Explorador rodeador = new Explorador();
        Nucleo fondo = new Nucleo("Núcleo", 100);
        desvio.colocar(rodeador, 10, 10);
        desvio.colocar(fondo, 12, 12);
        for (int f = 9; f <= 11; f++)
            desvio.colocar(new Explorador(), f, 11);
        desvio.colocar(new Explorador(), 9, 10);
        desvio.colocar(new Explorador(), 11, 10);
        rodeador.pasoDePrueba();
        comprobar(rodeador.getPos()[1] == 9,
                "Primer paso se aleja para rodear; no corta esquinas");
        for (int i = 0; i < 30 && fondo.getVida() == 100; i++)
            rodeador.pasoDePrueba();
        comprobar(fondo.getVida() < 100, "Sale de la U y alcanza el núcleo");
        for (Componente[] fila : desvio.getMat())
            for (Componente c : fila)
                if (c instanceof Explorador && c != rodeador) comprobar(c.getVida() == 1000, "No daña a sus aliados");

        // Núcleo encerrado: romper una barrera que abra una ruta real.
        Mapa cerrado = new Mapa();
        Explorador sitiador = new Explorador();
        sitiador.setDmg(20);
        Nucleo encerrado = new Nucleo("Encerrado", 100);
        cerrado.colocar(sitiador, 8, 12);
        cerrado.colocar(encerrado, 12, 12);
        Barrera distraccion = new Barrera("No bloquea la ruta", 20, 1);
        cerrado.colocar(distraccion, 8, 11);
        for (int f = 11; f <= 13; f++)
            for (int c = 11; c <= 13; c++)
                if (f != 12 || c != 12)
                    cerrado.colocar(new Barrera("Cerco", 20, 1), f, c);
        Componente elegido = sitiador.elegir();
        comprobar(elegido == distraccion,
                "La barrera adyacente se puede detectar y atacar");
        for (int i = 0; i < 25 && encerrado.getVida() == 100; i++)
            sitiador.pasoDePrueba();
        comprobar(encerrado.getVida() < 100 && distraccion.getVida() == 0,
                "Abre entrada y continúa al núcleo");

        Mapa soloMuros = new Mapa();
        Explorador sinObjetivo = new Explorador();
        soloMuros.colocar(sinObjetivo, 0, 0);
        soloMuros.colocar(new Barrera("Muro", 10, 1), 0, 1);
        comprobar(sinObjetivo.elegir() instanceof Barrera, "Las barreras no desaparecen del escaneo terrestre");
    }
}

