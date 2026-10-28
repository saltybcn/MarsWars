package modelo.atacantes;


public class Acechador extends Atacante {
    private static final long serialVersionUID = 1L;

    public Acechador(String nombre, int vida, int dmg, int velAtq, int costo, int velMov) {
        super(nombre, vida, dmg, velAtq, 1, costo, velMov);
    }

    public Acechador() {
        setAlcance(1);
    }


    @Override
    public void setAlcance(int alcance) {
        if (alcance != 1) throw new IllegalArgumentException("El alcance de contacto es 1");
        super.setAlcance(alcance);
    }
}
