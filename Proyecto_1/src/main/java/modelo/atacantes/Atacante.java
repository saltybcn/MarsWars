package modelo.atacantes;

import modelo.componentes.Combate;
import modelo.componentes.Componente;
import contratos.Movimiento;
public abstract class Atacante extends Combate implements Movimiento{
    private static final long serialVersionUID = 1L;
    private volatile int velMov = 500; // Milisegundos por paso; Configuracion asigna el intervalo del catálogo.
    
    protected Atacante() {
    }    
    protected Atacante(String nombre, int vida, int dmg, int velAtq, int alcance, int costo, int velMov) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
        setVelMov(velMov);
    }
    // Conserva los constructores existentes; el intervalo se puede configurar.
    protected Atacante(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        this(nombre, vida, dmg, velAtq, alcance, costo, 500);
    }

    @Override
    protected Componente scan() {
        return detectar(24); // Si no hay nadie en alcance, busca a quién acercarse
    }

    @Override
    protected boolean esEnemigo(Componente componente) {
        return !(componente instanceof Atacante);
    }

    @Override
    public void mover(int x, int y){
        getMapa().mover(this, x, y);
    }
    
    public int getVelMov() {
        return velMov;
    }

    public void setVelMov(int velMov) {
        if (velMov <= 0) {
            throw new IllegalArgumentException(
                    "La velocidad de movimiento debe ser mayor que 0");
        }
        this.velMov = velMov;
}
}

