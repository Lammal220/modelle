package net.me.modelle.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.io.*;

public class ObjParser {
    public static RawData parse(File file) {
        RawData model = new RawData();

        try (InputStream stream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v":
                        model.vertices.add(new Vec3(
                                Double.parseDouble(tokens[1]),
                                Double.parseDouble(tokens[2]),
                                Double.parseDouble(tokens[3])
                        ));
                        break;
                    case "vt":
                        model.uvs.add(new Vector2f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2])
                        ));
                        break;
                    case "vn":
                        model.normals.add(new Vec3(
                                Double.parseDouble(tokens[1]),
                                Double.parseDouble(tokens[2]),
                                Double.parseDouble(tokens[3])
                        ));
                        break;
                    case "f":
                        int vertexCount = tokens.length - 1;
                        if (vertexCount < 3) continue;

                        int[][] indices = new int[vertexCount][3];
                        for (int i = 0; i < vertexCount; i++) {
                            String[] parts = tokens[i + 1].split("/");
                            indices[i][0] = parseIndex(parts[0], model.vertices.size());
                            indices[i][1] = (parts.length > 1 && !parts[1].isEmpty()) ? parseIndex(parts[1], model.uvs.size()) : -1;
                            indices[i][2] = (parts.length > 2 && !parts[2].isEmpty()) ? parseIndex(parts[2], model.normals.size()) : -1;
                        }

                        for (int i = 1; i < vertexCount - 1; i++) {
                            RawData.Face face = new RawData.Face();
                            face.vertexIndices[0] = indices[0][0];
                            face.uvIndices[0] = indices[0][1];
                            face.normalIndices[0] = indices[0][2];

                            face.vertexIndices[1] = indices[i][0];
                            face.uvIndices[1] = indices[i][1];
                            face.normalIndices[1] = indices[i][2];

                            face.vertexIndices[2] = indices[i + 1][0];
                            face.uvIndices[2] = indices[i + 1][1];
                            face.normalIndices[2] = indices[i + 1][2];

                            model.faces.add(face);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    private static int parseIndex(String token, int size) {
        int idx = Integer.parseInt(token);
        return idx < 0 ? size + idx : idx - 1;
    }
}