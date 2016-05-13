package haven.combat;

import haven.Coord;

import java.awt.*;
import java.awt.image.BufferedImage;

public enum AttackType {
    OPPRESSIVE(0x1, new Color(210, 5, 10)),
    STRIKING(0x2, new Color(30, 186, 103)),
    BACKHANDED(0x4, new Color(6, 30, 210)),
    SWEEPING(0x8, new Color(255, 240, 1));

    public final int value;
    public final Color color;

    AttackType(int value, Color color) {
	this.value = value;
	this.color = color;
    }

    public static int get(int rgb) {
	for (AttackType type : values()) {
	    if(type.color.getRGB() == rgb)
		return type.value;
	}
	return 0;
    }

    public static int get(BufferedImage image) {
	int type = 0;
	int w = image.getWidth();
	int h = image.getHeight();
	Coord[] corners = {
	    new Coord(0, 0),
	    new Coord(0, h - 1),
	    new Coord(w - 1, h - 1),
	    new Coord(w - 1, 0)
	};
	for (Coord corner : corners) {
	    int rgb = image.getRGB(corner.x, corner.y);
	    type |= get(rgb);
	}
	return type;
    }
}
