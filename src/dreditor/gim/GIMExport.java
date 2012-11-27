package dreditor.gim;

import java.awt.image.*;
import java.io.*;

import dreditor.Constants;

/**
 * For converting BufferedImages to GIMs
 * @author /a/nonymous scanlations
 */
public class GIMExport
{
    public static byte[] toGIM(BufferedImage prequantized, GIMInfo info) throws IOException
    {
        if(info.width != prequantized.getWidth() || info.height != prequantized.getHeight())
            throw new RuntimeException("Incorrect image dimentions in GIMInfo");
        
        // Quantize image using octtree algorithm
        int[][] pixels = new int[prequantized.getWidth()][prequantized.getHeight()];
        int[] colors = Quantize32.quantizeImage(prequantized, info.numColors, pixels);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // 0x00: Magic
        out.write(Constants.GIM_MAGIC);
        // 0x0C: null?
        out.write(new byte[]{0, 0, 0, 0});
        // EOF ADDR
        writeSectionHeader(out, 0x02, 0xEFBEADDE, 0x10);
        // FILEINFO ADDR
        writeSectionHeader(out, 0x03, 0xEFBEADDE, 0x10);
        // IMAGE SECTION
        out.write(encodeImageSection(pixels, colors, info));
        // PALETTE SECTION
        out.write(encodePalette(colors, info));
        
        // Correct EOF pointers
        byte[] data = out.toByteArray();
        writeInt(data, 0x14, data.length - 0x10);
        writeInt(data, 0x24, data.length - 0x20);
        return(data);
    }
    
    private static byte[] encodeImageSection(int[][] pixels, int[] colors, GIMInfo info) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 0x00: part header
        writeSectionHeader(out, 0x04, 0xEFBEADDE, 0xEFBEADDE);
        // 0x10: data offset (always 0x30?)
        writeShort(out, 0x30);
        // 0x12: null?
        writeShort(out, 0);
        // 0x14: format (0x00 = RGBA5650, 0x01 = RGBA5551, 0x02 = RGBA4444, 0x03 = RGBA8888,
        //               0x04 = INDEX4, 0x05 = INDEX8, 0x06 = INDEX16, 0x07 = INDEX32)
        writeShort(out, info.dataFormat());
        // 0x16: pixel order; all image data in DR is swizzled
        writeShort(out, 1);
        // 0x18: visible width
        writeShort(out, info.width);
        // 0x1A: visible height
        writeShort(out, info.height);
        // 0x1C: color depth (bpp)
        writeShort(out, info.dataDepth());
        // 0x1E [02]: 0x10?
        writeShort(out, 0x10);
        // 0x20 [02]: 0x08?
        writeShort(out, 8);
        // 0x22 [02]: 0x02?
        writeShort(out, 2);
        // 0x24 [04]: null?
        writeInt(out, 0);
        // 0x28: 0x30?
        writeInt(out, 0x30);
        // 0x2C: 0x40?
        writeInt(out, 0x40);
        // 0x30: part size - 0x10
        writeInt(out, 0xEFBEADDE);
        // 0x34: null?
        writeInt(out, 0);
        // 0x38: 1?
        writeShort(out, 1);
        // 0x3A: 1?
        writeShort(out, 1);
        // 0x3C: 3?
        writeShort(out, 3);
        // 0x3E: 1?
        writeShort(out, 1);
        // 0x40: 0x40?
        writeInt(out, 0x40);
        // 0x44: null?
        writeInt(out, 0);
        writeInt(out, 0);
        writeInt(out, 0);
        
        // All image data in DR is swizzled
        writeImageData(out, swizzle(pixels, info), colors, info);
        
