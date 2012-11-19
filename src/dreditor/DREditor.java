package dreditor;

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

import dreditor.font.*;
import dreditor.gim.*;
import dreditor.gui.*;

import java.awt.image.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *
 * @author /a/nonymous scanlations
 */
public class DREditor
{
    public static File workspace, workspaceRaw, rawEBOOT, workspaceOrig, workspaceSrc, workspaceTrans;
    
    public final static Charset scriptCharset = Charset.forName("UTF-8");
    
    public static void main(String[] args) throws Exception
    {
        DREditor.setWorkspace(new File(PrefsUtils.PREFS.get("dir", "")));
        
        GUIUtils.initGUI();
    }
    
    public static void importFont(Config config, File font)
            throws IOException,
                    InvalidTOCException, JSONException,
                    ParserConfigurationException, SAXException,
                    IllegalArgumentException
    {
        File fontDir = new File(workspaceSrc, "font_pak");
        if(!fontDir.exists())
            unpack(config, UmdPAKFile.UMDIMAGE2, 169);
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(font);
        doc.getDocumentElement().normalize();
        NodeList pageNodes = doc.getElementsByTagName("page");
        if(pageNodes.getLength() != 1)
            throw new IllegalArgumentException("Font files should only have one page!");
        
        File pageFile = new File(font.getParentFile(), ((Element)pageNodes.item(0)).getAttribute("file"));
        BufferedImage img1 = ImageIO.read(pageFile);
        BufferedImage img2 = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        // drawImage is bad.
        for(int y = 0; y < img1.getHeight(); y++)
            for(int x = 0; x < img1.getWidth(); x++)
                img2.setRGB(x, y, img1.getRGB(x, y));
        ImageIO.write(img2, "bmp", new File(fontDir, "000.bin"));
        
        NodeList charNodes = doc.getElementsByTagName("char");
        TreeSet<Glyph> glyphs = new TreeSet<>();
        for(int i = 0; i < charNodes.getLength(); i++)
        {
            Element c = (Element)charNodes.item(i);
            Glyph g = new Glyph(Integer.parseInt(c.getAttribute("id")),
                                Integer.parseInt(c.getAttribute("x")),
                                Integer.parseInt(c.getAttribute("y")),
                                Integer.parseInt(c.getAttribute("width")),
                                Integer.parseInt(c.getAttribute("height")));
            glyphs.add(g);
            if(g.codepoint == 0x20)
            {
                Glyph blank = new Glyph(0x09, g.x, g.y, 0, g.height);
                glyphs.add(blank);
            }
        }
        
        int index = 0;
        int lastChar = 0;
        ByteArrayOutputStream part2 = new ByteArrayOutputStream(),
                            part3 = new ByteArrayOutputStream();
        for(Glyph g : glyphs)
        {
            while(lastChar < g.codepoint)
            {
                IOUtils.putShort(part2, 0xFFFF);
                lastChar++;
            }
            IOUtils.putShort(part2, index);
            lastChar++;
            
            IOUtils.putShort(part3, g.codepoint);
            IOUtils.putShort(part3, g.x);
            IOUtils.putShort(part3, g.y);
            IOUtils.putShort(part3, g.width);
            IOUtils.putShort(part3, g.height);
            IOUtils.putShort(part3, 0x0000);
            IOUtils.putShort(part3, 0x0000);
            IOUtils.putShort(part3, 0x08FA);
            
            index++;
        }
        
        ByteArrayOutputStream bin = new ByteArrayOutputStream();
        IOUtils.putInt(bin, 0x53704674);
        IOUtils.putInt(bin, 0x00000004);
        IOUtils.putInt(bin, index - 1);
        IOUtils.putInt(bin, 0x20 + part2.size());
        IOUtils.putInt(bin, lastChar - 1);
        IOUtils.putInt(bin, 0x00000020);
        IOUtils.putInt(bin, 0x0000002D);
        IOUtils.putInt(bin, 0x00000001);
        
        part2.writeTo(bin);
        part3.writeTo(bin);
        while(bin.size() % 0x10 != 0)
            IOUtils.putShort(bin, 0x0000);
        bin.writeTo(new FileOutputStream(new File(fontDir, "001.bin")));
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
    
    // Individual strings
    private static final int[] EBOOT_STRINGS_LOC = new int[]{0xEF328, 0xEFED4, 0xEFF20, 0xEFF34};
    private static final int[] EBOOT_STRINGS_LEN = new int[]{75, 75, 19, 27};
    
    // String sections
    private static final int[] EBOOT_STRING_SECT_LOC = new int[]{0x1098F0, 0x109A8C, 0x109AAC};
    private static final int[] EBOOT_STRING_SECT_LEN = new int[]{10, 7, 22};
    
    private static final int EBOOT_STRING_TOTAL_LEN = 43;
    
    public static void unpackEBOOTStrings() throws IOException
    {
        BinPAK pak = new BinPAK(4);
        try(SeekableByteChannel eboot = 
                Files.newByteChannel(new File(workspaceRaw, "EBOOT.BIN").toPath(), 
                    StandardOpenOption.READ);
            InputStream ebootIS = Channels.newInputStream(eboot))
        {
            long position;
            ByteBuffer bb = ByteBuffer.allocate(4);;
            bb.order(ByteOrder.LITTLE_ENDIAN);
            
            for(int i = 0; i < EBOOT_STRINGS_LOC.length; i++)
            {
                eboot.position(EBOOT_STRINGS_LOC[i]);
                pak.add(IOUtils.getNULTerminatedString(ebootIS, scriptCharset));
            }
            
            for(int i = 0; i < EBOOT_STRING_SECT_LOC.length; i++)
            {
                eboot.position(EBOOT_STRING_SECT_LOC[i]);
                do
                {
                    eboot.read(bb);
                    bb.flip();
                    position = eboot.position();
                    int ptr = bb.getInt();
                    eboot.position(ptr >= 0x10A000 ? ptr + 0xC0 : ptr + 0xA0);
                    bb.flip();
                    pak.add(IOUtils.getNULTerminatedString(ebootIS, scriptCharset));
                    eboot.position(position);
                }
                while(position < EBOOT_STRING_SECT_LOC[i] + 4 * EBOOT_STRING_SECT_LEN[i]);
            }
        }
        BinFactory.exportToJSFile("eboot", pak);
    }
    
    public static void prepareEBOOT(Config config) throws IOException
    {
        Files.copy(rawEBOOT.toPath(), new File(workspaceTrans, "EBOOT.BIN").toPath(), StandardCopyOption.REPLACE_EXISTING);
        try(SeekableByteChannel eboot = 
                Files.newByteChannel(new File(workspaceTrans, "EBOOT.BIN").toPath(), 
                    StandardOpenOption.READ, 
                    StandardOpenOption.WRITE))
        {
            // Home/save screen language
            eboot.position(0x1B2E0);
            eboot.write(ByteBuffer.wrap(new byte[]{
                (byte)(config.HOME_SCREEN_LANG), (byte)0x00, (byte)0x02, (byte)0x24,
            }));
            // Set button order
            eboot.position(0x1B3E4);
            eboot.write(ByteBuffer.wrap(new byte[]{
                (byte)(config.BUTTON_ORDER_SWITCHED ? sceImpose.PSP_CONFIRM_BUTTON_CROSS : sceImpose.PSP_CONFIRM_BUTTON_CIRCLE), (byte)0x00, (byte)0x02, (byte)0x24,
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
            
            if(config.EBOOT_STRINGS)
            {
                // Pack strings
                // Try JS importation
                BinPAK pak;
                try
                {
                    pak = (BinPAK)BinFactory.importFromJSFile(config, "eboot");
                }
                catch(IOException ioe)
                {
                    return;
                }
                catch(javax.script.ScriptException e)
                {
                    e.printStackTrace();
                    throw new RuntimeException("ScriptException caught in compilation of EBOOT strings.");
                }

                // Check eboot.js
                if(pak.size() != EBOOT_STRING_TOTAL_LEN)
                    throw new RuntimeException("Incorrect number of strings in \"eboot.js\".");
                for(int i = 0; i < EBOOT_STRING_TOTAL_LEN; i++)
                    if(!(pak.get(i) instanceof BinString))
                        throw new RuntimeException("\"eboot.js\" can only have strings.");

                // Write strings
                for(int i = 0; i < EBOOT_STRINGS_LOC.length; i++)
                {
                    byte[] bytes = ((BinString)pak.get(i)).getString().getBytes(Constants.UTF8);
                    if(bytes.length > EBOOT_STRINGS_LEN[i])
                        throw new RuntimeException("String #" + (i + 1) + " in \"eboot.js\" is too long! Must be less than or equal to 75 bytes in UTF-8.");
                    eboot.position(EBOOT_STRINGS_LOC[i]);
                    ByteBuffer bb = ByteBuffer.allocate(EBOOT_STRINGS_LEN[i] + 1);
                    bb.put(bytes);
                    bb.position(0);
                    eboot.write(bb);
                }

                // Write other strings
                eboot.position(0x10A400);
                long stringLoc, position;
                int stringIndex = EBOOT_STRINGS_LOC.length;
                for(int i = 0; i < EBOOT_STRING_SECT_LOC.length; i++)
                {
                    for(int j = 0; j < EBOOT_STRING_SECT_LEN[i]; j++)
                    {
                        stringLoc = eboot.position();
                        byte[] bytes = ((BinString)pak.get(stringIndex)).getString().getBytes(Constants.UTF8);
                        ByteBuffer bb = ByteBuffer.allocate(Constants.round(bytes.length + 1, 4));
                        bb.put(bytes);
                        bb.position(0);
                        eboot.write(bb);

                        position = eboot.position();
                        eboot.position(EBOOT_STRING_SECT_LOC[i] + 4 * j);
                        IOUtils.putInt(eboot, (int)stringLoc - 0xC0);
                        eboot.position(position);

                        stringIndex++;
                    }
                }
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
            throw new RuntimeException("ScriptException caught in compilation of file \"" + filename + "\".");
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
        workspaceOrig = new File(workspace, "orig");
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
    
    public static void packToISO(File oldISOFile, File newISOFile) throws IOException
    {
        packToISO(oldISOFile, newISOFile);
    }
    public static void packToISO(File oldISOFile, File newISOFile, Runnable listener) throws IOException
    {
        try(SeekableByteChannel newISO = Files.newByteChannel(newISOFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            SeekableByteChannel oldISO = Files.newByteChannel(oldISOFile.toPath(), StandardOpenOption.READ);)
        {
            ByteBuffer header = ByteBuffer.allocate(0xE800);
            oldISO.read(header);
            header.flip();
            newISO.write(header);
            
            ByteBuffer buf = ByteBuffer.allocate(0x800);
            for(ISOFiles f : ISOFiles.values())
            {
                SeekableByteChannel fileSource = oldISO;
                SeekableByteChannel transChan = null;
                
                // Get data
                oldISO.position(f.recordLoc + 2);
                int oldLBA = IOUtils.getInt(oldISO);
                oldISO.position(oldISO.position() + 4);
                int oldSize = IOUtils.getInt(oldISO);
                
                int newPos = Constants.round((int)newISO.position(), 0x800);
                int newLBA = newPos / 0x800;
                int newSize = oldSize;
                
                // Should we read from an external file instead?
                if(f.checkTrans)
                {
                    File transFile = new File(workspaceTrans, f.path.substring(f.path.lastIndexOf('/') + 1));
                    try
                    {
                        fileSource = transChan = Files.newByteChannel(transFile.toPath(), StandardOpenOption.READ);
                        newSize = (int)transChan.size();
                    }
                    catch(IOException e)
                    {
                        transChan = null;
                    }
                }
                
                // Update LBAs
                newISO.position(f.recordLoc + 2);
                IOUtils.putInt(newISO, newLBA, ByteOrder.LITTLE_ENDIAN);
                IOUtils.putInt(newISO, newLBA, ByteOrder.BIG_ENDIAN);
                IOUtils.putInt(newISO, newSize, ByteOrder.LITTLE_ENDIAN);
                IOUtils.putInt(newISO, newSize, ByteOrder.BIG_ENDIAN);
                
                // Write file to new ISO
                newISO.position(newPos);
                oldISO.position(oldLBA * 0x800);
                for(int i = 0; i < oldSize; i += 0x800)
                {
                    fileSource.read(buf);
                    buf.flip();
                    newISO.write(buf);
                    buf.flip();
                }
                buf.clear();
                
                // Closes external file
                if(transChan != null)
                    transChan.close();
                
                if(listener != null)
                    listener.run();
            }
        }
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
