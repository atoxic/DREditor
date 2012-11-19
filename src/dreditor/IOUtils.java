package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import jpcsp.filesystems.umdiso.*;
import org.stringtemplate.v4.*;

import dreditor.gui.*;

/**
 *
 * @author /a/nonymous scanlations
 */
public class IOUtils
{
    public static String toString(int[] ints)
    {
        String[] argsString = new String[ints.length];
        for(int i = 0; i < ints.length; i++)
            argsString[i] = String.format("0x%02X", ints[i]);
        return(Arrays.toString(argsString));
    }
    
    public static String toString(byte[] bytes)
    {
        String[] argsString = new String[bytes.length];
        for(int i = 0; i < bytes.length; i++)
            argsString[i] = String.format("0x%02X", bytes[i]);
        return(Arrays.toString(argsString));
    }
    
    public static String toID(int index, String filename)
    {
        StringBuilder sb = new StringBuilder(filename.replaceAll("[.]", "_"));
        if(sb.charAt(0) >= '0' && sb.charAt(0) <= '9')
            sb.insert(0, '_');
        if(index == 1999 || index == 2000 || index == 2166 || index == 2179)
            sb.append('_').append(index);
        return(sb.toString());
    }
    
    public static String escape(String s)
    {
        return(s.replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\"", "\\\\\"")
                .replaceAll("\n", "\\\\n"));
    }
    
    public static String getNULTerminatedString(InputStream is, Charset charset) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        
        while((c = is.read()) != 0)
            baos.write(c);
        
        baos.close();
        return(new String(baos.toByteArray(), charset));
    }
    
    public static void writeToSrc(String file, ByteBuffer bb) throws IOException
    {
        Files.write(new File(DREditor.workspaceSrc, file + ".txt").toPath(), bb.array(), 
                    StandardOpenOption.WRITE, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public static void putInt(SeekableByteChannel b, int i) throws IOException
    {
        putInt(b, i, ByteOrder.LITTLE_ENDIAN);
    }
    
    public static void putInt(SeekableByteChannel b, int i, ByteOrder bo) throws IOException
    {
        ByteBuffer tmp = ByteBuffer.allocate(4);
        tmp.order(bo);
        tmp.putInt(i);
        tmp.flip();
        b.write(tmp);
    }
    
    public static void putInt(OutputStream out, int i) throws IOException
    {
        out.write(i         & 0xFFFF);
        out.write((i >> 8)  & 0xFFFF);
        out.write((i >> 16) & 0xFFFF);
        out.write((i >> 24) & 0xFFFF);
    }
    
    public static int getInt(SeekableByteChannel b) throws IOException
    {
        ByteBuffer tmp = ByteBuffer.allocate(4);
        b.read(tmp);
        tmp.flip();
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        return(tmp.getInt());
    }
    
    public static void putShort(SeekableByteChannel b, int i) throws IOException
    {
        putShort(b, i, ByteOrder.LITTLE_ENDIAN);
    }
    
    public static void putShort(SeekableByteChannel b, int i, ByteOrder bo) throws IOException
    {
        ByteBuffer tmp = ByteBuffer.allocate(2);
        tmp.order(bo);
        tmp.putShort((short)i);
        tmp.flip();
        b.write(tmp);
    }
    
    public static void putShort(OutputStream out, int i) throws IOException
    {
        out.write(i         & 0xFFFF);
        out.write((i >> 8)  & 0xFFFF);
    }
    
    public static int getShort(SeekableByteChannel b) throws IOException
    {
        ByteBuffer tmp = ByteBuffer.allocate(2);
        b.read(tmp);
        tmp.flip();
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        return(tmp.getShort());
    }
    
    // String -> hex byte. May not catch some bugs
    public static byte parseHexByte(String s)
    {
        long parsed = Long.parseLong(s, 16);
        if(parsed < 0 || parsed > 255)
            throw new NumberFormatException("Not a byte");
        return((byte)parsed);
    }
    
    public static byte[] readFully(UmdIsoFile file) throws IOException
    {
        byte[] bytes = new byte[(int)file.length()];
        file.readFully(bytes);
        return(bytes);
    }
    
    // Turns remaining portion of ByteBuffer to byte[]
    public static byte[] toArray(ByteBuffer bb)
    {
        byte[] bytes = new byte[(int)(bb.limit() - bb.position())];
        bb.get(bytes);
        return(bytes);
    }
    
    public static boolean checkDir(File dir)
    {
        return((dir.exists() && dir.isDirectory()) || dir.mkdirs());
    }
    
    public static boolean checkExist(File dir, String filename)
    {
        File f = new File(dir, filename);
        if(!f.exists())
            return(true);
        if(!f.isFile())
            return(false);
        return(GUIUtils.confirm(new ST(GUIUtils.BUNDLE.getString("Dialogue.confirmOverwrite"))
                                .add("file", filename)
                                .add("dir", dir.getAbsolutePath())
                                .render()));
    }
    
    public static long copyAndCheck(byte[] bytes, Path p) throws IOException
    {
        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes))
        {
            return(copyAndCheck(bais, p));
        }
    }
    
    public static long copyAndCheck(InputStream is, Path p) throws IOException
    {
        Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
        try(SeekableByteChannel channel = Files.newByteChannel(p))
        {
            return(getChecksum(Channels.newInputStream(channel)));
        }
    }
    
    // Adapted from http://www.java2s.com/Tutorial/Java/0180__File/ChecksumforanInputStream.htm
    public static long getChecksum(InputStream is) throws IOException
    {        
        try(CheckedInputStream cis = new CheckedInputStream(is, new Adler32()))
        {
            byte[] tempBuf = new byte[128];
            while(cis.read(tempBuf) >= 0){}
            return(cis.getChecksum().getValue());
        } 
    }
    
    public static int[] parseTOC(SeekableByteChannel b) throws IOException, InvalidTOCException
    {
        int[] marks;
        long pos = b.position();
        
        try
        {
            int count = IOUtils.getInt(b);
            long size = ((long)count) * 4 + 4;
            if(count <= 0 || size > b.size())
                throw new InvalidTOCException("Invalid count");
            marks = new int[count + 1];
            marks[count] = (int)b.size();
            for(int i = 0; i < count; i++)
            {
                marks[i] = IOUtils.getInt(b);
                if(marks[i] < size || marks[i] > b.size())
                    throw new InvalidTOCException("Invalid mark");
                if(i > 0 && marks[i] < marks[i - 1])
                    throw new InvalidTOCException("Marks must be nondecreasing");
            }
        }
        finally
        {
            b.position(pos);
        }
        return(marks);
    }
    
    public static int[] parseTOC(ByteBuffer b) throws InvalidTOCException
    {
        long startPos = b.position();
        long limit = b.limit();
        long totalSize = limit - startPos;
        if(limit - startPos < 4)
            throw new InvalidTOCException("No TOC at all");
        
        int[] marks;
        
        try
        {
            b.mark();
            
            int count = b.getInt();
            long size = ((long)count) * 4 + 4;
            if(count <= 0 || size > totalSize)
                throw new InvalidTOCException("Invalid count");
            marks = new int[count + 1];
            marks[count] = -1;
            for(int i = 0; i < count; i++)
            {
                marks[i] = b.getInt();
                if(marks[i] < size || marks[i] > totalSize)
                    throw new InvalidTOCException(String.format("Invalid mark at %04X: %04X; size: %04X", b.position(), marks[i], size));
                if(i > 0 && marks[i] < marks[i - 1])
                    throw new InvalidTOCException("Marks must be nondecreasing");
            }
            int endMark = b.getInt();
            if(endMark <= totalSize && endMark > marks[count - 1])
                marks[count] = endMark;
        }
        finally
        {
            b.reset();
        }
        return(marks);
    }
}
