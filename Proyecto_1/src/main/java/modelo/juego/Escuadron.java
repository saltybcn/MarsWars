package modelo.juego;

import modelo.defensas.Defensa;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Escuadron implements Serializable {
    private static final long serialVersionUID = 1L;
    private int capacidadTotal = 20;
    private ArrayList<Defensa> defensas = new ArrayList<>();

    public Escuadron() {
    }

    public Escuadron(int capacidadTotal) {
        if (capacidadTotal < 0) throw new IllegalArgumentException("Capacidad negativa");
        this.capacidadTotal = capacidadTotal;
    }

    public boolean agregar(Defensa defensa) {
        Objects.requireNonNull(defensa);
        if (defensas.contains(defensa) || defensa.getCosto() <= 0
                || defensa.getCosto() > capacidadRestante()) return false;
        defensas.add(defensa);
        return true;
    }

    public void quitar(Defensa defensa) {
        defensas.remove(defensa);
    }

    public int capacidadUtilizada() {
        int total = 0;
        for (Defensa defensa : defensas) total += defensa.getCosto();
        return total;
    }

    public int capacidadRestante() {
        return capacidadTotal - capacidadUtilizada();
    }

    public int getCapacidadTotal() { return capacidadTotal; }

    public void setCapacidadTotal(int capacidadTotal) {
        if (capacidadTotal < 0){
            System.out.println("Error setCapacidadTotal: capacidadTotal negativo");
            return;
        }
        this.capacidadTotal = capacidadTotal;
    }

    public ArrayList<Defensa> getDefensas() { return new ArrayList<>(defensas); }
}
