package modelo.defensas;


public class AlcanceMedio extends Defensa {
    private static final long serialVersionUID = 1L;

    public AlcanceMedio(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
    }

    public AlcanceMedio() {
    }

}
