package dreditor;

import java.io.*;
import java.nio.*;
import java.util.*;

import dreditor.lin.*;

/**
 * 
 * @author /a/nonymous scanlations
 */
public interface BinPart
{
    public String toJSValue(String root, String ID) throws IOException;
    public List<String> toJSFunction(String root, String ID) throws IOException, LINParseException;
    public ByteBuffer getBytes() throws IOException;
}