        // Correct end-of-section pointers
        byte[] data = out.toByteArray();
        writeInt(data, 0x04, data.length);
        writeInt(data, 0x08, data.length);
        writeInt(data, 0x30, data.length - 0x10);
        return(data);
    }
    
    private static int[] swizzle(int[][] pixels, GIMInfo info) throws IOException
    {
        int blockWidth = Constants.GIM_BLOCK_WIDTH * 8 / info.dataDepth();
        int blockHeight = Constants.GIM_BLOCK_HEIGHT;
        int blockSize = blockWidth * blockHeight;
        
        int widthInBlocks = (int)Math.ceil(1.0 * info.width / blockWidth);
        int heightInBlocks = (int)Math.ceil(1.0 * info.height / blockHeight);
        
        int realWidth = widthInBlocks * blockWidth;
        int realHeight = heightInBlocks * blockHeight;
        
        int[] swizzled = new int[realWidth * realHeight];
        
        for(int y = 0; y < info.height; y++)
        {
            for(int x = 0; x < info.width; x++)
            {
                int blockX = x / blockWidth;
                int blockY = y / blockHeight;

                int blockIndex = blockX + (blockY * widthInBlocks);
                int blockAddr = blockIndex * blockSize;
                
                swizzled[blockAddr + ((y - blockY * blockHeight) * blockWidth) + (x - blockX * blockWidth)] = pixels[x][y];
            }
        }
        return(swizzled);
    }
    
    private static void writeImageData(ByteArrayOutputStream out, int[] pixels1D, int[] colors, GIMInfo info) throws IOException
    {
        int depth = info.dataDepth();
        PaletteFormat f = PaletteFormat.get(info.dataFormat());
        
        int buf = 0;
        for(int i = 0; i < pixels1D.length; i++)
        {
            int c = f.fromARGB(colors, pixels1D[i]);
            switch(depth)
            {
                case 4:
                {
                    if(i % 2 == 0)
                    {
                        buf |= (c & 0xF);
                    }
                    else
                    {
                        buf |= (c & 0xF) << 4;
                        out.write(buf);
                        buf = 0;
                    }
                    break;
                }
                case 8:
                {
                    out.write(c & 0xFF);
                    break;
                }
                case 16:
                {
                    writeShort(out, c);
                    break;
                }
                case 32:
                {
                    writeInt(out, c);
                    break;
                }
            }
        }
    }
    
    private static byte[] encodePalette(int[] colors, GIMInfo info) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // PALETTE
        writeSectionHeader(out, 5, 0xEFBEADDE, 0xEFBEADDE);
        // 0x10: data offset (always 0x30?)
        writeShort(out, 0x30);
        // 0x12: null?
        writeShort(out, 0);
        // 0x14: format (0x00 = RGBA5650, 0x01 = RGBA5551, 0x02 = RGBA4444, 0x03 = RGBA8888)
        writeShort(out, info.format);
        // 0x16: null?
        writeShort(out, 0);
        // 0x18: number of colors
        writeShort(out, info.numColors);
        // 0x1A: ?
        writeShort(out, 0x01);
        writeShort(out, 0x20);
        writeShort(out, 0x10);
        writeShort(out, 0x01);
        writeShort(out, 0x02);
        writeInt(out, 0);
        // 0x28: 0x30?
        writeInt(out, 0x30);
        // 0x2C: 0x40?
        writeInt(out, 0x40);
        // 0x30: part size - 0x10
        writeInt(out, 0xEFBEADED);
        // 0x34: null?
        writeInt(out, 0);
        // 0x38: 2?
        writeShort(out, 2);
        // 0x3A: 1?
        writeShort(out, 1);
        // 0x3C: 3?
        writeShort(out, 3);
        // 0x3E: 1?
        writeShort(out, 1);
        // 0x40: 0x40?
        writeInt(out, 0x40);
        // 0x44: null?
        writeInt(out, 0);
        writeInt(out, 0);
        writeInt(out, 0);
        
        PaletteFormat f = PaletteFormat.get(info.format);
        for(int i = 0; i < colors.length; i++)
        {
            if(f instanceof PaletteFormat.ABGR8888)
                writeInt(out, f.fromARGB(colors, i));
            else
                writeShort(out, f.fromARGB(colors, i));
        }
        // Need to pad palette
        for(int i = colors.length; i < info.numColors; i++)
        {
            if(f instanceof PaletteFormat.ABGR8888)
                writeInt(out, 0);
            else
                writeShort(out, 0);
        }
        
        // Correct end-of-section pointers
        byte[] data = out.toByteArray();
        writeInt(data, 0x04, data.length);
        writeInt(data, 0x08, data.length);
        writeInt(data, 0x30, data.length - 0x10);
        return(data);
    }
    
    private static void writeShort(OutputStream out, int val) throws IOException
    {
        out.write(val & 0xFF);
        out.write((val >> 8) & 0xFF);
    }
    
    private static void writeInt(OutputStream out, int val) throws IOException
    {
        out.write(val & 0xFF);
        out.write((val >> 8) & 0xFF);
        out.write((val >> 16) & 0xFF);
        out.write((val >> 24) & 0xFF);
    }
    
    private static void writeInt(byte[] bytes, int index, int val) throws IOException
    {
        bytes[index]     = (byte)(val & 0xFF);
        bytes[index + 1] = (byte)((val >> 8) & 0xFF);
        bytes[index + 2] = (byte)((val >> 16) & 0xFF);
        bytes[index + 3] = (byte)((val >> 24) & 0xFF);
    }
    
    private static void writeSectionHeader(OutputStream out, int partID, int var, int size) throws IOException
    {
        writeShort(out, partID);
        writeShort(out, 0);
        writeInt(out, var);
        writeInt(out, size);
        writeInt(out, 0x10);
    }
}