/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.*;
import java.util.List;

import static haven.CharWnd.attrf;
import static haven.Window.wbox;
import static haven.Inventory.invsq;

public class FightWnd extends Widget {
    public final int nsave;
    public int maxact;
    public final Actions actlist;
    public List<Action> acts = new ArrayList<Action>();
    public final Action[] order;
    private final List<Pair<Text, Integer>> saves;
    private final CharWnd.LoadingTextBox info;
    private Tex count;
    private Dropbox<Pair<Text, Integer>> schoolsDropdown;
    private static final Text.Foundry cardnum = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 12).aa(true);

    public class Action {
        public final Indir<Resource> res;
        private final int id;
        public int a, u;
        private Text rnm, ru, ra;
        private Tex ri;

        public Action(Indir<Resource> res, int id, int a, int u) {
            this.res = res;
            this.id = id;
            this.a = a;
            this.u = u;
        }

        public String rendertext() {
            StringBuilder buf = new StringBuilder();
            Resource res = this.res.get();
            buf.append("$img[" + res.name + "]\n\n");
            buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n");
            Resource.Pagina pag = res.layer(Resource.pagina);
            if (pag != null)
                buf.append(pag.text);
            return (buf.toString());
        }

        private void a(int a) {
            if(this.a != a) {
                this.a = a;
                this.ru = null;
                this.ra = null;
            }
        }

        private void u(int u) {
            if(this.u != u && u <= a) {
                this.u = u;
                this.ru = null;
                recount();
            }
        }
    }

    private void recount() {
        int u = 0;
        for (Action act : acts)
            u += act.u;
        count = cardnum.render(String.format("= %d/%d", u, maxact), (u > maxact) ? Color.RED : Color.WHITE).tex();
    }

    private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
            Resource.loadtex("gfx/hud/buttons/addd")};
    private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
            Resource.loadtex("gfx/hud/buttons/subd")};

    public class Actions extends Listbox<Action> {
        private boolean loading = false;
        UI.Grab d = null;
        Action drag = null;
        Coord dp;

        public Actions(int w, int h) {
            super(w, h, attrf.height() + 2);
        }

        protected Action listitem(int n) {
            return (acts.get(n));
        }

        protected int listitems() {
            return (acts.size());
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Action act, int idx) {
            g.chcolor((idx % 2 == 0) ? CharWnd.every : CharWnd.other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            try {
                if (act.ri == null)
                    act.ri = new TexI(PUtils.convolvedown(act.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
                g.image(act.ri, Coord.z);
            } catch (Loading l) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
            }
            int ty = (itemh - act.rnm.sz().y) / 2;
            g.image(act.rnm.tex(), new Coord(itemh + 2, ty));

            if (act.ra == null)
                act.ra = cardnum.render(String.valueOf(act.a));
            g.aimage(act.ra.tex(), new Coord(sz.x - 15, ty), 1.0, 0.0);
        }

        public void change(final Action act) {
            if (act != null)
                info.settext(new Indir<String>() {
                    public String get() {
                        return (act.rendertext());
                    }
                });
            else if (sel != null)
                info.settext("");
            super.change(act);
        }

        public void draw(GOut g) {
            if (loading) {
                loading = false;
                for (Action act : acts) {
                    try {
                        Resource res = act.res.get();
                        act.rnm = attrf.render(res.layer(Resource.tooltip).t);
                    } catch (Loading l) {
                        act.rnm = attrf.render("...");
                        loading = true;
                    }
                }

                Collections.sort(acts, new Comparator<Action>() {
                    public int compare(Action a, Action b) {
                        int ret = a.rnm.text.compareTo(b.rnm.text);
                        return (ret);
                    }
                });
            }
            if((drag != null) && (dp == null)) {
                try {
                    final Tex dt = drag.res.get().layer(Resource.imgc).tex();
                    ui.drawafter(new UI.AfterDraw() {
                        public void draw(GOut g) {
                            g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
                        }
                    });
                } catch(Loading l) {}
            }
            super.draw(g);
        }

        public boolean mousedown(Coord c, int button) {
            if (button == 1) {
                super.mousedown(c, button);
                if ((sel != null) && (c.x < sb.c.x)) {
                    d = ui.grabmouse(this);
                    drag = sel;
                    dp = c;
                }
                return(true);
            }
            return (super.mousedown(c, button));
        }

        public void mousemove(Coord c) {
            super.mousemove(c);
            if((drag != null) && (dp != null)) {
                if(c.dist(dp) > 5)
                    dp = null;
            }
        }

        public boolean mouseup(Coord c, int button) {
            if((d != null) && (button == 1)) {
                d.remove();
                d = null;
                if(drag != null) {
                    if(dp == null)
                        ui.dropthing(ui.root, c.add(rootpos()), drag);
                    drag = null;
                }
                return(true);
            }
            return(super.mouseup(c, button));
        }
    }

    public static final String[] keys = {"1", "2", "3", "4", "5", "\u21e71", "\u21e72", "\u21e73", "\u21e74", "\u21e75"};
    public class BView extends Widget implements DropTarget {
        private int subp = -1;
        private int addp = -1;
        private final int subOffX = 3;
        private final int addOffX = 16;
        private final int subOffY = invsq.sz().y + 10 + 10;


        private BView() {
            super(new Coord(((invsq.sz().x + 2) * (order.length - 1)) + (10 * ((order.length - 1) / 5)) + 60, 0).add(invsq.sz().x, invsq.sz().y + 35));
        }

        private Coord itemc(int i) {
            return(new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 5)), 0));
        }

        private int citem(Coord c) {
            for(int i = 0; i < order.length; i++) {
                if(c.isect(itemc(i), invsq.sz()))
                    return(i);
            }
            return(-1);
        }

        private int csub(Coord c) {
            for(int i = 0; i < order.length; i++) {
                if(c.isect(itemc(i).add(subOffX, subOffY), sub[0].sz()))
                    return(i);
            }
            return(-1);
        }

        private int cadd(Coord c) {
            for(int i = 0; i < order.length; i++) {
                if(c.isect(itemc(i).add(addOffX, subOffY), add[0].sz()))
                    return(i);
            }
            return(-1);
        }

        final Tex[] keys = new Tex[10];
        {
            for(int i = 0; i < 10; i++)
                this.keys[i] = Text.render(FightWnd.keys[i]).tex();
        }

        public void draw(GOut g) {
            int pcy = invsq.sz().y + 4;

            for(int i = 0; i < order.length; i++) {
                Coord c = itemc(i);
                g.image(invsq, c);
                Action act = order[i];
                try {
                    if(act != null) {
                        g.image(act.res.get().layer(Resource.imgc).tex(), c.add(1, 1));

                        if (act.ru == null)
                            act.ru = cardnum.render(String.format("%d/%d", act.u, act.a));

                        g.image(act.ru.tex(), c.add(invsq.sz().x / 2 - act.ru.sz().x / 2, pcy));
                        g.chcolor();

                        g.image(sub[subp == i ? 1 : 0], c.add(subOffX, subOffY));
                        g.image(add[addp == i ? 1 : 0], c.add(addOffX, subOffY));
                    }
                } catch(Loading l) {}
                g.chcolor(156, 180, 158, 255);
                g.aimage(keys[i], c.add(invsq.sz().sub(2, 0)), 1, 1);
                g.chcolor();
            }

            g.image(count, new Coord(370, pcy));
        }

        public boolean mousedown(Coord c, int button) {
            int s = citem(c);

            if(button == 3) {
                if(s >= 0) {
                    if(order[s] != null)
                        order[s].u(0);
                    order[s] = null;
                    return(true);
                }
            } else if (button == 1) {
                int acti = csub(c);
                if (acti >= 0) {
                    subp = acti;
                    return true;
                }
                acti = cadd(c);
                if (acti >= 0) {
                    addp = acti;
                    return true;
                }
            }
            return(super.mousedown(c, button));
        }

        public boolean mouseup(Coord c, int button) {
            subp = -1;
            addp = -1;

            int s = csub(c);
            if (s >= 0) {
                Action act = order[s];
                if (act != null) {
                    if (act.u == 1) {
                        if (order[s] != null)
                            order[s].u(0);
                        order[s] = null;
                    } else {
                        act.u(act.u - 1);
                    }
                    return true;
                }
            }

            s = cadd(c);
            if (s >= 0) {
                Action act = order[s];
                if (act != null) {
                    act.u(act.u + 1);
                    return true;
                }
            }

            return(super.mouseup(c, button));
        }

        public boolean dropthing(Coord c, Object thing) {
            if(thing instanceof Action) {
                Action act = (Action)thing;
                int s = citem(c);
                if(s < 0)
                    return(false);
                if(order[s] != act) {
                    if(order[s] != null)
                        order[s].u(0);
                    order[s] = act;
                    for(int i = 0; i < order.length; i++) {
                        if(i == s)
                            continue;
                        if(order[i] == act)
                            order[i] = null;
                    }
                    if(act.u < 1)
                        act.u(1);
                }
                return(true);
            }
            return(false);
        }
    }

    @RName("fmg")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return(new FightWnd((Integer)args[0], (Integer)args[1], (Integer)args[2]));
        }
    }

    public void load(int n) {
        wdgmsg("load", n);
    }

    public void save(int n) {
        List<Object> args = new LinkedList<Object>();
        args.add(n);
        for(int i = 0; i < order.length; i++) {
            if(order[i] == null) {
                args.add(null);
            } else {
                args.add(order[i].id);
                args.add(order[i].u);
            }
        }
        wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
        wdgmsg("use", n);
    }

    public FightWnd(int nsave, int nact, int max) {
        super(Coord.z);
        this.nsave = nsave;
        this.maxact = max;
        this.order = new Action[nact];
        this.saves = new ArrayList<>(nsave);
        for (int i = 0; i < nsave; i++)
            saves.add(i, new Pair<>(unused, i));

        schoolsDropdown = new Dropbox<Pair<Text, Integer>>(250, saves.size(), saves.get(0).a.sz().y) {
            @Override
            protected Pair<Text, Integer> listitem(int i) {
                return saves.get(i);
            }

            @Override
            protected int listitems() {
                return saves.size();
            }

            @Override
            protected void drawitem(GOut g, Pair<Text, Integer> item, int i) {
                g.image(item.a.tex(), Coord.z);
            }

            @Override
            public void change(Pair<Text, Integer> item) {
                super.change(item);
                load(item.b);
            }
        };

        info = add(new CharWnd.LoadingTextBox(new Coord(255, 152), "", CharWnd.ifnd), new Coord(0, 35).add(wbox.btloff()));

        info.bg = new Color(0, 0, 0, 128);
        Frame.around(this, Collections.singletonList(info));

        add(new Img(CharWnd.catf.render(Resource.getLocString(Resource.BUNDLE_LABEL,"Martial Arts & Combat Schools")).tex()), 0, 0);
        actlist = add(new Actions(235, Config.iswindows ? 7 : 8), new Coord(276, 35).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(actlist));
        Widget p = add(new BView(), 77, 200);

        add(schoolsDropdown, new Coord(10, 280));
        Frame.around(this, Collections.singletonList(schoolsDropdown));

        add(new Button(110, "Save", false) {
            public void click() {
                Pair<Text, Integer> sel = schoolsDropdown.sel;
                save(sel.b);
                use(sel.b);
            }
        }, 280, 277);
        add(new Button(110, "Rename", false) {
            public void click() {
                Pair<Text, Integer> sel = schoolsDropdown.sel;
                if (sel.a.text.equals("Unused save"))
                     return;

                SchoolRenameWnd renwnd = new SchoolRenameWnd("Rename School", schoolsDropdown, saves, sel.b, sel.a.text);
                GameUI gui = gameui();
                gui.add(renwnd, new Coord(gui.sz.x / 2 - 200, gui.sz.y / 2 - 200));
                renwnd.show();
            }
        }, 405, 277);

        pack();
    }

    public Action findact(int resid) {
        for(Action act : acts) {
            if(act.id == resid)
                return(act);
        }
        return(null);
    }

    private final Text unused = new Text.Foundry(attrf.font.deriveFont(java.awt.Font.ITALIC)).aa(true).render("unused save");

    public void uimsg(String nm, Object... args) {
        if (nm == "avail") {
            List<Action> acts = new ArrayList<Action>();
            int a = 0;
            while (true) {
                int resid = (Integer) args[a++];
                if (resid < 0)
                    break;
                int av = (Integer) args[a++];
                Action pact = findact(resid);
                if(pact == null) {
                    acts.add(new Action(ui.sess.getres(resid), resid, av, 0));
                } else {
                    acts.add(pact);
                    pact.a(av);
                }
            }
            this.acts = acts;
            actlist.loading = true;
        } else if(nm == "used") {
            int a = 0;
            for(Action act : acts)
                act.u(0);
            for(int i = 0; i < order.length; i++) {
                int resid = (Integer)args[a++];
                if(resid < 0) {
                    order[i] = null;
                    continue;
                }
                int us = (Integer)args[a++];
                (order[i] = findact(resid)).u(us);
            }
        } else if (nm == "saved") {
            int fl = (Integer) args[0];
            for (int i = 0; i < nsave; i++) {
                if ((fl & (1 << i)) != 0)
                    saves.set(i, new Pair<>(attrf.render(String.format("Saved school %d", i + 1)), i));
                else
                    saves.set(i, new Pair<>(unused, i));
            }
            // override school names
            try {
                String schoolsjson = Utils.getpref("schools_" + gameui().chrid, null);
                if (schoolsjson == null)
                    return;
                JSONArray arr = new JSONArray(schoolsjson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    String name = s.get("name").toString();
                    int idx = s.getInt("idx") - 1;
                    saves.set(idx, new Pair<>(attrf.render(name), idx));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (nm == "use") {
            int i = (int)args[0];
            if (i >= 0 && i < saves.size())
                schoolsDropdown.change(saves.get(i));
        } else if(nm == "max") {
            maxact = (Integer)args[0];
            recount();
        } else {
            super.uimsg(nm, args);
        }
    }
}
