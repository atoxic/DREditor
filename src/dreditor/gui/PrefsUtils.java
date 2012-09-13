package dreditor.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.prefs.*;

/**
 *
 * @author /a/nonymous scanlations
 */
public class PrefsUtils
{
    public static final Preferences PREFS = Preferences.userRoot().node("anonscanlations.dreditor");
    
    private static final HashMap<Window, String> KEYS = new HashMap<Window, String>();

    private static final WindowListener WINDOW_LISTENER = new WindowAdapter()
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            if(KEYS.containsKey(e.getWindow()))
                saveWindowPrefs(KEYS.get(e.getWindow()), e.getWindow());
        }
        @Override
        public void windowClosed(WindowEvent e)
        {
            if(KEYS.containsKey(e.getWindow()))
                saveWindowPrefs(KEYS.get(e.getWindow()), e.getWindow());
        }
    };
    private static final ComponentListener COMPONENT_LISTENER = new ComponentAdapter()
    {
        @Override
        public void componentHidden(ComponentEvent e)
        {
            Component c = e.getComponent();
            if(c instanceof Window && KEYS.containsKey((Window)c))
                saveWindowPrefs(KEYS.get((Window)c), (Window)c);
        }
    };
    
    public static void registerWindow(String key, Window w, boolean size)
    {
        loadWindowPrefs(key, w, size);
        KEYS.put(w, key);
        w.addWindowListener(WINDOW_LISTENER);
        w.addComponentListener(COMPONENT_LISTENER);
    }

    public static void saveWindowPrefs(String key, Window w)
    {
        Preferences node = PREFS.node(key);
        Rectangle bounds = w.getBounds();
        node.putInt("x", bounds.x);
        node.putInt("y", bounds.y);
        node.putInt("width", bounds.width);
        node.putInt("height", bounds.height);
    }

    public static void loadWindowPrefs(String key, Window w, boolean size)
    {
        try
        {
            if(PREFS.nodeExists(key))
            {
                Preferences node = PREFS.node(key);
                Rectangle bounds = w.getBounds();
                bounds.x = node.getInt("x", bounds.x);
                bounds.y = node.getInt("y", bounds.y);
                if(size)
                {
                    bounds.width = node.getInt("width", bounds.width);
                    bounds.height = node.getInt("height", bounds.height);
                }
                w.setBounds(bounds);
            }
        }
        catch(BackingStoreException bse)
        {
        }
    }
}
