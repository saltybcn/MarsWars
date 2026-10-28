package configuracion;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Credenciales locales. El archivo guarda verificadores, nunca la contraseña ni el código. */
public class Auth {
    private String usuario;
    private String sal;
    private String claveHash;
    private String recuperacionHash;
    private static final Path ARCHIVO = Path.of("src", "main", "java", "persistencia", "admin.json");

    public String getUsuario() { return usuario; }

    public static Auth cargar() throws IOException {
        // Prefiere la ubicación actual; admite el archivo anterior para conservar el acceso existente.
        Path origen = Files.exists(ARCHIVO) ? ARCHIVO : Path.of("datos", "admin.json");
        if (!Files.exists(origen)) return null;
        Auth auth;
        try (java.io.Reader entrada = Files.newBufferedReader(origen)) {
            auth = new com.google.gson.Gson().fromJson(entrada, Auth.class);
            if (auth == null || auth.usuario == null || auth.usuario.isBlank() || auth.sal == null
                    || auth.claveHash == null || auth.recuperacionHash == null
                    || Base64.getDecoder().decode(auth.sal).length != 16
                    || Base64.getDecoder().decode(auth.claveHash).length != 32
                    || Base64.getDecoder().decode(auth.recuperacionHash).length != 32)
                throw new IOException("Credenciales incompletas; no se reemplazaron.");
        } catch (RuntimeException e) { throw new IOException("No se pudo leer admin.json.", e); }
        // Cierra y valida antes de moverlo. Si falla, el archivo anterior permanece disponible.
        if (!origen.equals(ARCHIVO)) {
            Files.createDirectories(ARCHIVO.getParent());
            Files.move(origen, ARCHIVO); // Sin reemplazar unas credenciales nuevas que ya existan.
        }
        return auth;
    }

    public String configurar(String usuario, char[] clave) throws IOException {
        if (usuario == null || usuario.strip().isBlank() || usuario.strip().length() > 40
                || usuario.chars().anyMatch(Character::isISOControl) || clave == null || clave.length < 4)
            throw new IllegalArgumentException("Usuario de 1–40 caracteres y contraseña de al menos 4 caracteres.");
        Auth nueva = new Auth();
        nueva.usuario = usuario.strip();
        byte[] bytes = new byte[16]; new SecureRandom().nextBytes(bytes);
        nueva.sal = Base64.getEncoder().encodeToString(bytes);
        String codigo = UUID.randomUUID().toString();
        nueva.claveHash = nueva.derivar(clave);
        nueva.recuperacionHash = nueva.derivar(codigo.toCharArray());
        Files.createDirectories(ARCHIVO.getParent());
        Path temporal = Files.createTempFile(ARCHIVO.getParent(), "admin-", ".tmp");
        try {
            Files.writeString(temporal, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(nueva));
            Files.move(temporal, ARCHIVO, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporal); }
        this.usuario = nueva.usuario; sal = nueva.sal;
        claveHash = nueva.claveHash; recuperacionHash = nueva.recuperacionHash;
        return codigo; // Devuelve el código para mostrarlo una vez; cada cambio invalida el anterior.
    }

    public boolean validar(String usuario, char[] clave) {
        return Objects.equals(this.usuario, usuario == null ? null : usuario.strip())
                && coincide(clave, claveHash);
    }

    public boolean validar(String usuario, String clave) {
        return validar(usuario, clave == null ? null : clave.toCharArray());
    }

    public boolean recuperar(char[] codigo) { return coincide(codigo, recuperacionHash); }

    private boolean coincide(char[] valor, String hash) {
        if (valor == null || hash == null) return false;
        return MessageDigest.isEqual(Base64.getDecoder().decode(hash), Base64.getDecoder().decode(derivar(valor)));
    }

    private String derivar(char[] valor) {
        PBEKeySpec spec = new PBEKeySpec(valor, Base64.getDecoder().decode(sal), 160000, 256);
        try {
            return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo verificar la credencial", e); }
        finally { spec.clearPassword(); }
    }
}
