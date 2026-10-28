package modelo.registros;

import java.io.Serializable;

public class RegistroDmg implements Serializable {
    private static final long serialVersionUID = 1L;
    private String idComp;
    private String nombreComp;
    private int numGolpes;
    private int dmg;

    public RegistroDmg(String idComp, String nombreComp) {
        this.idComp = idComp;
        this.nombreComp = nombreComp;
    }

    public RegistroDmg() {
    }

    public synchronized void AcumularGolpe(int dmg) {
        this.numGolpes ++;
        this.dmg += dmg;
    }

    public String getIdComp() { return idComp; }

    public void setIdComp(String idComp) {
        this.idComp = idComp;
    }

    public String getNombreComp() { return nombreComp; }

    public void setNombreComp(String nombreComp) {
        this.nombreComp = nombreComp;
    }

    public synchronized int getNumGolpes() { return numGolpes; }

    public void setNumGolpes(int numGolpes) {
        this.numGolpes = numGolpes;
    }

    public synchronized int getDmg() { return dmg; }

    public void setDmg(int dmg) {
        if (dmg < 0) throw new IllegalArgumentException("dmg negativo");
        this.dmg = dmg;
    }
}
