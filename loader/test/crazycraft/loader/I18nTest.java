package crazycraft.loader;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class I18nTest {
    public static void main(String[] args) throws Exception {
        Map<String, Object> catalog;
        try (var in = I18nTest.class.getResourceAsStream("/crazycraft/loader/pt_br.json")) {
            catalog = Json.obj(Json.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        }
        I18n.select("pt-BR", false);
        for (var entry : catalog.entrySet()) {
            if (!(entry.getValue() instanceof String value) || value.isBlank()) throw new AssertionError(entry.getKey());
            if (!I18n.t(entry.getKey()).equals(entry.getValue())) throw new AssertionError(entry.getKey());
        }
        if (!I18n.t("Open folder").equals("Abrir pasta")) throw new AssertionError("Portuguese");
        if (!I18n.t("C:\\Games\\Downloads\\example.jar").equals("C:\\Games\\Downloads\\example.jar")) throw new AssertionError("File paths changed");
        if (!javax.swing.UIManager.getString("FileChooser.cancelButtonText").equals("Cancelar")) throw new AssertionError("File chooser");
        I18n.select("en_US", false);
        for (String english : catalog.keySet()) if (!I18n.t(english).equals(english)) throw new AssertionError("English fallback");
        I18n.initialize(new String[]{"--lang", "pt_br"});
        if (!I18n.portuguese()) throw new AssertionError("Command-line language override");
        System.out.println("Language checks passed: " + catalog.size() + " Portuguese translations; English and file paths preserved.");
    }
}
