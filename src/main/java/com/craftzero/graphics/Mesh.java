package com.craftzero.graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh class for VAO/VBO management.
 * Uses modern OpenGL (no immediate mode).
 */
public class Mesh {
    
    private int vaoId;
    private int posVboId;
    private int texVboId;
    private int normalVboId;
    private int idxVboId;
    private int vertexCount;
    
    /**
     * Create a mesh with positions, texture coordinates, normals, and indices.
     */
    public Mesh(float[] positions, float[] texCoords, float[] normals, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer texBuffer = null;
        FloatBuffer normalBuffer = null;
        IntBuffer indicesBuffer = null;
        
        try {
            vertexCount = indices.length;
            
            // Create VAO
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);
            
            // Position VBO
            posVboId = glGenBuffers();
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            
            // Texture coordinates VBO
            texVboId = glGenBuffers();
            texBuffer = MemoryUtil.memAllocFloat(texCoords.length);
            texBuffer.put(texCoords).flip();
            glBindBuffer(GL_ARRAY_BUFFER, texVboId);
            glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            
            // Normals VBO
            normalVboId = glGenBuffers();
            normalBuffer = MemoryUtil.memAllocFloat(normals.length);
            normalBuffer.put(normals).flip();
            glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            
            // Index VBO
            idxVboId = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
            
            // Unbind VAO
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            
        } finally {
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }
            if (texBuffer != null) {
                MemoryUtil.memFree(texBuffer);
            }
            if (normalBuffer != null) {
                MemoryUtil.memFree(normalBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }
    
    /**
     * Create a simple mesh with just positions and texture coordinates.
     */
    public Mesh(float[] positions, float[] texCoords, int[] indices) {
        this(positions, texCoords, createDefaultNormals(positions.length / 3), indices);
    }
    
    private static float[] createDefaultNormals(int vertexCount) {
        float[] normals = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            normals[i * 3] = 0;
            normals[i * 3 + 1] = 1;
            normals[i * 3 + 2] = 0;
        }
        return normals;
    }
    
    public int getVaoId() {
        return vaoId;
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        
        // Delete VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(texVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(idxVboId);
        
        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
    
    /**
     * Creates a simple cube mesh for testing.
     */
    public static Mesh createCube(float size) {
        float s = size / 2;
        
        // 8 vertices, but we need 24 for proper UV and normals (4 per face)
        float[] positions = {
            // Front face
            -s, -s,  s,   s, -s,  s,   s,  s,  s,  -s,  s,  s,
            // Back face
             s, -s, -s,  -s, -s, -s,  -s,  s, -s,   s,  s, -s,
            // Top face
            -s,  s,  s,   s,  s,  s,   s,  s, -s,  -s,  s, -s,
            // Bottom face
            -s, -s, -s,   s, -s, -s,   s, -s,  s,  -s, -s,  s,
            // Right face
             s, -s,  s,   s, -s, -s,   s,  s, -s,   s,  s,  s,
            // Left face
            -s, -s, -s,  -s, -s,  s,  -s,  s,  s,  -s,  s, -s
        };
        
        float[] texCoords = {
            // Front
            0, 1,  1, 1,  1, 0,  0, 0,
            // Back
            0, 1,  1, 1,  1, 0,  0, 0,
            // Top
            0, 1,  1, 1,  1, 0,  0, 0,
            // Bottom
            0, 1,  1, 1,  1, 0,  0, 0,
            // Right
            0, 1,  1, 1,  1, 0,  0, 0,
            // Left
            0, 1,  1, 1,  1, 0,  0, 0
        };
        
        float[] normals = {
            // Front
            0, 0, 1,  0, 0, 1,  0, 0, 1,  0, 0, 1,
            // Back
            0, 0, -1,  0, 0, -1,  0, 0, -1,  0, 0, -1,
            // Top
            0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0,
            // Bottom
            0, -1, 0,  0, -1, 0,  0, -1, 0,  0, -1, 0,
            // Right
            1, 0, 0,  1, 0, 0,  1, 0, 0,  1, 0, 0,
            // Left
            -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0
        };
        
        int[] indices = {
            // Front
            0, 1, 2,  2, 3, 0,
            // Back
            4, 5, 6,  6, 7, 4,
            // Top
            8, 9, 10,  10, 11, 8,
            // Bottom
            12, 13, 14,  14, 15, 12,
            // Right
            16, 17, 18,  18, 19, 16,
            // Left
            20, 21, 22,  22, 23, 20
        };
        
        return new Mesh(positions, texCoords, normals, indices);
    }
}
