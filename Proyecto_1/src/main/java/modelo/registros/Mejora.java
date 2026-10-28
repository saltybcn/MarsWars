package modelo.registros;

import java.io.Serializable;

public class Mejora implements Serializable {
    private static final long serialVersionUID = 1L;
    private int mision;
    private double porcentajeVida;
    private double porcentajeDmg;

    public Mejora() {
    }

    public Mejora(int mision, double porcentajeVida, double porcentajeDmg) {
        this.mision = mision;
        this.porcentajeVida = porcentajeVida;
        this.porcentajeDmg = porcentajeDmg;
    }

    public int getMision() { return mision; }

    public void setMision(int mision) {
        this.mision = mision;
    }

    public double getPorcentajeVida() { return porcentajeVida; }

    public void setPorcentajeVida(double porcentajeVida) {
        this.porcentajeVida = porcentajeVida;
    }

    public double getPorcentajeDmg() { return porcentajeDmg; }

    public void setPorcentajeDmg(double porcentajeDmg) {
        this.porcentajeDmg = porcentajeDmg;
    }
}
