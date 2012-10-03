package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Simple BinPart for files that can't be parsed. Supports extensions.
 * @author /a/nonymous scanlations
 */
public class BinBytes implements BinPart
{
    private ByteBuffer bytes;
    private String ext;
    
    public BinBytes(byte[] _bytes)
    {
        setBytes(_bytes);
        ext = "bin";
    }
    public BinBytes(ByteBuffer _bytes)
    {
        setBytes(_bytes);
        ext = "bin";
    }
    public BinBytes(String filename) throws IOException
    {
        this(Files.readAllBytes(new File(DREditor.workspaceSrc, filename).toPath()));
    }
    public BinBytes(String root, String filename) throws IOException
    {
        this(DREditor.workspaceSrc, root, filename);
    }
    public BinBytes(File dir, String root, String filename) throws IOException
    {
        this(Files.readAllBytes(new File(new File(dir, root), filename).toPath()));
    }
    public static BinBytes create(int[] _bytes)
    {
        byte[] bytes = new byte[_bytes.length];
        for(int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)_bytes[i];
        return(new BinBytes(bytes));
    }
    
    public void setExt(String _ext)
    {
        ext = _ext;
    }
    
    @Override
    public ByteBuffer getBytes() throws IOException
    {
        return(bytes.duplicate());
    }
    public final void setBytes(byte[] _bytes)
    {
        bytes = ByteBuffer.wrap(_bytes);
    }
    public final void setBytes(ByteBuffer _bytes)
    {
        bytes = _bytes.duplicate();
    }
    
    @Override
    public final String toString()
    {
        return("BinBytes (" + bytes.limit() + ")");
    }

    @Override
    public List<String> toJSFunction(String root, String ID) throws IOException
    {
        return(null);
    }
    
    @Override
    public String toJSValue(String root, String ID) throws IOException
    {
        return(toJS(root, ID, ext, IOUtils.toArray(bytes)));
    }
    
    public static String toJS(String root, String ID, String ext, byte[] bytes) throws IOException
    {
        if(bytes.length <= 128)
            return(IOUtils.toString(bytes));
        
        // Save file
        File rootFile = new File(DREditor.workspaceSrc, root);
        if(!IOUtils.checkDir(rootFile))
            throw new RuntimeException("Cannot make data directory at \"" + rootFile + "\"!");
        File f = new File(rootFile, ID + "." + ext);
        Files.write(f.toPath(), 
                    bytes,
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
        
        return(String.format("loadBin(\"%s\")", ID + "." + ext));
    }
}
