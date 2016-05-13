package haven.combat;

import haven.*;

import java.util.Set;

public class CombatInfo extends GobInfo {
    public static final int PAD = 3;

    private Defences defences;

    public CombatInfo(Gob owner) {
	super(owner);
	up = 10;
	center = new Pair<>(0.5, 1.0);
    }

    public void defences(Set<Buff> buffs) {
	if(buffs.isEmpty()) {return;}
	defences = new Defences(false);
	for (Buff buff : buffs) {
	    if(!buff.dest) {defences.addBuff(buff);}
	}
    }

    @Override
    public boolean setup(RenderList d) {
	super.setup(d);
	return true;
    }

    @Override
    public void cdraw(GOut g, Coord sc) {
	super.cdraw(g, sc);
	if(defences != null) {
	    Coord sz = defences.sz();
	    g.chcolor(0, 0, 0, 128);
	    Coord bl = sc.add(10, 0);
	    g.frect(bl.sub(PAD, sz.y + PAD), sz.add(2*PAD, 2*PAD));
	    g.chcolor();
	    defences.draw(g, bl);
	    defences = null;
	}
	defences = null;
    }


    public boolean hasInfo() {
	return defences != null;
    }

    @Override
    protected Tex render() {
	return null;
    }
}
