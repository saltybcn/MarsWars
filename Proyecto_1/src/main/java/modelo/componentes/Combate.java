package modelo.componentes;

import java.util.ArrayList;
import modelo.defensas.Barrera;
import modelo.juego.Mapa;
import javax.swing.ImageIcon;


/** Comparte detección, intervalos y ejecución; cada tipo redefine sus acciones específicas. */
public abstract class Combate extends Componente implements Runnable {

    private static final long serialVersionUID = 1L;
    private ImageIcon imgAtq;
    private ImageIcon imgMov;
    // false conserva ambos sonidos al leer catálogos y partidas anteriores.
    private boolean ataqueSilenciado;
    private boolean destruccionSilenciada;
    private String sonidoAtacando; // Rutas relativas; vacío o null utiliza el efecto genérico.
    private String sonidoDestruido;
    private volatile boolean mirandoIzquierda;
    private volatile long esperaVisual;
    private volatile boolean pasoAlterno;
    private int lvl;
    private int mision;
    private int costo;
    private int dmg;
    private volatile int velAtq;
    private int alcance;
    private transient volatile long ultimoTick; // Reloj de esta ejecución; no incluye el tiempo pausado.
    private transient Thread hilo; // La unidad crea su hilo; no se guarda en archivos
    private transient volatile boolean activo = true;
    private transient volatile boolean pausado = false;
    private boolean atacaAereos = false;
    // Tiempo restante de cada acción, en nanosegundos; se conserva al pausar.
    private long esperaAtaque;
    protected long esperaMovimiento; // El volador consulta si sigue en tránsito
    protected ArrayList<Componente> detectados = new ArrayList<>(); // Enemigos dentro del alcance

    protected Combate() {
    }

    protected Combate(String nombre, int vida, int dmg, int velAtq, int alcance, int costo) {
        super(nombre, vida);
        setDmg(dmg);
        setVelAtq(velAtq);
        setAlcance(alcance);
        setCosto(costo);
        lvl = 1;
        mision = 1;
    }

    public ArrayList<Componente> getDetectados() {
        Mapa mapa = getMapa();
        if (mapa == null) return new ArrayList<>();
        synchronized (mapa) {
            return new ArrayList<>(detectados); // La copia evita que el ataque modifique la detección
        }
    }

    public void atacar(java.util.List<Componente> objetivos) {
        // El ataque común golpea a uno; los especiales sobrescriben esta versión.
        for (Componente objetivo : objetivos) {
            if (objetivo != null && objetivo.getVida() > 0
                    && (!objetivo.esAereo() || isAtacaAereos())) {
                atacar(objetivo);
                return;
            }
        }
    }

    public void atacar(Componente objetivo) {
        if (objetivo == null || objetivo.getVida() <= 0) {
            return;
        }
        // El permiso antiaéreo filtra el objetivo antes del único punto de aplicación del golpe.
        if (objetivo.esAereo() && !atacaAereos) return;
        aplicarGolpe(objetivo);
    }

    public int aplicarGolpe(Componente objetivo) {
        // Verifica que el objetivo sea valido, luego registra el daño
        if (objetivo != null && objetivo.getVida() > 0) {
            int[] origenVisual = getPos(), destinoVisual = objetivo.getPos();
            // Toma la dirección antes del daño: el golpe puede retirar al objetivo del mapa.
            // Si comparten columna, conserva la orientación anterior.
            if (origenVisual[1] >= 0 && destinoVisual[1] >= 0
                    && origenVisual[1] != destinoVisual[1])
                mirandoIzquierda = destinoVisual[1] < origenVisual[1];
            // La pose necesita al menos dos refrescos de 50 ms para poder verse.
            // No reinicia el pulso por cada golpe: una ráfaga rápida también debe mostrar reposo.
            if (esperaVisual == 0)
                esperaVisual = Math.max(120L, getVelAtq() / 2L) * 1_000_000L;
            pasoAlterno = false; // El ataque siempre vuelve al sprite base, no al segundo paso.
            // El sonido corresponde al ataque, no al daño ni al fotograma que esté visible.
            // Encola antes del daño: una detonación puede causar varias muertes y retirarse aquí mismo.
            // Sonidos agrupa las emisiones de una misma ráfaga para no superponer el WAV por cada objetivo.
            audio.Sonidos.ataque(this);
            int infligido = objetivo.recibirDmg(dmg);
            if (infligido > 0) { // No registrara golpes que sean 0
                getHistorico().RegistrarInfligido(objetivo.getId(), objetivo.getNombre(), infligido);
                objetivo.getHistorico().RegistrarRecibido(getId(), getNombre(), infligido);
                return infligido;
            }
        }
        return 0;
    }

