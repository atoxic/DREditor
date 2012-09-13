package dreditor.lin;

import java.io.*;
import java.nio.*;
import java.util.*;

import dreditor.*;

/**
 * BIN for containing LIN instructions
 * @author /a/nonymous scanlations
 */
public class InstructionBin implements BinPart, InstructionListener
{
    // Opcodes -> number of arguments (-1 = variable arguments)
    public static final Map<Integer, Integer> NUMARGS = new HashMap<>();
    
    static
    {
        NUMARGS.put(0x00, 2);   // Header
        NUMARGS.put(0x01, 3);
        NUMARGS.put(0x02, 2);   // Show line
        NUMARGS.put(0x03, 1);
        NUMARGS.put(0x04, 4);
        NUMARGS.put(0x05, 2);   // Play movie
        NUMARGS.put(0x06, 8);
        NUMARGS.put(0x08, 5);   // Play voice
        NUMARGS.put(0x09, 3);   // Play BGM
        NUMARGS.put(0x0A, 3);   // Play sound effect A
        NUMARGS.put(0x0B, 2);   // Play sound effect B
        NUMARGS.put(0x0C, 2);   // Evidence management
        NUMARGS.put(0x0D, 3);       // Something to do with presents
        NUMARGS.put(0x0E, 2); 
        NUMARGS.put(0x0F, 3);   // Set character title
        NUMARGS.put(0x10, 3);   // Set character info
        NUMARGS.put(0x11, 4);       // Something to do with multiple-choice
        NUMARGS.put(0x14, 3);       // Camera movement in trial sections?
        NUMARGS.put(0x15, 3);       // Places?
        NUMARGS.put(0x19, 3);   // Go to script
        NUMARGS.put(0x1A, 0);       // Cleanup?
        NUMARGS.put(0x1B, 3);
        NUMARGS.put(0x1C, 0);       // Cleanup?
        NUMARGS.put(0x1E, 5);
        NUMARGS.put(0x1F, 7);       // Screen flash effect?
        NUMARGS.put(0x20, 5);
        NUMARGS.put(0x21, 1);
        NUMARGS.put(0x22, 3);
        NUMARGS.put(0x23, 5);
        NUMARGS.put(0x25, 2);
        NUMARGS.put(0x26, 3);       // If arg[0] == 0x10, then it's marking people who've died
        NUMARGS.put(0x27, 1);       // Character interaction?
        NUMARGS.put(0x29, 1);       // Object interaction?
        NUMARGS.put(0x2A, 2);
        NUMARGS.put(0x2B, 1);       // Something to do with yes/no
        NUMARGS.put(0x2E, 2);
        NUMARGS.put(0x30, 3);       // arg[1] = character
        NUMARGS.put(0x33, 4);
        NUMARGS.put(0x34, 2);
        NUMARGS.put(0x35, -1);      // Object select/script flow?
        NUMARGS.put(0x36, -1);
        NUMARGS.put(0x38, 5);
        NUMARGS.put(0x39, 5);
        NUMARGS.put(0x3A, 0);
        NUMARGS.put(0x3B, 0);
        NUMARGS.put(0x3C, 0);
    };
    
    private int stringCount, count;
    private ByteArrayOutputStream out;
    private ByteBuffer source;
    
    public InstructionBin()
    {
        out = new ByteArrayOutputStream();
        stringCount = 0;
        count = 0;
    }
    public InstructionBin(ByteBuffer buf) throws LINParseException
    {
        this();
        source = buf.duplicate();
        parse(buf.duplicate(), this);
    }
    
    /**
     * Gives the original ByteBuffer that this IntructionBin was made with (if it was made with the InstructionBin(ByteBuffer buf) constructor).
     * Used if the Instruction was misidentified.
     * @return  Original ByteBuffer
     */
    public ByteBuffer getSource()
    {
        return(source);
    }
    
    private static void dumpBytes(ByteBuffer buf, int num)
    {
        System.err.print("bytes: ");
        for(int j = 0; j < num && buf.hasRemaining(); j++)
            System.err.printf("%02X ", buf.get());
        System.err.println("");
    }
    
    public static void parse(ByteBuffer buf, InstructionListener listener) throws LINParseException
    {
        while(buf.hasRemaining())
        {
            buf.mark();
            
            int head = buf.get();
            if(head == 0x00)
                break;
            else if(head != 0x70)
            {
                buf.reset();
                //dumpBytes(buf, 20);
                throw new LINParseException("Not 70: " + Integer.toHexString(head));
            }
            
            int op = buf.get();
            if(!NUMARGS.containsKey(op))
            {
                buf.reset();
                //dumpBytes(buf, 20);
                throw new LINParseException("Unknown OP: " + Integer.toHexString(op));
            }
            
            int argc = NUMARGS.get(op);
            if(argc == -1)
            {
                buf.mark();
                argc = 0;
                while(buf.get() != 0x70)
                    argc++;
                buf.reset();
            }
            byte[] args = new byte[argc];
            buf.get(args);
            listener.op(op, args);
        }
        listener.end();
    }
    
    public void iterate(InstructionListener listener) throws LINParseException
    {
        parse(ByteBuffer.wrap(out.toByteArray()), listener);
    }
    
    @Override
    public void op(int op, byte[] args)
    {
        if(op == 0x00)
            return;
        
        count++;
        if(op == 0x02)
            stringCount++;
        out.write(0x70);
        out.write(op);
        for(byte b : args)
            out.write(b);
    }
    
    @Override
    public void end()
    {
        // Do nothing
    }
    
    public int count()
    {
        return(count);
    }

    @Override
    public ByteBuffer getBytes() throws IOException
    {
        ByteBuffer bb = ByteBuffer.allocate(4 + out.size());

        // Header
        bb.put((byte)0x70);
        bb.put((byte)0x00);
        bb.put((byte)stringCount);
        bb.put((byte)(stringCount >> 8));

        bb.put(out.toByteArray());
        bb.flip();
        return(bb);
    }
    
    @Override
    public List<String> toJSFunction(String root, String ID) throws IOException
    {
        throw new UnsupportedOperationException("Invalid operation");
    }
    
    @Override
    public String toJSValue(String root, String ID) throws IOException
    {
        throw new UnsupportedOperationException("Invalid operation");
    }
}
