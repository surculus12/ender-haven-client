package haven;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class GobDamageInfo extends GobInfo {
    private static Map<Long, Integer> gobDamage = new LinkedHashMap<Long, Integer>() {
	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > 50;
	}
    };

    private int damage = 0;

    public GobDamageInfo(Gob owner) {
	super(owner);
	up = 3;
	center = new Pair<>(0.5, 1.0);
	if(gobDamage.containsKey(gob.id)) {
	    damage = gobDamage.get(gob.id);
	}
    }

    @Override
    public boolean setup(RenderList d) {
	return super.setup(d);
    }

    @Override
    protected Tex render() {
	if(damage > 0) {
	    return Text.std.renderstroked(String.format("%d", damage), Color.RED, Color.BLACK).tex();
	}
	return null;
    }

    public void update(int v) {
	damage += v;
	gobDamage.put(gob.id, damage);
	tex = render();
    }
}
