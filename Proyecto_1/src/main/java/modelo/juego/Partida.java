package modelo.juego;

import configuracion.Catalogo;
import configuracion.Configuracion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import modelo.atacantes.Atacante;

import modelo.componentes.Combate;
import modelo.componentes.Componente;

public class Partida implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nombreComandante;
    private int misionActual = 1;
    private Mapa mapa;
    private Escuadron escuadron;
    private Batalla batalla;
    private Catalogo catalogo;
    private byte[] estadoInicial; // una copia del estado inicial del mapa antes de iniciar una batalla, sirve para repetir la mision

    public Partida(Mapa mapa) {
        this.mapa = java.util.Objects.requireNonNull(mapa);
        this.escuadron = new Escuadron();
        this.batalla = new Batalla(mapa);
    }

    public Partida(String nombreComandante, Mapa mapa) {
        this(mapa);
        if (nombreComandante == null || nombreComandante.isBlank())
            throw new IllegalArgumentException("Nombre de comandante vacío");
        this.nombreComandante = nombreComandante;
    }

    public void prepararMision(Catalogo catalogo) {
        if (batalla != null && batalla.isEnCurso()) return;
        java.util.Objects.requireNonNull(catalogo, "Catálogo requerido");
        synchronized (mapa) {
            // Se comprueba otra vez bajo el bloqueo que usa Batalla.iniciar.
            if (batalla != null && batalla.isEnCurso()) return;
            int capacidad = escuadron.getCapacidadTotal();
            ArrayList<Configuracion> habilitadas = new ArrayList<>();

            // Solo participan plantillas válidas, activas y disponibles para esta misión.
            for (Configuracion config : catalogo.getConfigs()) {
                if (("Acechador".equals(config.getTipo())
                        || "Escupidor".equals(config.getTipo())
                        || "Enjambre".equals(config.getTipo())
                        || "Demoledor".equals(config.getTipo())
                        || "Volador".equals(config.getTipo()))
                        && config.isActivo() && config.validar()
                        && misionActual >= config.getMisionMinima()
                        && config.getCostoCapacidad() <= capacidad) {
                    habilitadas.add(config);
                }
            }
            if (habilitadas.isEmpty()) {
                throw new IllegalStateException("No hay atacantes habilitados que quepan en la capacidad");
            }

            // La referencia del núcleo también debe corresponder a una casilla del mapa.
            Componente[][] celdas = mapa.getMat();
            int[] posNucleo = mapa.getNucleo() == null ? null : mapa.getNucleo().getPos();
            if (posNucleo == null || posNucleo.length != 2
                    || posNucleo[0] < 0 || posNucleo[0] >= celdas.length
                    || posNucleo[1] < 0 || posNucleo[1] >= celdas[posNucleo[0]].length
                    || celdas[posNucleo[0]][posNucleo[1]] != mapa.getNucleo()
                    || mapa.getNucleo().getVidaMax() <= 0) {
                throw new IllegalStateException("No hay un núcleo válido colocado en el mapa");
            }

            // Excluye solo el cuadrado de radio 6 del núcleo, incluido su borde.
            // Una defensa no amplía esta zona: únicamente ocupa su propia casilla.
            // Los atacantes antiguos liberarán sus casillas al limpiar.
            int minF = posNucleo[0] - 6, maxF = posNucleo[0] + 6;
            int minC = posNucleo[1] - 6, maxC = posNucleo[1] + 6;
            ArrayList<int[]> disponibles = new ArrayList<>();
            for (int fila = 0; fila < celdas.length; fila++) {
                for (int columna = 0; columna < celdas[fila].length; columna++) {
                    if ((celdas[fila][columna] == null || celdas[fila][columna] instanceof Atacante)
                            && (fila < minF || fila > maxF || columna < minC || columna > maxC)) {
                        disponibles.add(new int[]{fila, columna});
                    }
                }
            }
            if (disponibles.isEmpty()) {
                throw new IllegalStateException("No hay casillas disponibles fuera del área del núcleo");
            }

            // Las validaciones anteriores conservan la misión anterior si no puede prepararse.
            if (batalla != null) batalla.finalizar();
            mapa.limpiar();
            // Preparar enemigos no cura al núcleo: el daño forma parte de la dificultad acumulada.
            mapa.getNucleo().setVidaInicial(mapa.getNucleo().getVida());
            while (capacidad > 0 && !disponibles.isEmpty()) {
                for (int i = habilitadas.size() - 1; i >= 0; i--) {
                    if (habilitadas.get(i).getCostoCapacidad() > capacidad) 
                        habilitadas.remove(i);
                }
                if (habilitadas.isEmpty()) break;

                Configuracion config = habilitadas.get((int) (Math.random() * habilitadas.size()));
                // Cada casilla se intenta una sola vez: no hay reintentos infinitos.
                int[] pos = disponibles.remove((int) (Math.random() * disponibles.size()));
                Combate unidad = config.crearUnidad();
                // Cada criatura nueva acumula crecimiento desde su misión mínima de aparición.
                // Se calcula sobre el resultado anterior, no siempre sobre la plantilla base.
                for (int nivel = config.getMisionMinima() + 1; nivel <= misionActual; nivel++) {
                    unidad.setMision(nivel); // mejorar registra la misión correspondiente al aumento.
                    unidad.mejorar(java.util.concurrent.ThreadLocalRandom.current().nextInt(5, 21) / 100.0,
                            java.util.concurrent.ThreadLocalRandom.current().nextInt(5, 21) / 100.0);
                }
                unidad.setMision(misionActual); // Misión en que aparece esta instancia.
                unidad.setVidaInicial(unidad.getVidaMax()); // Los resultados usan su vida al entrar a batalla.
                if (mapa.colocar(unidad, pos[0], pos[1])) capacidad -= config.getCostoCapacidad();
            }
            this.catalogo = catalogo; // avanzar reutiliza el catálogo recibido al preparar.
            batalla = new Batalla(mapa);
            estadoInicial = null;
            guardarEstadoInicial();
            // Si cambia la colocación de defensas, guarda otra vez antes de iniciar la batalla.
        }
    }

    public void guardarEstadoInicial() {
        synchronized (mapa) {
            if (batalla != null && batalla.isEnCurso()) {
                throw new IllegalStateException(
                        "No se puede guardar el escenario inicial durante el combate");
            }

            try (ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                 ObjectOutputStream salida = new ObjectOutputStream(memoria)) {

                // Una sola escritura conserva las referencias compartidas.
                // Las defensas del escuadrón también pertenecen al mapa.
                salida.writeObject(new Object[]{mapa, escuadron});
                salida.flush();

                estadoInicial = memoria.toByteArray();

            } catch (IOException e) {
                throw new IllegalStateException(
                        "No se pudo copiar el escenario inicial", e);
            }
        }
    }
    public void avanzar() {
        synchronized (mapa) {
            if (batalla == null || batalla.isEnCurso()
                    || !("VICTORIA".equals(batalla.getResultado())
                    || "DERROTA".equals(batalla.getResultado()))) {
                throw new IllegalStateException("Termine la batalla antes de avanzar");
            }
            if (catalogo == null) {
                throw new IllegalStateException("Prepare la misión con un catálogo antes de avanzar");
            }
            // Trabaja sobre una copia del estado actual, no sobre la copia inicial del reintento.
            // Si falla la preparación o una mejora, la partida original permanece intacta.
            try (ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                 ObjectOutputStream salida = new ObjectOutputStream(memoria)) {
                // No copia el byte[] de escenarios anteriores: prepararMision los reemplaza.
                // El mismo flujo conserva las referencias compartidas de mapa, escuadrón y participantes.
                salida.writeObject(new Object[]{mapa, escuadron, batalla});
                salida.flush();
                Partida siguiente;
                try (ObjectInputStream entrada = new ObjectInputStream(
                        new ByteArrayInputStream(memoria.toByteArray()))) {
                    Object[] estado = (Object[]) entrada.readObject();
                    siguiente = new Partida((Mapa) estado[0]);
                    siguiente.nombreComandante = nombreComandante;
                    siguiente.misionActual = misionActual;
                    siguiente.escuadron = (Escuadron) estado[1];
                    siguiente.batalla = (Batalla) estado[2];
                }

                // Conserva los participantes antes de que prepararMision reemplaza la batalla.
                ArrayList<Componente> anteriores = siguiente.batalla.getParticipantes();
                for (modelo.defensas.Defensa defensa : siguiente.escuadron.getDefensas()) {
                    if (!anteriores.contains(defensa)) anteriores.add(defensa);
                }
                // Si fue destruido, recupera el núcleo para poder jugar la misión siguiente.
                // Un núcleo que aún tiene vida conserva todo el daño recibido.
                if (siguiente.mapa.getNucleo().getVida() == 0)
                    siguiente.mapa.getNucleo().setVida(siguiente.mapa.getNucleo().getVidaMax());
                siguiente.misionActual++; // Habilita las configuraciones de la siguiente misión.
                // Cada avance aumenta la capacidad, tanto después de victoria como de derrota.
                siguiente.escuadron.setCapacidadTotal(escuadron.getCapacidadTotal()+5);
                for (Componente componente : anteriores) {
                    if (componente instanceof modelo.defensas.Defensa defensa) {
                        int aparicion = defensa.getMision();
                        // Dos sorteos independientes: vida y daño no comparten el porcentaje.
                        defensa.setMision(siguiente.misionActual);
                        defensa.mejorar(java.util.concurrent.ThreadLocalRandom.current().nextInt(5, 21) / 100.0,
                                java.util.concurrent.ThreadLocalRandom.current().nextInt(5, 21) / 100.0);
                        defensa.setMision(aparicion); // Conserva la misión original de aparición.
                        defensa.setVidaInicial(defensa.getVida());
                    }
                }
                // Prepara después de mejorar: su única copia inicial ya incluye las estadísticas finales.
                // Si falla, todos los cambios siguen en la copia y la partida original permanece intacta.
                siguiente.prepararMision(catalogo);
                batalla.finalizar();
                mapa = siguiente.mapa;
                escuadron = siguiente.escuadron;
                batalla = siguiente.batalla;
                misionActual = siguiente.misionActual;
                estadoInicial = siguiente.estadoInicial;
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("No se pudo preparar la siguiente misión", e);
            }
        }
    }

    public void repetir() {
        if (estadoInicial == null) {
            throw new IllegalStateException(
                    "Todavía no se ha guardado el escenario inicial");
        }

        try (ByteArrayInputStream memoria =
                    new ByteArrayInputStream(estadoInicial);
             ObjectInputStream entrada = new ObjectInputStream(memoria)) {

            // Cada lectura produce objetos nuevos.
            // La copia guardada permanece intacta para futuros reintentos.
            Object[] estado = (Object[]) entrada.readObject();

            Mapa mapaRestaurado = (Mapa) estado[0];
            Escuadron escuadronRestaurado = (Escuadron) estado[1];

            // Detiene el escenario anterior antes de reemplazarlo.
            if (batalla != null) batalla.finalizar();

            mapa = mapaRestaurado;
            escuadron = escuadronRestaurado;
            batalla = new Batalla(mapa);
            // Reiniciar siempre cura al núcleo, incluso si empezó esta misión dañado.
            mapa.getNucleo().setVida(mapa.getNucleo().getVidaMax());
            mapa.getNucleo().setVidaInicial(mapa.getNucleo().getVidaMax());

            // No cambia la misión ni inicia los hilos.

        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(
                    "No se pudo restaurar el escenario inicial", e);
        }
    }

    public Catalogo getCatalogo() { return catalogo; }

    public String getNombreComandante() { return nombreComandante; }

    public void setNombreComandante(String nombreComandante) {
        this.nombreComandante = nombreComandante;
    }

    public int getMisionActual() { return misionActual; }

    public void setMisionActual(int misionActual) {
        this.misionActual = misionActual;
    }

    public Mapa getMapa() { return mapa; }

    public void setMapa(Mapa mapa) {
        this.mapa = mapa;
    }

    public Escuadron getEscuadron() { return escuadron; }

    public void setEscuadron(Escuadron escuadron) {
        this.escuadron = escuadron;
    }

    public Batalla getBatalla() { return batalla; }

    public void setBatalla(Batalla batalla) {
        this.batalla = batalla;
    }
}

