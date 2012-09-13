package dreditor.gim;

import java.awt.image.*;
import java.nio.*;

import dreditor.Constants;

/**
 * For converting GIMs to BufferedImages
 * @author /a/nonymous scanlations
 */
public class GIMImport
{
    // Struct for parseSectionHeader
    private static class SectionHeader
    {
        int var, size;
        public SectionHeader(int _var, int _size)
        {
            var = _var;
            size = _size;
        }
    };
    
    public static BufferedImage toImage(ByteBuffer bb)
    {
        return(toImage(bb, null));
    }
    /**
     * Converts a GIM to a BufferedImage
     * @param bb    ByteBuffer containing the GIM
     * @param info  Info about the GIM file will be stored here. Can be null.
     * @return 
     */
    public static BufferedImage toImage(ByteBuffer bb, GIMInfo info)
    {
        if(info == null)
            info = new GIMInfo();
        
        // 0x00: Magic
        for(int i = 0; i < Constants.GIM_MAGIC.length; i++)
            if(bb.get() != Constants.GIM_MAGIC[i])
                return(null);
        // 0x0C: null?
        assertNull(bb, 4);
        
        // EOF ADDR
        SectionHeader EOFHeader = decodeSectionHeader(bb, 2);
        EOFHeader.var += 0x10;
        if(EOFHeader.var > bb.limit())
            throw new RuntimeException(String.format("EOF pointer (%d) is greater than b.limit() (%d)", EOFHeader.var, (int)bb.limit()));
        assertEquals(EOFHeader.size, 0x10);
        
        // FILEINFO ADDR
        SectionHeader fileInfoAddrHeader = decodeSectionHeader(bb, 3);
        fileInfoAddrHeader.var += 0x20;
        if(fileInfoAddrHeader.var > bb.limit())
            throw new RuntimeException(String.format("File info pointer (%d) is greater than b.limit() (%d)", fileInfoAddrHeader.var, (int)bb.limit()));
        assertEquals(fileInfoAddrHeader.size, 0x10);
        
        // Peek to see order of palette and image
        bb.mark();
        int header = getShort(bb);
        bb.reset();
        
        int palettePos = 0;
        int imagePos = 0;
        if(header == 0x04)
        {
            imagePos = bb.position();
            
            SectionHeader imageHeader = decodeSectionHeader(bb, 4);
            assertEquals(imageHeader.var, imageHeader.size);
            
            // Make sure that there is a palette
            if(imagePos + imageHeader.size != fileInfoAddrHeader.var)
                palettePos = imagePos + imageHeader.size;
        }
        else
        {
            palettePos = bb.position();
            
            SectionHeader paletteHeader = decodeSectionHeader(bb, 5);
            assertEquals(paletteHeader.var, paletteHeader.size);
            imagePos = palettePos + paletteHeader.size;
        }
        
        int[] palette = null;
        if(palettePos > 0)
        {
            bb.position(palettePos);
            palette = decodePalette(bb, info);
        }
        
        bb.position(imagePos);
        return(decodeImageSection(bb, palette, info));
    }
    
