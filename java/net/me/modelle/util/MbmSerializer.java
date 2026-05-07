package net.me.modelle.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MbmSerializer {
    private static final String MAGIC = "MBM";
    private static final int VERSION = 1;

    public static void save(File file, MbmData data) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeUTF(MAGIC);
            out.writeInt(VERSION);

            // 1. Текстура
            out.writeInt(data.textureBytes().length);
            out.write(data.textureBytes());

            // 2. Вершины
            out.writeInt(data.vertices().size());
            for (RawData.BakedVertex v : data.vertices()) {
                out.writeFloat(v.x); out.writeFloat(v.y); out.writeFloat(v.z);
                out.writeFloat(v.u); out.writeFloat(v.v);
                out.writeInt(v.rgba); // Пишем 1 число вместо 4
            }
        }
    }

    public static MbmData load(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            if (!in.readUTF().equals(MAGIC)) throw new IOException("Not a MBM file");
            int ver = in.readInt();

            // 1. Текстура
            int texLen = in.readInt();
            byte[] texBytes = new byte[texLen];
            in.readFully(texBytes);

            // 2. Вершины
            int vCount = in.readInt();
            List<RawData.BakedVertex> vertices = new ArrayList<>(vCount);
            for (int i = 0; i < vCount; i++) {
                vertices.add(new RawData.BakedVertex(
                        in.readFloat(), in.readFloat(), in.readFloat(),
                        in.readFloat(), in.readFloat(),
                        in.readInt() // Читаем 1 число
                ));
            }
            return new MbmData(texBytes, vertices);
        }
    }
}
