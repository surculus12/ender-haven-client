package mapper;

import haven.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MapComponent extends JComponent {
    private final MapFile map;
    private MapFile.Segment seg;
    private Defer.Future<BufferedImage> fimg;
    
    public MapComponent(MapFile map) {
	super();
	this.map = map;
	Dimension size = new Dimension(100, 100);
	setMinimumSize(size);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
	super.paintComponent(g);
	if(fimg != null) {
	    if(fimg.done()) {
		g.drawImage(fimg.get(), 0, 0, null);
	    } else {
		Defer.later((Defer.Callable<Void>) () -> {
		    MapComponent.this.repaint();
		    return null;
		});
	    }
	}
	g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }
    
    public void set(Long segment) {
	fimg = Defer.later(() -> {
	    if(!map.lock.readLock().tryLock())
		throw (new Loading("Map file is busy"));
	    try {
		seg = map.segments.get(segment);
		Coord c = seg.getGridCoords().stream().findFirst().get();
		Indir<MapFile.Grid> igrid = seg.grid(c);
		MapFile.Grid grid = igrid.get();
		return grid.render(Coord.z);
//	    }catch (Exception e){
//	        e.printStackTrace();
//	        throw e;
	    } finally {
		map.lock.readLock().unlock();
	    }
	});
	repaint();
    }
}
