package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * For strings in PAKs.
 * @author /a/nonymous scanlations
 */
public class BinString implements BinPart
{
    private String string;
    private Charset charset;
    public BinString(ByteBuffer _buf)
    {
        this(_buf, Constants.UTF16LE);
    }
    public BinString(ByteBuffer _buf, Charset _charset)
    {
        charset = _charset;
        string = new String(IOUtils.toArray(_buf), charset);
        while(string.length() > 0 && string.charAt(string.length() - 1) == '\0')
            string = string.substring(0, string.length() - 1);
    }
    public BinString(String _string)
    {
        string = _string;
        charset = Constants.UTF16LE;
    }
    public BinString(String _string, String _charsetName)
    {
        string = _string;
        charset = Charset.forName(_charsetName);
    }
    
    public String getString()
    {
        return(string);
    }
    
    @Override
    public final String toString()
    {
        return("BinString (" + IOUtils.escape(string) + ")");
    }
    
    @Override
    public ByteBuffer getBytes() throws IOException
    {
        String tmp = string;
        if(tmp.length() == 0 || tmp.charAt(tmp.length() - 1) != '\0')
            tmp += '\0';
        return(ByteBuffer.wrap(tmp.getBytes(charset)));
    }

    @Override
    public String toJSValue(String root, String ID)
    {
        if(charset.equals(Constants.UTF16LE))
            return(String.format("\"%s\"", IOUtils.escape(string)));
        return(String.format("\"%s\", \"%s\"", IOUtils.escape(string), charset.name()));
    }
    
    @Override
    public List<String> toJSFunction(String root, String ID)
    {
        return(null);
    }
}
