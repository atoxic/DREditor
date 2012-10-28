package dreditor.lin;

import java.util.*;

import dreditor.*;

/**
 * Used to extract lines from LINScripts
 * @author /a/nonymous scanlations
 */
public class LineExtractor implements InstructionVisitor
{
    private LINScript lin;
    
    private StringBuilder lines;
    private String currentSpeaker;
    
    public LineExtractor(LINScript _lin)
    {
        lin = _lin;
        
        lines = new StringBuilder();
        currentSpeaker = null;
    }
    
    @Override
    public String toString()
    {
        return(lines.toString());
    }
    
    private static String getCharacter(int ID)
    {
        return(Constants.CHARACTERS_INV.get(ID));
    }
    
    private static int toShort(byte hi, byte lo)
    {
        return(((int)hi & 0xFF) << 8 
                | ((int)lo & 0xFF));
    }

    @Override
    public void op(int op, byte[] args)
    {
        switch(op)
        {
            // Show line
            case 0x02:
                int lineIndex = toShort(args[0], args[1]);
                StringBuilder line = new StringBuilder();
                line.append(currentSpeaker).append(": ");
                while(line.length() < 25)
                    line.insert(' ', 0);
                String str = lin.getString(lineIndex);
                str = str.replaceAll("<[^>]+>", "").trim()
                         .replaceAll("\n", "\n                         ");
                line.append(str);
                
                lines.append(line).append('\n');
                break;
            // Show sprite
            case 0x1E:
                currentSpeaker = getCharacter(args[1]);
                break;
            // Set speaker
            case 0x21:
                if(args[0] != 0x1C)
                    currentSpeaker = getCharacter(args[0]);
                break;
        }
    }

    @Override
    public void end()
    {
    }
}