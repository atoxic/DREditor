package dreditor;

import java.io.*;
import java.util.*;

/**
 * Contains information on top-level PAK files
 * @author /a/nonymous scanlations
 */
public enum UmdPAKFile
{
    UMDIMAGE("umdimage.dat", "PSP_GAME/USRDIR/umdimage.dat", 1395628980L, 0xF5A18, 1024, 0xF09),
    UMDIMAGE2("umdimage2.dat", "PSP_GAME/USRDIR/umdimage2.dat", 3769471295L, 0xF5200, 64, 0xAA);
    
    public final String name, umdPath;
    public final long crc;
    public final int ebootTOCPos, padding, numFiles;
    private UmdPAKFile(String _name, String _umdPath, long _crc, int _ebootTOCPos, int _padding, int _numFiles)
    {
        name = _name;
        umdPath = _umdPath;
        crc = _crc;
        ebootTOCPos = _ebootTOCPos;
        padding = _padding;
        numFiles = _numFiles;
    }
}
