import configuracion.*;
import modelo.atacantes.Acechador;
import modelo.componentes.Componente;
import java.io.*;
import java.util.*;

/** Los ID nuevos son cortos; las copias y archivos conservan la identidad original. */
public class PruebasIdentificadores {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args) throws Exception {
        Set<String> usados=new HashSet<>();
        for(int i=0;i<5000;i++) {
            var unidad=new Acechador();var config=new Configuracion();
            for(String id:List.of(unidad.getId(),config.getId()))
                comprobar(id.matches("[0-9a-f]{16}")&&usados.add(id),"ID corto inválido o duplicado en la muestra");
        }
        Acechador anterior=new Acechador();String id="aefae02f-13ab-4a17-968c-98f3004e638a";anterior.setId(id);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream salida=new ObjectOutputStream(bytes)){salida.writeObject(anterior);}
        try(ObjectInputStream entrada=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            comprobar(((Componente)entrada.readObject()).getId().equals(id),"Reescribe un ID de una partida anterior");
        }
        System.out.println("OK 10000 ID nuevos de 16 caracteres y conservación de identificadores antiguos.");
    }
}
