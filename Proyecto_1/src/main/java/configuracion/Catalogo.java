package configuracion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Catalogo implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<Configuracion> configs = new ArrayList<Configuracion>();

    public Catalogo() {
    }

    public void agregar(Configuracion config) {
        Objects.requireNonNull(config);
        if (!config.validar()) throw new IllegalArgumentException("Configuración inválida");
        if (consultar(config.getId()) != null) throw new IllegalArgumentException("ID duplicado");
        configs.add(config);
    }

    public void modificar(Configuracion config) {
        Objects.requireNonNull(config);
        if (!config.validar()) throw new IllegalArgumentException("Configuración inválida");
        for (int i = 0; i < configs.size(); i++) {
            if (Objects.equals(configs.get(i).getId(), config.getId())) {
                configs.set(i, config); return;
            }
        }
        throw new IllegalArgumentException("ID inexistente");
    }

    public void alternarActivo(String id) {
        Configuracion config = consultar(id);
        if (config == null) throw new IllegalArgumentException("ID inexistente");
        config.setActivo(!config.isActivo());
    }

    public void eliminar(String id) {
        Objects.requireNonNull(id);
        // Quita la plantilla; las unidades que ya se crearon son objetos independientes.
        if (!configs.removeIf(config -> Objects.equals(config.getId(), id))) {
            throw new IllegalArgumentException("ID inexistente");
        }
    }

    public Configuracion consultar(String id) {
        for (Configuracion config : configs) {
            if (Objects.equals(config.getId(), id)) return config;
        }
        return null;
    }

    public ArrayList<Configuracion> getConfigs() { return new ArrayList<Configuracion>(configs); }
}

