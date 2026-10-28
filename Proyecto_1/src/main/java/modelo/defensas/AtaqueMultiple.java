package modelo.defensas;

import modelo.componentes.Componente;

public class AtaqueMultiple extends Defensa {
    private static final long serialVersionUID = 1L;
    private int maxObjetivos;

    public AtaqueMultiple(String nombre, int vida, int dmg, int velAtq, int alcance, int costo, int maxObjetivos) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
        setMaxObjetivos(maxObjetivos);
    }

    public AtaqueMultiple() {
    }

    @Override
    public void atacar(java.util.List<Componente> objetivos) {
        // Una llamada representa una ronda; run controla la espera entre rondas.
        int limite = Math.min(getMaxObjetivos(), objetivos.size());
        for (int i = 0; i < limite; i++) {
            Componente objetivo = objetivos.get(i);
            if (objetivo != null && objetivo.getVida() > 0
                    && (!objetivo.esAereo() || isAtacaAereos())) {
                atacar(objetivo); // Reutiliza el golpe singular y su historial
            }
        }
    }

    public int getMaxObjetivos() { return maxObjetivos; }

    public void setMaxObjetivos(int maxObjetivos) {
        if (maxObjetivos < 0) throw new IllegalArgumentException("maxObjetivos negativo");
        this.maxObjetivos = maxObjetivos;
    }
}
