package modelo.defensas;

import modelo.atacantes.Atacante;
import modelo.componentes.Combate;
import modelo.componentes.Componente;

public abstract class Defensa extends Combate {
    private static final long serialVersionUID = 1L;

    protected Defensa() {
    }

    protected Defensa(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
    }
    @Override
    public Componente scan() {
        return detectar(getAlcance()); // Las defensas fijas solo necesitan enemigos en alcance
    }

    @Override
    protected boolean esEnemigo(Componente componente) {
        return componente instanceof Atacante;
    }
}
