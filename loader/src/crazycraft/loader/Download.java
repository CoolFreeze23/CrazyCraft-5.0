package crazycraft.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Downloads with a hash check, retries, and a watchdog that drops a connection which stops sending. */
final class Download {
    static final String USER_AGENT = "CrazyCraft-Loader/" + Main.VERSION + " (github.com/CoolFreeze23/CrazyCraft-5.0)";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "download-watchdog");
        t.setDaemon(true);
        return t;
    });
    private static final long STALL_MS = 45_000;

    interface Progress {
        void bytes(long delta);
    }

    private Download() {
    }

    /** Fetch url into dest, checking size and hash (algo "SHA-512" or "SHA-1"); tries up to four times. */
    static void fetch(String url, Path dest, long size, String hash, String algo, Progress progress)
            throws IOException, InterruptedException {
        IOException last = null;
        long[] backoff = {0, 2_000, 5_000, 12_000};
        for (long wait : backoff) {
            if (wait > 0) {
                Thread.sleep(wait);
            }
            AtomicLong counted = new AtomicLong();
            try {
                once(url, dest, size, hash, algo, d -> {
                    counted.addAndGet(d);
                    progress.bytes(d);
                });
                return;
            } catch (IOException e) {
                last = e;
                progress.bytes(-counted.get());
                Files.deleteIfExists(dest);
                String msg = String.valueOf(e.getMessage());
                if (msg.startsWith("HTTP 4") && !msg.startsWith("HTTP 429")) {
                    break;
                }
            }
        }
        throw last;
    }

    private static void once(String url, Path dest, long size, String hash, String algo, Progress progress)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(60))
                .GET().build();
        HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            res.body().close();
            throw new IOException("HTTP " + res.statusCode() + " from " + URI.create(url).getHost());
        }
        MessageDigest md = Hashes.digest(algo);
        AtomicLong lastRead = new AtomicLong(System.currentTimeMillis());
        long total = 0;
        try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(dest)) {
            ScheduledFuture<?> dog = WATCHDOG.scheduleAtFixedRate(() -> {
                if (System.currentTimeMillis() - lastRead.get() > STALL_MS) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // closing is how the stalled read is interrupted
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) {
                    lastRead.set(System.currentTimeMillis());
                    out.write(buf, 0, n);
                    md.update(buf, 0, n);
                    total += n;
                    progress.bytes(n);
                }
            } finally {
                dog.cancel(false);
            }
        }
        if (size > 0 && total != size) {
            throw new IOException("got " + total + " bytes, expected " + size);
        }
        String got = Hashes.hex(md.digest());
        if (hash != null && !hash.equalsIgnoreCase(got)) {
            throw new IOException("the file's checksum does not match (it may have been changed on the server)");
        }
    }

    /** A small text resource, such as a checksum file or the latest release's tag. */
    static String text(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode());
        }
        return res.body();
    }
}
