package modelo.defensas;


public class Contacto extends Defensa {
    private static final long serialVersionUID = 1L;

    public Contacto(String nombre, int vida, int dmg, int velAtq, int costo) {
        super(nombre, vida, dmg, velAtq, 1, costo);
    }

    public Contacto() {
        setAlcance(1);
    }


    @Override
    public void setAlcance(int alcance) {
        if (alcance != 1) throw new IllegalArgumentException("El alcance de contacto es 1");
        super.setAlcance(alcance);
    }
}
