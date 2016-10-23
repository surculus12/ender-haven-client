package haven.events;

import java.awt.Component;

import jogamp.newt.awt.event.AWTNewtEventFactory;

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.event.KeyEvent;

public class NEWT2AWT {
    private static final int KEY_NOT_FOUND = 0xFFFFFFFF;
    private static final IntIntHashMap newt2AWTEventMap = new IntIntHashMap() {
        {
            setKeyNotFoundValue(KEY_NOT_FOUND);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED, java.awt.event.MouseEvent.MOUSE_CLICKED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED, java.awt.event.MouseEvent.MOUSE_PRESSED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED, java.awt.event.MouseEvent.MOUSE_RELEASED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED, java.awt.event.MouseEvent.MOUSE_MOVED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_ENTERED, java.awt.event.MouseEvent.MOUSE_ENTERED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_EXITED, java.awt.event.MouseEvent.MOUSE_EXITED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED, java.awt.event.MouseEvent.MOUSE_DRAGGED);
            put(com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED, java.awt.event.MouseEvent.MOUSE_WHEEL);
            put(com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED, java.awt.event.KeyEvent.KEY_PRESSED);
            put(com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED, java.awt.event.KeyEvent.KEY_RELEASED);
        }
    };
    private static final Component dummycomp = new Component() {};


    public static java.awt.event.MouseEvent convert(com.jogamp.newt.event.MouseEvent event) {
        int id = newt2AWTEventMap.get(event.getEventType());
        if (id == KEY_NOT_FOUND)
            return null;

        int mods = getAwtModifiers(event.getModifiers());

        if (id == java.awt.event.MouseEvent.MOUSE_WHEEL) {
            float[] rot = event.getRotation();
            int rotval = event.isShiftDown() ? (int) -rot[0] : (int) -rot[1];

            return new java.awt.event.MouseWheelEvent(dummycomp,
                    id,
                    event.getWhen(),
                    mods,
                    event.getX(), event.getY(),
                    event.getClickCount(),
                    false,
                    java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    rotval,
                    rotval);
        } else {
            int button;
            switch (event.getButton()) {
                case com.jogamp.newt.event.MouseEvent.BUTTON1:
                    button = java.awt.event.MouseEvent.BUTTON1;
                    break;
                case com.jogamp.newt.event.MouseEvent.BUTTON2:
                    button = java.awt.event.MouseEvent.BUTTON2;
                    break;
                case com.jogamp.newt.event.MouseEvent.BUTTON3:
                    button = java.awt.event.MouseEvent.BUTTON3;
                    break;
                default:
                    button = 0;
            }

            return new java.awt.event.MouseEvent(dummycomp,
                    id,
                    event.getWhen(),
                    mods,
                    event.getX(),
                    event.getY(),
                    event.getClickCount(),
                    false,
                    button);
        }
    }

    public static java.awt.event.KeyEvent convert(com.jogamp.newt.event.KeyEvent event) {
        int id = newt2AWTEventMap.get(event.getEventType());
        if (id == KEY_NOT_FOUND)
            return null;

        return new java.awt.event.KeyEvent(dummycomp,
                id,
                event.getWhen(),
                getAwtModifiers(event.getModifiers()),
                AWTNewtEventFactory.newtKeyCode2AWTKeyCode(event.getKeyCode()),
                event.getKeyChar(),
                java.awt.event.KeyEvent.KEY_LOCATION_STANDARD);
    }

    public static java.awt.event.KeyEvent convertToTyped(com.jogamp.newt.event.KeyEvent event) {
        int id = newt2AWTEventMap.get(event.getEventType());
        if (id == KEY_NOT_FOUND)
            return null;

        // enter is defined as '\r' in NEWT rather than '\n' in AWT
        char c = event.getKeyChar();
        if (c == KeyEvent.VK_ENTER)
            c = '\n';

        try {
            // code is 0 since internal haven code assumes keyCode to be 0 for typed event
            // (while NEWTs released event has a proper keyCode set (for printable keys))
            return new java.awt.event.KeyEvent(dummycomp,
                    java.awt.event.KeyEvent.KEY_TYPED,
                    event.getWhen(),
                    getAwtModifiers(event.getModifiers()),
                    0,
                    c);
        } catch (IllegalArgumentException e) {
            // if keyChar is CHAR_UNDEFINED or if keyCode is not VK_UNDEFINED
            return null;
        }
    }

    private static final int getAwtModifiers(int newtMods) {
        int awtMods = 0;
        if ((newtMods & com.jogamp.newt.event.InputEvent.SHIFT_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.CTRL_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.CTRL_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.META_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.META_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.ALT_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.ALT_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.ALT_GRAPH_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.BUTTON1_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.BUTTON2_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.BUTTON2_DOWN_MASK;

        if ((newtMods & com.jogamp.newt.event.InputEvent.BUTTON3_MASK) != 0)
            awtMods |= java.awt.event.InputEvent.BUTTON3_DOWN_MASK;

        return awtMods;
    }
}