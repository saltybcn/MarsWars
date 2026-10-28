package modelo.atacantes;

import modelo.componentes.Componente;
import modelo.juego.Mapa;


public class Volador extends Atacante {
    private static final long serialVersionUID = 1L;

    public Volador(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida, dmg, velAtq, alcance, costo);
        setAereo(true);
    }

    public Volador() {
        setAereo(true);
    }

    @Override
    protected boolean esEnemigo(Componente componente) {
        // El volador ignora barreras terrestres; los demás atacantes sí pueden seleccionarlas.
        return super.esEnemigo(componente) && !(componente instanceof modelo.defensas.Barrera);
    }

    @Override
    protected void actuar() {
        // Mientras transcurre el salto no puede atacar ni comenzar otro movimiento.
        if (esperaMovimiento > 0) return;
        super.actuar();
    }

    @Override
    protected void avanzarHacia(Componente objetivo) {
        Mapa mapa = getMapa();
        if (mapa == null || objetivo == null) return;
        synchronized (mapa) {
            int[] origen = getPos(), destino = objetivo.getPos();
            if (mapa.obtener(origen[0], origen[1]) != this
                    || mapa.obtener(destino[0], destino[1]) != objetivo || objetivo.getVida() <= 0) return;
            mover(destino[0], destino[1]);
            // Sin aterrizaje disponible espera; un volador no necesita destruir barreras terrestres.
        }
    }

    @Override
    public void mover(int x, int y) { // Recibe el destino hacia el que debe volar
        // Validación del mapa y del destino
        Mapa mapa = getMapa();
        if (mapa == null) return;
        synchronized (mapa) {
            int[] origen = getPos();
            if (mapa.obtener(origen[0], origen[1]) != this) return;
            Componente[][] celdas = mapa.getMat();
            int filas = celdas.length, columnas = celdas[0].length;
            if (x < 0 || x >= filas || y < 0 || y >= columnas) return;
            int distanciaOriginal = (x - origen[0]) * (x - origen[0])
                    + (y - origen[1]) * (y - origen[1]);
            int mejorDistancia = Integer.MAX_VALUE, mejorRecta = Integer.MAX_VALUE;
            int[] aterrizaje = null;

            // Búsqueda de una casilla de aterrizaje
            for (int df = -1; df <= 1; df++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (df == 0 && dc == 0) continue;
                    int fila = origen[0] + df, col = origen[1] + dc;
                    // Sobrevuela los ocupantes sin retirarlos ni compartir su casilla.
                    while (fila >= 0 && fila < filas && col >= 0 && col < columnas
                            && celdas[fila][col] != null) {
                        fila += df;
                        col += dc;
                    }
                    if (fila < 0 || fila >= filas || col < 0 || col >= columnas) continue;
                    int distancia = Math.max(Math.abs(x - fila), Math.abs(y - col));
                    int recta = (x - fila) * (x - fila) + (y - col) * (y - col);
                    if (recta >= distanciaOriginal) continue; // Evita saltos que lo alejen o lo hagan oscilar
                    if (distancia < mejorDistancia || (distancia == mejorDistancia && recta < mejorRecta)) {
                        mejorDistancia = distancia;
                        mejorRecta = recta;
                        aterrizaje = new int[]{fila, col};
                    }
                }
            }

            // Ejecución del salto
            // Si no hay una casilla libre que lo acerque, espera. actuar cobra la distancia recorrida.
            if (aterrizaje != null) mapa.mover(this, aterrizaje[0], aterrizaje[1]);
        }
    }

}
