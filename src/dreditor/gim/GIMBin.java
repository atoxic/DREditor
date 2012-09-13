package dreditor.gim;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.Files;
import javax.imageio.*;

import org.json.JSONException;

import dreditor.*;
import dreditor.lin.*;

/**
 * BIN file that contains a GIM image.
 * @author /a/nonymous scanlations
 */
public class GIMBin implements BinPart
{
    private BufferedImage img;
    private GIMInfo info;
    
    public GIMBin(ByteBuffer _buf) throws IOException
    {
        info = new GIMInfo();
        img = GIMImport.toImage(_buf, info);
    }
    public GIMBin(BufferedImage _img, GIMInfo _info) throws IOException
    {
        info = _info;
        img = _img;
    }
    public GIMBin(String filename) throws IOException, JSONException
    {
        byte[] jsonBytes = Files.readAllBytes(new File(DREditor.workspaceSrc, filename + ".json").toPath());
        info = GIMInfo.fromJSON(new String(jsonBytes, DREditor.scriptCharset));
        img = ImageIO.read(new File(DREditor.workspaceSrc, filename + ".png"));
    }
    public GIMBin(String root, String filename, GIMInfo _info) throws IOException
    {
        info = _info;
        img = ImageIO.read(new File(new File(DREditor.workspaceSrc, root), filename));
    }
    
    public GIMInfo getInfo()
    {
        return(info);
    }
    public BufferedImage getImage()
    {
        return(img);
    }
    
    @Override
    public final String toString()
    {
        return("GIMBin {" + info + "}");
    }
    
    @Override
    public ByteBuffer getBytes() throws IOException
    {
        return(ByteBuffer.wrap(GIMExport.toGIM(img, info)));
    }

    @Override
    public List<String> toJSFunction(String root, String ID) throws IOException, LINParseException
    {
        return(null);
    }

    @Override
    public String toJSValue(String root, String ID) throws IOException
    {
        if(ID == null)
        {
            System.err.println("root: " + ID);
            System.err.println("ID: " + ID);
            throw new RuntimeException("NULL!");
        }
        // Save file
        File rootFile = new File(DREditor.workspaceSrc, root);
        if(!IOUtils.checkDir(rootFile))
            throw new RuntimeException("Cannot make data directory at \"" + rootFile + "\"!");
        File f = new File(rootFile, ID + ".png");
        ImageIO.write(img, "PNG", f);
        
        if(info.indexed)
            return(String.format("loadGIM(\"%s\", new GIMInfo(%d, %d, %d, %d))", 
                                        ID + ".png", info.width, info.height, info.format, info.numColors));
        return(String.format("loadGIM(\"%s\", new GIMInfo(%d, %d, %d, %d, %b))", 
                                    ID + ".png", info.width, info.height, info.format, info.numColors, info.indexed));
    }
}