    private static BufferedImage decodeImageSection(ByteBuffer bb, int[] palette, GIMInfo info)
    {
        if(palette != null)
            info.numColors = palette.length;
        
        // IMAGE
        SectionHeader imageHeader = decodeSectionHeader(bb, 4);
        assertEquals(imageHeader.var, imageHeader.size);
        // 0x10: data offset (always 0x30?)
        assertEquals(getShort(bb), 0x30);
        // 0x12: null?
        assertNull(bb, 2);
        // 0x14: format (0x00 = RGBA5650, 0x01 = RGBA5551, 0x02 = RGBA4444, 0x03 = RGBA8888,
        //               0x04 = INDEX4, 0x05 = INDEX8, 0x06 = INDEX16, 0x07 = INDEX32)
        int format = getShort(bb);
        if(format < 4)
            info.format = format;
        else
            info.indexed = true;
        // 0x16: pixel order; always swizzled
        assertEquals(getShort(bb), 1);
        // 0x18: visible width
        info.width = getShort(bb);
        // 0x1A: visible height
        info.height = getShort(bb);
        // 0x1C: color depth (bpp)
        assertEquals(getShort(bb), info.dataDepth());
        // 0x1E [02]: 0x10?
        assertEquals(getShort(bb), 0x10);
        // 0x20 [02]: 0x08?
        assertEquals(getShort(bb), 8);
        // 0x22 [02]: 0x02?
        assertEquals(getShort(bb), 2);
        // 0x24 [04]: null?
        assertNull(bb, 4);
        // 0x28: 0x30?
        assertEquals(bb.getInt(), 0x30);
        // 0x2C: 0x40?
        assertEquals(bb.getInt(), 0x40);
        // 0x30: part size - 0x10
        assertEquals(bb.getInt(), imageHeader.size - 0x10);
        // 0x34: null?
        assertNull(bb, 4);
        // 0x38: 1?
        assertEquals(getShort(bb), 1);
        // 0x3A: 1?
        assertEquals(getShort(bb), 1);
        // 0x3C: 3?
        assertEquals(getShort(bb), 3);
        // 0x3E: 1?
        assertEquals(getShort(bb), 1);
        // 0x40: 0x40?
        assertEquals(bb.getInt(), 0x40);
        // 0x44: null?
        assertNull(bb, 12);
        
        return(decodeSwizzledImageData(bb, imageHeader.size, palette, info));
    }
    
    // Unused because all GIMs in DR are swizzled
    private static BufferedImage decodeImageData(ByteBuffer bb, int size, int[] palette, GIMInfo info)
    {
        int depth = info.dataDepth();
        BufferedImage img = new BufferedImage(info.width, info.height, BufferedImage.TYPE_INT_ARGB);
        PaletteFormat f = PaletteFormat.get(info.dataFormat());
        int lastRead = 0;
        for(int i = 0; i < info.width * info.height; i++)
        {
            int c = 0;
            switch(depth)
            {
                case 0x04:
                {
                    if(i % 2 == 0)
                    {
                        lastRead = bb.get();
                        c = (lastRead & 0xF);
                    }
                    else
                    {
                        c = (lastRead & 0xF0) >> 4;
                    }
                    break;
                }
                case 0x08:
                {
                    c = bb.get() & 0xFF;
                    break;
                }
                case 0x16:
                {
                    c = getShort(bb);
                    break;
                }
                case 0x20:
                {
                    c = bb.getInt();
                    break;
                }
            }
            img.setRGB(i % info.width, i / info.width, f.toARGB(palette, c));
        }
        return(img);
    }
    
    private static BufferedImage decodeSwizzledImageData(ByteBuffer bb, int size, int[] palette, GIMInfo info)
    {
        int depth = info.dataDepth();
        int blockWidth = Constants.GIM_BLOCK_WIDTH * 8 / depth;
        int blockHeight = Constants.GIM_BLOCK_HEIGHT;
        int blockSize = blockWidth * blockHeight;
        
        int widthInBlocks = (int)Math.ceil(1.0 * info.width / blockWidth);
        int heightInBlocks = (int)Math.ceil(1.0 * info.height / blockHeight);
        
        int realWidth = widthInBlocks * blockWidth;
        int realHeight = heightInBlocks * blockHeight;
        
        PaletteFormat f = PaletteFormat.get(info.dataFormat());
        int[] swizzled = new int[realWidth * realHeight];
        int lastRead = 0;
        int length = Math.min((size - 0x50) * 8 / depth, swizzled.length);
        for(int i = 0; i < length; i++)
        {
            int c = 0;
            switch(depth)
            {
                case 0x04:
                {
                    if(i % 2 == 0)
                    {
                        lastRead = bb.get();
                        c = (lastRead & 0xF);
                    }
                    else
                    {
                        c = (lastRead & 0xF0) >> 4;
                    }
                    break;
                }
                case 0x08:
                {
                    c = bb.get() & 0xFF;
                    break;
                }
                case 0x16:
                {
                    c = getShort(bb);
                    break;
                }
                case 0x20:
                {
                    c = bb.getInt();
                    break;
                }
            }
            swizzled[i] = f.toARGB(palette, c);
        }
        
        BufferedImage img = new BufferedImage(info.width, info.height, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < info.height; y++)
        {
            for(int x = 0; x < info.width; x++)
            {
                int blockX = x / blockWidth;
                int blockY = y / blockHeight;

                int blockIndex = blockX + (blockY * widthInBlocks);
                int blockAddr = blockIndex * blockSize;
                
                img.setRGB(x, y, swizzled[blockAddr
                                        + ((y - blockY * blockHeight) * blockWidth) 
                                        + (x - blockX * blockWidth)]);
            }
        }
        return(img);
    }
    
