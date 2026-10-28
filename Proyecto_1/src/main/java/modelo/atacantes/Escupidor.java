package modelo.atacantes;


public class Escupidor extends Atacante {
    private static final long serialVersionUID = 1L;

    public Escupidor(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
    }

    public Escupidor() {
    }

}
