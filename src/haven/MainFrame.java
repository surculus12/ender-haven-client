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

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.*;

import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.awt.ImageUtil;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GLAutoDrawable;
import haven.error.ErrorGui;
import haven.events.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class MainFrame implements Runnable, GLEventListener, Console.Directory {
    private static final String TITLE = "Haven and Hearth (Amber v" + Config.version + ")";
    public static final GLState.Slot<GLState> global = new GLState.Slot<>(GLState.Slot.Type.SYS, GLState.class);
    public static final GLState.Slot<GLState> proj2d = new GLState.Slot<>(GLState.Slot.Type.SYS, GLState.class, global);
    public static UI ui;
    public static int w, h;
    public boolean bgmode = false;
    public static long bgfd = Utils.getprefi("bghz", 200);
    long fd = 10, fps = 0;
    public MouseEvent mousemv;
    double uidle = 0.0, ridle = 0.0;
    public static Queue<InputEvent> events = new LinkedList<>();
    private Resource lastcursor = null;
    private CPUProfile uprof = new CPUProfile(300), rprof = new CPUProfile(300);
    private GPUProfile gprof = new GPUProfile(300);
    private GLState gstate, ostate;
    private GLState.Applier state = null;
    private GLConfig glconf = null;
    public static boolean needtotakescreenshot;
    public static GLWindow glw;
    private static GLCapabilities caps;
    public static Coord mousepos = new Coord(0, 0);
    public static List<MonitorMode> monitorModes;
    public static boolean isATI;
    private static ThreadGroup g;

    static {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

            // Since H&H IPs aren't likely to change (at least mid client run), and the client constantly needs to fetch
            // resources from the server, we enable "cache forever" policy so to overcome sporadic UnknownHostException
            // due to flaky DNS. Bad practice, but still better than forcing the user to modify hosts file.
            // NOTE: this needs to be done early as possible before InetAddressCachePolicy is initialized.
            java.security.Security.setProperty("networkaddress.cache.ttl", "-1");
        } catch (Exception e) {
        }

        System.setProperty("newt.window.icons", "haven/icon.png,haven/icon.png");

        WebBrowser.self = DesktopBrowser.create();
    }

    public MainFrame(GLWindow glw) {
        this.glw = glw;
    }

    public static void main(final String[] args) {
        try {
            Config.cmdline(args);

            Coord wndsz = Utils.getprefc("wndsz", new Coord(800, 600));
            w = wndsz.x;
            h = wndsz.y;

            if (Config.playerposfile != null)
                new Thread(new PlayerPosStreamer(), "Player position thread").start();

            final haven.error.ErrorHandler hg = new haven.error.ErrorHandler();
            hg.sethandler(new haven.error.ErrorGui(null));
            g = hg;

            GLProfile prof = GLProfile.getDefault();
            caps = new GLCapabilities(prof);
            caps.setDoubleBuffered(true);
            caps.setAlphaBits(8);
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setSampleBuffers(true);
            caps.setNumSamples(4);

            Display display = NewtFactory.createDisplay(null);
            Screen screen = NewtFactory.createScreen(display, 0);
            GLWindow glw = GLWindow.create(screen, caps);
            MainFrame mainframe = new MainFrame(glw);

            setupres();

            if (ResCache.global != null) {
                try {
                    Writer w = new OutputStreamWriter(ResCache.global.store("tmp/allused"), "UTF-8");
                    try {
                        Resource.dumplist(Resource.remote().used(), w);
                    } finally {
                        w.close();
                    }
                } catch (IOException e) {
                }
            }

            Runnable rbl = new Runnable() {
                @Override
                public void run() {
                    try {
                        Session sess = null;
                        while (true) {
                            UI.Runner fun;
                            if (sess == null) {
                                Bootstrap bill = new Bootstrap(Config.defserv, Config.mainport);
                                if ((Config.authuser != null) && (Config.authck != null)) {
                                    bill.setinitcookie(Config.authuser, Config.authck);
                                    Config.authck = null;
                                }
                                fun = bill;
                                glw.setTitle(TITLE);
                            } else {
                                fun = new RemoteUI(sess);
                                glw.setTitle(TITLE + " \u2013 " + sess.username);
                            }

                            ui = mainframe.newui(sess);
                            synchronized (this) {
                                notify();
                            }
                            sess = fun.run(ui);
                        }
                    } catch (InterruptedException e) {
                    }
                }
            };
            new HackThread(g, rbl, "Haven main thread").start();

            synchronized (rbl) {
                try {
                    while (ui == null)
                        rbl.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }

            final AdapterMouse mouseAdapter = new AdapterMouse(events, mainframe);
            final AdapterKey keyAdapter = new AdapterKey(events);
            mainframe.glw.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDestroyNotify(WindowEvent e) {
                    g.interrupt();
                    System.exit(0);
                }

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    mainframe.bgmode = false;
                }

                @Override
                public void windowLostFocus(WindowEvent e) {
                    mainframe.bgmode = true;
                }
            });

            glw.addMouseListener(mouseAdapter);
            glw.addKeyListener(keyAdapter);
            glw.addGLEventListener(mainframe);
            if (!Config.fullscreen) {
                if (Utils.getprefb("wndmax", false))
                    glw.setMaximized(true, true);
                else
                    glw.setSize(w, h);
            }
            glw.setTitle(TITLE);
            glw.setVisible(true);

            MonitorDevice monitor = glw.getMainMonitor();
            MonitorMode mmCurrent = monitor.queryCurrentMode();
            monitorModes = monitor.getSupportedModes();
            monitorModes = MonitorModeUtil.filterByFlags(monitorModes, 0);  // no interlace, double-scan etc
            monitorModes = MonitorModeUtil.filterByRotation(monitorModes, mmCurrent.getRotation());
            monitorModes = MonitorModeUtil.filterByRate(monitorModes, mmCurrent.getRefreshRate());
            monitorModes = MonitorModeUtil.getHighestAvailableBpp(monitorModes);
            ListIterator<MonitorMode> iter = monitorModes.listIterator();
            while (iter.hasNext()) {
                DimensionImmutable dim = iter.next().getSurfaceSize().getResolution();
                if (dim.getHeight() < 600 || dim.getWidth() < 800)
                    iter.remove();
            }

            if (Config.fullscreen)
                goFullscreen();

            new HackThread(g, mainframe, "Haven UI thread").start();
        } catch (Exception e) {
            new ErrorGui(null).goterror(e);
        }

    }

    public static void goFullscreen() {
        MonitorDevice monitor = glw.getMainMonitor();
        MonitorMode defMode = monitorModes.get(0);

        DimensionImmutable defres = defMode.getSurfaceSize().getResolution();
        Coord selres = Utils.getprefc("fullscreen_res", new Coord(defres.getWidth(), defres.getHeight()));

        MonitorMode mode = defMode;
        for (MonitorMode m : monitorModes) {
            DimensionImmutable res = m.getSurfaceSize().getResolution();
            if (res.getWidth() == selres.x && res.getHeight() == selres.y) {
                mode = m;
                break;
            }
        }
        monitor.setCurrentMode(mode);
        glw.setFullscreen(true);
    }

    public static void setupres() {
        if (ResCache.global != null)
            Resource.setcache(ResCache.global);
        if (Config.resurl != null)
            Resource.addurl(Config.resurl);
        if (ResCache.global != null) {
            try {
                Resource.loadlist(Resource.remote(), ResCache.global.fetch("tmp/allused"), -10);
            } catch (IOException e) {
            }
        }
        if (!Config.nopreload) {
            try {
                InputStream pls;
                pls = Resource.class.getResourceAsStream("res-preload");
                if (pls != null)
                    Resource.loadlist(Resource.remote(), pls, -5);
                pls = Resource.class.getResourceAsStream("res-bgload");
                if (pls != null)
                    Resource.loadlist(Resource.remote(), pls, -10);
            } catch (IOException e) {
                throw (new Error(e));
            }
        }
    }

    private UI newui(Session sess) {
        if (ui != null)
            ui.destroy();
        ui = new UI(new Coord(w, h), sess);
        ui.root.guprof = uprof;
        ui.root.grprof = rprof;
        ui.root.ggprof = gprof;
        ui.cons.add(this);
        if (glconf != null)
            ui.cons.add(glconf);
        return ui;
    }

    private static void dumplist(Collection<Resource> list, String fn) {
        try {
            if (fn != null) {
                Writer w = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
                try {
                    Resource.dumplist(list, w);
                } finally {
                    w.close();
                }
            }
        } catch (IOException e) {
            throw (new RuntimeException(e));
        }
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GL gl = glAutoDrawable.getGL();
        gl.setSwapInterval(0);

        glconf = GLConfig.fromgl(gl, glAutoDrawable.getContext(), caps);
        glconf.pref = GLSettings.load(glconf, true);
        ui.cons.add(glconf);
        final haven.error.ErrorHandler h = haven.error.ErrorHandler.find();
        if (h != null) {
            String vendor = gl.glGetString(gl.GL_VENDOR);
            isATI = vendor.contains("AMD") || vendor.contains("ATI");
            h.lsetprop("gl.vendor", vendor);
            h.lsetprop("gl.version", gl.glGetString(gl.GL_VERSION));
            h.lsetprop("gl.renderer", gl.glGetString(gl.GL_RENDERER));
            h.lsetprop("gl.exts", Arrays.asList(gl.glGetString(gl.GL_EXTENSIONS).split(" ")));
            h.lsetprop("gl.caps", glAutoDrawable.getChosenGLCapabilities().toString());
            h.lsetprop("gl.conf", glconf);
        }
        gstate = new GLState() {
            @Override
            public void apply(GOut g) {
                BGL gl = g.gl;
                gl.glColor3f(1, 1, 1);
                gl.glPointSize(4);
                gl.joglSetSwapInterval(1);
                gl.glEnable(GL.GL_BLEND);
                //gl.glEnable(GL.GL_LINE_SMOOTH);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                if (g.gc.glmajver >= 2)
                    gl.glBlendEquationSeparate(GL.GL_FUNC_ADD, GL2.GL_MAX);
                if (g.gc.havefsaa()) {
                    /* Apparently, having sample
                     * buffers in the config enables
                     * multisampling by default on
                     * some systems. */
                    g.gl.glDisable(GL.GL_MULTISAMPLE);
                }
                GOut.checkerr(gl);
            }

            @Override
            public void unapply(GOut g) {
            }

            @Override
            public void prep(Buffer buf) {
                buf.put(global, this);
            }
        };
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL2 gl = glAutoDrawable.getGL().getGL2();
        redraw(gl);
        if (needtotakescreenshot) {
            takeScreenshot(glw.getWidth(), glw.getHeight());
            needtotakescreenshot = false;
        }
    }

    private void takeScreenshot(int width, int height) throws GLException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        GLContext glc = GLContext.getCurrent();
        GL gl = glc.getGL();

        GLPixelStorageModes psm = new GLPixelStorageModes();
        psm.setPackAlignment(gl, 1);

        gl.glReadPixels(0, 0, width, height, GL2ES3.GL_BGR,
                GL.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData()));

        psm.restore(gl);

        if (glc.getGLDrawable().isGLOriented())
            ImageUtil.flipImageVertically(image);

        try {
            String curtimestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS").format(new Date());
            File outputfile = new File(String.format("screenshots/%s.png", curtimestamp));
            outputfile.getParentFile().mkdirs();
            ImageIO.write(image, "png", outputfile);
            ui.root.findchild(GameUI.class).msg(String.format("Screenshot has been saved as \"%s\"", outputfile.getName()), Color.WHITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch() {
        synchronized (events) {
            if (mousemv != null) {
                mousepos = new Coord(mousemv.getX(), mousemv.getY());
                ui.mousemove(mousemv, mousepos);
                mousemv = null;
            }
            InputEvent e;
            while ((e = events.poll()) != null) {
                if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    if (me.getEventType() == MouseEvent.EVENT_MOUSE_PRESSED) {
                        ui.mousedown(me, new Coord(me.getX(), me.getY()), me.getButton());
                    } else if (me.getEventType() == MouseEvent.EVENT_MOUSE_RELEASED) {
                        ui.mouseup(me, new Coord(me.getX(), me.getY()), me.getButton());
                    } else if (me.getEventType() == MouseEvent.EVENT_MOUSE_WHEEL_MOVED) {
                        float[] rot = me.getRotation();
                        int rotval = me.isShiftDown() ? (int) -rot[0] : (int) -rot[1];
                        ui.mousewheel(me, new Coord(me.getX(), me.getY()), rotval);
                    }
                } else if (e instanceof KeyEvent) {
                    java.awt.event.KeyEvent ke = NEWT2AWT.convert((KeyEvent) e);
                    if (ke == null) {
                        continue;
                    } else if (ke.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
                        ui.keydown(ke);
                    } else if (ke.getID() == java.awt.event.KeyEvent.KEY_RELEASED) {
                        java.awt.event.KeyEvent awtke = NEWT2AWT.convertToTyped((KeyEvent) e);
                        if (awtke != null)
                            ui.type(awtke);
                        if (!e.isAutoRepeat())
                            ui.keyup(ke);
                    }
                }
                ui.lastevent = System.currentTimeMillis();
            }
        }
    }

    private void rootdraw(GLState.Applier state, UI ui, BGL gl) {
        GLState.Buffer ibuf = new GLState.Buffer(state.cfg);
        gstate.prep(ibuf);
        ostate.prep(ibuf);
        GOut g = new GOut(gl, state.cgl, state.cfg, state, ibuf, new Coord(w, h));
        state.set(ibuf);

        g.state(ostate);
        g.apply();
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        synchronized (ui) {
            ui.draw(g);
        }

        if (Config.showfps) {
            FastText.aprint(g, new Coord(w - 50, 15), 0, 1, "FPS: " + fps);
        }

        if (Config.dbtext) {
            int y = h - 165;
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "FPS: %d (%d%%, %d%% idle)", fps, (int) (uidle * 100.0), (int) (ridle * 100.0));
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory();
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mem: %,011d/%,011d/%,011d/%,011d", free, total - free, total, rt.maxMemory());
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Tex-current: %d", TexGL.num());
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "GL progs: %d", g.st.numprogs());
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Stats slots: %d", GLState.Slot.num());
            GameUI gi = ui.root.findchild(GameUI.class);
            if ((gi != null) && (gi.map != null)) {
                try {
                    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mapview: %s", gi.map);
                } catch (Loading e) {
                }
                if (gi.map.rls != null)
                    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Rendered: %,d+%,d(%,d), cached %,d/%,d+%,d(%,d)", gi.map.rls.drawn, gi.map.rls.instanced, gi.map.rls.instancified, gi.map.rls.cacheroots, gi.map.rls.cached, gi.map.rls.cacheinst, gi.map.rls.cacheinstn);
            }
            if (Resource.remote().qdepth() > 0)
                FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "RQ depth: %d (%d)", Resource.remote().qdepth(), Resource.remote().numloaded());
        }
        Object tooltip;
        try {
            synchronized (ui) {
                tooltip = ui.root.tooltip(mousepos, ui.root);
            }
        } catch (Loading e) {
            tooltip = "...";
        }
        Tex tt = null;
        if (tooltip != null) {
            if (tooltip instanceof Text) {
                tt = ((Text) tooltip).tex();
            } else if (tooltip instanceof Tex) {
                tt = (Tex) tooltip;
            } else if (tooltip instanceof Indir<?>) {
                Indir<?> t = (Indir<?>) tooltip;
                Object o = t.get();
                if (o instanceof Tex)
                    tt = (Tex) o;
            } else if (tooltip instanceof String) {
                if (((String) tooltip).length() > 0)
                    tt = (Text.render((String) tooltip)).tex();
            }
        }
        if (tt != null) {
            Coord sz = tt.sz();
            Coord pos = mousepos.add(sz.inv());
            if (pos.x < 10)
                pos.x = 10;
            if (pos.y < 10)
                pos.y = 10;
            g.chcolor(244, 247, 21, 192);
            g.rect(pos.add(-3, -3), sz.add(6, 6));
            g.chcolor(35, 35, 35, 192);
            g.frect(pos.add(-2, -2), sz.add(4, 4));
            g.chcolor();
            g.image(tt, pos);
        }
        ui.lasttip = tooltip;
        Resource curs = ui.root.getcurs(mousepos);
        if (curs != null && curs != lastcursor) {
            BufferedImage img = curs.layer(Resource.imgc).img;

            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
            final IntBuffer pixelIntBuff = Buffers.newDirectIntBuffer(pixels);
            final ByteBuffer pixelBuff = Buffers.copyIntBufferAsByteBuffer(pixelIntBuff);

            PixelRectangle.GenericPixelRect pixelRect = new PixelRectangle.GenericPixelRect(
                    PixelFormat.BGRA8888,
                    new com.jogamp.nativewindow.util.Dimension(img.getWidth(), img.getHeight()),
                    0,
                    false,
                    pixelBuff);

            Display.PointerIcon picon = glw.getScreen().getDisplay().createPointerIcon(pixelRect, 0, 0);
            glw.setPointerIcon(picon);
            lastcursor = curs;
        }
        state.clean();
        GLObject.disposeall(state.cgl, gl);
    }

    private static class Frame {
        BufferBGL buf;
        CPUProfile.Frame pf;
        CurrentGL on;
        long doneat;

        Frame(BufferBGL buf, CurrentGL on) {
            this.buf = buf;
            this.on = on;
        }
    }

    private Frame[] curdraw = {null};

    void redraw(GL2 gl) {
        if ((state == null) || (state.cgl.gl != gl))
            state = new GLState.Applier(new CurrentGL(gl, glconf));

        Frame f;
        synchronized (curdraw) {
            f = curdraw[0];
            curdraw[0] = null;
        }
        if ((f != null) && (f.on.gl == gl)) {
            GPUProfile.Frame curgf = null;
            if (Config.profilegpu)
                curgf = gprof.new Frame((GL3) gl);
            if (f.pf != null)
                f.pf.tick("awt");
            f.buf.run(gl);
            GOut.checkerr(gl);
            if (f.pf != null)
                f.pf.tick("gl");
            if (curgf != null) {
                curgf.tick("draw");
                curgf.fin();
            }

            if (glconf.pref.dirty) {
                glconf.pref.save();
                glconf.pref.dirty = false;
            }
            f.doneat = System.currentTimeMillis();
        }
    }

    private Frame bufdraw = null;
    private final Runnable drawfun = new Runnable() {
        public void run() {
            try {
                glw.display();
                synchronized (drawfun) {
                    drawfun.notifyAll();
                }
                while (true) {
                    long then = System.currentTimeMillis();
                    int waited = 0;
                    Frame current;
                    synchronized (drawfun) {
                        while ((current = bufdraw) == null)
                            drawfun.wait();
                        bufdraw = null;
                        drawfun.notifyAll();
                        waited += System.currentTimeMillis() - then;
                    }
                    CPUProfile.Frame curf = null;
                    if (Config.profile)
                        current.pf = curf = rprof.new Frame();
                    synchronized (curdraw) {
                        curdraw[0] = current;
                    }
                    if (glw.isVisible()) {
                        glw.display();
                    } else {
                        glw.getContext().makeCurrent();
                        redraw(glw.getGL().getGL2());
                    }
                    if (curf != null) {
                        curf.tick("aux");
                        curf.fin();
                    }
                    long now = System.currentTimeMillis();
                    waited += now - current.doneat;
                    ridle = (ridle * 0.95) + (((double) waited / ((double) (now - then))) * 0.05);
                    current = null; /* Just for the GC. */
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    };

    @Override
    public void run() {
        try {
            Thread drawthread = new HackThread(drawfun, "Render thread");
            drawthread.start();
            synchronized (drawfun) {
                while (state == null)
                    drawfun.wait();
            }
            try {
                long now, then;
                long frames[] = new long[128];
                int framep = 0, waited[] = new int[128];
                while (true) {
                    int fwaited = 0;
                    Debug.cycle();
                    UI ui = this.ui;
                    then = System.currentTimeMillis();
                    CPUProfile.Frame curf = null;
                    if (Config.profile)
                        curf = uprof.new Frame();
                    synchronized (ui) {
                        if (ui.sess != null)
                            ui.sess.glob.ctick();
                        dispatch();
                        ui.tick();
                        if ((ui.root.sz.x != w) || (ui.root.sz.y != h))
                            ui.root.resize(new Coord(w, h));
                    }
                    if (curf != null)
                        curf.tick("dsp");

                    BufferBGL buf = new BufferBGL();
                    GLState.Applier state = this.state;
                    rootdraw(state, ui, buf);
                    if (curf != null)
                        curf.tick("draw");
                    synchronized (drawfun) {
                        now = System.currentTimeMillis();
                        while (bufdraw != null)
                            drawfun.wait();
                        bufdraw = new Frame(buf, state.cgl);
                        drawfun.notifyAll();
                        fwaited += System.currentTimeMillis() - now;
                    }

                    ui.audio.cycle();
                    if (curf != null)
                        curf.tick("aux");

                    now = System.currentTimeMillis();
                    long fd = bgmode ? this.bgfd : this.fd;
                    if (now - then < fd) {
                        synchronized (events) {
                            events.wait(fd - (now - then));
                        }
                        fwaited += System.currentTimeMillis() - now;
                    }

                    frames[framep] = now;
                    waited[framep] = fwaited;
                    for (int i = 0, ckf = framep, twait = 0; i < frames.length; i++) {
                        ckf = (ckf - 1 + frames.length) % frames.length;
                        twait += waited[ckf];
                        if (now - frames[ckf] > 1000) {
                            fps = i;
                            uidle = ((double) twait) / ((double) (now - frames[ckf]));
                            break;
                        }
                    }
                    framep = (framep + 1) % frames.length;

                    if (curf != null)
                        curf.tick("wait");
                    if (curf != null)
                        curf.fin();
                    if (Thread.interrupted())
                        throw (new InterruptedException());
                }
            } finally {
                drawthread.interrupt();
                drawthread.join();
            }
        } catch (InterruptedException e) {
        } finally {
            ui.destroy();
        }
    }

    public static abstract class OrthoState extends GLState {
        protected abstract Coord sz();

        public void apply(GOut g) {
            Coord sz = sz();
            g.st.proj = Projection.makeortho(new Matrix4f(), 0, sz.x, sz.y, 0, -1, 1);
        }

        public void unapply(GOut g) {
        }

        public void prep(Buffer buf) {
            buf.put(proj2d, this);
        }

        public static OrthoState fixed(final Coord sz) {
            return (new OrthoState() {
                protected Coord sz() {
                    return (sz);
                }
            });
        }
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
        this.w = w;
        this.h = h;
        Utils.setprefc("wndsz", new Coord(w, h));
        Utils.setprefb("wndmax", glw.isMaximizedVert() || glw.isMaximizedHorz());
        ostate = OrthoState.fixed(new Coord(w, h));
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<>();

    {
        cmdmap.put("hz", (cons, args) -> fd = 1000 / Integer.parseInt(args[1]));
        cmdmap.put("bghz", (cons, args) -> {
            bgfd = 1000 / Integer.parseInt(args[1]);
            Utils.setprefi("bghz", (int) bgfd);
        });
        cmdmap.put("dumplist", (cons, args) -> {
            if ("all".equals(args[1]))
                dumplist(Resource.remote().cached(),  "reslist_all");
            else
                dumplist(Resource.remote().loadwaited(), "reslist_loadwaited");
        });
    }

    public Map<String, Console.Command> findcmds() {
        return cmdmap;
    }
}
