package modelo.juego;

import modelo.componentes.Componente;
import java.io.Serializable;
import java.util.ArrayList;
import modelo.atacantes.Atacante;
import modelo.componentes.Combate;
import modelo.defensas.Barrera;

public class Batalla implements Serializable {
    private static final long serialVersionUID = 1L;
    private Mapa mapa;
    private ArrayList<Componente> participantes = new ArrayList<>();
    private transient volatile boolean enCurso;
    private volatile boolean pausada;
    private boolean iniciada;
    private transient volatile Thread comprobador;
    private String resultado = "PENDIENTE";

    public Batalla(Mapa mapa) {
        this.mapa = java.util.Objects.requireNonNull(mapa);
    }

    public Batalla() {
    }

    public void iniciar() {
        if (mapa == null) return;
        synchronized (mapa) {
            if (enCurso || !"PENDIENTE".equals(resultado)) return;
            // Una carga conserva también los participantes destruidos y sus estadísticas.
            if (!iniciada) {
                participantes.clear();
                boolean hayAtacantes = false;
                for (Componente[] fila : mapa.getMat()) {
                    for (Componente componente : fila) {
                        if (componente != null) {
                            participantes.add(componente);
                            if (componente instanceof Atacante && componente.getVida() > 0) hayAtacantes = true;
                        }
                    }
                }
                if (!hayAtacantes || !participantes.contains(mapa.getNucleo())) {
                    participantes.clear();
                    throw new IllegalStateException("Coloque un núcleo y atacantes antes de iniciar.");
                }
                iniciada = true;
            }
            enCurso = true;
            pausada = false;
            // Resuelve un guardado tomado justo al terminar el último ataque.
            comprobarResultado();
            if (!enCurso) return;
            for (Componente componente : participantes) {
                int[] pos = componente.getPos();
                if (componente instanceof Combate combate && !(componente instanceof Barrera)
                        && componente.getVida() > 0 && pos != null
                        && mapa.obtener(pos[0], pos[1]) == componente) combate.start();
            }
            comprobador = new Thread(() -> {
                try {
                    // La identidad impide que un monitor anterior se una a una nueva ejecución.
                    while (enCurso && Thread.currentThread() == comprobador) {
                        comprobarResultado();
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Comprobar-resultado");
            comprobador.start();
        }
    }

    public void pausar() {
        if (mapa == null) return;
        synchronized (mapa) {
            if (!"PENDIENTE".equals(resultado)) return;
            // También permite abrir el menú durante la colocación, sin iniciar la batalla.
            pausada = true;
            for (Componente componente : participantes) {
                if (componente instanceof Combate combate) combate.pausar();
            }
        }
    }

    public void reanudar() {
        if (mapa == null) return;
        synchronized (mapa) {
            if (!pausada || !"PENDIENTE".equals(resultado)) return;
            if (iniciada && !enCurso) {
                iniciar(); // Tras cargar, los campos transient no contienen hilos.
                return;
            }
            for (Componente componente : participantes) {
                if (componente instanceof Combate combate) combate.reanudar();
            }
            pausada = false;
        }
    }

    public void comprobarResultado() {
        if (mapa == null) return;
        synchronized (mapa) {
            // Comprueba dentro del bloqueo: la pausa pudo ocurrir mientras esperaba.
            if (!enCurso || pausada) return;
            if (mapa.getNucleo() == null || !participantes.contains(mapa.getNucleo())
                    || mapa.getNucleo().getVida() <= 0) {
                resultado = "DERROTA";
                finalizar();
                return;
            }
            boolean hayAtacantes = false;
            for (Componente componente : participantes) {
                int[] pos = componente.getPos();
                if (componente instanceof Atacante && componente.getVida() > 0
                        && pos != null && mapa.obtener(pos[0], pos[1]) == componente) {
                    hayAtacantes = true;
                    break;
                }
            }
            if (!hayAtacantes) {
                resultado = "VICTORIA";
                finalizar();
            }
        }
    }

    public void finalizar() {
        if (mapa == null) return;
        synchronized (mapa) {
            enCurso = false;
            pausada = false;
            for (Componente componente : participantes) {
                if (componente instanceof Combate combate) combate.detener();
            }
            if (comprobador != null) comprobador.interrupt();
        }
    }

    public boolean isPausada() { return pausada; }
    public boolean isIniciada() { return iniciada; }

    private void readObject(java.io.ObjectInputStream entrada)
            throws java.io.IOException, ClassNotFoundException {
        entrada.defaultReadObject();
        // Los guardados anteriores no tenían iniciada; su lista permite reconocer el combate.
        iniciada = iniciada || !participantes.isEmpty();
        pausada = "PENDIENTE".equals(resultado);
        // enCurso y comprobador son transient: cargar nunca arranca una batalla.
    }

    public Mapa getMapa() { return mapa; }

    public void setMapa(Mapa mapa) {
        this.mapa = mapa;
    }

    public ArrayList<Componente> getParticipantes() {
        if (mapa == null) return new ArrayList<>();
        synchronized (mapa) { return new ArrayList<>(participantes); }
    }


    public boolean isEnCurso() { return enCurso; }

    public void setEnCurso(boolean enCurso) {
        this.enCurso = enCurso;
    }

    public String getResultado() { return resultado; }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
