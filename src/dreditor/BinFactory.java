package dreditor;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import javax.script.*;

import dreditor.gim.*;
import dreditor.lin.*;

/**
 * Parsing and exporting functions for binary files
 * @author /a/nonymous scanlations
 */
public class BinFactory
{
    public static UmdFileInfo findFile(List<UmdFileInfo> list, int pos)
    {
        for(UmdFileInfo f : list)
            if(f.pos <= pos && f.pos + f.size >= pos)
                return(f);
        return(null);
    }
    
    public static ByteBuffer getFile(UmdPAKFile fileInfo, UmdFileInfo file) throws IOException, InvalidTOCException
    {
        try(PAKReader pakIn = new PAKReader(new File(DREditor.workspaceRaw, fileInfo.name)))
        {
            return(pakIn.read(file.index));
        }
    }
    
    public static List<UmdFileInfo> parseTOC(UmdPAKFile fileInfo) throws IOException
    {
        try(SeekableByteChannel eboot = 
                Files.newByteChannel(DREditor.rawEBOOT.toPath(), StandardOpenOption.READ);
            InputStream ebootIS = Channels.newInputStream(eboot))
        {
            List<UmdFileInfo> list = new ArrayList<>();
            eboot.position(fileInfo.ebootTOCPos);
            
            long tocPtr;
            int namePtr;
            for(int i = 0; i < fileInfo.numFiles; i++)
            {
                // Get file name
                namePtr = IOUtils.getInt(eboot);
                tocPtr = eboot.position();
                eboot.position(namePtr + 0xA0);
                String file = IOUtils.getNULTerminatedString(ebootIS, Constants.UTF8);
                
                // Go back
                eboot.position(tocPtr);
                int pos = IOUtils.getInt(eboot);
                int size = IOUtils.getInt(eboot);
                
                list.add(new UmdFileInfo(i, file, pos, size));
            }
            
            return(list);
        }
    }
    
    public static BinPart parseBinFile(Config config, UmdPAKFile fileInfo, UmdFileInfo file) throws IOException, InvalidTOCException
    {
        ByteBuffer bb = getFile(fileInfo, file);
        bb.limit(file.size);
        return(parseBinPart(config, bb));
    }
    
    private static int gcd(int p, int q)
    {
        if(q == 0)
            return(p);
        return(gcd(q, p % q));
    }
    
    public static IBinPAK tryParseBinPAK(Config config, ByteBuffer b)
    {
        try
        {
            int[] toc = IOUtils.parseTOC(b);
            boolean endMark = (toc[toc.length - 1] != -1);
            int tocSize = toc.length * 4 + (endMark ? 4 : 0);
            boolean padTOC = (toc[0] != tocSize);
            int gcd = 1;
            
            if(padTOC)
                tocSize = toc[0];
            
            // Calculate padding
            if(toc.length > 3 || (toc.length == 3 && endMark))
            {
                int max = endMark ? toc.length : toc.length - 1;
                
                gcd = gcd(toc[0], toc[1]);
                for(int i = 2; i < max; i++)
                    gcd = gcd(gcd, toc[i]);
            }
            else
                gcd = toc[0];
            if(gcd == 0)
                gcd = 1;
            
            // Fill in the last mark
            if(!endMark)
                toc[toc.length - 1] = (int)b.limit();
            
            // Valid TOC
            List<BinPart> parts = new ArrayList<>();
            for(int i = 0; i < toc.length - 1; i++)
            {
                b.position(toc[i]);

                ByteBuffer section = b.slice();
                section.order(ByteOrder.LITTLE_ENDIAN);
                section.limit(toc[i + 1] - toc[i]);

                BinPart bp = parseBinPart(config, section);
                section.position(0);
                
                // False positive of PAKs
                if(bp instanceof BinPAK && ((BinPAK)bp).size() <= 2 && section.limit() <= 128
                        && !(i == 1 && parts.get(0) instanceof InstructionBin))
                    parts.add(new BinBytes(section));
                else
                    parts.add(bp);
            }
            // String-less LIN script
            if(parts.size() == 1 
                    && parts.get(0) instanceof InstructionBin)
            {
                LINScript lin = new LINScript((InstructionBin)parts.get(0));
                b.position(toc[toc.length - 1] - 2);
                lin.setFlags(b.get(), b.get());
                return(lin);
            }
            // LIN script
            else if(parts.size() == 2
                    && parts.get(0) instanceof InstructionBin
                    && parts.get(1) instanceof BinPAK)
            {
                LINScript lin = new LINScript((InstructionBin)parts.get(0),
                                    (BinPAK)parts.get(1));
                b.position(toc[toc.length - 1] - 2);
                lin.setFlags(b.get(), b.get());
                return(lin);
            }
            // Just a PAK
            else
            {
                BinPAK bs = new BinPAK();
                for(BinPart bp : parts)
                {
                    // False positive of instructions
                    if(bp instanceof InstructionBin)
                        bs.add(new BinBytes(((InstructionBin)bp).getSource()));
                    else 
                        bs.add(bp);
                }
                bs.setHasEndMark(endMark);
                bs.setPadding(gcd);
                bs.setPadTOC(padTOC);
                return(bs);
            }
        }
        catch(InvalidTOCException e)
        {
            //e.printStackTrace();
            return(null);
        }
    }
    
