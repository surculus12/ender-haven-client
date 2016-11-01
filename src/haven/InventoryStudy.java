package haven;

import java.awt.*;

public class InventoryStudy extends Inventory {
    private static final Resource studyalarmsfx = Resource.local().loadwait("sfx/study");

    public InventoryStudy(Coord sz) {
        super(sz);
    }

    @Override
    public void addchild(Widget child, Object... args) {
        super.addchild(child, args);
    }

    @Override
    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (!(w instanceof WItem))
            return;
        GItem item = ((WItem) w).item;
        try {
            Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
            if (ci != null && item.meter >= 99) {
                Resource.Tooltip tt = item.resource().layer(Resource.Tooltip.class);
                if (tt != null)
                    gameui().syslog.append(tt.t + " LP: " + ci.exp, Color.LIGHT_GRAY);

                if (Config.studyalarm)
                    Audio.play(studyalarmsfx, Config.studyalarmvol);

                if (Config.autostudy) {
                    Window invwnd = gameui().getwnd("Inventory");
                    Window cupboard = gameui().getwnd("Cupboard");
                    Resource res = item.resource();
                    if (res != null) {
                        if (!replacecurio(invwnd, res, ((WItem) w).c) && cupboard != null)
                            replacecurio(cupboard, res, ((WItem) w).c);
                    }
                }
            }
        } catch (Loading l) {
        }
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        return Config.studylock ? false : super.mousedown(c, button);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (Config.studylock && msg.equals("invxf")) {
            return;
        } else if (Config.studylock && msg.equals("drop")) {
            Coord c = (Coord) args[0];
            for (Widget witm = lchild; witm != null; witm = witm.prev) {
                if (witm instanceof WItem) {
                    WItem itm = (WItem) witm;
                    for (int x = itm.c.x; x < itm.c.x + itm.sz.x; x += Inventory.sqsz.x) {
                        for (int y = itm.c.y; y < itm.c.y + itm.sz.y; y += Inventory.sqsz.y) {
                            if (x / Inventory.sqsz.x == c.x && y / Inventory.sqsz.y == c.y)
                                return;
                        }
                    }
                }
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    private boolean replacecurio(Window wnd, Resource res, Coord c) {
        try {
            for (Widget invwdg = wnd.lchild; invwdg != null; invwdg = invwdg.prev) {
                if (invwdg instanceof Inventory) {
                    Inventory inv = (Inventory) invwdg;
                    for (Widget witm = inv.lchild; witm != null; witm = witm.prev) {
                        if (witm instanceof WItem) {
                            GItem ngitm = ((WItem) witm).item;
                            Resource nres = ngitm.resource();
                            if (nres != null && nres.name.equals(res.name)) {
                                ngitm.wdgmsg("take", witm.c);
                                wdgmsg("drop", c.add(sqsz.div(2)).div(invsq.sz()));
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        } catch (Exception e) { // ignored
        }
        return false;
    }
}
