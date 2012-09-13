package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

/**
 * Tool for reading PAKs from files.
 * @author /a/nonymous scanlations
 */
public class PAKReader implements AutoCloseable
{
    private SeekableByteChannel in;
    private int[] toc;
    public PAKReader(File file) throws IOException, InvalidTOCException
    {
        in = Files.newByteChannel(file.toPath(), StandardOpenOption.READ);
        toc = IOUtils.parseTOC(in);        
    }
    
    public ByteBuffer read(int file) throws IOException
    {
        ByteBuffer bb = ByteBuffer.allocate(toc[file + 1] - toc[file]);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        in.position(toc[file]);
        in.read(bb);
        bb.flip();
        return(bb);
    }
    
    @Override
    public void close() throws IOException
    {
        in.close();
    }
}
