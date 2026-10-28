package modelo.componentes;

public class Nucleo extends Componente {
    private static final long serialVersionUID = 1L; // Compatibilidad con archivos

    public Nucleo(String nombre, int vida) {
        super(nombre, vida);
        setImgIdle(cargarImagen("nucleo/nucleo.png"));
    }
    public Nucleo() {
        setImgIdle(cargarImagen("nucleo/nucleo.png"));
    }
}
