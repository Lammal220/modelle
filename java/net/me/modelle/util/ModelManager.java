package net.me.modelle.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;

public class ModelManager {
    public static final File MODELS_DIR = FMLPaths.CONFIGDIR.get().resolve("modelle/models").toFile();
    public static final File CACHE_DIR = FMLPaths.CONFIGDIR.get().resolve("modelle/cache").toFile();

    // 🔒 Лимит: 50 МБ на модель. При превышении — отказ.
    public static final long MAX_MODEL_SIZE = 50L * 1024 * 1024;

    public static void init() {
        if (!MODELS_DIR.exists()) MODELS_DIR.mkdirs();
        if (!CACHE_DIR.exists()) CACHE_DIR.mkdirs();
        clearTemporaryCache();
    }

    public static void clearTemporaryCache() {
        File[] files = CACHE_DIR.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
    }

    public static File getModelStorageDir(Level level) {
        File worldDir;
        if (level.isClientSide) {
            if (Minecraft.getInstance().getSingleplayerServer() != null) {
                worldDir = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();
            } else {
                return CACHE_DIR;
            }
        } else {
            worldDir = level.getServer().getWorldPath(LevelResource.ROOT).toFile();
        }
        File storage = new File(worldDir, "modelle_data");
        if (!storage.exists()) storage.mkdirs();
        return storage;
    }

    // 🔒 Валидация имени папки: только безопасные символы
    private static boolean isValidFolderName(String name) {
        if (name == null || name.isEmpty() || name.length() > 100) return false;
        return name.matches("^[a-zA-Z0-9_\\-\\.]+$");
    }

    public static String convertAndSaveToWorld(String folderName, Level level) {
        if (!isValidFolderName(folderName)) {
            System.err.println("[Modelle] Недопустимое имя папки: " + folderName);
            return "";
        }
        try {
            File folder = new File(MODELS_DIR, folderName);
            File obj = new File(folder, "model.obj");
            File png = new File(folder, "texture.png");

            if (!obj.exists() || !png.exists()) return "";

            // 🔒 Проверка размера исходников
            if (obj.length() > MAX_MODEL_SIZE || png.length() > MAX_MODEL_SIZE / 2) {
                System.err.println("[Modelle] Файлы модели слишком большие");
                return "";
            }

            RawData model = ObjParser.parse(obj);
            model.bake();
            byte[] pngBytes = Files.readAllBytes(png.toPath());
            MbmData data = new MbmData(pngBytes, model.bakedVertices);

            File temp = File.createTempFile("modelle", ".tmp");
            MbmSerializer.save(temp, data);

            // 🔒 Проверка результата
            if (temp.length() > MAX_MODEL_SIZE) {
                temp.delete();
                System.err.println("[Modelle] Сконвертированная модель превышает лимит");
                return "";
            }

            String hash = getFileHash(temp);

            File storage = getModelStorageDir(level);
            File finalFile = new File(storage, hash + ".mbm");
            if (!finalFile.exists()) {
                Files.move(temp.toPath(), finalFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } else {
                temp.delete();
            }

            MbmSerializer.save(new File(folder, "model.mbm"), data);
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}