package modelo.defensas;

import modelo.componentes.Componente;

public class Impacto extends Defensa{
    private static final long serialVersionUID = 1L;

    public Impacto(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
    }

    public Impacto() {
    }

    @Override
    public void atacar(java.util.List<Componente> objetivos) {
        // El área de explosión corresponde a los enemigos detectados en alcance.
        boolean golpeo = false;
        for (Componente objetivo : objetivos) {
            if (objetivo != null && objetivo.getVida() > 0
                    && (!objetivo.esAereo() || isAtacaAereos())) {
                atacar(objetivo);
                golpeo = true;
            }
        }
        // No se consume si no había ningún objetivo válido para la explosión.
        if (golpeo) recibirDmg(getVida()); // Cada sonido respeta su propia casilla Activado.
    }
}
