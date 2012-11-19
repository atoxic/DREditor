package dreditor.font;

/**
 *
 * @author Administrator
 */
public class Glyph implements Comparable<Glyph>
{
    public int codepoint, x, y, width, height;
    public Glyph(int _cp, int _x, int _y, int _width, int _height)
    {
        codepoint = _cp & 0xFFFF;
        x = _x & 0xFFFF;
        y = _y & 0xFFFF;
        width = _width & 0xFFFF;
        height = _height & 0xFFFF;
    }
    
    @Override
    public int compareTo(Glyph g)
    {
        if(codepoint < g.codepoint)
            return(-1);
        else if(codepoint > g.codepoint)
            return(1);
        else
            return(0);
    }
}
    