package modelo.atacantes;

import modelo.componentes.Componente;

public class Demoledor extends Atacante{
    private static final long serialVersionUID = 1L;

    public Demoledor(String nombre, int vida, int dmg, int velAtq, int costo, int alcance) {
        // La explosión usa detectados: el alcance define el área, igual que en Impacto.
        super(nombre, vida, dmg, velAtq, alcance, costo);
    }

    public Demoledor() {
        setAlcance(1);
    }

    @Override
    protected int alcanceParaAtacar() { return 1; } // Contacto activa la explosión; alcance es su radio.

    @Override
    public void atacar(java.util.List<Componente> objetivos) {
        if (objetivos == null || getMapa() == null) return;
        synchronized (getMapa()) {
        int[] origen = getPos();
        boolean contacto = false;
        for (Componente objetivo : objetivos) {
            if (objetivo == null || objetivo.getVida() <= 0 || !esEnemigo(objetivo)
                    || (objetivo.esAereo() && !isAtacaAereos())) continue;
            int[] pos = objetivo.getPos();
            if (getMapa().obtener(pos[0], pos[1]) == objetivo
                    && Math.max(Math.abs(pos[0] - origen[0]), Math.abs(pos[1] - origen[1])) <= 1)
                contacto = true;
        }
        if (!contacto) return;
        boolean golpeo = false;
        // Recorre el radio al detonar; detectados contiene solo los objetivos de contacto.
        for (int f = Math.max(0, origen[0] - getAlcance()); f <= Math.min(24, origen[0] + getAlcance()); f++) {
            for (int c = Math.max(0, origen[1] - getAlcance()); c <= Math.min(24, origen[1] + getAlcance()); c++) {
                Componente objetivo = getMapa().obtener(f, c);
                if (objetivo != null && objetivo != this && objetivo.getVida() > 0 && esEnemigo(objetivo)
                        && (!objetivo.esAereo() || isAtacaAereos())) {
                    atacar(objetivo); golpeo = true;
                }
            }
        }
        // No se consume si no había ningún objetivo válido para la explosión.
        if (golpeo) recibirDmg(getVida()); // Cada sonido respeta su propia casilla Activado.
        } // La retirada del mapa ocurre al llegar a cero
    }
}

