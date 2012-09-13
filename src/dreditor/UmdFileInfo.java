package dreditor;

/**
 * Used to contain TOC information from top-level PAK files
 * @author /a/nonymous scanlations
 */
public class UmdFileInfo
{
    public int index, pos, size;
    public String file;
    
    public UmdFileInfo(int _index, String _file, int _pos, int _size)
    {
        index = _index;
        file = _file;
        pos = _pos;
        size = _size;
    }
    
    @Override
    public String toString()
    {
        return(String.format("File %04d: \"%s\" at 0x%08X, size %d", index, file, pos, size));
    }
}
