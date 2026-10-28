package configuracion;

import modelo.componentes.Combate;
import modelo.atacantes.*;
import modelo.defensas.*;
import java.io.Serializable;
import java.util.Arrays;

public class Configuracion implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String nombre;
    private String tipo;
    private int vida;
    private int dmg;
    private Double intervaloMovimiento; // Segundos por paso; null conserva los 0.5 s de configuraciones anteriores.
    private double frecuenciaAtaque; // Segundos entre ataques; crearUnidad convierte a ms
    private int alcance;
    private int costoCapacidad;
    private int misionMinima;
    private boolean activo;
    private String rutaIdle;
    private String rutaAtaque;
    private String rutaMovimiento;
    // false conserva ambos sonidos al leer catálogos y partidas anteriores.
    private boolean ataqueSilenciado;
    private boolean destruccionSilenciada;
    private String sonidoAtacando;
    private String sonidoDestruido;
    private int maxObjetivos;
    private boolean atacaAereos;

    public Configuracion() {
        id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16); // También las plantillas creadas con setters necesitan ID
        activo = true;
    }

    public Configuracion(String nombre, String tipo, int vida, int dmg,
            double frecuenciaAtaque, int alcance, int costoCapacidad, int misionMinima) {
        this.id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.nombre = nombre;
        this.tipo = tipo;
        this.vida = vida;
        this.dmg = dmg;
        this.frecuenciaAtaque = frecuenciaAtaque;
        this.alcance = alcance;
        this.costoCapacidad = costoCapacidad;
        this.misionMinima = misionMinima;
        this.activo = true;
    }

    public boolean validar() {
        // Comprobaciones basicas
        String[] tiposValidos = {"Contacto","Acechador","AlcanceMedio","Escupidor","AtaqueMultiple",
            "Enjambre","Impacto","Demoledor","Dron","Volador","Barrera"}; // Tipos concretos admitidos por el catálogo. 
        boolean movil = switch (tipo == null ? "" : tipo) {
            case "Acechador", "Escupidor", "Enjambre", "Demoledor", "Volador", "Dron" -> true;
            default -> false;
        };
        return !((movil && (!Double.isFinite(getIntervaloMovimiento()) || getIntervaloMovimiento() <= 0
                || getIntervaloMovimiento() * 1000.0 > Integer.MAX_VALUE)) || id == null || id.isBlank()
                || nombre == null || nombre.isBlank() || tipo == null || tipo.isBlank()
                || vida <= 0 || dmg < 0 || !Double.isFinite(frecuenciaAtaque)
                || frecuenciaAtaque < 0 || frecuenciaAtaque * 1000.0 > Integer.MAX_VALUE
                || (!"Barrera".equals(tipo) && frecuenciaAtaque == 0)
                || alcance < 0 || costoCapacidad <= 0 || misionMinima < 1
                || !Arrays.asList(tiposValidos).contains(tipo) // Rechaza tipos que no están en la lista
                || (("Contacto".equals(tipo) || "Acechador".equals(tipo)) && alcance != 1)
                || (("AtaqueMultiple".equals(tipo) || "Enjambre".equals(tipo)) && maxObjetivos <= 0)
                || ("Barrera".equals(tipo) && (dmg != 0 || alcance != 0)));
    }

    public Combate crearUnidad() {
        // Rechaza una configuración inválida antes de construir la unidad.
        if (!validar()) {
            throw new IllegalArgumentException("Configuración inválida");
        }

        // El administrador guarda segundos entre ataques, no ataques por segundo.
        int velAtq = (int) Math.max(1, Math.round(frecuenciaAtaque * 1000.0));

        int velMov = (int) Math.max(1, Math.round(getIntervaloMovimiento() * 1000.0));

        Combate unidad = switch (tipo) {
            // Defensas
            case "Contacto" -> new Contacto(
                    nombre, vida, dmg, velAtq, costoCapacidad);

            case "AlcanceMedio" -> new AlcanceMedio(
                    nombre, vida, dmg, velAtq, alcance, costoCapacidad);

            case "AtaqueMultiple" -> new AtaqueMultiple(
                    nombre, vida, dmg, velAtq, alcance,
                    costoCapacidad, maxObjetivos);

            case "Impacto" -> new Impacto(
                    nombre, vida, dmg, velAtq, alcance, costoCapacidad);

            case "Dron" -> new Dron(
                    nombre, vida, dmg, velAtq, alcance,
                    costoCapacidad, velMov);

            case "Barrera" -> new Barrera(
                    nombre, vida, costoCapacidad);

            // Atacantes
            case "Acechador" -> new Acechador(
                    nombre, vida, dmg, velAtq, costoCapacidad, velMov);

            case "Escupidor" -> new Escupidor(
                    nombre, vida, dmg, velAtq, alcance, costoCapacidad);

            case "Enjambre" -> new Enjambre(
                    nombre, vida, dmg, velAtq, alcance,
                    costoCapacidad, maxObjetivos);

            case "Demoledor" -> new Demoledor(
                    nombre, vida, dmg, velAtq, costoCapacidad, alcance);

            case "Volador" -> new Volador(
                    nombre, vida, dmg, velAtq, alcance, costoCapacidad);

            default -> throw new IllegalArgumentException(
                    "Tipo de unidad desconocido: " + tipo);
        };
        // Los seis tipos móviles reciben el intervalo configurado, también los que usan constructores comunes.
        if (unidad instanceof Atacante atacante) atacante.setVelMov(velMov);
        else if (unidad instanceof Dron dron) dron.setVelMov(velMov);
        // Componente ya genera un ID nuevo; no se copia el ID de la plantilla.
        unidad.setAtacaAereos(atacaAereos);
        // Reutiliza la misma miniatura cuando las tres acciones comparten archivo.
        unidad.setImgIdle(modelo.componentes.Componente.cargarImagen(rutaIdle));
        unidad.setImgAtq(java.util.Objects.equals(rutaAtaque, rutaIdle)
                ? unidad.getImgIdle() : modelo.componentes.Componente.cargarImagen(rutaAtaque));
        unidad.setImgMov(java.util.Objects.equals(rutaMovimiento, rutaIdle)
                ? unidad.getImgIdle() : java.util.Objects.equals(rutaMovimiento, rutaAtaque)
                ? unidad.getImgAtq() : modelo.componentes.Componente.cargarImagen(rutaMovimiento));
        unidad.setAtaqueSilenciado(ataqueSilenciado);
        unidad.setDestruccionSilenciada(destruccionSilenciada);
        unidad.setSonidoAtacando(sonidoAtacando);
        unidad.setSonidoDestruido(sonidoDestruido);
        return unidad;
    }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() { return tipo; }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getVida() { return vida; }

    public void setVida(int vida) {
        if (vida < 0) throw new IllegalArgumentException("vida negativo");
        this.vida = vida;
    }

    public int getDmg() { return dmg; }

    public void setDmg(int dmg) {
        if (dmg < 0) throw new IllegalArgumentException("dmg negativo");
        this.dmg = dmg;
    }

    public double getIntervaloMovimiento() { return intervaloMovimiento == null ? 0.5 : intervaloMovimiento; }
    public void setIntervaloMovimiento(double intervaloMovimiento) { this.intervaloMovimiento = intervaloMovimiento; }

    public double getFrecuenciaAtaque() { return frecuenciaAtaque; }

    public void setFrecuenciaAtaque(double frecuenciaAtaque) {
        this.frecuenciaAtaque = frecuenciaAtaque;
    }

    public int getAlcance() { return alcance; }

    public void setAlcance(int alcance) {
        if (alcance < 0) throw new IllegalArgumentException("alcance negativo");
        this.alcance = alcance;
    }

    public int getCostoCapacidad() { return costoCapacidad; }

    public void setCostoCapacidad(int costoCapacidad) {
        this.costoCapacidad = costoCapacidad;
    }

    public int getMisionMinima() { return misionMinima; }

    public void setMisionMinima(int misionMinima) {
        this.misionMinima = misionMinima;
    }

    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getRutaIdle() { return rutaIdle; }

    public void setRutaIdle(String rutaIdle) {
        this.rutaIdle = rutaIdle;
    }

    public String getRutaAtaque() { return rutaAtaque; }

    public void setRutaAtaque(String rutaAtaque) {
        this.rutaAtaque = rutaAtaque;
    }

    public String getRutaMovimiento() { return rutaMovimiento; }

    public void setRutaMovimiento(String rutaMovimiento) {
        this.rutaMovimiento = rutaMovimiento;
    }

    public boolean isAtaqueSilenciado() { return ataqueSilenciado; }
    public void setAtaqueSilenciado(boolean ataqueSilenciado) { this.ataqueSilenciado = ataqueSilenciado; }
    public boolean isDestruccionSilenciada() { return destruccionSilenciada; }
    public void setDestruccionSilenciada(boolean destruccionSilenciada) { this.destruccionSilenciada = destruccionSilenciada; }

    public String getSonidoAtacando() { return sonidoAtacando; }
    public void setSonidoAtacando(String sonidoAtacando) { this.sonidoAtacando = sonidoAtacando; }
    public String getSonidoDestruido() { return sonidoDestruido; }
    public void setSonidoDestruido(String sonidoDestruido) { this.sonidoDestruido = sonidoDestruido; }

    public int getMaxObjetivos() { return maxObjetivos; }

    public void setMaxObjetivos(int maxObjetivos) {
        if (maxObjetivos < 0) throw new IllegalArgumentException("maxObjetivos negativo");
        this.maxObjetivos = maxObjetivos;
    }

    public boolean isAtacaAereos() { return atacaAereos; }

    public void setAtacaAereos(boolean atacaAereos) {
        this.atacaAereos = atacaAereos;
    }
}

