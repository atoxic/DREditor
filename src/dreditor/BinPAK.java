package dreditor;

import java.io.*;
import java.util.*;

import dreditor.lin.*;

/**
 * Extension of IBinPAK that makes most functions public.
 * @author /a/nonymous scanlations
 */
public class BinPAK extends IBinPAK
{
    public BinPAK()
    {
        super(1);
    }
    public BinPAK(int _padding)
    {
        super(_padding);
    }
    public BinPAK(int _padding, boolean _endMark)
    {
        super(_padding, _endMark);
    }
    public BinPAK(int _padding, boolean _endMark, boolean _padTOC)
    {
        super(_padding, _endMark, _padTOC);
    }
    
    @Override
    public int size()
    {
        return(super.size());
    }
    
    @Override
    public BinPart get(int index)
    {
        return(super.get(index));
    }
    
    @Override
    public List<BinPart> getAll()
    {
        return(super.getAll());
    }
    
    @Override
    public void add(BinPart b)
    {
        super.add(b);
    }
    
    @Override
    public BinPart set(int index, BinPart b)
    {
        return(super.set(index, b));
    }
    
    public void add(int[] bytes)
    {
        super.add(BinBytes.create(bytes));
    }
    
    public void add(String string)
    {
        super.add(new BinString(string));
    }
    
    public void add(String string, String charset)
    {
        super.add(new BinString(string, charset));
    }
    
    @Override
    public String toJSValue(String root, String ID)
    {
        return(null);
    }

    @Override
    public List<String> toJSFunction(String root, String ID) throws IOException, LINParseException
    {
        List<String> funcs = new ArrayList<>();
        String fullID = ID == null ? root : root + "_" + ID;
        StringBuilder sb = new StringBuilder(String.format("function %s()\n{\n\tvar p = new BinPAK(%d, %b, %b);\n", fullID, getPadding(), hasEndMark(), padTOC()));
        for(int i = 0; i < size(); i++)
        {
            String subID = ID == null ? String.format("%03d", i) : String.format("%s_%03d", ID, i);
            List<String> sublist = get(i).toJSFunction(root, subID);
            if(sublist == null)
            {
                sb.append(String.format("\tp.add(%s);\n", get(i).toJSValue(root, subID)));
            }
            else
            {
                sb.append(String.format("\tp.add(%s());\n", root + "_" + subID));
                funcs.addAll(sublist);
            }
        }
        sb.append("\treturn(p);\n}\n");
        funcs.add(0, sb.toString());
        return(funcs);
    }
}
