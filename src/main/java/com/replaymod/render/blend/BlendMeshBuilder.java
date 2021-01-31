package com.replaymod.render.blend;

import com.mojang.datafixers.util.Pair;
import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DMesh;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.ReadableVector3f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.getElements;

public class BlendMeshBuilder
        extends BufferBuilder {
    private static final ReadableVector3f VEC3F_ZERO = new Vector3f(0, 0, 0);
    private final DMesh mesh;
    private Vector3f offset;
    private boolean isDrawing;
    private boolean wellBehaved;

    public BlendMeshBuilder(DMesh mesh) {
        super(1024);
        this.mesh = mesh;
    }

    public void setWellBehaved(boolean wellBehaved) {
        this.wellBehaved = wellBehaved;
    }

    public void setReverseOffset(Vector3f offset) {
        this.offset = offset;
    }

    @Override
    public void begin(int mode, VertexFormat vertexFormat) {
        if (isDrawing) {
            if (!wellBehaved) {
                // Someone probably finished drawing with the global instance instead of this one,
                // let's just assume that what's happened and finish our last draw by ourselves
                // (might miss correct texture though)
                super.finishDrawing();
                addBufferToMesh();
            } else {
                throw new IllegalStateException("Already drawing!");
            }
        }
        this.isDrawing = true;

        if (!wellBehaved) {
            // In case the calling code finishes with Tessellator.getInstance().draw()
            Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
        }

        super.begin(mode, vertexFormat);
    }

    public void maybeFinishDrawing() {
        if (isDrawing) {
            isDrawing = false;
            super.finishDrawing();
            addBufferToMesh();
        }
    }

    @Override
    public void finishDrawing() {
        if (!isDrawing) {
            throw new IllegalStateException("Not building!");
        } else {
            if (!wellBehaved) {
                Tessellator.getInstance().getBuffer().finishDrawing();
            }

            super.finishDrawing();

            addBufferToMesh();

        }
    }

    private void addBufferToMesh() {
        addBufferToMesh(this, mesh, offset);
    }

    public static DMesh addBufferToMesh(BufferBuilder bufferBuilder, DMesh mesh, ReadableVector3f vertOffset) {
        Pair<DrawState, ByteBuffer> data = bufferBuilder.getNextBuffer();
        return addBufferToMesh(data.getSecond(), data.getFirst().getDrawMode(), data.getFirst().getFormat(), mesh, vertOffset);
    }

    public static DMesh addBufferToMesh(ByteBuffer buffer, int mode, VertexFormat vertexFormat, DMesh mesh, ReadableVector3f vertOffset) {
        int vertexCount = buffer.remaining() / vertexFormat.getSize();
        return addBufferToMesh(buffer, vertexCount, mode, vertexFormat, mesh, vertOffset);
    }

    public static DMesh addBufferToMesh(ByteBuffer buffer, int vertexCount, int mode, VertexFormat vertexFormat, DMesh mesh, ReadableVector3f vertOffset) {
        if (mesh == null) {
            mesh = new DMesh();
        }
        if (vertOffset == null) {
            vertOffset = VEC3F_ZERO;
        }

        // Determine offset of vertex components
        int posOffset = -1, colorOffset = -1, uvOffset = -1;
        int index = 0;
        int elementOffset = 0;
        for (VertexFormatElement element : getElements(vertexFormat)) {
            int offset = elementOffset;
            elementOffset += element.getSize();
            switch (element.getUsage()) {
                case POSITION:
                    if (element.getType() != VertexFormatElement.Type.FLOAT) {
                        throw new UnsupportedOperationException("Only float is supported for position elements!");
                    }
                    posOffset = offset;
                    break;
                case COLOR:
                    if (element.getType() != VertexFormatElement.Type.UBYTE) {
                        throw new UnsupportedOperationException("Only unsigned byte is supported for color elements!");
                    }
                    colorOffset = offset;
                    break;
                case UV:
                    if (element.getIndex() != 0) break;
                    if (element.getType() != VertexFormatElement.Type.FLOAT) {
                        throw new UnsupportedOperationException("Only float is supported for UV elements!");
                    }
                    uvOffset = offset;
                    break;
            }
            index++;
        }
        if (posOffset == -1) throw new IllegalStateException("No position element in " + vertexFormat);

        // Extract vertex components from byte buffer
        List<DMesh.Vertex> vertices = new ArrayList<>(vertexCount);
        List<Vector2f> uvs = new ArrayList<>(vertexCount);
        List<Integer> colors = new ArrayList<>(vertexCount);
        int step = vertexFormat.getSize();
        for (int offset = 0; offset < vertexCount * step; offset += step) {
            vertices.add(new DMesh.Vertex(
                    buffer.getFloat(offset) - vertOffset.getX(),
                    -buffer.getFloat(offset + 8) + vertOffset.getZ(),
                    buffer.getFloat(offset + 4) - vertOffset.getY()
            ));

            if (colorOffset != -1) {
                colors.add(buffer.getInt(offset + colorOffset));
            } else {
                colors.add(0xffffffff);
            }

            if (uvOffset != -1) {
                uvs.add(new Vector2f(
                        buffer.getFloat(offset + uvOffset),
                        1 - buffer.getFloat(offset + uvOffset + 4)
                ));
            } else {
                uvs.add(new Vector2f(0, 0));
            }
        }

        // Determine and store current material
        DMaterial activeMaterial = BlendState.getState().getMaterials().getActiveMaterial();
        int materialSlot = mesh.materials.indexOf(activeMaterial);
        if (materialSlot < 0) {
            materialSlot = mesh.materials.size();
            mesh.materials.add(activeMaterial);
        }

        // Bundle vertices into shapes and add them to the mesh
        switch (mode) {
            case GL11.GL_TRIANGLES:
                for (int i = 0; i < vertices.size(); i += 3) {
                    mesh.addTriangle(
                            vertices.get(i),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            uvs.get(i),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            colors.get(i),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            materialSlot
                    );
                }
                break;
            case GL11.GL_QUADS:
                for (int i = 0; i < vertices.size(); i += 4) {
                    mesh.addQuad(
                            vertices.get(i),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            vertices.get(i + 3),
                            uvs.get(i),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            uvs.get(i + 3),
                            colors.get(i),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            colors.get(i + 3),
                            materialSlot
                    );
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported mode: " + mode);
        }

        return mesh;
    }
}
