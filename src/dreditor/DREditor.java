package dreditor;

import dreditor.gui.PrefsUtils;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

import jpcsp.format.PSP;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.HLE.modules150.sceImpose;
import org.stringtemplate.v4.ST;
import org.json.JSONException;

import dreditor.gim.*;
import dreditor.gui.*;

/**
 *
 * @author /a/nonymous scanlations
 */
public class DREditor
{
    public static File workspace, workspaceRaw, rawEBOOT, workspaceSrc, workspaceTrans;
    
    public final static Charset scriptCharset = Charset.forName("UTF-8");
    
    public static void main(String[] args) throws Exception
    {
        DREditor.setWorkspace(new File(PrefsUtils.PREFS.get("dir", "")));
        
        GUIUtils.initGUI();
    }
    
    private static void jsToBin(UmdPAKFile f, int num) throws javax.script.ScriptException, IOException
    {
        List<UmdFileInfo> list = BinFactory.parseTOC(f);
        BinPart p = BinFactory.importFromJSFile(Config.DEFAULT, list.get(num));
        ByteBuffer bb = p.getBytes();
        Files.write(new File(workspace, String.format("%04d.bin", num)).toPath(), 
                    IOUtils.toArray(bb),
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public static void unpackAll(Config config, UmdPAKFile f) throws IOException, InvalidTOCException, JSONException
    {
        unpack(config, f, 0, f.numFiles);
    }
    
    public static void unpack(Config config, UmdPAKFile f, int index) throws IOException, InvalidTOCException, JSONException
    {
        List<UmdFileInfo> list = BinFactory.parseTOC(f);
        UmdFileInfo file = list.get(index);
        BinPart bp = BinFactory.parseBinFile(config, f, file);
        if(bp instanceof BinBytes)
        {
            Files.write(new File(DREditor.workspaceSrc, file.file).toPath(), 
                IOUtils.toArray(bp.getBytes()),
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
        }
        else if(bp instanceof GIMBin)
        {
            ImageIO.write(((GIMBin)bp).getImage(), "PNG", new File(DREditor.workspaceSrc, file.file + ".png"));
            Files.write(new File(DREditor.workspaceSrc, file.file + ".json").toPath(),
                ((GIMBin)bp).getInfo().toJSON().getBytes(scriptCharset),
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
        }
        else
        {
            BinFactory.exportToJSFile(IOUtils.toID(index, file.file), bp);
        }
    }
    
    public static void unpack(Config config, UmdPAKFile f, int start, int end) throws IOException, InvalidTOCException, JSONException
    {
        for(int i = start; i < end; i++)
            unpack(config, f, i);
    }
    
    public static void prepareEBOOT(Config config) throws IOException
    {
        Files.copy(rawEBOOT.toPath(), new File(workspaceTrans, "EBOOT.BIN").toPath(), StandardCopyOption.REPLACE_EXISTING);
        try(SeekableByteChannel eboot = 
                Files.newByteChannel(new File(workspaceTrans, "EBOOT.BIN").toPath(), 
                    StandardOpenOption.READ, 
                    StandardOpenOption.WRITE))
        {
            // sceImposeSetLanguageMode fix: changes Home screne language and button order
            eboot.position(0x65AC);
            eboot.write(ByteBuffer.wrap(new byte[]{
                (byte)(config.HOME_SCREEN_LANG), (byte)0x00, (byte)0x50, (byte)0x24,
                (byte)(config.BUTTON_ORDER_SWITCHED ? sceImpose.PSP_CONFIRM_BUTTON_CROSS : sceImpose.PSP_CONFIRM_BUTTON_CIRCLE), (byte)0x00, (byte)0x45, (byte)0x24
            }));
            if(config.BUTTON_ORDER_SWITCHED)
            {
                // Changes button order
                eboot.position(0xE284);
                eboot.write(ByteBuffer.wrap(new byte[]{(byte)0x90, (byte)0x38, (byte)0x24, (byte)0x0A})); // j 0x0890E240
                eboot.position(0x10A300);
                eboot.write(ByteBuffer.wrap(new byte[]{
                    (byte)0x04, (byte)0x00, (byte)0xB1, (byte)0x8F, // lw $s1, 4($sp)
                    (byte)0x21, (byte)0x20, (byte)0x20, (byte)0x02, // addu $a0, $s1, $zr
                    (byte)0x00, (byte)0x20, (byte)0x25, (byte)0x32, // andi $a1, $s1, 8192
                    (byte)0x02, (byte)0x00, (byte)0xA0, (byte)0x14, // bne $a1, $zr, 2
                    (byte)0x00, (byte)0x40, (byte)0x31, (byte)0x36, // ori $s1, $s1, 16384
                    (byte)0x00, (byte)0x40, (byte)0x31, (byte)0x3A, // xori $s1, $s1, 16384
                    (byte)0x00, (byte)0x40, (byte)0x84, (byte)0x30, // andi $a0, $a0, 16384
                    (byte)0x02, (byte)0x00, (byte)0x80, (byte)0x14, // bne $a0, $zr, 2
                    (byte)0x00, (byte)0x20, (byte)0x31, (byte)0x36, // ori $s1, $s1, 8192
                    (byte)0x00, (byte)0x20, (byte)0x31, (byte)0x3A, // xori $s1, $s1, 8192
                    (byte)0x7B, (byte)0x48, (byte)0x20, (byte)0x0A  // j 0x088121EC
                }));
            }
        }
    }
    
    public static ByteBuffer compileSourceFile(Config config, PAKReader pakIn, UmdFileInfo file) throws IOException
    {
        return(compileSourceFile(config, pakIn, file.index, file.file));
    }
    public static ByteBuffer compileSourceFile(Config config, PAKReader pakIn, int index, String filename) throws IOException
    {
        ByteBuffer bytes = null;
                
        // Try JS importation
        try
        {
            bytes = BinFactory.importFromJSFile(config, index, filename).getBytes();
        }
        catch(IOException ioe){}
        catch(javax.script.ScriptException e)
        {
            e.printStackTrace();
            throw new RuntimeException("ScriptException caught in pack.");
        }
        // Try binary file importation
        if(bytes == null)
        {
            try
            {
                bytes = ByteBuffer.wrap(Files.readAllBytes(new File(DREditor.workspaceSrc, filename).toPath()));
            }
            catch(IOException e)
            {
                // munch
            }
        }
        if(bytes == null)
        {
            try
            {
                GIMBin bin = new GIMBin(filename);
                bytes = bin.getBytes();
            }
            catch(IOException|JSONException e)
            {
                // munch
            }
        }
        
        // Get it from the pak
        if(bytes == null)
            bytes = pakIn.read(index);
        
        return(bytes);
    }
    
    public static void pack(Config config, UmdPAKFile fileInfo) throws IOException, InvalidTOCException
    {
        pack(config, fileInfo, null);
    }
    public static void pack(Config config, UmdPAKFile f, Runnable listener) throws IOException, InvalidTOCException
    {
        try(SeekableByteChannel eboot = 
                Files.newByteChannel(new File(workspaceTrans, "EBOOT.BIN").toPath(), 
                    StandardOpenOption.READ, 
                    StandardOpenOption.WRITE);
            PAKReader pakIn = new PAKReader(new File(workspaceRaw, f.name));
            PAKWriter pakOut = new PAKWriter(new File(workspaceTrans, f.name), 
                                            f.numFiles, 
                                            f.padding))
        {
            ByteBuffer bytes;
            List<UmdFileInfo> list = BinFactory.parseTOC(f);
            for(UmdFileInfo file : list)
            {
                bytes = compileSourceFile(config, pakIn, file);
                
                if(config.BUILD_SCREEN && file.index == 2177 && file.file.equals("startup.pak"))
                {
                    try
                    {
                        ByteBuffer tmp = bytes.duplicate();
                        tmp.order(ByteOrder.LITTLE_ENDIAN);
                        bytes = tryModStartup(config, tmp);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                
                eboot.position(f.ebootTOCPos + 0xC * file.index + 0x4);
                IOUtils.putInt(eboot, pakOut.write(bytes));
                IOUtils.putInt(eboot, bytes.limit());
                
                if(listener != null)
                    listener.run();
            }
        }
    }
    
    public static ByteBuffer tryModStartup(Config config, ByteBuffer bytes) throws Exception
    {
        BinPAK bp = (BinPAK)BinFactory.tryParseBinPAK(config, bytes);
        BinPart oldImg1 = bp.get(2);
        GIMInfo info;
        
        if(oldImg1 instanceof GIMBin)
        {
            info = ((GIMBin)oldImg1).getInfo();
        }
        else
        {
            info = new GIMInfo();
            ByteBuffer oldImg1BB = oldImg1.getBytes();
            oldImg1BB.order(ByteOrder.LITTLE_ENDIAN);
            GIMImport.toImage(oldImg1BB, info);
        }
        
        bp.set(2, new GIMBin(GUIUtils.generateStartupImage(info.width, info.height, config.VERSION, config.AUTHOR, config.COMMENT), info));
        ByteBuffer ret = bp.getBytes();
        return(ret);
    }
    
    public static void setWorkspace(File _workspace)
    {
        workspace = _workspace;
        workspaceRaw = new File(workspace, "raw");
        rawEBOOT = new File(workspaceRaw, "EBOOT.BIN");
        workspaceSrc = new File(workspace, "src");
        workspaceTrans = new File(workspace, "trans");
    }
    
    public static void unpackFromISO(File isoFile) throws IOException
    {
        unpackFromISO(isoFile, null);
    }
    
    public static void unpackFromISO(File isoFile, Runnable listener) throws IOException
    {
        // 1) Check dir. If there are files in there, confirm with user
        if(!IOUtils.checkDir(workspaceRaw)
            || !IOUtils.checkDir(workspaceSrc)
            || !IOUtils.checkDir(workspaceTrans))
        {
            GUIUtils.error(GUIUtils.BUNDLE.getString("Error.directoriesInWorkspace"));
            throw new RuntimeException(GUIUtils.BUNDLE.getString("Error.directoriesInWorkspace"));
        }
        if(!IOUtils.checkExist(workspaceRaw, "EBOOT.BIN")
            || !IOUtils.checkExist(workspaceRaw, "umdimage.dat")
            || !IOUtils.checkExist(workspaceRaw, "umdimage2.dat"))
        {
            throw new RuntimeException(GUIUtils.BUNDLE.getString("Error.abort"));
        }
        
        // 2) Unpacking
        UmdIsoReader iso = new UmdIsoReader(isoFile.getAbsolutePath());
        try(UmdIsoFile eboot = iso.getFile("PSP_GAME/SYSDIR/EBOOT.BIN"))
        {
            byte[] bytes = IOUtils.readFully(eboot);
            // Check if it's decrypted
            if(bytes[0] == (byte)0x7F
                && bytes[1] == (byte)0x45
                && bytes[2] == (byte)0x4C
                && bytes[3] == (byte)0x46)
            {
                GUIUtils.warning(GUIUtils.BUNDLE.getString("Dialogue.ebootDecrypted"));
                IOUtils.copyAndCheck(bytes, new File(workspaceRaw, "EBOOT.BIN").toPath());
            }
            else
            {
                ByteBuffer f = ByteBuffer.wrap(bytes);
                PSP psp = new PSP(f);
                if(!psp.isValid())
                {
                    GUIUtils.error("EBOOT.BIN is not valid!");
                    throw new RuntimeException("EBOOT.BIN is not valid!");
                }
                byte[] decBytes = IOUtils.toArray(psp.decrypt(f));
                // Bug with JPCSP?
                decBytes[4] = 0x01;
                decBytes[7] = 0x00;
                
                long crc = IOUtils.copyAndCheck(decBytes, rawEBOOT.toPath());
                if(crc != 3716702853L)
                {
                    GUIUtils.warning(new ST(GUIUtils.BUNDLE.getString("Error.CRC"))
                                            .add("file", "EBOOT.BIN")
                                            .render());
                }
            }
        }
        listener.run();
        copyFile(iso, UmdPAKFile.UMDIMAGE);
        listener.run();
        copyFile(iso, UmdPAKFile.UMDIMAGE2);
        listener.run();
    }
    
    private static void copyFile(UmdIsoReader iso, UmdPAKFile fileInfo) throws IOException
    {
        try(UmdIsoFile file = iso.getFile(fileInfo.umdPath))
        {
            long crc = IOUtils.copyAndCheck(file, new File(workspaceRaw, fileInfo.name).toPath());
            if(crc != fileInfo.crc)
            {
                GUIUtils.warning(new ST(GUIUtils.BUNDLE.getString("Error.CRC"))
                                            .add("file", fileInfo.name)
                                            .render());
            }
        }
    }
}
