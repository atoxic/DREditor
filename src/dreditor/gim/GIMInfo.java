package dreditor.gim;

import org.json.*;

/**
 * Necessary information about a GIM file
 * @author /a/nonymous scanlations
 */
public class GIMInfo
{
    public static final int BGR565 = 0, 
                            ABGR1555 = 1,
                            ABGR4444 = 2,
                            ABGR8888 = 3,
                            INDEX4 = 4,
                            INDEX8 = 5,
                            INDEX16 = 6,
                            INDEX32 = 7;
    public static final int[] DEPTHS = new int[]{0x10, 0x10, 0x10, 0x20, 0x4, 0x8, 0x10, 0x20};
    
    public int width, height,
            /**
             * If the image is indexed (has palette), then this is the palette format;
             * otherwise, this is the image data format.
             **/
            format, numColors;
    public boolean indexed;
    
    public GIMInfo()
    {
        
    }
    public GIMInfo(int _width, int _height, int _format, int _numColors)
    {
        this(_width, _height, _format, _numColors, true);
    }
    public GIMInfo(int _width, int _height, int _format, int _numColors, boolean _indexed)
    {
        width = _width;
        height = _height;
        format = _format;
        numColors = _numColors;
        indexed = _indexed;
    }
    
    public int depth()
    {
        return(DEPTHS[format]);
    }
    public int dataDepth()
    {
        return(DEPTHS[dataFormat()]);
    }
    /**
     * Calculates the format of the image data section from the number of colors.
     * @return  Format of the image data section.
     */
    public int dataFormat()
    {
        if(!indexed)
            return(format);
        if(numColors <= 1 << 4)
            return(INDEX4);
        else if(numColors <= 1 << 8)
            return(INDEX8);
        else if(numColors <= 1 << 16)
            return(INDEX16);
        else
            return(INDEX32);
    }
    
    @Override
    public String toString()
    {
        return(String.format("GIM:\n  dimensions: %dx%d\n  format: %d\n  numColors: %d\n  indexed?: %b", width, height, format, numColors, indexed));
    }
    
    public String toJSON() throws JSONException
    {
        JSONObject root = new JSONObject();
        root.put("width", width);
        root.put("height", height);
        root.put("format", format);
        root.put("numColors", numColors);
        root.put("indexed", indexed);
        return(root.toString());
    }
    
    public static GIMInfo fromJSON(String jsonString) throws JSONException
    {
        JSONObject root = new JSONObject(jsonString);
        GIMInfo info = new GIMInfo(root.getInt("width"), root.getInt("height"), 
                                root.getInt("format"), root.getInt("numColors"), 
                                root.getBoolean("indexed"));
        return(info);
    }
}
