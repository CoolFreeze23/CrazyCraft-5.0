package crazycraft.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Applies a mod's pack fix to its untouched original jar. The edits are small and exact (remove an entry, swap a
 * few bytes, replace one piece of text), and the result is checked against the manifest before it is used.
 */
final class Patcher {
    private Patcher() {
    }

    static void apply(Path original, Manifest.Patch patch, Path out) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        Map<String, ZipEntry> meta = new HashMap<>();
        Map<String, Boolean> dirs = new LinkedHashMap<>();
        try (ZipFile z = new ZipFile(original.toFile())) {
            for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    dirs.put(e.getName(), true);
                    files.putIfAbsent(e.getName(), null);
                    continue;
                }
                if (meta.containsKey(e.getName())) {
                    continue;
                }
                try (InputStream in = z.getInputStream(e)) {
                    files.put(e.getName(), in.readAllBytes());
                }
                meta.put(e.getName(), e);
            }
        }
        for (Map<String, Object> op : patch.ops()) {
            String entry = Json.str(op, "entry");
            switch (Json.str(op, "op")) {
                case "remove" -> {
                    if (files.remove(entry) == null) {
                        throw new IOException(entry + " is not in " + original.getFileName());
                    }
                }
                case "add" -> files.put(entry, Json.str(op, "text").getBytes(StandardCharsets.UTF_8));
                case "bytes" -> {
                    byte[] data = need(files, entry, original);
                    int off = (int) Json.num(op, "offset");
                    byte[] expect = HexFormat.of().parseHex(Json.str(op, "expect"));
                    byte[] repl = HexFormat.of().parseHex(Json.str(op, "replace"));
                    if (off + expect.length > data.length
                            || !Arrays.equals(Arrays.copyOfRange(data, off, off + expect.length), expect)) {
                        throw new IOException(entry + ": the bytes to change are not what the fix expects");
                    }
                    data = data.clone();
                    System.arraycopy(repl, 0, data, off, repl.length);
                    files.put(entry, data);
                }
                case "replace" -> {
                    byte[] data = need(files, entry, original);
                    byte[] find = Json.str(op, "find").getBytes(StandardCharsets.UTF_8);
                    byte[] with = Json.str(op, "with").getBytes(StandardCharsets.UTF_8);
                    int at = indexOf(data, find, 0);
                    if (at < 0 || indexOf(data, find, at + 1) >= 0) {
                        throw new IOException(entry + ": the text to change is not there exactly once");
                    }
                    byte[] res = new byte[data.length - find.length + with.length];
                    System.arraycopy(data, 0, res, 0, at);
                    System.arraycopy(with, 0, res, at, with.length);
                    System.arraycopy(data, at + find.length, res, at + with.length, data.length - at - find.length);
                    files.put(entry, res);
                }
                default -> throw new IOException("Unknown edit '" + Json.str(op, "op") + "' (this loader is too old)");
            }
        }
        Map<String, byte[]> onlyFiles = new LinkedHashMap<>();
        files.forEach((k, v) -> {
            if (v != null) {
                onlyFiles.put(k, v);
            }
        });
        String got = Hashes.contentHash(onlyFiles);
        if (!got.equals(patch.result())) {
            throw new IOException("The fixed jar does not match what the pack expects (" + got.substring(0, 12) + ")");
        }
        try (OutputStream os = Files.newOutputStream(out); ZipOutputStream zo = new ZipOutputStream(os)) {
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                String name = e.getKey();
                ZipEntry src = meta.get(name);
                ZipEntry ne = new ZipEntry(name);
                if (src != null && src.getTime() != -1) {
                    ne.setTime(src.getTime());
                }
                byte[] data = e.getValue();
                if (data == null) {
                    if (!dirs.containsKey(name)) {
                        continue;
                    }
                    zo.putNextEntry(ne);
                    zo.closeEntry();
                    continue;
                }
                if (src != null && src.getMethod() == ZipEntry.STORED) {
                    CRC32 crc = new CRC32();
                    crc.update(data);
                    ne.setMethod(ZipEntry.STORED);
                    ne.setSize(data.length);
                    ne.setCompressedSize(data.length);
                    ne.setCrc(crc.getValue());
                }
                zo.putNextEntry(ne);
                zo.write(data);
                zo.closeEntry();
            }
        }
    }

    private static byte[] need(Map<String, byte[]> files, String entry, Path jar) throws IOException {
        byte[] data = files.get(entry);
        if (data == null) {
            throw new IOException(entry + " is not in " + jar.getFileName());
        }
        return data;
    }

    private static int indexOf(byte[] hay, byte[] needle, int from) {
        outer:
        for (int i = from; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    static List<String> describe(Manifest.Patch p) {
        return p.ops().stream().map(op -> Json.str(op, "op") + " " + Json.str(op, "entry")).toList();
    }
}
