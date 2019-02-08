package mapper;

import haven.MapFile;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainScreen extends JFrame {
    
    public static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    
    public MainScreen(MapFile map) {
	super("Mapper");
	setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	
	MapComponent mapComponent = new MapComponent(map);
	
	JList<String> list = new JList<>();
	list.setModel(new SegmentsModel(map));
	list.setFont(FONT);
	
	list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	list.addListSelectionListener(e -> {
	    if(!e.getValueIsAdjusting()) {
	        mapComponent.set(((SegmentsModel) list.getModel()).ids.get(e.getFirstIndex()));
	    }
	});
	list.setMinimumSize(new Dimension(175, 100));
	
	JPanel p = new JPanel();
	p.setLayout(new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();
	
	c.fill = GridBagConstraints.BOTH;
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 1;
	c.weighty = 1;
	p.add(mapComponent, c);
	
	c.fill = GridBagConstraints.VERTICAL;
	c.gridx = 1;
	c.gridy = 0;
	c.weightx = 0;
	p.add(list, c);
	
	add(p);
	setSize(400, 300);
    }
    
    private static class SegmentsModel implements ListModel<String> {
	private final List<String> strings;
	public final List<Long> ids;
	
	public SegmentsModel(MapFile map) {
	    this.strings = map.knownsegs.stream()
		.map(v -> String.format("%16s", Long.toHexString(v).toUpperCase()).replace(' ', '0'))
		.collect(Collectors.toList());
	    
	    this.ids = new ArrayList<>(map.knownsegs);
	}
	
	@Override
	public int getSize() {
	    return strings.size();
	}
	
	@Override
	public String getElementAt(int index) {
	    return strings.get(index);
	}
	
	@Override
	public void addListDataListener(ListDataListener l) {
	    
	}
	
	@Override
	public void removeListDataListener(ListDataListener l) {
	    
	}
    }
}