    public synchronized void start() {
        // No crea dos hilos ni inicia una unidad destruida.
        if (getVida() <= 0 || (hilo != null && hilo.isAlive())) return;
        activo = true;
        pausado = false; // Una ejecución nueva comienza sin la pausa anterior
        ultimoTick = System.nanoTime();
        hilo = new Thread(this, "Combate-" + getId());
        hilo.start(); // run se ejecuta en otro hilo, no en quien llamó a start
    }

    @Override
    public void run() {
        synchronized (this) {
            // start es el único punto de entrada para crear y registrar la ejecución.
            if (Thread.currentThread() != hilo) {
                throw new IllegalStateException("Usar start() para ejecutar el combatiente");
            }
        }
        try {
            while (activo && getVida() > 0) {
                synchronized (this) {
                    while (pausado && activo && getVida() > 0) {
                        wait();
                        // El tiempo pausado no consume los intervalos.
                        ultimoTick = System.nanoTime();
                    }
                }
                if (!activo || getVida() <= 0) {
                    break;
                }
                Mapa mapa = getMapa();
                synchronized (mapa == null ? this : mapa) {
                    // La pausa y el guardado usan este mismo bloqueo: no copia un turno a medias.
                    if (activo && !pausado && getVida() > 0) {
                        long ahora = System.nanoTime();
                        long transcurrido = Math.max(0, ahora - ultimoTick);
                        ultimoTick = ahora;
                        if (esperaVisual > 0) {
                            esperaVisual -= transcurrido;
                            // Un valor negativo reserva 100 ms de pose base antes de otro pulso.
                            // Este reloj es visual: no retrasa golpes ni altera el daño.
                            if (esperaVisual <= 0) esperaVisual = -100_000_000L;
                        } else if (esperaVisual < 0) {
                            esperaVisual = Math.min(0, esperaVisual + transcurrido);
                        }
                        esperaAtaque = Math.max(0, esperaAtaque - transcurrido);
                        esperaMovimiento = Math.max(0, esperaMovimiento - transcurrido);
                        actuar();
                    }
                }
                // Una espera corta permite atender ambos intervalos.
                // detener() y pausar() pueden despertarla con notifyAll().
                synchronized (this) {
                    if (activo && !pausado && getVida() > 0) {
                        wait(20);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activo = false;
        }
    }

    protected void actuar() {
        // Validación del turno
        Mapa mapa = getMapa();
        if (mapa == null || !activo || pausado || getVida() <= 0
                || (mapa.getNucleo() != null && mapa.getNucleo().getVida() <= 0)) return;
        synchronized (mapa) {
            // Pudo detenerse mientras esperaba el mapa: no ejecutar una acción pendiente.
            if (!activo || pausado || getVida() <= 0
                    || (mapa.getNucleo() != null && mapa.getNucleo().getVida() <= 0)) return;
            int[] pos = getPos();
            if (mapa.obtener(pos[0], pos[1]) != this) return;
            Componente objetivo = scan(); // Actualiza detectados y el objetivo de persecución

            // Ataque de los enemigos que ya están en alcance
            if (!detectados.isEmpty()) {
                if (esperaAtaque == 0) {
                    atacar(getDetectados()); // Cada override decide cuántos objetivos utiliza
                    esperaAtaque = Math.max(1, getVelAtq()) * 1_000_000L;
                }
                return;
            }

            // Movimiento cuando todavía no hay enemigos dentro del alcance
            if (objetivo != null && esperaMovimiento == 0) {
                avanzarHacia(objetivo);
                int[] nuevaPos = getPos();
                int pasos = Math.max(Math.abs(nuevaPos[0] - pos[0]), Math.abs(nuevaPos[1] - pos[1]));
                if (pasos > 0) {
                    pasoAlterno = !pasoAlterno; // Un cambio por paso real, independiente del refresco de Swing.
                    if (nuevaPos[1] != pos[1]) mirandoIzquierda = nuevaPos[1] < pos[1];
                    esperaMovimiento = pasos * (long) Math.max(1, getVelMov()) * 1_000_000L;
                }
            }
        }
    }

    // Cada tipo decide el radio de búsqueda y reutiliza la detección común.
    protected abstract Componente scan();

    // Cada bando define sus enemigos sin condicionales de tipos en el turno común.
    protected abstract boolean esEnemigo(Componente componente);

    protected int getVelMov() {
        return 0; // Las defensas fijas no se desplazan; las móviles ya tienen su getter
    }

    protected Componente detectar(int radioBusqueda) {
        // Validación del mapa y limpieza de detectados
        Mapa mapa = getMapa();
        if (mapa == null) {
            detectados.clear();
            return null;
        }
        synchronized (mapa) {
            int[] origen = getPos();
            if (mapa.obtener(origen[0], origen[1]) != this) {
                detectados.clear();
                return null;
            }
            detectados.removeIf(componente -> {
                int[] pos = componente.getPos();
                return componente.getVida() <= 0 || !esEnemigo(componente)
                        || (componente.esAereo() && !isAtacaAereos())
                        || mapa.obtener(pos[0], pos[1]) != componente
                        || Math.max(Math.abs(pos[0] - origen[0]), Math.abs(pos[1] - origen[1])) > alcanceParaAtacar();
            });

            // Búsqueda por anillos cuadrados, sin duplicar casillas
            int ultimoRadio = Math.min(radioBusqueda, Math.max(Math.max(origen[0], 24 - origen[0]),
                    Math.max(origen[1], 24 - origen[1])));
            Componente cercano = null;
            for (int radio = 1; radio <= ultimoRadio; radio++) {
                ArrayList<Componente> nuevos = new ArrayList<>();
                Componente elegido = null;
                for (int lado = 0; lado < 4; lado++) {
                    for (int desplazamiento = -radio; desplazamiento < radio; desplazamiento++) {
                        int fila = origen[0] + (lado == 0 ? -radio : lado == 1 ? desplazamiento
                                : lado == 2 ? radio : -desplazamiento);
                        int col = origen[1] + (lado == 0 ? desplazamiento : lado == 1 ? radio
                                : lado == 2 ? -desplazamiento : -radio);
                        Componente candidato = mapa.obtener(fila, col);
                        if (candidato == null || candidato == this
                                || candidato.getVida() <= 0 || !esEnemigo(candidato)
                                || (candidato.esAereo() && !isAtacaAereos())) continue;
                        if (elegido == null || candidato instanceof Nucleo
                                || (elegido instanceof Barrera && !(candidato instanceof Barrera))) elegido = candidato;
                        if (radio <= alcanceParaAtacar() && !detectados.contains(candidato)) {
                            nuevos.add(candidato); // Incluye muros enemigos: también absorben ataques múltiples.
                        }
                    }
                }
                // Solo desempata las nuevas detecciones; no cambia objetivos que siguen en el FIFO.
                nuevos.sort(java.util.Comparator.comparingInt(c -> c instanceof Nucleo ? 0 : c instanceof Barrera ? 2 : 1));
                detectados.addAll(nuevos);
                if (cercano == null) cercano = elegido;
                // Completa todo el alcance para los ataques múltiples; más lejos basta el primer objetivo.
                if (radio >= alcanceParaAtacar() && cercano != null) break;
            }
            return detectados.isEmpty() ? cercano : detectados.get(0);
        }
    }

    protected void avanzarHacia(Componente objetivo) {
        // Validación del movimiento
        Mapa mapa = getMapa();
        if (getVelMov() <= 0 || mapa == null || objetivo == null) return;
        synchronized (mapa) {
            int[] origen = getPos(), destino = objetivo.getPos();
            if (mapa.obtener(origen[0], origen[1]) != this
                    || mapa.obtener(destino[0], destino[1]) != objetivo
                    || objetivo.getVida() <= 0) return;
            Componente[][] celdas = mapa.getMat();
            int filas = celdas.length, columnas = celdas[0].length;

            // Preparación del camino terrestre
            // Una sola búsqueda hacia el objetivo elegido, sin pesos ni segundos intentos.
            int total = filas * columnas;
            int inicio = origen[0] * columnas + origen[1];
            int[] cola = new int[total];
            int[] primerPaso = new int[total];
            java.util.Arrays.fill(primerPaso, -1);
            Componente[] primerBloqueo = new Componente[total];
            cola[0] = inicio;
            primerPaso[inicio] = inicio;
            int cabeza = 0, fin = 1;

            // Búsqueda del camino
            while (cabeza < fin) {
                int actual = cola[cabeza++];
                int fila = actual / columnas, col = actual % columnas;
                int distancia = Math.max(Math.abs(destino[0] - fila), Math.abs(destino[1] - col));
                if (distancia <= alcanceParaAtacar()) {
                    int paso = primerPaso[actual];
                    Componente bloqueo = primerBloqueo[actual];
                    if (bloqueo != null) {
                        int[] posBloqueo = bloqueo.getPos();
                        int distanciaBloqueo = Math.max(Math.abs(posBloqueo[0] - origen[0]),
                                Math.abs(posBloqueo[1] - origen[1]));
                        // El objetivo no cambia, pero primero debe abrir paso.
                        if (distanciaBloqueo <= alcanceParaAtacar()) {
                            if (esperaAtaque == 0) {
                                atacar(new ArrayList<>(java.util.Collections.singletonList(bloqueo)));
                                esperaAtaque = Math.max(1, getVelAtq()) * 1_000_000L;
                            }
                            return;
                        }
                    }
                    mapa.mover(this, paso / columnas, paso % columnas);
                    return;
                }

                // Orden de los vecinos
                ArrayList<int[]> vecinos = new ArrayList<>(8);
                for (int df = -1; df <= 1; df++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (df == 0 && dc == 0) continue;
                        int nf = fila + df, nc = col + dc;
                        if (nf < 0 || nf >= filas || nc < 0 || nc >= columnas) continue;
                        int recta = (destino[0] - nf) * (destino[0] - nf)
                                + (destino[1] - nc) * (destino[1] - nc);
                        vecinos.add(new int[]{nf, nc, recta});
                    }
                }
                // Entre caminos de los mismos pasos, explora antes el más directo.
                vecinos.sort((a, b) -> Integer.compare(a[2], b[2]));

                // Comprobación de aliados, enemigos y esquinas
                for (int[] vecino : vecinos) {
                    int nf = vecino[0], nc = vecino[1];
                    int indice = nf * columnas + nc;
                    if (primerPaso[indice] != -1) continue;
                    Componente ocupante = celdas[nf][nc];
                    boolean enemigo = ocupante != null && ocupante != this
                            && esEnemigo(ocupante)
                            && ocupante.getVida() > 0 && (!ocupante.esAereo() || isAtacaAereos());
                    if (ocupante != null && !enemigo) continue; // Los aliados se rodean
                    if (nf != fila && nc != col
                            && ((celdas[fila][nc] != null && celdas[fila][nc] != this)
                            || (celdas[nf][col] != null && celdas[nf][col] != this))) continue;
                    primerPaso[indice] = actual == inicio ? indice : primerPaso[actual];
                    primerBloqueo[indice] = primerBloqueo[actual] != null
                            ? primerBloqueo[actual] : ocupante;
                    cola[fin++] = indice;
                }
            }
            // Si los aliados cierran todas las salidas, espera al siguiente turno.
        }
    }

    public synchronized void detener() {
        pausado = false;
        activo = false;
        if (getVida() > 0) {
            // Una unidad detenida vuelve a su pose base; pausar conserva su animación.
            // Las detonaciones ya retiradas mantienen aparte su efecto visual de 150 ms.
            pasoAlterno = false;
            esperaVisual = 0;
        }
        if (hilo != null) hilo.interrupt(); // Despierta el hilo incluso si estaba esperando
        notifyAll();
    }

    public synchronized void pausar() { // En caso de necesitar pausar el thread
        pausado = true;
        notifyAll();
    }

    public synchronized void reanudar() {
        ultimoTick = System.nanoTime(); // No descuenta la duración del menú de pausa.
        pausado = false;
        notifyAll();
    }

    public synchronized void mejorar(double porcentajeVida, double porcentajeDmg) {
        if (!Double.isFinite(porcentajeVida)
                || !Double.isFinite(porcentajeDmg)
                || porcentajeVida < 0
                || porcentajeDmg < 0) {
            System.out.println("Porcentaje invalido");
            return;
        }
        if (porcentajeVida == 0 && porcentajeDmg == 0) {
            return; // no registrar porcentajes vacios
        }
        int vidaMaxAnterior = getVidaMax();
        int dmgAnterior = getDmg();
        // Calculo de porcentajes
        int vidaMaxNueva = (int) Math.round(vidaMaxAnterior * (1 + porcentajeVida));
        int dmgNuevo = (int) Math.round(dmgAnterior * (1 + porcentajeDmg));
        int aumentoVida = vidaMaxNueva - vidaMaxAnterior; // La vida actual aumenta un poquitin cuando mejora
        // set de atributos
        // Una defensa destruida puede mejorar sus estadísticas, pero continúa con vida cero.
        if (getVida() > 0) setVida(getVida() + aumentoVida);
        setVidaMax(vidaMaxNueva);
        setDmg(dmgNuevo);
        lvl++; // aumenta el nivel
        // Registrar la mejora
        getHistorico().RegistrarMejora(mision, porcentajeVida, porcentajeDmg);
    }

    public boolean isAtaqueSilenciado() { return ataqueSilenciado; }
    public void setAtaqueSilenciado(boolean ataqueSilenciado) { this.ataqueSilenciado = ataqueSilenciado; }
    public boolean isDestruccionSilenciada() { return destruccionSilenciada; }
    public void setDestruccionSilenciada(boolean destruccionSilenciada) { this.destruccionSilenciada = destruccionSilenciada; }

    public String getSonidoAtacando() { return sonidoAtacando; }
    public void setSonidoAtacando(String sonidoAtacando) { this.sonidoAtacando = sonidoAtacando; }
    public String getSonidoDestruido() { return sonidoDestruido; }
    public void setSonidoDestruido(String sonidoDestruido) { this.sonidoDestruido = sonidoDestruido; }

    public boolean isMirandoIzquierda() { return mirandoIzquierda; }

    public boolean isAtaqueVisible() {
        return esperaVisual > 0; // Se descuenta con el reloj del combate; la pausa lo congela.
    }

    public boolean isPasoAlterno() { return pasoAlterno; }

    protected int alcanceParaAtacar() { return getAlcance(); }

    public ImageIcon getImgAtq() {
        return imgAtq;
    }

    public void setImgAtq(ImageIcon imgAtq) {
        this.imgAtq = imgAtq;
    }

    public ImageIcon getImgMov() {
        return imgMov;
    }

    public void setImgMov(ImageIcon imgMov) {
        this.imgMov = imgMov;
    }

    public int getLvl() {
        return lvl;
    }

    public void setLvl(int lvl) {
        this.lvl = lvl;
    }

    public int getMision() {
        return mision;
    }

    public void setMision(int mision) {
        this.mision = mision;
    }

    public int getCosto() {
        return costo;
    }

    public void setCosto(int costo) {
        if (costo < 0) {
            throw new IllegalArgumentException("costo negativo");
        }
        this.costo = costo;
    }

    public int getDmg() {
        return dmg;
    }

    public void setDmg(int dmg) {
        if (dmg < 0) {
            throw new IllegalArgumentException("dmg negativo");
        }
        this.dmg = dmg;
    }

    public int getVelAtq() {
        return velAtq;
    }

    public void setVelAtq(int velAtq) {
        this.velAtq = velAtq;
    }

    public int getAlcance() {
        return alcance;
    }

    public void setAlcance(int alcance) {
        if (alcance < 0) {
            throw new IllegalArgumentException("alcance negativo");
        }
        this.alcance = alcance;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isAtacaAereos() {
        return atacaAereos;
    }

    public void setAtacaAereos(boolean atacaAereos) {
        this.atacaAereos = atacaAereos;
    }

}
