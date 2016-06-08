package haven.combat;

import haven.Buff;
import haven.Coord;
import haven.GOut;
import haven.Resource;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Defences {

    private static final Coord bsz = new Coord(20, 8);
    private static final Coord pad = new Coord(5, 5);
    private static final Coord step = bsz.add(pad);

    private final Map<AttackType, List<Buff>> defs = new HashMap<AttackType, List<Buff>>();
    private final int invert;

    public Defences(boolean invert) {
	this.invert = invert ? -1 : 1;
    }

    public void addBuff(Buff buff) {
	Resource.Image img = buff.img();
	if(img != null) {
	    int attackType = AttackType.get(img.img);
	    for (AttackType type : AttackType.values()) {
		if((type.value & attackType) != 0) {
		    List<Buff> buffs = defs.get(type);
		    if(buffs == null) {
			buffs = new ArrayList<Buff>();
			defs.put(type, buffs);
		    }
		    buffs.add(buff);
		}
	    }
	}
    }

    public boolean isEmpty() {
	for (Map.Entry<AttackType, List<Buff>> e : defs.entrySet()) {
	    if(!e.getValue().isEmpty()) {return false;}
	}
	return true;
    }

    public void draw(GOut g, Coord c) {
	int y = bsz.y;
	for (AttackType type : defs.keySet()) {
	    int x = 0;
	    for (Buff buff : defs.get(type)) {
		g.chcolor(type.color, 200);
		g.frect(c.add(x, -y), new Coord((buff.ameter * bsz.x) / 100, bsz.y));
		g.chcolor(Color.BLACK);
		g.rect(c.add(x, -y), bsz);
		g.chcolor();
		x += step.x * invert;
	    }
	    y += step.y;
	}
    }

    public Coord sz() {
	Coord sz = new Coord();
	int y = 0;
	for (AttackType type : defs.keySet()) {
	    int x = step.x * defs.get(type).size();
	    sz.x = Math.max(sz.x, x);
	    y += step.y;
	}
	sz.y = y;
	return sz.sub(pad);
    }
}
