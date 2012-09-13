package dreditor;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Parses "font.pak" in "umdimage2.dat"
 * @author /a/nonymous scanlations
 */
public class BitmapFont
{
    public static BitmapFont FONT1 = null, FONT2 = null;
    
    private class Glyph
    {
        int codepoint, x, y, width, height;
        public Glyph(int _cp, int _x, int _y, int _width, int _height)
        {
            codepoint = _cp & 0xFFFF;
            x = _x & 0xFFFF;
            y = _y & 0xFFFF;
            width = _width & 0xFFFF;
            height = _height & 0xFFFF;
        }
    }
    
    private BufferedImage img;
    private int maxHeight;
    private HashMap<Integer, Glyph> glyphs;
    public BitmapFont(BufferedImage _img, ByteBuffer _data) throws IOException
    {
        img = _img;
        maxHeight = 0;
        glyphs = new HashMap<>();
        
        _data.order(ByteOrder.LITTLE_ENDIAN);
        _data.position(0xC);
        _data.position(_data.getInt());
        while(_data.limit() - _data.position() >= 0x10)
        {
            Glyph g = new Glyph(_data.getShort(), _data.getShort(), _data.getShort(), _data.getShort(), _data.getShort());
            if(g.codepoint == 0)
                return;
            if(g.height > maxHeight)
                maxHeight = g.height;
            glyphs.put(g.codepoint, g);
            _data.position(_data.position() + 6);
        }
    }
    
    public BufferedImage toImage(String s)
    {
        BufferedImage ret = new BufferedImage(480, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ret.createGraphics();
        int dx = 18, dy = 8;
        for(int i = 0; i < s.codePointCount(0, s.length()); i++)
        {
            int cp = s.codePointAt(i);
            if(cp == '\n')
            {
                dx = 18;
                dy += maxHeight;
                continue;
            }
            
            Glyph glyph = glyphs.get(cp);
            // ≡ (U+2261) is the default character
            if(glyph == null)
                if(glyphs.containsKey(0x2261))
                    glyph = glyphs.get(0x2261);
                else
                    continue;
            if(dx + glyph.width > 480 - 18)
            {
                dx = 18;
                dy += maxHeight;
            }
            g.drawImage(img, dx, dy, dx + glyph.width, dy + glyph.height,
                            glyph.x, glyph.y, glyph.x + glyph.width, glyph.y + glyph.height, null);
            dx += glyph.width;
        }
        return(ret);
    }
    
    public static void init(Config config) throws IOException, InvalidTOCException
    {
        try(PAKReader pakIn = new PAKReader(new File(DREditor.workspaceRaw, UmdPAKFile.UMDIMAGE2.name)))
        {
            ByteBuffer bb = DREditor.compileSourceFile(config, pakIn, 169, "font.pak");
            BinPAK fontPak = (BinPAK)BinFactory.tryParseBinPAK(config, bb);
            BufferedImage font1Img, font2Img;
            try(InputStream is = new ByteArrayInputStream(IOUtils.toArray(fontPak.get(0).getBytes())))
            {
                font1Img = ImageIO.read(is);
            }
            try(InputStream is = new ByteArrayInputStream(IOUtils.toArray(fontPak.get(2).getBytes())))
            {
                font2Img = ImageIO.read(is);
            }
            FONT1 = new BitmapFont(font1Img, fontPak.get(1).getBytes());
            FONT2 = new BitmapFont(font2Img, fontPak.get(3).getBytes());
        }
    }
}
