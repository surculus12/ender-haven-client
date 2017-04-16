package haven.livestock;


import haven.*;
import haven.Label;

import java.awt.*;
import java.util.*;
import java.util.List;


public abstract class Animal extends HashMap<String, Integer> {
    public long gobid;
    public long wndid;
    public String name;
    public String type;

    public Animal(long wndid, String type) {
        super(12);
        this.wndid = wndid;
        this.type = type;
        put("Quality:", null);
        put("Breeding quality:", null);
        put("Meat quantity:", null);
        put("Milk quantity:", null);
        put("Meat quality:", null);
        put("Milk quality:", null);
        put("Hide quality:", null);
        // horse
        put("Endurance:", null);
        put("Stamina:", null);
        put("Metabolism:", null);
        // sheep
        put("Wool quantity:", null);
        put("Wool quality:", null);
    }

    public static int addColumn(Map<String, Column> columns, String name, String displayName, int index, int x) {
        Label lbl = new Label(displayName, Text.std, true) {
            @Override
            public boolean mousedown(Coord c, int button) {
                for (Widget child = parent.lchild; child != null; child = child.prev) {
                    if (child instanceof Label) {
                        Label lbl = ((Label) child);
                        if (lbl.col != Color.WHITE) {
                            lbl.setcolor(Color.WHITE);
                            break;
                        }
                    }
                }
                setcolor(Color.RED);

                Scrollport.Scrollcont cont = ((LivestockManager.Panel) parent).scrollPort.cont;
                List<DetailsWdg> entries = new ArrayList<>();
                for (Widget child = cont.lchild; child != null; child = child.prev)
                    entries.add((DetailsWdg) child);

                Collections.sort(entries, (a, b) -> {
                    int result = a.animal.type.compareTo(b.animal.type);
                    if (result != 0)
                        return result;
                    return a.animal.get(name) < b.animal.get(name) ? -1 : 1;
                });

                int y = 0;
                for (DetailsWdg details : entries) {
                    details.c = new Coord(0, y);
                    y += DetailsWdg.HEIGHT;
                }

                return true;
            }
        };
        columns.put(name, new Column(lbl, index, x));
        return x + lbl.sz.x + 25;
    }

    public abstract boolean hasAllAttributes();

    public abstract void attributeResolved();

    public abstract Tex getAvatar();

    public abstract Map<String, Column> getColumns();
}
