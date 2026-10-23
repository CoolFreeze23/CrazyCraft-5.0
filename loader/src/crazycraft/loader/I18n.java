package crazycraft.loader;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JComboBox;
import javax.swing.UIManager;

/** Installer language, independent of the game's language and file formats. */
final class I18n {
    private static final Map<String, Object> PT = load();
    private static String language = initial();

    private static Map<String, Object> load() {
        try (var in = I18n.class.getResourceAsStream("/crazycraft/loader/pt_br.json")) {
            if (in == null) throw new IllegalStateException("Missing Portuguese installer translations");
            return Json.obj(Json.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    private static String initial() {
        String env = System.getenv("CRAZYCRAFT_LANG");
        if (env != null && !env.isBlank()) return normalize(env);
        try {
            String saved = Preferences.userRoot().node("crazycraft/setup").get("language", "");
            if (!saved.isEmpty()) return normalize(saved);
        } catch (SecurityException ignored) { }
        return Locale.getDefault().getLanguage().equals("pt") ? "pt_br" : "en_us";
    }

    private static String normalize(String language) {
        return language.toLowerCase(Locale.ROOT).replace('-', '_').startsWith("pt") ? "pt_br" : "en_us";
    }

    static boolean portuguese() { return language.equals("pt_br"); }
    static void select(String value, boolean remember) {
        language = normalize(value);
        if (remember) try { Preferences.userRoot().node("crazycraft/setup").put("language", language); }
        catch (SecurityException ignored) { }
        // Swing's native file chooser does not consistently ship a Brazilian Portuguese bundle.
        String[][] labels = {
            {"OptionPane.okButtonText", "OK", "OK"}, {"OptionPane.cancelButtonText", "Cancel", "Cancelar"},
            {"FileChooser.openButtonText", "Open", "Abrir"}, {"FileChooser.cancelButtonText", "Cancel", "Cancelar"},
            {"FileChooser.openDialogTitleText", "Open", "Abrir"}, {"FileChooser.lookInLabelText", "Look in:", "Procurar em:"},
            {"FileChooser.fileNameLabelText", "File name:", "Nome do arquivo:"}, {"FileChooser.filesOfTypeLabelText", "Files of type:", "Tipo de arquivo:"},
            {"FileChooser.upFolderToolTipText", "Up one level", "Subir um nível"}, {"FileChooser.homeFolderToolTipText", "Home", "Pasta pessoal"},
            {"FileChooser.newFolderToolTipText", "New folder", "Nova pasta"}, {"FileChooser.listViewButtonToolTipText", "List", "Lista"},
            {"FileChooser.detailsViewButtonToolTipText", "Details", "Detalhes"}, {"FileChooser.acceptAllFileFilterText", "All files", "Todos os arquivos"},
            {"FileChooser.directoryOpenButtonText", "Open", "Abrir"}, {"FileChooser.saveButtonText", "Save", "Salvar"}
        };
        for (String[] label : labels) UIManager.put(label[0], label[portuguese() ? 2 : 1]);
    }

    static void initialize(String[] args) {
        for (int i = 0; i + 1 < args.length; i++) if (args[i].equals("--lang")) language = normalize(args[i + 1]);
        select(language, false);
    }

    static String t(String english) { return portuguese() ? (String) PT.getOrDefault(english, english) : english; }

    static JComboBox<String> chooser(Runnable changed) {
        JComboBox<String> box = new JComboBox<>(new String[]{"English", "Português (Brasil)"});
        box.setSelectedIndex(portuguese() ? 1 : 0);
        box.setToolTipText("Language / Idioma");
        box.addActionListener(e -> { select(box.getSelectedIndex() == 1 ? "pt_br" : "en_us", true); changed.run(); });
        return box;
    }
}
