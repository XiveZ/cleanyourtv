package local.tvhome.wallfetch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** The wallhaven.cc search the work/anime-walls.sh script made, and its answer. No key, safe-for-work only. */
final class Wallhaven {
    private Wallhaven() { }

    static final String[] TAGS = {"scenery", "sky", "night sky", "clouds", "sunset", "landscape", "city lights"};
    static final String[] COLORS = {"333399", "663399", "0066cc", "424153", "993399", "ea4c88", "0099cc"};
    private static final String NOT = " -weapon -gun -blood -horror -calendar -skull";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9]{1,16}");

    static final class Item {
        final String id, url;

        Item(String id, String url) {
            this.id = id;
            this.url = url;
        }
    }

    /** Each tag with three colours of the blue-violet-pink palette, starting at its own colour. */
    static List<String[]> pairs() {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < TAGS.length; i++) {
            for (int k = 0; k < 3; k++) out.add(new String[]{TAGS[i], COLORS[(i + k) % COLORS.length]});
        }
        return out;
    }

    static String searchUrl(String tag, String color, int page) {
        return "https://wallhaven.cc/api/v1/search?q=" + enc(tag + NOT) + "&categories=010&purity=100&ratios=16x9"
                + "&atleast=1920x1080&colors=" + enc(color) + "&sorting=favorites&page=" + page;
    }

    /** The pictures in a search answer: a safe id and a full-size https image on wallhaven only. */
    static List<Item> parse(String json) throws JSONException {
        List<Item> items = new ArrayList<>();
        JSONArray data = new JSONObject(json).optJSONArray("data");
        if (data == null) return items;
        for (int i = 0; i < data.length(); i++) {
            JSONObject o = data.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", ""), url = o.optString("path", "");
            if (ID.matcher(id).matches() && url.startsWith("https://w.wallhaven.cc/")) items.add(new Item(id, url));
        }
        return items;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
