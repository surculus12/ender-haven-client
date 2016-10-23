package haven.events;


import java.util.Queue;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;

public class AdapterKey extends KeyAdapter {
    private Queue<InputEvent> events;

    public AdapterKey(Queue<InputEvent> events) {
        this.events = events;
    }

    public void keyPressed(final KeyEvent e) {
        if (e.isModifierKey())
            return;
        synchronized (events) {
            events.add(e);
        }
    }

    public void keyReleased(final KeyEvent e) {
        if (e.isModifierKey())
            return;
        synchronized (events) {
            events.add(e);
        }
    }
}
