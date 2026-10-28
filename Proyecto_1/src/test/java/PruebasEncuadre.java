import configuracion.*;
import persistencia.Archivo;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.google.gson.*;

/** Ejecute desde Proyecto_1. Revise todos los ataques y las anclas de cada segundo paso. */
public class PruebasEncuadre {
    static void comprobar(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        Catalogo catalogo=new Archivo().cargarCatalogo("src/main/java/persistencia/catalogo.json");
        JsonArray metadatos=JsonParser.parseString(Files.readString(Path.of("src/test/resources/encuadre_sprites.json"))).getAsJsonArray();
        int ataques=0,pasos=0;
        for(Configuracion c:catalogo.getConfigs()){
            BufferedImage base=ImageIO.read(Path.of(c.getRutaIdle()).toFile());
            BufferedImage ataque=ImageIO.read(Path.of(c.getRutaAtaque()).toFile());
            JsonArray zonas=null;
            for(JsonElement m:metadatos){JsonObject o=m.getAsJsonObject();
                if(c.getTipo().equals(o.get("tipo").getAsString())&&"ataque".equals(o.get("estado").getAsString()))zonas=o.getAsJsonArray("zonasEfectoAtaque");
            }
            boolean barrera="Barrera".equals(c.getTipo());
            comprobar(barrera||zonas!=null,"Faltan las zonas del efecto: "+c.getTipo());
            int diferencias=0,anclaVisible=0;
            for(int y=0;y<64;y++)for(int x=0;x<64;x++){
                boolean efecto=false;
                if(zonas!=null)for(JsonElement z:zonas){JsonArray b=z.getAsJsonArray();
                    int x0=b.get(0).getAsInt(),y0=b.get(1).getAsInt();
                    efecto|=x>=x0&&x<x0+b.get(2).getAsInt()&&y>=y0&&y<y0+b.get(3).getAsInt();
                }
                int antes=base.getRGB(x,y),despues=ataque.getRGB(x,y);
                if(!efecto){
                    comprobar(antes==despues,"Cuerpo desplazado fuera del efecto: "+c.getTipo());
                    if((antes>>>24)>0)anclaVisible++;
                }
                if(antes!=despues)diferencias++;
            }
            if(!barrera){
                comprobar(diferencias>=64,"Ataque demasiado sutil: "+c.getTipo()+" ("+diferencias+" píxeles)");
                comprobar(anclaVisible>=64,"No conserva suficiente cuerpo de referencia: "+c.getTipo());
                BufferedImage miniBase=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB),miniAtaque=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
                var g=miniBase.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(base,0,0,24,24,null);g.dispose();
                g=miniAtaque.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(ataque,0,0,24,24,null);g.dispose();
                int distintos=0;for(int y=0;y<24;y++)for(int x=0;x<24;x++)if(miniBase.getRGB(x,y)!=miniAtaque.getRGB(x,y))distintos++;
                comprobar(distintos>=8,"El efecto desaparece a tamaño de casilla: "+c.getTipo());ataques++;
            }
            if(c.getRutaMovimiento()!=null&&!c.getRutaMovimiento().isBlank()){
                BufferedImage paso=ImageIO.read(Path.of(c.getRutaMovimiento()).toFile());
                JsonArray anclas=null;
                for(JsonElement m:metadatos){JsonObject o=m.getAsJsonObject();
                    if(c.getTipo().equals(o.get("tipo").getAsString())&&"movimiento".equals(o.get("estado").getAsString()))anclas=o.getAsJsonArray("anclasMovimiento");
                }
                comprobar(anclas!=null&&!anclas.isEmpty(),"Falta el ancla corporal: "+c.getTipo());
                for(JsonElement e:anclas){JsonArray b=e.getAsJsonArray();
                    int x0=b.get(0).getAsInt(),y0=b.get(1).getAsInt(),w=b.get(2).getAsInt(),h=b.get(3).getAsInt();
                    for(int y=y0;y<y0+h;y++)for(int x=x0;x<x0+w;x++)
                        comprobar(base.getRGB(x,y)==paso.getRGB(x,y),"Torso/cabeza desplazado al caminar: "+c.getTipo());
                }
                boolean distinto=false;
                for(int y=0;y<64;y++)for(int x=0;x<64;x++)if(base.getRGB(x,y)!=paso.getRGB(x,y))distinto=true;
                comprobar(distinto,"Se perdió el segundo paso: "+c.getTipo());pasos++;
            }
        }
        comprobar(ataques==10&&pasos==6,"Cobertura incompleta del catálogo");
        System.out.println("OK 10 ataques visibles a 24 px, cuerpo fijo fuera del efecto, Barrera estática y 6 pasos anclados.");
    }
}
