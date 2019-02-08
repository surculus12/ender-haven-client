package mapper;

import haven.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static haven.HashDirCache.findbase;

public class Main {
    public static void main(String[] args) throws MalformedURLException {
	try {
	    Config.cmdline(args);
	    MainFrame.setupres();
	    
	    File base = findbase("data");
	    Config.resurl = new URL("http://game.havenandhearth.com/hres/"); 
	    URI id = Config.resurl.toURI();
	    ResCache cache = new HashDirCache(id, base);
	    MapFile map = MapFile.load(cache, "");
	    
	    MainScreen screen = new MainScreen(map);
	    screen.setVisible(true);
	} catch (URISyntaxException e) {
	    e.printStackTrace();
	}
    }
}
