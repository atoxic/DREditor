package dreditor.gui;

import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;

import dreditor.*;
import dreditor.gim.*;

/**
 *
 * @author Administrator
 */
public class GUIUtils
{
    //public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("dreditor/gui/Bundle");
    public static ResourceBundle BUNDLE;
    
    private static final Map<AttributedCharacterIterator.Attribute, Object> map = new HashMap<>();
    static
    {
        map.put(TextAttribute.SIZE, new Float(12.0));
    }
    
    public static void initGUI()
    {
        //Locale.setDefault(Locale.CHINA);
        BUNDLE = ResourceBundle.getBundle("dreditor/gui/Bundle");
        
        // Try to use native look and feel
        try
        {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e)
        {
            // couldn't find native look and feel: it couldn't be helped
        }
        
        GUI g = new GUI();
        g.setVisible(true);
    }
    
    public static boolean confirm(String msg)
    {
        return(JOptionPane.showConfirmDialog(null, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
    }
    public static void warning(String msg)
    {
        JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    public static void error(String msg)
    {
        JOptionPane.showMessageDialog(null, msg, "Error!", JOptionPane.ERROR_MESSAGE);
    }
    
    public static BufferedImage generateStartupImage(int width, int height, String version, String authors, String comments) throws IOException
    {
        BufferedImage img1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img1.createGraphics();
        g2d.setColor(Color.white);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        TextContext ctx = new TextContext(g2d, width);
        drawText(ctx, "Build ver. " + version, 2);
        drawText(ctx, "umdimage.dat pack time: " + new Date(), 2);
        drawText(ctx, "Authors: /a/nonymous scanlations", 2);
        if(authors != null && authors.trim().length() > 0)
            drawText(ctx, authors, 50);
        if(comments != null && comments.trim().length() > 0)
        {
            drawText(ctx, "Comments: ", 2);
            drawText(ctx, comments, 50);
        }
        return(img1);
    }
    
    private static class TextContext
    {
        private Graphics2D g2d;
        private float drawPosY;
        private int width;
        private TextContext(Graphics2D _g2d, int _width)
        {
            g2d = _g2d;
            drawPosY = 0;
            width = _width;
        }
    };
    
    private static void drawText(TextContext ctx, String string, int margin)
    {
        for(String s : string.split("\n"))
        {
            AttributedString text = new AttributedString(s, map);
            AttributedCharacterIterator paragraph = text.getIterator();
            int paragraphStart = paragraph.getBeginIndex();
            int paragraphEnd = paragraph.getEndIndex();
            LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, new FontRenderContext(null, false, false));

            lineMeasurer.setPosition(paragraphStart);
            while(lineMeasurer.getPosition() < paragraphEnd)
            {
                TextLayout layout = lineMeasurer.nextLayout(ctx.width - 2 - margin);
                ctx.drawPosY += layout.getAscent();

                float drawPosX;
                if(layout.isLeftToRight())
                    drawPosX = 0;
                else
                    drawPosX = ctx.width - layout.getAdvance();

                layout.draw(ctx.g2d, drawPosX + margin, ctx.drawPosY);
                ctx.drawPosY += layout.getDescent() + layout.getLeading();
            }
        }
    }
}
