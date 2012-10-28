package dreditor.lin;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

import dreditor.*;

/**
 *
 * @author /a/nonymous scanlations
 */
public class LINScript extends IBinPAK
{    
    private InstructionBin insBin;
    private BinPAK strings;
    byte flag1, flag2;
    
    public LINScript()
    {
        this(new InstructionBin(), new BinPAK());
    }
    public LINScript(InstructionBin _insBin)
    {
        this(_insBin, new BinPAK());
    }
    public LINScript(InstructionBin _insBin, BinPAK _strings)
    {
        super(4, true);
        
        insBin = _insBin;
        strings = _strings;
        add(insBin);
        add(strings);
        
        flag1 = flag2 = 0;
    }
    
    public void setFlags(int _flag1, int _flag2)
    {
        flag1 = (byte)_flag1;
        flag2 = (byte)_flag2;
    }
    
    @Override
    public String toJSValue(String root, String ID)
    {
        return(null);
    }
    
    @Override
    public List<String> toJSFunction(String root, String ID) throws LINParseException
    {
        LINtoJS js = new LINtoJS(this);
        insBin.iterate(js);
        String fullID = ID == null ? root : root + "_" + ID;
        StringBuilder sb = new StringBuilder(String.format("function %s()\n{\n\tvar s = new LINScript();\n", fullID));
        sb.append(js.getStringBuilder()).append("\treturn(s);\n}\n");
        List<String> list = new ArrayList<>();
        list.add(sb.toString());
        return(list);
    }
    
    public String toLines() throws LINParseException
    {
        LineExtractor ex = new LineExtractor(this);
        insBin.iterate(ex);
        return(ex.toString());
    }
    
    public int stringCount()
    {
        return(strings.size());
    }
    
    public String getString(int index)
    {
        return(((BinString)strings.get(index)).getString());
    }
    
    @Override
    public String toString()
    {
        return(String.format("LINScript (%d instructions, %d strings)", 
                                    insBin.count(), strings.size()));
    }
    
    // 2 if there are strings, 1 if not.
    @Override
    protected int size()
    {
        return(strings.size() > 0 ? 2 : 1);
    }
    // Don't let IBinPAK see the strings bin if there aren't any strings.
    @Override
    protected List<BinPart> getAll()
    {
        if(strings.size() > 0)
            return(super.getAll());
        List<BinPart> list = new ArrayList<>();
        list.add(insBin);
        return(list);
    }
    
    @Override
    public ByteBuffer getBytes() throws IOException
    {
        ByteBuffer bb = super.getBytes();
        bb.mark();
        bb.position(bb.limit() - 2);
        bb.put(flag1);
        bb.put(flag2);
        bb.reset();
        return(bb);
    }
    
    /*******************************************************************
     ********************** Javascript functions **********************
     *******************************************************************/
    
    /**
     * Adds a line to the string section, but doesn't show it.
     * This is here to support files like "e00_002_000.lin" that have orphaned strings.
     * @param line Line to add
     */
    public void addLine(String line)
    {
        strings.add(new BinString(line));
    }
    
    /**
     * Opcode 0x02.
     * Adds a line to the strings section, and shows it.
     * @param line Line to show
     */
    public void showLine(String line)
    {
        int index = strings.size();
        strings.add(new BinString(line));
        op(0x02, new int[]{index >> 8, index});
    }
    
    /**
     * Opcode 0x05.
     * Play movie
     * @param id        Movie ID (i.e. id = 3 means playing movie_03.pmf)
     * @param arg1      Unknown argument
     */
    public void playMovie(int id, boolean arg1)
    {
        op(0x05, new int[]{id, arg1 ? 0x01 : 0x00});
    }
    
    /**
     * Opcode 0x08.
     * Plays a voice sample.
     * @param speaker   Speaker (name) of the voice sample
     * @param chapter   Chapter that the voice sample is associated with
     * @param id        ID of the voice sample 
     */
    public void playVoice(String speaker, int chapter, int id)
    {
        playVoice(Constants.CHARACTERS.get(speaker), chapter, id);
    }
    
    /**
     * Opcode 0x08.
     * Plays a voice sample.
     * @param speaker   Speaker (index) of the voice sample
     * @param chapter   Chapter that the voice sample is associated with
     * @param id        ID of the voice sample 
     */
    public void playVoice(int speaker, int chapter, int id)
    {
        op(0x08, new int[]{speaker, chapter, (id >> 8) & 0xFF, id & 0xFF, 100});
    }
    
    /**
     * Opcode 0x09.
     * Plays a BGM.
     * @param id        ID of the BGM. 0xFF = off (no BGM).
     * @param volume    Volume (100 = 100%, but it can be higher than 100%)
     * @param leadIn    How long it takes the BGM to reach full volume. I don't know the units, but I think it's in frames.
     */
    public void playBGM(int id, int volume, int leadIn)
    {
        op(0x09, new int[]{id, volume, leadIn});
    }
    
    /**
     * Opcode 0x0A.
     * Plays a sound effect.
     * @param id        ID of the sound effect
     * @param volume    Volume (100 = 100%, but it can be higher than 100%)
     */
    public void playSoundEffectA(int id, int volume)
    {
        op(0x0A, new int[]{(id >> 8) & 0xFF, id & 0xFF, volume});
    }
    