    private static BinBytes parseBinBytes(Config config, ByteBuffer b)
    {
        BinBytes bb = new BinBytes(b);
        if(b.limit() - b.position() >= 12)
        {
            byte[] head = new byte[12];
            b.mark();
            b.get(head);
            b.reset();
            if(Arrays.equals(head, Constants.GIM_MAGIC))
                bb.setExt("gim");
            else if(Arrays.equals(head, Constants.GMO_MAGIC))
                bb.setExt("gmo");
            else if(head[0] == 'V' && head[1] == 'A' && head[2] == 'G')
                bb.setExt("vag");
        }
        return(bb);
    }
    
    private static BinPart parseBinPart(Config config, ByteBuffer b)
    {
        IBinPAK pak = tryParseBinPAK(config, b);
        if(pak != null)
            return(pak);
        int size = b.limit() - b.position();
        // 0xF0 is the minimum size of a GIM
        if(config.CONVERT_GIM && size > 0xF0)
        {
            byte[] head = new byte[12];
            b.mark();
            b.get(head);
            b.reset();
            if(Arrays.equals(head, Constants.GIM_MAGIC))
            {
                try
                {
                    return(new GIMBin(b));
                }
                catch(IOException e)
                {
                    // munch
                }
            }
        }
        if(size >= 2)
        {
            byte[] head = new byte[2];
            b.mark();
            b.get(head);
            b.reset();
            
            // Instructions
            if(head[0] == (byte)0x70)
            {
                try
                {
                    return(new InstructionBin(b));
                }
                catch(LINParseException e)
                {
                    // munch
                }
            }
            // UTF-16LE string
            else if(head[0] == (byte)0xFF && head[1] == (byte)0xFE)
            {
                return(new BinString(b));
            }
            // ASCII strings
            else
            {
                int c;
                boolean allValid = true;
                while((c = b.get()) != 0 && b.hasRemaining())
                {
                    if(c < 33 || c > 126)
                        allValid = false;
                }
                if(c != 0 || b.position() < 4)
                    allValid = false;
                else
                    while(b.hasRemaining())
                        if(b.get() != 0)
                            allValid = false;
                b.position(0);
                if(allValid)
                    return(new BinString(b, Constants.ASCII));
            }
        }
        // Just some bytes
        return(parseBinBytes(config, b));
    }
    
    public static BinPart importFromJSFile(Config config, UmdFileInfo file) throws IOException, ScriptException
    {
        return(importFromJSFile(config, file.index, file.file));
    }
    public static BinPart importFromJSFile(Config config, int index, String file) throws IOException, ScriptException
    {
        String ID = IOUtils.toID(index, file);
        byte[] bytes = Files.readAllBytes(new File(DREditor.workspaceSrc, ID + ".js").toPath());
        StringBuilder sb = new StringBuilder();
        sb.append("importPackage(Packages.dreditor);\n");
        sb.append("importPackage(Packages.dreditor.lin);\n");
        sb.append("importPackage(Packages.dreditor.gim);\n");
        sb.append("function loadScript(s){ engine.eval(new java.io.FileReader(new java.io.File(workspaceSrc, s))); }\n");
        sb.append("function load(s){ return(BinFactory.loadBin(\"").append(ID).append("\", s)); }\n");
        sb.append("function loadBin(s){ return(BinFactory.loadBin(\"").append(ID).append("\", s)); }\n");
        sb.append("function loadGIM(s, info){ return(BinFactory.loadGIM(\"").append(ID).append("\", s, info)); }\n\n");
        sb.append(new String(bytes, DREditor.scriptCharset));
        sb.append(String.format("%s();\n", ID));
        return(importFromJS(config, sb.toString()));
    }
    
    public static BinBytes loadBin(String ID, String file) throws IOException
    {
        try
        {
            return(new BinBytes(ID, file));
        }
        catch(IOException ioe)
        {
            return(new BinBytes(DREditor.workspaceOrig, ID, file));
        }
    }
    
    public static GIMBin loadGIM(String ID, String file, GIMInfo info) throws IOException
    {
        try
        {
            return(new GIMBin(ID, file, info));
        }
        catch(IOException ioe)
        {
            return(new GIMBin(DREditor.workspaceOrig, ID, file, info));
        }
    }
    
    private static BinPart importFromJS(Config config, String src) throws ScriptException
    {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        engine.put("workspaceSrc", DREditor.workspaceSrc);
        engine.put("engine", engine);
        engine.put("config", config);
        return((BinPart)engine.eval(src));
    }
    
    public static void exportToJSFile(String ID, BinPart bp) throws IOException
    {
        String js = exportToJS(ID, bp);
        Files.write(new File(DREditor.workspaceSrc, ID + ".js").toPath(),
                    js.getBytes(DREditor.scriptCharset), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private static String exportToJS(String ID, BinPart bp) throws IOException
    {
        try
        {
            List<String> funcs = bp.toJSFunction(ID, null);
            if(funcs == null)
                return(bp.toJSValue(ID, null));
            StringBuilder sb = new StringBuilder();
            for(String func : funcs)
                sb.append(func);
            return(sb.toString());
        }
        catch(LINParseException e)
        {
            return(null);
        }
    }
}
