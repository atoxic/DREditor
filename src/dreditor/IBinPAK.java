package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Provides functionality for PAKs, but is abstract and keeps most of its
 * functions protected so that LINScript can use this as well without
 * exposing this functionality.
 * @author /a/nonymous scanlations
 */
public abstract class IBinPAK implements BinPart
{
    private boolean endMark, padTOC;
    private ArrayList<BinPart> parts;
    private int padding;
    public IBinPAK()
    {
        this(1, true, false);
    }
    public IBinPAK(int _padding)
    {
        this(_padding, true, true);
    }
    public IBinPAK(int _padding, boolean _endMark)
    {
        this(_padding, true, true);
    }
    public IBinPAK(int _padding, boolean _endMark, boolean _padTOC)
    {
        parts = new ArrayList<>();
        padding = _padding;
        endMark = _endMark;
        padTOC = _padTOC;
    }
    
    public final boolean hasEndMark()
    {
        return(endMark);
    }
    public final void setHasEndMark(boolean _endMark)
    {
        endMark = _endMark;
    }
    
    public final boolean padTOC()
    {
        return(padTOC);
    }
    public final void setPadTOC(boolean _padTOC)
    {
        padTOC = _padTOC;
    }    
    
    public final int getPadding()
    {
        return(padding);
    }
    public final void setPadding(int _padding)
    {
        padding = _padding;
    }
    
    protected int size()
    {
        return(parts.size());
    }
    protected BinPart get(int index)
    {
        return(getAll().get(index));
    }
    protected List<BinPart> getAll()
    {
        return(parts);
    }
    
    @Override
    public ByteBuffer getBytes() throws IOException
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ArrayList<Integer> marks = new ArrayList<>();
        
        try(WritableByteChannel dataChan = Channels.newChannel(data))
        {
            marks.add(0);
            
            List<BinPart> localParts = getAll();
            for(int i = 0; i < localParts.size(); i++)
            {
                if(localParts.get(i) == null)
                    continue;
                dataChan.write(localParts.get(i).getBytes());
                if(endMark || i < localParts.size() - 1)
                    pad(data);
                marks.add(data.size());
            }
            if(!endMark)
                marks.remove(marks.size() - 1);
        }
        
        int tocSize = size() * 4 + (endMark ? 8 : 4);
        if(padTOC)
            tocSize = Constants.round(tocSize, padding);
        ByteBuffer bb = ByteBuffer.allocate(tocSize + data.size());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(size());
        for(int mark : marks)
            bb.putInt(mark + tocSize);
        bb.position(tocSize);
        bb.put(data.toByteArray());
        bb.flip();
        return(bb);
    }
    private void pad(ByteArrayOutputStream bos)
    {
        while(bos.size() % padding != 0)
            bos.write(0x00);
    }
    
    protected void add(BinPart b)
    {
        parts.add(b);
    }
    protected BinPart set(int index, BinPart b)
    {
        return(parts.set(index, b));
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BinSection (").append(parts.size()).append(")\n");
        for(BinPart bp : parts)
            sb.append("  ").append(bp.toString().replaceAll("\n", "\n  ")).append("\n");
        return(sb.toString());
    }
}