    private static int[] decodePalette(ByteBuffer bb, GIMInfo info)
    {
        // PALETTE
        SectionHeader paletteHeader = decodeSectionHeader(bb, 5);
        assertEquals(paletteHeader.var, paletteHeader.size);
        // 0x10: data offset (always 0x30?)
        assertEquals(getShort(bb), 0x30);
        // 0x12: null?
        assertNull(bb, 2);
        // 0x14: format (0x00 = RGBA5650, 0x01 = RGBA5551, 0x02 = RGBA4444, 0x03 = RGBA8888)
        info.format = getShort(bb);
        // 0x16: null?
        assertNull(bb, 2);
        // 0x18: number of colors
        int colors = getShort(bb);
        if(colors != 0x10 && colors != 0x100)
            throw new RuntimeException(String.format("colors (0x%04d) is not 0x10 or 0x100", colors));
        // 0x1A: ?
        assertEquals(getShort(bb), 0x01);
        assertEquals(getShort(bb), 0x20);
        assertEquals(getShort(bb), 0x10);
        assertEquals(getShort(bb), 0x01);
        assertEquals(getShort(bb), 0x02);
        assertNull(bb, 4);
        // 0x28: 0x30?
        assertEquals(bb.getInt(), 0x30);
        // 0x2C: 0x40?
        assertEquals(bb.getInt(), 0x40);
        // 0x30: part size - 0x10
        assertEquals(bb.getInt(), paletteHeader.size - 0x10);
        // 0x34: null?
        assertNull(bb, 4);
        // 0x38: 2?
        assertEquals(getShort(bb), 2);
        // 0x3A: 1?
        assertEquals(getShort(bb), 1);
        // 0x3C: 3?
        assertEquals(getShort(bb), 3);
        // 0x3E: 1?
        assertEquals(getShort(bb), 1);
        // 0x40: 0x40?
        assertEquals(bb.getInt(), 0x40);
        // 0x44: null?
        assertNull(bb, 12);
        
        PaletteFormat f = PaletteFormat.get(info.format);
        int[] palette = new int[colors];
        for(int i = 0; i < colors; i++)
        {
            if(f instanceof PaletteFormat.ABGR8888)
                palette[i] = f.toARGB(null, bb.getInt());
            else
                palette[i] = f.toARGB(null, getShort(bb));
        }
        
        return(palette);
    }
    
    private static void assertNull(ByteBuffer b, int count)
    {
        for(int i = 0; i < count; i++)
            if(b.get() != 0)
                throw new RuntimeException("assertNull is false");
    }
    
    private static void assertEquals(int val, int exp)
    {
        if(val != exp)
            throw new RuntimeException(String.format("assertVal is false: %d != %d", val, exp));
    }
    
    private static SectionHeader decodeSectionHeader(ByteBuffer bb, int partID)
    {
        // Part header
        // 0x00: Header
        assertEquals(getShort(bb), partID & 0xFFFF);
        // 0x02: null
        assertNull(bb, 2);
        // 0x04: Variable
        int var = bb.getInt();
        // 0x08: Part size
        int size = bb.getInt();
        // 0x0C: Always 0x10?
        assertEquals(bb.getInt(), 0x10);
        
        return(new SectionHeader(var, size));
    }
    
    private static int getShort(ByteBuffer bb)
    {
        return(bb.getShort() & 0xFFFF);
    }
}
