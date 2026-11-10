package com.legendfps;

import net.fabricmc.loader.api.FabricLoader;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

/** Imports and validates a classic 64x64 or legacy 64x32 Minecraft skin PNG. */
public final class SkinImport {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("legendfps");
    private static final Path SKIN = DIR.resolve("imported-skin.png");
    private SkinImport() {}

    public static Path importedSkin() {
        return Files.isRegularFile(SKIN) ? SKIN : null;
    }

    public static String importPng() {
        if (GraphicsEnvironment.isHeadless()) return "File picker unavailable in headless mode";
        AtomicReference<File> selected = new AtomicReference<>();
        try {
            Runnable choose = () -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Import Minecraft skin PNG");
                chooser.setFileFilter(new FileNameExtensionFilter("PNG skin (64x64 or 64x32)", "png"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected.set(chooser.getSelectedFile());
                }
            };
            if (SwingUtilities.isEventDispatchThread()) choose.run();
            else SwingUtilities.invokeAndWait(choose);
            File file = selected.get();
            if (file == null) return "Skin import cancelled";
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() != 64 ||
                    (image.getHeight() != 64 && image.getHeight() != 32)) {
                return "Invalid skin: PNG must be 64x64 or 64x32";
            }
            Files.createDirectories(DIR);
            Files.copy(file.toPath(), SKIN, StandardCopyOption.REPLACE_EXISTING);
            return "Skin PNG imported: " + image.getWidth() + "x" + image.getHeight()
                    + " • saved locally (not uploaded to your Minecraft account)";
        } catch (Exception e) {
            return "Could not import skin: " + e.getClass().getSimpleName();
        }
    }
}
