package modelo.defensas;

import modelo.componentes.Componente;

public class Barrera extends Defensa {
    private static final long serialVersionUID = 1L;

    public Barrera(String nombre, int vida, int costo) {
        super(nombre, vida, 0, 1000, 0, costo);
    }

    public Barrera() {
        setDmg(0);
    }

    @Override
    protected void actuar() {
        // La barrera no escanea ni ataca.
    }

    @Override
    public void atacar(Componente objetivo) {
        // La barrera recibe daño, pero no ataca.
    }
}
