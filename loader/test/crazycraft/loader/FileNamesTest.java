package crazycraft.loader;

import java.nio.file.Files;
import java.nio.file.Path;

/** File names the system can't use fall back to a plain name instead of stopping the install. */
public final class FileNamesTest {
    public static void main(String[] args) throws Exception {
        check(FileNames.plain("§c1.21.2 Redstone Torch§r.zip"), "1.21.2 Redstone Torch.zip");
        check(FileNames.plain("Pão de Queijo Ação.zip"), "Pao de Queijo Acao.zip");
        check(FileNames.plain("a:b?c.zip"), "a_b_c.zip");
        check(FileNames.plain("§§"), "file");
        check(FileNames.plain(".hidden"), "file.hidden");
        Path dir = Files.createTempDirectory("crazycraft-names-");
        // Windows refuses '?' in a name the same way a C-locale Linux refuses '§': both must fall back.
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            System.out.println("File name checks passed (fallback check runs on Windows; Linux uses test_locale.sh).");
            return;
        }
        if (FileNames.representable(dir, "bad?name.zip")) throw new AssertionError("'?' should be refused on Windows");
        check(FileNames.resolve(dir, "bad?name.zip").getFileName().toString(), "bad_name.zip");
        check(FileNames.resolve(dir, "§c1.21.2 Redstone Torch§r.zip").getFileName().toString(), "§c1.21.2 Redstone Torch§r.zip");
        System.out.println("File name checks passed: odd names fall back to a plain name, good names stay as they are.");
    }

    private static void check(String got, String want) {
        if (!got.equals(want)) throw new AssertionError("got '" + got + "', want '" + want + "'");
    }
}
