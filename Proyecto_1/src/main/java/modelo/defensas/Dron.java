package modelo.defensas;

import contratos.Movimiento;
import modelo.componentes.Componente;
public class Dron extends Defensa implements Movimiento{
    private static final long serialVersionUID = 1L;

    private volatile int velMov = 500; // default

    public Dron(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
        setAereo(false); // El dron se desplaza por tierra
    }

    public Dron(String nombre, int vida, int dmg, int velAtq, int alcance, int costo, int velMov) {
        this(nombre, vida, dmg, velAtq, alcance, costo);
        setVelMov(velMov);
    }

    @Override
    public Componente scan() {
        return detectar(24); // Igual que los atacantes: detecta cerca o busca a quién perseguir
    }

    @Override
    public int getVelMov() { return velMov; }

    public void setVelMov(int velMov) {
        if (velMov <= 0) throw new IllegalArgumentException("Intervalo de movimiento inválido");
        this.velMov = velMov;
    }

    public Dron() {
        setAereo(false); // También al crearlo sin configuración
    }
    @Override
    public void mover(int x, int y){
        getMapa().mover(this, x, y);
    }
}

