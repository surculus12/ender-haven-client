package haven.events;

import java.util.Queue;

import haven.*;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

public class AdapterMouse extends MouseAdapter {
    private Queue<InputEvent> events;
    private MainFrame mainframe;

    public AdapterMouse(Queue<InputEvent> events, MainFrame mainframe) {
        this.events = events;
        this.mainframe = mainframe;
    }

    public void mousePressed(final MouseEvent e) {
        synchronized (events) {
            events.add(e);
        }
    }

    public void mouseReleased(final MouseEvent e) {
        synchronized (events) {
            events.add(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        mainframe.mousemv = e;
    }

    public void mouseDragged(MouseEvent e) {
        mainframe.mousemv = e;
    }

    public void mouseWheelMoved(MouseEvent e) {
        synchronized (events) {
            events.add(e);
        }
    }
}
