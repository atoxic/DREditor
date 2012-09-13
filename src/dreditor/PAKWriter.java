package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

/**
 * Tool for writing PAKs to files.
 * @author /a/nonymous scanlations
 */
public class PAKWriter implements AutoCloseable
{
    private SeekableByteChannel out;
    private int numFiles, tocSize, tocPos, dataPos, padTo;
    public PAKWriter(File file, int _numFiles, int _padTo) throws IOException
    {
        out = Files.newByteChannel(file.toPath(), 
                                    StandardOpenOption.WRITE, 
                                    StandardOpenOption.CREATE, 
                                    StandardOpenOption.TRUNCATE_EXISTING);
        numFiles = _numFiles;
        padTo = _padTo;
        tocSize = Constants.round(numFiles * 4 + 8, padTo);
        tocPos = 4;
        dataPos = tocSize;
        
        // Output numFiles
        IOUtils.putInt(out, numFiles);
    }
    
    public int write(File file) throws IOException
    {
        return(write(ByteBuffer.wrap(Files.readAllBytes(file.toPath()))));
    }
    
    public int write(BinPart part) throws IOException
    {
        return(write(part.getBytes()));
    }
    
    public int write(ByteBuffer file) throws IOException
    {
        out.position(tocPos);
        IOUtils.putInt(out, dataPos);
        tocPos += 4;
        
        out.position(dataPos);
        out.write(file);
        
        int oldDataPos = dataPos;
        dataPos = Constants.round(dataPos + file.limit(), padTo);
        return(oldDataPos);
    }
    
    @Override
    public void close() throws IOException
    {
        out.close();
    }
}
