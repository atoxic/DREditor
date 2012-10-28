package dreditor.lin;

/**
 * For iterating through InstructionBins
 * @author /a/nonymous scanlations
 */
public interface InstructionVisitor
{
    public void op(int op, byte[] args);
    public void end();
}
