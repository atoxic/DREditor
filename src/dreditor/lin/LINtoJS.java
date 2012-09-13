package dreditor.lin;

import java.util.*;

import dreditor.*;

/**
 * Used for converting LINScripts to Javascript
 * @author /a/nonymous scanlations
 */
public class LINtoJS implements InstructionListener
{
    private LINScript lin;
    
    private StringBuilder javascript, perLine;
    private int lineIndex = 0;

    private String currentSpeaker;
    // Used for 0x0F
    private boolean inAltTitles = false;
    private ArrayList<String> normTitles, altTitles;
    // Used for 0x3B
    private int waitCount;
    
    public LINtoJS(LINScript _lin)
    {
        lin = _lin;
        
        javascript = new StringBuilder();
        perLine = new StringBuilder();
        
        lineIndex = 0;
        currentSpeaker = null;
        
        inAltTitles = false;
        normTitles = new ArrayList<>();
        altTitles = new ArrayList<>();
        waitCount = 0;
    }
    
    @Override
    public String toString()
    {
        return(javascript.toString());
    }
    
    public StringBuilder getStringBuilder()
    {
        return(javascript);
    }
    
    private static String getCharacter(int ID)
    {
        if(Constants.CHARACTERS_INV.containsKey(ID))
            return('"' + Constants.CHARACTERS_INV.get(ID) + '"');
        else
            return(String.format("0x%02X", ID));
    }
    
    private static int toByte(byte b)
    {
        return((int)b & 0xFF);
    }
    
    private static int toShort(byte hi, byte lo)
    {
        return(((int)hi & 0xFF) << 8 
                | ((int)lo & 0xFF));
    }

    @Override
    public void op(int op, byte[] args)
    {
        if(inAltTitles && op != 0x0F)
        {
            perLine.append(String.format("\ts.setCharacterTitles(%s, %s);\n", 
                    normTitles.toString(), altTitles.toString()));
            normTitles.clear();
            altTitles.clear();
            inAltTitles = false;
        }
        if(waitCount > 0 && op != 0x3B)
        {
            perLine.append(String.format("\ts.waitFrames(%d);\n", 
                    waitCount));
            waitCount = 0;
        }
        switch(op)
        {
            // Ignore header; will be automatically added on
            case 0x00:
                break;
            // Show line
            case 0x02:
                lineIndex = toShort(args[0], args[1]);
                String line = IOUtils.escape(lin.getString(lineIndex));
                perLine.append(String.format("\ts.showLine(\"%s\");    // \"%s\"\n", 
                                        line, line));
                break;
            // Play movie
            case 0x05:
                perLine.append(String.format("\ts.playMovie(%d, %b);\n", 
                                        toByte(args[0]), args[1] == 0x01));
                break;
            // Character voice
            case 0x08:
                currentSpeaker = getCharacter(args[0]);
                perLine.append(String.format("\ts.playVoice(%s, %d, %d);\n",
                        currentSpeaker, 
                        toByte(args[1]), 
                        toShort(args[2], args[3])));
                break;
            // BGM
            case 0x09:
                perLine.append(String.format("\ts.playBGM(%d, %d, %d);\n",
                        toByte(args[0]), toByte(args[1]), toByte(args[2])));
                break;
            // Sound effect A
            case 0x0A:
                perLine.append(String.format("\ts.playSoundEffectA(%d, %d);\n",
                        toShort(args[0], args[1]), toByte(args[2])));
                break;
            // Sound effect B
            case 0x0B:
                perLine.append(String.format("\ts.playSoundEffectB(%d, %d);\n",
                        toByte(args[0]), toByte(args[1])));
                break;
            // Evidence management
            case 0x0C:
                switch(args[1])
                {
                    case 0x00:  
                        perLine.append("\ts.resetEvidence();\n");
                        break;
                    case 0x01: 
                        perLine.append(String.format("\ts.addEvidence(%d);\n", toByte(args[0])));
                        break;
                    case 0x02:
                        perLine.append(String.format("\ts.updateEvidence(%d);\n", toByte(args[0])));
                        break;
                }
                break;
            // Character titles
            case 0x0F:
                inAltTitles = true;
                if(args[2] == 0x00)
                    normTitles.add(getCharacter(args[0]));
                else if(args[2] == 0x01)
                    altTitles.add(getCharacter(args[0]));
                break;
            // Character info
            case 0x10:
                perLine.append(String.format("\ts.updateCharacterInfo(%s, %d);\n",
                        getCharacter(args[0]), toByte(args[2])));
                break;
            // Go to script
            case 0x19:
                perLine.append(String.format("\ts.goToScript(%d, %d, %d);\n",
                        toByte(args[0]), toByte(args[1]), toByte(args[2])));
                break;
            // Show sprite
            case 0x1E:
                currentSpeaker = getCharacter(args[1]);
                perLine.append(String.format("\ts.showSprite(%d, %s, %d, %d, %d);\n",
                        toByte(args[0]), currentSpeaker, 
                        toByte(args[2]), toByte(args[3]),
                        toByte(args[4])));
                break;
            // Set speaker
            case 0x21:
                String speaker = getCharacter(args[0]);
                if(args[0] != 0x1C)
                    currentSpeaker = speaker;
                perLine.append(String.format("\ts.setSpeaker(%s);\n",
                        speaker));
                break;
            // Wait for input
            case 0x3A:
                perLine.append("\ts.waitForInput();\n\n\n");
                javascript.append(String.format("\t// Line %d: %s\n", lineIndex + 1, currentSpeaker));
                javascript.append(perLine);
                perLine.setLength(0);
                break;
            // Wait for a frame
            case 0x3B:
                waitCount++;
                break;
            default:
                if(args.length == 0)
                    perLine.append(String.format("\ts.op(0x%02X);\n", op));
                else
                    perLine.append(String.format("\ts.op(0x%02X, %s);\n", op, IOUtils.toString(args)));
        }
    }

    @Override
    public void end()
    {
        if(inAltTitles)
            perLine.append(String.format("\ts.setCharacterTitles(%d, %s);\n", normTitles.toString(), altTitles.toString()));
        if(waitCount > 0)
            perLine.append(String.format("\ts.waitFrames(%d);\n", waitCount));
        if(lin.flag1 != 0 || lin.flag2 != 0)
            perLine.append(String.format("\ts.setFlags(0x%02X, 0x%02X);\n", lin.flag1, lin.flag2));
        if(currentSpeaker != null)
            javascript.append(String.format("\t// Line %d: %s\n", lineIndex + 1, currentSpeaker));
        javascript.append(perLine);

        lineIndex++;
        if(lineIndex < lin.stringCount())
            javascript.append("\n\n\t// Extra lines\n");
        while(lineIndex < lin.stringCount())
        {
            String line = IOUtils.escape(lin.getString(lineIndex));
            javascript.append(String.format("\ts.addLine(\"%s\");    // \"%s\"\n", 
                                    line, line));
            lineIndex++;
        }
    }
}