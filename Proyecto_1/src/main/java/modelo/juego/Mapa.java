package modelo.juego;

import modelo.componentes.Componente;
import modelo.componentes.Nucleo;
import modelo.componentes.Combate;
import java.io.Serializable;
import modelo.atacantes.Atacante;

public class Mapa implements Serializable {
    private static final long serialVersionUID = 1L;
    private Componente[][] mat = new Componente[25][25];
    private Nucleo nucleo;

    public Mapa() {
    }
    public synchronized void limpiar(){
        for(int fila = 0;fila<mat.length;fila++){
            for(int columna = 0;columna<mat[0].length;columna++){
                // Solo retira criaturas de la misión anterior; conserva defensas y núcleo.
                if(mat[fila][columna] instanceof Atacante) retirar(fila,columna);
            }
        }
    }
    public synchronized boolean colocar(Componente unidad, int x, int y) {
        // Valida los límites antes de acceder a la matriz.
        if (unidad == null
                || x < 0 || x >= mat.length
                || y < 0 || y >= mat[x].length) {
            return false;
        }

        if (unidad instanceof Nucleo && (x != 12 || y != 12)) return false;

        if (mat[x][y] != null) {
            return false;
        }

        int[] pos = unidad.getPos();

        // Acepta null o [-1, -1] como posición sin colocar.
        // Cualquier otra posición indica que la unidad ya está colocada.
        if (pos != null
                && (pos.length != 2 || pos[0] != -1 || pos[1] != -1)) {
            return false;
        }

        // Evita duplicar el objeto si su posición fue modificada por error.
        for (Componente[] fila : mat) {
            for (Componente componente : fila) {
                if (componente == unidad) {
                    return false;
                }
            }
        }

        // El mapa solo puede tener un núcleo.
        if (unidad instanceof Nucleo
                && nucleo != null && nucleo != unidad) {
            return false;
        }

        unidad.setPos(new int[]{x, y});
        mat[x][y] = unidad;

        // Todos los componentes pertenecen al mapa, incluido el núcleo.
        // El detalle y el guardado usan esa referencia para consultar un estado coherente.
        unidad.setMapa(this);

        // Mantiene la referencia al núcleo al colocarlo.
        if (unidad instanceof Nucleo) {
            nucleo = (Nucleo) unidad;
        }

        return true;
    }

    public synchronized boolean mover(Componente unidad, int x, int y) {
        if (!(unidad instanceof contratos.Movimiento)) return false;
        if (x < 0 || x >= mat.length
                || y < 0 || y >= mat[x].length) {
            return false;
        }

        int[] pos = unidad.getPos();

        // Valida también la posición original antes de consultar la matriz.
        if (pos == null || pos.length != 2
                || pos[0] < 0 || pos[0] >= mat.length
                || pos[1] < 0 || pos[1] >= mat[pos[0]].length) {
            return false;
        }

        // No basta con que la casilla esté ocupada:
        // debe contener exactamente el objeto que intentamos mover.
        if (mat[pos[0]][pos[1]] != unidad) {
            return false;
        }

        if (mat[x][y] != null) {
            return false;
        }
        // Todas las validaciones ocurren antes de modificar el mapa.
        unidad.setPos(new int[]{x, y});
        mat[pos[0]][pos[1]] = null;
        mat[x][y] = unidad;

        return true;
    }

    public synchronized void retirar(int x, int y) {
        if (x < 0 || x >= mat.length || y < 0 || y >= mat[x].length) {
            System.out.println("Retirar falló: posición fuera del mapa");
            return;
        }
        Componente unidad = mat[x][y];
        if (unidad == null) {
            System.out.println("Retirar falló: casilla vacía");
            return;
        }
        // Comprueba el tipo aunque la referencia nucleo
        // todavía no se haya configurado.
        if (unidad instanceof Nucleo) {
            System.out.println("Retirar falló: no se puede retirar el núcleo");
            return;
        }
        // Comprueba si el componente es Combate:
        // una conversión incorrecta produciría ClassCastException.
        if (unidad instanceof Combate) { 
            ((Combate) unidad).detener();
        }
        // Retirar del mapa conserva el objeto y su historial.
        unidad.setPos(new int[]{-1, -1});
        mat[x][y] = null;
    }

    public synchronized Componente obtener(int x, int y) {
        // Una consulta fuera del mapa devuelve null.
        if (x < 0 || x >= mat.length
                || y < 0 || y >= mat[x].length) {
            return null;
        }

        return mat[x][y];
    }

    public synchronized Componente[][] getMat() {
        // Synchronized evita copiar la matriz a mitad de un movimiento.
        // Se copian las filas, pero los componentes siguen siendo
        // los mismos objetos originales.
        Componente[][] copia = new Componente[mat.length][];

        for (int i = 0; i < mat.length; i++) {
            copia[i] = mat[i].clone();
        }

        return copia;
    }

    public synchronized Nucleo getNucleo() {
        return nucleo;
    }

    public synchronized void setNucleo(Nucleo nucleo) {
        // Solo asigna la referencia; colocar() ocupa la casilla.
        this.nucleo = nucleo;
    }
}
