# Project Specification: CraftZero (Minecraft 1.0 Clone)

**Project Name:** CraftZero
**Target Replication:** Minecraft Java Edition 1.0.0
**Language:** Java 21+
**Core Technology:** LWJGL 3, OpenGL 3.3+

---

## 1. Executive Summary
The goal is to recreate the core engine mechanics of Minecraft version 1.0. This includes infinite procedural voxel terrain, chunk-based rendering, player physics, and block interaction. We are avoiding general-purpose game engines (Unity/Unreal) in favor of a custom engine written in Java to replicate the original technical architecture.

## 2. Technical Stack

* **Language:** Java 21 (LTS).
* **Build Tool:** Maven.
* **Windowing & Input:** LWJGL 3 (GLFW).
* **Graphics API:** OpenGL 3.3 Core Profile.
    * *Constraint:* strictly **NO** deprecated Immediate Mode (`glBegin`/`glEnd`). Use VBOs and VAOs only.
* **Math:** JOML (Java OpenGL Math Library) for Matrix4f, Vector3f, and frustration culling.
* **Image Parsing:** STB Image (via LWJGL) for loading textures.

## 3. Project Architecture (MVC)

The project follows a modular architecture separating data, logic, and rendering.

### Directory Structure
```text
src/main/java/com/craftzero/
├── main/
│   └── Main.java            # Entry point, Game Loop instantiation
├── engine/
│   ├── Window.java          # GLFW window creation and callbacks
│   ├── Timer.java           # Delta time calculation
│   └── Input.java           # Static keyboard/mouse state manager
├── graphics/
│   ├── ShaderProgram.java   # GLSL compilation and uniform handling
│   ├── Renderer.java        # Master renderer
│   ├── Mesh.java            # VBO/VAO management
│   ├── Texture.java         # Texture loading and binding
│   └── Camera.java          # View/Projection matrix calculations
├── world/
│   ├── World.java           # Manages active chunks
│   ├── Chunk.java           # 16x256x16 voxel data & mesh generation
│   ├── Block.java           # Block logic
│   └── BlockType.java       # Enum (DIRT, STONE, GRASS, etc.)
├── math/
│   └── Noise.java           # Perlin/Simplex noise implementation
└── physics/
    └── AABB.java            # Axis-Aligned Bounding Box for collision

# CraftZero: Technical Specifications & Roadmap

## 4. Core System Specifications

### A. The Rendering Pipeline
1.  **Shaders:**
    * Implement standard Vertex (`scene.vert`) and Fragment (`scene.frag`) shaders.
    * **Attributes:** Position (vec3), TexCoord (vec2), Normal (vec3) (optional if using face culling strictly), Lighting (float/int).
    * **Uniforms:** ProjectionMatrix, ViewMatrix, TransformationMatrix (if needed per chunk).
2.  **Texture Atlas:**
    * Load a single `atlas.png` file using `STBImage`.
    * **UV Mapping:** Calculate UV coordinates dynamically in the mesh builder based on:
        * Block ID (e.g., ID 1 = Stone).
        * Face Direction (e.g., Grass block: Top face = index 0, Side faces = index 3).
    * *Tip:* Use texture coordinates divisible by 16 (if using a 16x16 pixel grid) to prevent "bleeding" between blocks.

### B. The Chunk System (Critical Optimization)
* **Dimensions:** 16 (Width X) * 256 (Height Y) * 16 (Depth Z).
* **Data Structure:** Flattened 1D array is mandatory for cache locality.
    * `short[] blocks = new short[65536];`
    * Access formula: `index = x + (z * 16) + (y * 16 * 16)` (Adjust based on Y-up preference).
* **The "Greedy Meshing" or "Face Culling" Algorithm:**
    * **Goal:** Reduce triangle count.
    * **Logic:** Iterate through all `x, y, z` in the chunk.
    * **Check:** For every block, check its 6 neighbors (North, South, East, West, Up, Down).
    * **Rule:** If a neighbor is **Solid/Opaque**, **do not** add vertices for that face to the Mesh. Only add vertices if the neighbor is **Air** or **Transparent** (Water/Glass).
    * **Batching:** The `Chunk` class must compile all valid vertices into a *single* FloatBuffer to be uploaded to the GPU as one VAO.

### C. World Generation
* **Method:** Perlin Noise (using 2D noise for surface height, 3D noise for caves).
* **Biomes:** Implement 3 basic distinct terrain types:
    1.  **Plains:** Low amplitude, low frequency noise.
    2.  **Hills:** High amplitude noise.
    3.  **Forest:** Plains terrain with "Tree" structures (Vertical log column + leaf sphere) generated during the population pass.
* **Seed System:** Use a `Random(seed)` instance to ensure world generation is deterministic.

### D. Physics & Controls
* **Movement:** Standard WASD movement.
    * Implement `velX`, `velY`, `velZ`.
    * Apply friction/drag when keys are released.
* **Camera:** First Person View.
    * Mouse moves `yaw` and `pitch`.
    * Pitch clamped: `-90.0f` to `90.0f` (cannot look inside own body or flip over).
* **Collision Detection:**
    * **AABB (Axis-Aligned Bounding Box):**
        * Player Box: `0.6w x 1.8h x 0.6d`.
        * Block Box: `1.0 x 1.0 x 1.0`.
    * **Collision Resolution:**
        1.  Apply X velocity. Check for collision. If hit, reset X to edge of block.
        2.  Apply Y velocity. Check for collision. If hit, reset Y (land on ground or hit head).
        3.  Apply Z velocity. Check for collision. If hit, reset Z.
* **Interaction (Raycasting):**
    * Use **DDA (Digital Differential Analyzer)** to cast a ray from `Camera.position` along `Camera.forward` vector.
    * Max distance: 5.0 blocks.
    * Return: The exact integer coordinates `(x, y, z)` of the hit block.

---

## 5. Development Phases (Instruction for Agent)

**Phase 1: Skeleton & Window**
* **Goal:** A blank window running at 60 FPS.
* **Tasks:** Initialize Maven project, set up LWJGL 3 window, create the Game Loop with delta time calculation.

**Phase 2: The Rendering Core**
* **Goal:** See a single block in 3D space.
* **Tasks:** Create `ShaderProgram`, `Mesh` (VAO/VBO), and `Texture` classes. Render a single static cube with a test texture.

**Phase 3: The Chunk Architecture**
* **Goal:** Efficient voxel rendering.
* **Tasks:** Implement `Chunk` class with the `blocks` array. Write the **Face Culling** logic. Render a full 16x16x16 chunk of random blocks (without lagging).

**Phase 4: World & Camera**
* **Goal:** Walking around a generated world.
* **Tasks:** Implement the FPS Camera (Mouse/Keyboard). Create `World` class to manage a grid of chunks. Implement basic Perlin Noise heightmaps.

**Phase 5: Physics & Interaction**
* **Goal:** Gameplay loop.
* **Tasks:** Implement AABB collision (gravity, jumping, wall stops). Implement Raycasting to destroy (Left Click) and place (Right Click) blocks.

---

## 6. Asset Requirements

* **Textures:**
    * File: `src/main/resources/textures/atlas.png`
    * Format: Standard 16x16 grid Minecraft-style atlas.
* **Shaders:**
    * Vertex: `src/main/resources/shaders/scene.vert`
    * Fragment: `src/main/resources/shaders/scene.frag`
* **Libraries:**
    * LWJGL 3 (Assimp, GLFW, OpenGL, OpenAL, STB).
    * JOML (Java OpenGL Math Library).