    /**
     * Opcode 0x0B.
     * Plays a sound effect. Different from 0x0A.
     * @param id        ID of the sound effect
     * @param volume    Volume (100 = 100%, but it can be higher than 100%)
     */
    public void playSoundEffectB(int id, int volume)
    {
        op(0x0B, new int[]{id, volume});
    }
    
    /**
     * Opcode 0x0C. args[0] = 0xFF, args[1] = 0x00
     * Resets all evidence in handbook.
     */
    public void resetEvidence()
    {
        op(0x0C, new int[]{0xFF, 0x00});
    }
    
    /**
     * Opcode 0x0C. args[1] = 0x01
     * Adds evidence to handbook
     */
    public void addEvidence(int id)
    {
        op(0x0C, new int[]{id, 0x01});
    }
    
    /**
     * Opcode 0x0C. args[1] = 0x02
     * Adds evidence to handbook
     */
    public void updateEvidence(int id)
    {
        op(0x0C, new int[]{id, 0x02});
    }
    
    /**
     * Opcode 0x0F.
     * Set character titles (i.e. "超高校級の幸運（不運？）").
     * @param normal    These characters get their normal titles. For example, if Kyouko is in this list, then her title would get set to "超高校級の？？？".
     * @param alt       These characters get their alt titles. For example, if Kyouko is in this list, then her title would get set to "超高校級の探偵".
     */
    public void setCharacterTitles(String[] normal, String[] alt)
    {
        Map<Integer, Integer> map = new TreeMap<>();
        for(String character : normal)
            map.put(Constants.CHARACTERS.get(character), 0);
        for(String character : alt)
            map.put(Constants.CHARACTERS.get(character), 1);
        for(Map.Entry<Integer, Integer> e : map.entrySet())
        {
            op(0x0F, new int[]{e.getKey(), 0x00, e.getValue()});
        }
    }
    
    /**
     * Opcode 0x10.
     * Set the number of pieces of information about a character in the handbook.
     * @param character Character to set.
     * @param num       Number of pieces of information
     */
    public void updateCharacterInfo(String character, int num)
    {
        op(0x10, new int[]{Constants.CHARACTERS.get(character), 0x00, num});
    }
    
    /**
     * Opcode 0x19.
     * Goes to another script.
     * For example, goToScript(0, 2, 0) will make it go to "e00_002_000.lin".
     * @param chapter   Chapter
     * @param id1       First ID
     * @param id2       Second ID
     */
    public void goToScript(int chapter, int id1, int id2)
    {
        op(0x19, new int[]{chapter, id1, id2});
    }
    
    /**
     * Opcode 0x1E.
     * Shows a character sprite (e.g. "bustup_01_08.gim")
     * @param gbuffer       Which graphics buffer to load the sprite into
     * @param character     Character that the sprite belongs to
     * @param id            ID (e.g. ID = 8 in "bustup_01_08.gim")
     * @param effect        Effect? 1 = fast fade in, 2 = slow fade in
     * @param angle         Camera angle in 3D scenes
     */
    public void showSprite(int gbuffer, String character, int id, int effect, int angle)
    {
        op(0x1E, new int[]{gbuffer, Constants.CHARACTERS.get(character), id, effect, angle});
    }
    
    /**
     * Opcode 0x21.
     * Sets the speaker of the message window.
     * This is used mostly for Makoto and unknown speakers ("???").
     * Most of the characters use 0x1E to set the speaker.
     * @param speaker   Speaker name
     */
    public void setSpeaker(String speaker)
    {
       setSpeaker(Constants.CHARACTERS.get(speaker));
    }
    
    /**
     * Opcode 0x21.
     * Sets the speaker of the message window.
     * This is used mostly for Makoto and unknown speakers ("???").
     * Most of the characters use 0x1E to set the speaker.
     * @param speaker   Speaker index
     */
    public void setSpeaker(int speaker)
    {
        op(0x21, new int[]{speaker});
    }
    
    /**
     * Opcode 0x3A.
     * Waits for input. After it gets input, it clears the message window.
     */
    public void waitForInput()
    {
        op(0x3A, new int[]{});
    }
    
    /**
     * Opcode 0x3B.
     * Waits for the given number of frames.
     * @param frames    Number of frames to wait for
     */
    public void waitFrames(int frames)
    {
        for(int i = 0; i < frames; i++)
            op(0x3B, new int[]{});
    }
    
    public void op(int op)
    {
        if(op != 0x00)
            opBytes(op, new byte[]{});
    }
    
    public void op(int op, int[] args)
    {
        if(op == 0x00)
            return;
        byte[] bArgs = new byte[args.length];
        for(int i = 0; i < args.length; i++)
            bArgs[i] = (byte)args[i];
        opBytes(op, bArgs);
    }
    
    public void opBytes(int op, byte[] args)
    {
        if(op != 0x00)
        {
            if(!InstructionBin.NUMARGS.containsKey(op))
                throw new RuntimeException(String.format("Unknown opcode: 0x%02X", op));
            int numArgs = InstructionBin.NUMARGS.get(op);
            if(numArgs != -1 && args.length != numArgs)
                throw new RuntimeException(String.format("Incorrect number of arguments: Opcode 0x%02X takes %d arguments, not %d.", op, numArgs, args.length));
            insBin.op(op, args);
        }
    }
}
