package crazycraft.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The pack's crazycraft-manifest.json: the NeoForge version, and every mod with its official download. */
record Manifest(String packName, String packVersion, String repo, String minecraft, NeoForge neoforge, List<Mod> mods,
                List<Extract> extracts) {

    record NeoForge(String version, String installer, String sha1) {
    }

    record Source(String type, String url, String page, String download, String note) {
        String label() {
            return switch (type) {
                case "modrinth" -> "Modrinth";
                case "curseforge" -> "CurseForge";
                case "github" -> "GitHub";
                case "manual" -> "CurseForge (by hand)";
                default -> type;
            };
        }
    }

    record Patch(String originalFile, List<Map<String, Object>> ops, String result, String why) {
    }

    record Mod(String file, String dir, String name, String version, String side, long size, String sha512,
               Source source, String license, Patch patch) {
        boolean onServer() {
            return !"client".equals(side);
        }

        boolean onClient() {
            return !"server".equals(side);
        }

        boolean manual() {
            return "manual".equals(source.type());
        }
    }

    record Extract(String from, String entry, String to, String sha256, String side, String why) {
        boolean onServer() {
            return !"client".equals(side);
        }

        boolean onClient() {
            return !"server".equals(side);
        }
    }

    static Manifest parse(String text) {
        Map<String, Object> root = Json.obj(Json.parse(text));
        if (Json.num(root, "format") != 1) {
            throw new IllegalArgumentException("This manifest needs a newer loader (format " + root.get("format") + ")");
        }
        Map<String, Object> pack = Json.obj(root.get("pack"));
        Map<String, Object> neo = Json.obj(root.get("neoforge"));
        List<Mod> mods = new ArrayList<>();
        for (Object o : Json.arr(root.get("mods"))) {
            Map<String, Object> m = Json.obj(o);
            Map<String, Object> src = Json.obj(m.get("source"));
            Patch patch = null;
            if (m.get("patch") != null) {
                Map<String, Object> p = Json.obj(m.get("patch"));
                List<Map<String, Object>> ops = new ArrayList<>();
                for (Object op : Json.arr(p.get("ops"))) {
                    ops.add(Json.obj(op));
                }
                patch = new Patch(Json.str(p, "originalFile"), ops, Json.str(p, "result"), Json.str(p, "why"));
            }
            String dir = Json.str(m, "dir");
            mods.add(new Mod(Json.str(m, "file"), dir == null ? "mods" : dir, Json.str(m, "name"), Json.str(m, "version"),
                    Json.str(m, "side"),
                    Json.num(m, "size"), Json.str(m, "sha512"),
                    new Source(Json.str(src, "type"), Json.str(src, "url"), Json.str(src, "page"),
                            Json.str(src, "download"), Json.str(src, "note")),
                    Json.str(m, "license"), patch));
        }
        List<Extract> extracts = new ArrayList<>();
        for (Object o : Json.arr(root.get("extract"))) {
            Map<String, Object> x = Json.obj(o);
            extracts.add(new Extract(Json.str(x, "from"), Json.str(x, "entry"), Json.str(x, "to"), Json.str(x, "sha256"),
                    Json.str(x, "side"), Json.str(x, "why")));
        }
        return new Manifest(Json.str(pack, "name"), Json.str(pack, "version"), Json.str(pack, "repo"),
                Json.str(pack, "minecraft"),
                new NeoForge(Json.str(neo, "version"), Json.str(neo, "installer"), Json.str(neo, "sha1")),
                mods, extracts);
    }

    List<Mod> serverMods() {
        return mods.stream().filter(Mod::onServer).toList();
    }
}
