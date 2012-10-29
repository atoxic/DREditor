package dreditor;

import java.nio.charset.*;
import com.google.common.collect.*;

/**
 * Place to put constants.
 * @author /a/nonymous scanlations
 */
public class Constants
{
    // Charsets
    public final static Charset UTF8 = Charset.forName("UTF-8");
    public final static Charset UTF16LE = Charset.forName("UnicodeLittle");
    public final static Charset ASCII = Charset.forName("US-ASCII");
    
    // Magic numbers for some files
    public static final byte[] GIM_MAGIC = { 0x4D, 0x49, 0x47, 0x2E, 0x30, 0x30, 0x2E, 0x31, 0x50, 0x53, 0x50, 0x00 },
                                GMO_MAGIC = { 0x4F, 0x4D, 0x47, 0x2E, 0x30, 0x30, 0x2E, 0x31, 0x50, 0x53, 0x50, 0x00 };
    
    // For GIM swizzling
    public static final int GIM_BLOCK_WIDTH = 16, GIM_BLOCK_HEIGHT = 8;
    
    // Character ID
    public static final BiMap<String, Integer> CHARACTERS = HashBiMap.create();
    public static final BiMap<Integer, String> CHARACTERS_INV = CHARACTERS.inverse();
    
    static
    {
        CHARACTERS.put("Makoto Naegi", 0x00);
        CHARACTERS.put("Kiyotaka Ishimaru", 0x01);
        CHARACTERS.put("Byakuya Togami", 0x02);
        CHARACTERS.put("Mondo Oowada", 0x03);
        CHARACTERS.put("Reon Kuwata", 0x04);
        CHARACTERS.put("Hifumi Yamada", 0x05);
        CHARACTERS.put("Yasuhiro Hagakure", 0x06);
        CHARACTERS.put("Sayaka Maizono", 0x07);
        CHARACTERS.put("Kyouko Kirigiri", 0x08);
        CHARACTERS.put("Aoi Asahina", 0x09);
        CHARACTERS.put("Touko Fukawa", 0x0A);
        CHARACTERS.put("Sakura Oogami", 0x0B);
        CHARACTERS.put("Celestia Rudenberk", 0x0C);
        CHARACTERS.put("Junko Enoshima (fake)", 0x0D);
        CHARACTERS.put("Chihiro Fujisaki", 0x0E);
        CHARACTERS.put("Monokuma", 0x0F);
        CHARACTERS.put("Junko Enoshima", 0x10);
        CHARACTERS.put("Alter Ego", 0x11);
        CHARACTERS.put("Genocider Shou", 0x12);
        CHARACTERS.put("Principal", 0x13);
        CHARACTERS.put("Makoto's Mom", 0x14);
        CHARACTERS.put("Makoto's Dad", 0x15);
        CHARACTERS.put("Makoto's Sister", 0x16);
        CHARACTERS.put("Kiyotaka Ishimaru and Mondo Oowada", 0x18);
        CHARACTERS.put("Daia Oowada", 0x19);
        CHARACTERS.put("[Current Sprite]", 0x1C);
        CHARACTERS.put("???", 0x1E);
        CHARACTERS.put("[No Name]", 0x1F);
    }
    
    // Round n up to the nearest multiple of m
    public static int round(int n, int m)
    {
        return((int)Math.ceil(n * 1.0 / m) * m);
    }
}
