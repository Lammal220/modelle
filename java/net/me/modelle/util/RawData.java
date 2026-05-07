package net.me.modelle.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class RawData {
    public final List<Vec3> vertices = new ArrayList<>();
    public final List<Vector2f> uvs = new ArrayList<>();
    public final List<Vec3> normals = new ArrayList<>();
    public final List<Face> faces = new ArrayList<>();

    public final List<BakedVertex> bakedVertices = new ArrayList<>();

    public static class BakedVertex {
        public final float x, y, z, u, v;
        public final int rgba;

        public BakedVertex(float x, float y, float z, float u, float v, int r, int g, int b, int a) {
            this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
            this.rgba = ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
        }

        public BakedVertex(float x, float y, float z, float u, float v, int rgba) {
            this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
            this.rgba = rgba;
        }
    }

    public void bake() {
        bakedVertices.clear();
        for (Face face : faces) {
            for (int i = 0; i < 3; i++) {
                int vertexIndex = face.vertexIndices[i];
                int uvIndex = face.uvIndices[i];

                if (vertexIndex < 0 || vertexIndex >= vertices.size()) continue;

                Vec3 vertex = vertices.get(vertexIndex);
                Vector2f uv = (uvIndex >= 0 && uvIndex < uvs.size()) ? uvs.get(uvIndex) : new Vector2f(0, 0);

                float nx = 0, ny = 1, nz = 0;
                if (face.normalIndices[i] >= 0 && face.normalIndices[i] < normals.size()) {
                    Vec3 normal = normals.get(face.normalIndices[i]);
                    nx = (float) normal.x;
                    ny = (float) normal.y;
                    nz = (float) normal.z;
                }

                float shadow = 0.6f + 0.4f * ny;
                if (ny < 0) shadow = 0.6f + 0.3f * ny;
                shadow *= (1.0f - Math.abs(nx) * 0.1f);
                shadow *= (1.0f - Math.abs(nz) * 0.05f);

                int colorValue = (int)(255 * shadow);

                bakedVertices.add(new BakedVertex(
                        (float)vertex.x, (float)vertex.y, (float)vertex.z,
                        uv.x, 1f - uv.y,
                        colorValue, colorValue, colorValue, 255
                ));
            }
        }
    }

    public static class Face {
        public final int[] vertexIndices = new int[3];
        public final int[] uvIndices = new int[3];
        public final int[] normalIndices = new int[3];

        public Face() {
            for (int i = 0; i < 3; i++) {
                vertexIndices[i] = -1;
                uvIndices[i] = -1;
                normalIndices[i] = -1;
            }
        }
    }
}