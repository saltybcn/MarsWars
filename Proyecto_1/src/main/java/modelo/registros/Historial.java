package modelo.registros;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Historial implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<RegistroDmg> recibido = new ArrayList<>();
    private ArrayList<RegistroDmg> infligido = new ArrayList<>();
    private ArrayList<Mejora> mejoras = new ArrayList<>();

    public Historial() {
    }

    public synchronized void RegistrarRecibido(String idComp, String nombreComp, int dmg) {
        for (RegistroDmg registro : recibido) { // Revisa que no exista un registro con el mismo ID
            if (Objects.equals(registro.getIdComp(), idComp)) {
                registro.AcumularGolpe(dmg); return; // si existe, registra un golpe mas
            }
        }
        RegistroDmg registro = new RegistroDmg(idComp, nombreComp);
        registro.AcumularGolpe(dmg);
        recibido.add(registro);
    }

    public synchronized void RegistrarInfligido(String idComp, String nombreComp, int dmg) {
        for (RegistroDmg registro : infligido) {
            if (Objects.equals(registro.getIdComp(), idComp)) {
                registro.AcumularGolpe(dmg); return;
            }
        }
        RegistroDmg registro = new RegistroDmg(idComp, nombreComp);
        registro.AcumularGolpe(dmg);
        infligido.add(registro);
    }

    public synchronized void RegistrarMejora(int mision, double porcentajeVida, double porcentajeDmg) {
        mejoras.add(new Mejora(mision, porcentajeVida, porcentajeDmg));
    }

    public synchronized ArrayList<RegistroDmg> getRecibido() { return new ArrayList<>(recibido); }

    public synchronized ArrayList<RegistroDmg> getInfligido() { return new ArrayList<>(infligido); }

    public synchronized ArrayList<Mejora> getMejoras() { return new ArrayList<>(mejoras); }
}
