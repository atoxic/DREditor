package dreditor;

/**
 *
 * @author /a/nonymous scanlations
 */
public enum ISOFiles
{
    UMDDATA     ("UMD_DATA.BIN",                        0xB098),
    SYSOPNSSMP  ("PSP_GAME/SYSDIR/OPNSSMP.BIN",         0xC8D0),
    SYSEBOOT    ("PSP_GAME/SYSDIR/EBOOT.BIN",           0xC898, true),
    UPPARAM     ("PSP_GAME/SYSDIR/UPDATE/PARAM.SFO",    0xE0D0),
    UPEBOOT     ("PSP_GAME/SYSDIR/UPDATE/EBOOT.BIN",    0xE098),
    UPDATA      ("PSP_GAME/SYSDIR/UPDATE/DATA.BIN",     0xE060),
    SYSBOOT     ("PSP_GAME/SYSDIR/BOOT.BIN",            0xC860),
    GAMEPARAM   ("PSP_GAME/PARAM.SFO",                  0xB8CE),
    GAMEICON    ("PSP_GAME/ICON0.PNG",                  0xB860),
    GAMEPIC0    ("PSP_GAME/PIC0.PNG",                   0xB906),
    GAMEPIC1    ("PSP_GAME/PIC1.PNG",                   0xB93E),
    INSICON     ("PSP_GAME/INSDIR/ICON0.PNG",           0xC060),
    INSPIC1     ("PSP_GAME/INSDIR/PIC1.PNG",            0xC098),
    INSUMD      ("PSP_GAME/INSDIR/UMDIMAGE.DAT",        0xC0D0),
    LIBPSMF     ("PSP_GAME/USRDIR/libpsmfplayer.prx",   0xD096),
    PSMF        ("PSP_GAME/USRDIR/psmf.prx",            0xD8F0),
    BGMPAK      ("PSP_GAME/USRDIR/bgm.pak",             0xD060),
    UMD         ("PSP_GAME/USRDIR/umdimage.dat",        0xD928, true),
    UMD2        ("PSP_GAME/USRDIR/umdimage2.dat",       0xD964, true),
    VOICE       ("PSP_GAME/USRDIR/voice.pak",           0xD9A0),
    MOVIE00     ("PSP_GAME/USRDIR/movie_00.pmf",        0xD0D6),
    MOVIE01     ("PSP_GAME/USRDIR/movie_01.pmf",        0xD112),
    MOVIE02     ("PSP_GAME/USRDIR/movie_02.pmf",        0xD14E),
    MOVIE03     ("PSP_GAME/USRDIR/movie_03.pmf",        0xD18A),
    MOVIE04     ("PSP_GAME/USRDIR/movie_04.pmf",        0xD1C6),
    MOVIE05     ("PSP_GAME/USRDIR/movie_05.pmf",        0xD202),
    MOVIE06     ("PSP_GAME/USRDIR/movie_06.pmf",        0xD23E),
    MOVIE07     ("PSP_GAME/USRDIR/movie_07.pmf",        0xD27A),
    MOVIE08     ("PSP_GAME/USRDIR/movie_08.pmf",        0xD2B6),
    MOVIE09     ("PSP_GAME/USRDIR/movie_09.pmf",        0xD2F2),
    MOVIE10     ("PSP_GAME/USRDIR/movie_10.pmf",        0xD32E),
    MOVIE11     ("PSP_GAME/USRDIR/movie_11.pmf",        0xD36A),
    MOVIE12     ("PSP_GAME/USRDIR/movie_12.pmf",        0xD3A6),
    MOVIE13     ("PSP_GAME/USRDIR/movie_13.pmf",        0xD3E2),
    MOVIE14     ("PSP_GAME/USRDIR/movie_14.pmf",        0xD41E),
    MOVIE15     ("PSP_GAME/USRDIR/movie_15.pmf",        0xD45A),
    MOVIE16     ("PSP_GAME/USRDIR/movie_16.pmf",        0xD496),
    MOVIE17     ("PSP_GAME/USRDIR/movie_17.pmf",        0xD4D2),
    MOVIE18     ("PSP_GAME/USRDIR/movie_18.pmf",        0xD50E),
    MOVIE19     ("PSP_GAME/USRDIR/movie_19.pmf",        0xD54A),
    MOVIE20     ("PSP_GAME/USRDIR/movie_20.pmf",        0xD586),
    MOVIE21     ("PSP_GAME/USRDIR/movie_21.pmf",        0xD5C2),
    MOVIE22     ("PSP_GAME/USRDIR/movie_22.pmf",        0xD5FE),
    MOVIE23     ("PSP_GAME/USRDIR/movie_23.pmf",        0xD63A),
    MOVIE24     ("PSP_GAME/USRDIR/movie_24.pmf",        0xD676),
    MOVIE25     ("PSP_GAME/USRDIR/movie_25.pmf",        0xD6B2),
    MOVIE26     ("PSP_GAME/USRDIR/movie_26.pmf",        0xD6EE),
    MOVIE27     ("PSP_GAME/USRDIR/movie_27.pmf",        0xD72A),
    MOVIE28     ("PSP_GAME/USRDIR/movie_28.pmf",        0xD766),
    MOVIE29     ("PSP_GAME/USRDIR/movie_29.pmf",        0xD7A2),
    MOVIE30     ("PSP_GAME/USRDIR/movie_30.pmf",        0xD800),
    MOVIE31     ("PSP_GAME/USRDIR/movie_31.pmf",        0xD83C),
    MOVIE32     ("PSP_GAME/USRDIR/movie_32.pmf",        0xD878),
    MOVIE33     ("PSP_GAME/USRDIR/movie_33.pmf",        0xD8B4);
    
    public String path;
    public int recordLoc;
    public boolean checkTrans;
    private ISOFiles(String _path, int _recordLoc)
    {
        this(_path, _recordLoc, false);
    }
    private ISOFiles(String _path, int _recordLoc, boolean _checkTrans)
    {
        path = _path;
        recordLoc = _recordLoc;
        checkTrans = _checkTrans;
    }
}
