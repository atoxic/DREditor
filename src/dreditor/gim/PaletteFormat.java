package dreditor.gim;

/**
 * Classes and functions for converting to and from GIM palette formats
 * @author /a/nonymous scanlations
 */
public abstract class PaletteFormat
{
    public abstract int toARGB(int[] palette, int c);
    public abstract int fromARGB(int[] palette, int c);
    
    public static PaletteFormat get(int format)
    {
        switch(format)
        {
            case GIMInfo.BGR565:    return(new BGR565());
            case GIMInfo.ABGR1555:  return(new ABGR1555());
            case GIMInfo.ABGR4444:  return(new ABGR4444());
            case GIMInfo.ABGR8888:  return(new ABGR8888());
            case GIMInfo.INDEX4:
            case GIMInfo.INDEX8:
            case GIMInfo.INDEX16:
            case GIMInfo.INDEX32:   return(new Index());
        }
        return(null);
    }
    
    /**
     * BBBB BGGG GGGR RRRR
     **/
    public static class BGR565 extends PaletteFormat
    {
        @Override
        public int toARGB(int[] palette, int c)
        {
            int r = (c & 0x001F) << 3;
            int g = (c & 0x07E0) >> 3;
            int b = (c & 0xF800) >> 8;
            return(0xFF000000 | (r << 16) | (g << 8) | b);
        }
        @Override
        public int fromARGB(int[] palette, int c)
        {
            int p = palette[c];
            return((((p & 0x000000FF) >> 3)  << 11)
                |  (((p & 0x0000FF00) >> 10) << 5) 
                |   ((p & 0x00FF0000) >> 19));
        }
    }
    
    /**
     * ABBB BBGG GGGR RRRR
     **/
    public static class ABGR1555 extends PaletteFormat
    {
        @Override
        public int toARGB(int[] palette, int c)
        {
            int a = ((c & 0x8000) >> 15) * 0xFF;
            int r = (c & 0x001F) << 3;
            int g = (c & 0x03E0) >> 2;
            int b = (c & 0x7C00) >> 7;
            return((a << 24) | (r << 16) | (g << 8) | b);
        }
        @Override
        public int fromARGB(int[] palette, int c)
        {
            int p = palette[c];
            int a = ((p >> 24) & 0xFF) >= 128 ? 0x8000 : 0x0000;
            return(a
              | (((p & 0x000000FF) >> 3)  << 10)
              | (((p & 0x0000FF00) >> 11) << 5) 
              |  ((p & 0x00FF0000) >> 19));
        }
    }
    
    /**
     * AAAA BBBB GGGG RRRR
     **/
    public static class ABGR4444 extends PaletteFormat
    {
        @Override
        public int toARGB(int[] palette, int c)
        {
            int a = (c & 0xF000) >> 8;
            int r = (c & 0x000F) << 4;
            int g = (c & 0x00F0);
            int b = (c & 0x0F00) >> 4;
            return((a << 24) | (r << 16) | (g << 8) | b);
        }
        @Override
        public int fromARGB(int[] palette, int c)
        {
            int p = palette[c];
            return(((p & 0xF0000000) >> 16)
                 | ((p & 0x000000F0) << 4)
                 | ((p & 0x0000F000) >> 8) 
                 | ((p & 0x00F00000) >> 20));
        }
    }
    
    /**
     * AAAA AAAA BBBB BBBB GGGG GGGG RRRR RRRR
     **/
    public static class ABGR8888 extends PaletteFormat
    {
        @Override
        public int toARGB(int[] palette, int c)
        {
            int a = (c >> 24)   & 0xFF;
            int r = c           & 0xFF;
            int g = (c >> 8)    & 0xFF;
            int b = (c >> 16)   & 0xFF;
            return((a << 24) | (r << 16) | (g << 8) | b);
        }
        @Override
        public int fromARGB(int[] palette, int c)
        {
            int p = palette[c];
            return((p & 0xFF00FF00)          // alpha and green
                | ((p & 0x000000FF) << 16)   // blue
                | ((p & 0x00FF0000) >> 16)); // red 
        }
    }
    
    public static class Index extends PaletteFormat
    {
        @Override
        public int toARGB(int[] palette, int c)
        {
            return(palette[c]);
        }
        @Override
        public int fromARGB(int[] palette, int c)
        {
            return(c);
        }
    }
}
