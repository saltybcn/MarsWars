package contratos;

/** Comparte el contrato de desplazamiento de atacantes y Dron. */
public interface Movimiento {
    void mover(int x, int y);
    int getVelMov(); // Milisegundos entre pasos.
}

