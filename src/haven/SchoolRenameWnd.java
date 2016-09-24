package haven;


import org.json.JSONObject;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static haven.CharWnd.attrf;

public class SchoolRenameWnd extends Window {

    public SchoolRenameWnd(String cap, Dropbox<Pair<Text, Integer>> dd, final List<Pair<Text, Integer>> saves, final int idx, String name) {
        super(new Coord(225, 100), cap);

        final TextEntry txtname = new TextEntry(200, name);
        add(txtname, new Coord(15, 20));

        Button add = new Button(60, "Save") {
            @Override
            public void click() {
                Pair<Text, Integer> newsel = new Pair<Text, Integer>(attrf.render(txtname.text), idx);
                saves.set(idx, newsel);
                dd.sel = newsel;
                saveschools(saves);
                parent.reqdestroy();
            }
        };
        add(add, new Coord(15, 60));

        Button cancel = new Button(60, "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, new Coord(155, 60));
    }

    private void saveschools(final List<Pair<Text, Integer>> saves) {
        try {
            List<String> arr = new ArrayList<String>();
            for (Pair<Text, Integer> school : saves) {
                if (school.a.text.equals("unused save"))
                    continue;
                String sjson = new JSONObject(new HashMap<String, Object>(2) {{
                    put("name", school.a.text);
                    put("idx", school.b + 1);
                }}).toString();
                arr.add(sjson);
            }
            String jsonobjs = "";
            for (String s : arr)
                jsonobjs += s + ",";
            if (jsonobjs.length() > 0)
                jsonobjs = jsonobjs.substring(0, jsonobjs.length()-1);
            Utils.setpref("schools_" + gameui().chrid, "[" + jsonobjs + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn)
            reqdestroy();
        else
            super.wdgmsg(sender, msg, args);
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if (key == 27) {
            reqdestroy();
            return true;
        }
        return super.type(key, ev);
    }
}
