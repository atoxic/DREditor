package dreditor;

import jpcsp.HLE.modules150.sceImpose;

/**
 * Configuration optiosn.
 * @author /a/nonymous scanlations
 */
public class Config
{
    public static final Config DEFAULT = new Config();
    
    // Home screen language
    public int HOME_SCREEN_LANG = sceImpose.PSP_LANGUAGE_ENGLISH;
    
    // Whether the confirm button is O or X
    public boolean BUTTON_ORDER_SWITCHED = false;
    
    // Which files to repack
    public boolean PACK_UMDIMAGE = true;
    public boolean PACK_UMDIMAGE2 = true;
    
    // Convert GIM assets to PNG
    public boolean CONVERT_GIM = true;
    
    // Build screen (replacing the first warning screen with build information)
    public boolean BUILD_SCREEN = true;
    public String VERSION = "0.0.1";
    public String AUTHOR = "";
    public String COMMENT = "";
    
    public String getConfirmButton()
    {
        return(BUTTON_ORDER_SWITCHED ? "X" : "O");
    }
}
