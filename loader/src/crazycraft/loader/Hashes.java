package crazycraft.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Hashes {
    private Hashes() {
    }

    static MessageDigest digest(String algo) {
        try {
            return MessageDigest.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] b) {
        return HexFormat.of().formatHex(b);
    }

    static String of(Path file, String algo) throws IOException {
        MessageDigest md = digest(algo);
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        return hex(md.digest());
    }

    static String sha256(byte[] data) {
        return hex(digest("SHA-256").digest(data));
    }

    /** Every file entry of a jar (directories left out); a name that appears twice keeps its last copy. */
    static Map<String, byte[]> files(Path jar) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipFile z = new ZipFile(jar.toFile())) {
            for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                try (InputStream in = z.getInputStream(e)) {
                    out.put(e.getName(), in.readAllBytes());
                }
            }
        }
        return out;
    }

    /**
     * What a patched jar is checked against: SHA-256 over the sorted "name TAB sha256(content)" lines of its files.
     * It ignores zip details such as timestamps and compression, which differ between tools.
     */
    static String contentHash(Map<String, byte[]> files) {
        List<String> lines = new ArrayList<>(files.size());
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            lines.add(e.getKey() + "\t" + sha256(e.getValue()) + "\n");
        }
        Collections.sort(lines);
        MessageDigest md = digest("SHA-256");
        for (String l : lines) {
            md.update(l.getBytes(StandardCharsets.UTF_8));
        }
        return hex(md.digest());
    }
}
