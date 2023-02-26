package io.xeros.model.collisionmap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.xeros.Server;
import io.xeros.model.collisionmap.doors.Location;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.player.Boundary;
import io.xeros.model.entity.player.Player;
import io.xeros.model.world.objects.GlobalObject;
import io.xeros.util.Misc;

public class Region {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(Region.class.getName());
    private static final String CUSTOM_MAPS_DIR = Server.getDataDirectory() + "/mapdata/custom_maps/";
    private final int id;
    private int[][][] clips = new int[4][][];
    private int[][][] shootable = new int[4][][];
    private final boolean members;
    private final ArrayList<WorldObject> worldObjects = new ArrayList<>();
    private final RegionProvider provider;
    private static int customMapFiles;
    private static final ArrayList<Integer> errors = Lists.newArrayList();

    public Region(RegionProvider provider, int id, boolean members) {
        this.id = id;
        this.members = members;
        this.provider = provider;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Region other = (Region) obj;
        return id == other.id;
    }

    public Region clone(RegionProvider provider) {
        Region region = new Region(provider, id, false);
        region.clips = clone(clips);
        region.shootable = clone(shootable);
        region.worldObjects.addAll(worldObjects);
        Preconditions.checkState(region.equals(this));
        return region;
    }

    private int[][][] clone(int[][][] array) {
        int[][][] clone = new int[array.length][][];
        for (int z = 0; z < array.length; z++) {
            if (array[z] != null) {
                clone[z] = new int[array[z].length][];
                for (int x = 0; x < array[z].length; x++) {
                    clone[z][x] = new int[array[z][z].length];
                    for (int y = 0; y < array[z][x].length; y++) {
                        clone[z][x][y] = array[z][x][y];
                        if (provider.isOccupiedByFullBlockNpc(x, y, z))
                            clone[z][x][y] -= RegionProvider.FULL_NPC_TILE_FLAG;
                        if (provider.isOccupiedByNpc(x, y, z))
                            clone[z][x][y] -= RegionProvider.NPC_TILE_FLAG;
                    }
                }
            }
        }
        return clone;
    }

    public int[][][] getClips() {
        return clips;
    }

    /**
     * Determines if an object is real or not. If the Collection of regions and real objects contains the properties passed in the parameters then the object will be determined
     * real
     *
     * @param id the id of the object
     * @param x the x coordinate of the object
     * @param y the y coordinate of the object
     * @param height the height of the object
     * @return
     */
    public boolean isWorldObject(int id, int x, int y, int height) {
        int z = height % 4;
        return worldObjects.stream().anyMatch(object -> object.id == id && object.x == x && object.y == y && object.height == z);
    }

    public Optional<WorldObject> getWorldObject(int id, int x, int y, int height) {
        final int z = height % 4;
        return worldObjects.stream().filter(object -> object.id == id && object.x == x && object.y == y && object.height == z).findFirst();
    }

    /**
     * Adds a {@link WorldObject} to the {@link WorldObject} map based on the x, y, height, and identification of the object.
     *
     * @param id the id of the object
     * @param x the x position of the object
     * @param y the y position of the objec
     *             t
     * @param height the height of the object
     */
    public void addWorldObject(int id, int x, int y, int height, int type, int face) {
        int z = height % 4;
        worldObjects.add(new WorldObject(id, x, y, z, type, face));
    }

    /**
     * A convenience method for lamda expressions
     *
     * @param object the world object being added
     */
    public void addWorldObject(WorldObject object) {
        addWorldObject(object.getId(), object.getX(), object.getY(), object.getHeight(), object.getType(), object.getFace());
    }

    public void addWorldObject(GlobalObject object) {
        addWorldObject(object.getObjectId(), object.getX(), object.getY(), object.getHeight(), object.getType(), object.getFace());
    }

    public void removeWorldObject(GlobalObject object) {
        worldObjects.removeIf(object::samePositionAndType);
    }

    private void addProjectileClip(int x, int y, int height, int shift) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (shootable[height] == null) {
            shootable[height] = new int[64][64];
        }
        shootable[height][x - regionAbsX][y - regionAbsY] |= shift;
    }

    private void removeProjectileClip(int x, int y, int height, int shift) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (shootable[height] == null) {
            shootable[height] = new int[64][64];
        }
        shootable[height][x - regionAbsX][y - regionAbsY] += shift;
    }

    public void addClip(int x, int y, int height, int shift) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (clips[height] == null) {
            clips[height] = new int[64][64];
        }

        clips[height][x - regionAbsX][y - regionAbsY] |= shift;
    }

    public void removeClip(int x, int y, int height, int shift) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (clips[height] == null) {
            clips[height] = new int[64][64];
        }
        clips[height][x - regionAbsX][y - regionAbsY] += shift;
        if (clips[height][x - regionAbsX][y - regionAbsY] < 0) clips[height][x - regionAbsX][y - regionAbsY] = 0;
    }

    public void setClipToZero(int x, int y, int height) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (clips[height] == null) {
            clips[height] = new int[64][64];
        }
        clips[height][x - regionAbsX][y - regionAbsY] = 0;
    }

    public int getClip(int x, int y, int height) {
        height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (clips[height] == null) {
            return 0;
        }
        return clips[height][x - regionAbsX][y - regionAbsY];
    }

    int getProjectileClip(int x, int y, int height) {
        if (height > 3) height = height % 4;
        int regionAbsX = (id >> 8) * 64;
        int regionAbsY = (id & 255) * 64;
        if (shootable[height] == null) {
            return 0;
        }
        return shootable[height][x - regionAbsX][y - regionAbsY];
    }

    public void removeObject(int objectId, int x, int y, int height, int type, int direction) {
        ObjectDef def = ObjectDef.getObjectDef(objectId);
        if (def == null) {
            return;
        }
        int xLength;
        int yLength;
        if (direction != 1 && direction != 3) {
            xLength = def.xLength();
            yLength = def.yLength();
        } else {
            xLength = def.yLength();
            yLength = def.xLength();
        }
        if (type == 22) {
            if (def.aBoolean767() && def.hasActions()) {
                addClipping(x, y, height, -2097152);
                if (def.aBoolean757) {
                    addProjectileClipping(x, y, height, -2097152);
                }
            }
        } else if (type >= 9) {
            if (def.aBoolean767()) {
                removeClippingForSolidObject(x, y, height, xLength, yLength, false);
                if (def.aBoolean757) {
                    removeProjectileClippingForSolidObject(x, y, height, xLength, yLength, true);
                }
            }
        } else if (type >= 0 && type <= 3) {
            if (def.aBoolean767()) {
                setClippingForVariableObject(x, y, height, type, direction, def.solid(), true);
                if (def.aBoolean757) {
                    setProjectileClippingForVariableObject(x, y, height, type, direction, def.solid(), true);
                }
            }
        }
    }

//	private static boolean removeDoor(int regionId, int objectId, int x, int y, int height, int type, int direction) {
//		ObjectDef def = ObjectDef.getObjectDef(objectId);
//		if (def != null) {
//			if (def.name != null) {
//				if (def.name.toLowerCase().contains(("door")) || def.name.toLowerCase().contains(("gate"))) {
//					Server.getGlobalObjects().add(new GlobalObject(-1, x, y, height, direction, type));
//					return true;
//				}
//			}
//		}
//		
//		return false;
//	}
    public void addObject(int objectId, int x, int y, int height, int type, int direction) {
        ObjectDef def = ObjectDef.getObjectDef(objectId);
        if (def == null) {
            return;
        }
        int xLength;
        int yLength;
        if (direction != 1 && direction != 3) {
            xLength = def.xLength();
            yLength = def.yLength();
        } else {
            xLength = def.yLength();
            yLength = def.xLength();
        }
        if (objectId == 29165) {
            return; // Idk why this is popping up in edgeville? Mounted coins.
        }
        if (type == 22) {
            if (def.hasActions() && def.aBoolean767()) {
                addClipping(x, y, height, 2097152);
                if (def.aBoolean757) {
                    addProjectileClipping(x, y, height, 2097152);
                }
            }
        } else if (type >= 9) {
            if (def.aBoolean767()) {
                addClippingForSolidObject(x, y, height, xLength, yLength, def.solid());
                if (def.aBoolean757) {
                    addProjectileClippingForSolidObject(x, y, height, xLength, yLength, true);
                }
            }
        } else if (type >= 0 && type <= 3) {
            if (def.aBoolean767()) {
                setClippingForVariableObject(x, y, height, type, direction, def.solid(), false);
                if (def.aBoolean757) {
                    setProjectileClippingForVariableObject(x, y, height, type, direction, def.solid(), false);
                }
            }
        }
    }

    public int[] getNextStep(int baseX, int baseY, int toX, int toY, int height, int xLength, int yLength) {
        int moveX = 0;
        int moveY = 0;
        if (baseX - toX > 0) {
            moveX--;
        } else if (baseX - toX < 0) {
            moveX++;
        }
        if (baseY - toY > 0) {
            moveY--;
        } else if (baseY - toY < 0) {
            moveY++;
        }
        if (canMove(baseX, baseY, baseX + moveX, baseY + moveY, height, xLength, yLength)) {
            return new int[] {baseX + moveX, baseY + moveY};
        } else if (moveX != 0 && canMove(baseX, baseY, baseX + moveX, baseY, height, xLength, yLength)) {
            return new int[] {baseX + moveX, baseY};
        } else if (moveY != 0 && canMove(baseX, baseY, baseX, baseY + moveY, height, xLength, yLength)) {
            return new int[] {baseX, baseY + moveY};
        }
        return new int[] {baseX, baseY};
    }

    public boolean canMove(int startX, int startY, int endX, int endY, int height, int xLength, int yLength) {
        int diffX = endX - startX;
        int diffY = endY - startY;
        int max = Math.max(Math.abs(diffX), Math.abs(diffY));
        for (int ii = 0; ii < max; ii++) {
            int currentX = endX - diffX;
            int currentY = endY - diffY;
            for (int i = 0; i < xLength; i++) {
                for (int i2 = 0; i2 < yLength; i2++) {
                    if (diffX < 0 && diffY < 0) {
                        if ((provider.getClipping(currentX + i - 1, currentY + i2 - 1, height) & 19398926) != 0 || (provider.getClipping(currentX + i - 1, currentY + i2, height) & 19398920) != 0 || (provider.getClipping(currentX + i, currentY + i2 - 1, height) & 19398914) != 0) {
                            return false;
                        }
                    } else if (diffX > 0 && diffY > 0) {
                        if ((provider.getClipping(currentX + i + 1, currentY + i2 + 1, height) & 19399136) != 0 || (provider.getClipping(currentX + i + 1, currentY + i2, height) & 19399040) != 0 || (provider.getClipping(currentX + i, currentY + i2 + 1, height) & 19398944) != 0) {
                            return false;
                        }
                    } else if (diffX < 0 && diffY > 0) {
                        if ((provider.getClipping(currentX + i - 1, currentY + i2 + 1, height) & 19398968) != 0 || (provider.getClipping(currentX + i - 1, currentY + i2, height) & 19398920) != 0 || (provider.getClipping(currentX + i, currentY + i2 + 1, height) & 19398944) != 0) {
                            return false;
                        }
                    } else if (diffX > 0 && diffY < 0) {
                        if ((provider.getClipping(currentX + i + 1, currentY + i2 - 1, height) & 19399043) != 0 || (provider.getClipping(currentX + i + 1, currentY + i2, height) & 19399040) != 0 || (provider.getClipping(currentX + i, currentY + i2 - 1, height) & 19398914) != 0) {
                            return false;
                        }
                    } else if (diffX > 0 && diffY == 0) {
                        if ((provider.getClipping(currentX + i + 1, currentY + i2, height) & 19399040) != 0) {
                            return false;
                        }
                    } else if (diffX < 0 && diffY == 0) {
                        if ((provider.getClipping(currentX + i - 1, currentY + i2, height) & 19398920) != 0) {
                            return false;
                        }
                    } else if (diffX == 0 && diffY > 0) {
                        if ((provider.getClipping(currentX + i, currentY + i2 + 1, height) & 19398944) != 0) {
                            return false;
                        }
                    } else if (diffX == 0 && diffY < 0) {
                        if ((provider.getClipping(currentX + i, currentY + i2 - 1, height) & 19398914) != 0) {
                            return false;
                        }
                    }
                }
            }
            if (diffX < 0) {
                diffX++;
            } else if (diffX > 0) {
                diffX--;
            }
            if (diffY < 0) {
                diffY++;
            } else if (diffY > 0) {
                diffY--;
            }
        }
        return true;
    }

    private void addProjectileClipping(int x, int y, int height, int shift) {
        Region region = provider.get(x, y);
        if (region != null) {
            if (shift > 0) {
                region.addProjectileClip(x, y, height, shift);
            } else {
                region.removeProjectileClip(x, y, height, shift);
            }
        }
    }

    private void addClipping(int x, int y, int height, int shift) {
        Region r = provider.get(x, y);
        if (r != null) {
            if (shift >= 0) {
                r.addClip(x, y, height, shift);
            } else {
                r.removeClip(x, y, height, shift);
            }
        }
    }

    public void removeClippingForSolidObject(int x, int y, int height, int xLength, int yLength, boolean flag) {
        int clipping = 256;
        if (flag) {
            clipping += 131072;
        }
        for (int i = x; i < x + xLength; i++) {
            for (int i2 = y; i2 < y + yLength; i2++) {
                addClipping(i, i2, height, -clipping);
            }
        }
    }

//	public static Region getRegion(int x, int y) {
//		int regionX = x >> 3;
//		int regionY = y >> 3;
//		int regionId = (regionX / 8 << 8) + regionY / 8;
//		for (Region region : regions) {
//			if (region.id() == regionId) {
//				return region;
//			}
//		}
//		return null;
//	}
    private static final int[][] DIR = {{-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    private void addProjectileClippingForSolidObject(int x, int y, int height, int xLength, int yLength, boolean flag) {
        int clipping = 256;
        if (flag) {
            clipping += 131072;
        }
        for (int i = x; i < x + xLength; i++) {
            for (int i2 = y; i2 < y + yLength; i2++) {
                addProjectileClipping(i, i2, height, clipping);
            }
        }
    }

    private void removeProjectileClippingForSolidObject(int x, int y, int height, int xLength, int yLength, boolean flag) {
        int clipping = 256;
        if (flag) {
            clipping += -131072;
        }
        for (int i = x; i < x + xLength; i++) {
            for (int i2 = y; i2 < y + yLength; i2++) {
                addProjectileClipping(i, i2, height, -clipping);
            }
        }
    }

    private void addClippingForSolidObject(int x, int y, int height, int xLength, int yLength, boolean flag) {
        int clipping = 256;
        if (flag) {
            clipping += 131072;
        }
        for (int i = x; i < x + xLength; i++) {
            for (int i2 = y; i2 < y + yLength; i2++) {
                addClipping(i, i2, height, clipping);
            }
        }
    }

    public static void load() {
        try {
            System.out.println("Loading mapdata..");
            verifyCustomMaps();
            File f = new File(Server.getDataDirectory() + "/mapdata/map_index");
            byte[] buffer = new byte[(int) f.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(f));
            dis.readFully(buffer);
            dis.close();
            ByteStream in = new ByteStream(buffer);
            int size = in.readUnsignedWord();
            int[] regionIds = new int[size];
            int[] mapGroundFileIds = new int[size];
            int[] mapObjectsFileIds = new int[size];
            RegionData[] data = new RegionData[size];
            for (int i = 0; i < size; i++) {
                regionIds[i] = in.readUnsignedWord();
                mapGroundFileIds[i] = in.readUnsignedWord();
                mapObjectsFileIds[i] = in.readUnsignedWord();
                data[i] = new RegionData(regionIds[i], mapGroundFileIds[i], mapObjectsFileIds[i]);
            }
            for (int i = 0; i < size; i++) {
                RegionProvider.getGlobal().add(new Region(RegionProvider.getGlobal(), regionIds[i], false));
            }
            Arrays.stream(data).forEach(Region::loadMap);
            Arrays.asList(EXISTANT_OBJECTS).forEach(object -> RegionProvider.getGlobal().get(object.getX(), object.getY()).addWorldObject(object));
            log.info("Loaded " + customMapFiles + " custom maps.");
            log.info("Error loading map files: " + errors.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int x = Boundary.INFERNO.getMinimumX(); x < Boundary.INFERNO.getMaximumX(); x++) {
            for (int y = Boundary.INFERNO.getMinimumY(); y < Boundary.INFERNO.getMaximumY(); y++) {
                RegionProvider.getGlobal().get(x, y).setClipToZero(x, y, 0);
            }
        }

        //agility
        RegionProvider.getGlobal().get(3204, 3395).setClipToZero(3204, 3395, 3);
        RegionProvider.getGlobal().get(3502, 3476).setClipToZero(3502, 3476, 3);
        RegionProvider.getGlobal().get(3510, 3482).setClipToZero(3510, 3482, 2);
        RegionProvider.getGlobal().get(2710, 3490).setClipToZero(2710, 3490, 2);
        RegionProvider.getGlobal().get(2710, 3477).setClipToZero(2710, 3477, 2);
        RegionProvider.getGlobal().get(3201, 3415).setClipToZero(3201, 3415, 3);
        RegionProvider.getGlobal().get(3099, 3277).setClipToZero(3099, 3277, 3);
        RegionProvider.getGlobal().get(3091, 3276).setClipToZero(3091, 3276, 3);
        RegionProvider.getGlobal().get(3089, 3265).setClipToZero(3089, 3265, 3);
        RegionProvider.getGlobal().get(3101, 3261).setClipToZero(3101, 3261, 3);
        RegionProvider.getGlobal().get(3301, 3163).setClipToZero(3301, 3163, 3);
        RegionProvider.getGlobal().get(3318, 3165).setClipToZero(3318, 3165, 1);
        RegionProvider.getGlobal().get(3267, 3166).setClipToZero(3267, 3166, 3);

        //SKILL ISLAND
        RegionProvider.getGlobal().get(3799, 3526).setClipToZero(3799, 3526, 0);
        //Pest Control Doors
        RegionProvider.getGlobal().get(2656, 2585).setClipToZero(2656, 2585, 0);
        RegionProvider.getGlobal().get(2657, 2585).setClipToZero(2657, 2585, 0);
        RegionProvider.getGlobal().get(2656, 2584).setClipToZero(2656, 2584, 0);
        RegionProvider.getGlobal().get(2657, 2584).setClipToZero(2657, 2584, 0);

        RegionProvider.getGlobal().get(2643, 2593).setClipToZero(2643, 2593, 0);
        RegionProvider.getGlobal().get(2643, 2592).setClipToZero(2643, 2592, 0);
        RegionProvider.getGlobal().get(2642, 2593).setClipToZero(2642, 2593, 0);
        RegionProvider.getGlobal().get(2642, 2592).setClipToZero(2642, 2592, 0);

        RegionProvider.getGlobal().get(2670, 2593).setClipToZero(2670, 2593, 0);
        RegionProvider.getGlobal().get(2670, 2592).setClipToZero(2670, 2592, 0);
        RegionProvider.getGlobal().get(2671, 2593).setClipToZero(2671, 2593, 0);
        RegionProvider.getGlobal().get(2671, 2592).setClipToZero(2671, 2592, 0);
    }

    /**
     * Check for duplicates in custom maps
     */
    private static void verifyCustomMaps() throws IOException {
        List<File> files = Files.walk(Paths.get(CUSTOM_MAPS_DIR)).filter(Files::isRegularFile).map(it -> it.toFile()).collect(Collectors.toList());
        List<String> fileNames = files.stream().map(it -> it.getName()).collect(Collectors.toList());
        for (int i = 0; i < fileNames.size(); i++) {
            for (int k = 0; k < fileNames.size(); k++) {
                if (i == k) continue;
                String file1 = fileNames.get(i);
                String file2 = fileNames.get(k);
                if (file1.equalsIgnoreCase(file2)) {
                    throw new IllegalStateException("Two custom maps with the same id: " + file1);
                }
            }
        }
    }

    private static byte[] getCustomFile(String directoryPath, int fileId) throws Exception {
        File directory = new File(directoryPath);
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                byte[] fileData = getCustomFile(file.getPath(), fileId);
                if (fileData != null) {
                    return fileData;
                }
            } else if (file.getName().equals(fileId + ".gz")) {
                return getBuffer(file);
            }
        }
        return null;
    }

    private static void loadMap(RegionData regionData) {
        try {
            byte[] file1 = getCustomFile(CUSTOM_MAPS_DIR, regionData.getObjects());
            byte[] file2 = getCustomFile(CUSTOM_MAPS_DIR, regionData.getLandscape());
            if (file1 == null) {
                file1 = getBuffer(new File(Server.getDataDirectory() + "/mapdata/index4/" + regionData.getObjects() + ".gz"));
            } else {
                customMapFiles++;
            }
            if (file2 == null) {
                file2 = getBuffer(new File(Server.getDataDirectory() + "/mapdata/index4/" + regionData.getLandscape() + ".gz"));
            } else {
                customMapFiles++;
            }
            if (file1 == null || file2 == null) {
                return;
            }
            loadMaps(regionData.getRegionHash(), new ByteStream(file1), new ByteStream(file2));
        } catch (Exception e) {
            errors.add(regionData.getLandscape());
            errors.add(regionData.getObjects());
        }
        //System.out.println("Error loading map region: " + regionData.getRegionHash() + ", objectFile: " + regionData.getObjects() + ", floorFile: " + regionData.getLandscape());
        //e.printStackTrace();
    }

    private static final int[][] fixClips = {{2207, 3057}};

    private static void loadMaps(int regionId, ByteStream str1, ByteStream str2) {
        int absX = (regionId >> 8) * 64;
        int absY = (regionId & 255) * 64;
        int[][][] someArray = new int[4][64][64];
        for (int i = 0; i < 4; i++) {
            for (int i2 = 0; i2 < 64; i2++) {
                for (int i3 = 0; i3 < 64; i3++) {
                    while (true) {
                        int v = str2.getUByte();
                        if (v == 0) {
                            break;
                        } else if (v == 1) {
                            str2.skip(1);
                            break;
                        } else if (v <= 49) {
                            str2.skip(1);
                        } else if (v <= 81) {
                            someArray[i][i2][i3] = v - 49;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            for (int i2 = 0; i2 < 64; i2++) {
                for (int i3 = 0; i3 < 64; i3++) {
                    if ((someArray[i][i2][i3] & 1) == 1) {
                        int height = i;
                        if ((someArray[1][i2][i3] & 2) == 2) {
                            height--;
                        }
                        if (height >= 0 && height <= 3) {
                            int x = absX + i2;
                            int y = absY + i3;
                            if (!Arrays.stream(fixClips).anyMatch(c -> c[0] == x && c[1] == y)) {
                                RegionProvider.getGlobal().get(x, y).addClipping(x, y, height, 2097152);
                            }
                        }
                    }
                }
            }
        }
        int objectId = -1;
        int incr;
        while ((incr = str1.getUSmart()) != 0) {
            objectId += incr;
            int location = 0;
            int incr2;
            while ((incr2 = str1.getUSmart()) != 0) {
                location += incr2 - 1;
                int localX = (location >> 6 & 63);
                int localY = (location & 63);
                int height = location >> 12;
                int objectData = str1.getUByte();
                int type = objectData >> 2;
                int direction = objectData & 3;
                if (localX < 0 || localX >= 64 || localY < 0 || localY >= 64) {
                    continue;
                }
                if ((someArray[1][localX][localY] & 2) == 2) {
                    height--;
                }
                if (height >= 0 && height <= 3) {
                    int x = absX + localX;
                    int y = absY + localY;
                    Region region = RegionProvider.getGlobal().get(x, y);
                    region.addObject(objectId, x, y, height, type, direction);
                    region.addWorldObject(objectId, absX + localX, absY + localY, height, type, direction);
                }
            }
        }
    }

    public static byte[] getBuffer(File f) throws Exception {
        if (!f.exists()) {
            return null;
        }
        byte[] buffer = new byte[(int) f.length()];
        try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
            dis.readFully(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] gzipInputBuffer = new byte[999999];
        int bufferlength = 0;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buffer))) {
            do {
                if (bufferlength == gzipInputBuffer.length) {
                    System.out.println("Error inflating data.\nGZIP buffer overflow.");
                    break;
                }
                int readByte = gzip.read(gzipInputBuffer, bufferlength, gzipInputBuffer.length - bufferlength);
                if (readByte == -1) break;
                bufferlength += readByte;
            } while (true);
            byte[] inflated = new byte[bufferlength];
            System.arraycopy(gzipInputBuffer, 0, inflated, 0, bufferlength);
            buffer = inflated;
            if (buffer.length < 10) {
                return null;
            }
        }
        return buffer;
    }

    public Location getNextStepLocation(int baseX, int baseY, int toX, int toY, int height, int xLength, int yLength) {
        int[] nextStep = getNextStep(baseX, baseY, toX, toY, height, xLength, yLength);
        return new Location(nextStep[0], nextStep[1], height);
    }

    /**
     * An array of {@link WorldObject} objects that will be added after the maps have been loaded. int id, int x, int y, int height, int face
     */
    private static final WorldObject[] EXISTANT_OBJECTS = {
    //new WorldObject(33320, 3073, 3504, 0, 0),
    //new WorldObject(172, 3099, 3512, 0, 0),
    // Magebank
    new WorldObject(-1, 3094, 3501, 0, 0), new WorldObject(-1, 3095, 3501, 0, 0), 
    // Lletya
    new WorldObject(24101, 2353, 3164, 0, 0), 
    // Staffzone
    new WorldObject(24101, 3164, 3488, 2, 0), 
    // Abyss
    new WorldObject(24101, 3039, 4834, 0, 0), new WorldObject(13642, 3039, 4831, 0, 2), 
    // Edgeville
    new WorldObject(6150, 3105, 3502, 0, 3), new WorldObject(6150, 3107, 3502, 0, 3), 
    // Kbd gates
    new WorldObject(1596, 3008, 3850, 0, 1), new WorldObject(1596, 3008, 3849, 0, -1), 
    // Resource area
    new WorldObject(9030, 3190, 3929, 0, 1), new WorldObject(9030, 3191, 3930, 0, 1), new WorldObject(9030, 3190, 3931, 0, 1), 
    // Skilling area
    new WorldObject(24009, 3030, 3375, 0, 2), new WorldObject(10822, 3008, 3387, 0, 0), new WorldObject(10822, 3011, 3388, 0, 0), new WorldObject(4674, 3014, 3387, 0, 0), new WorldObject(4674, 3016, 3387, 0, 0), new WorldObject(10834, 3010, 3383, 0, 0), new WorldObject(10834, 3013, 3383, 0, 0), new WorldObject(10829, 3021, 3373, 0, 0), new WorldObject(10829, 3018, 3371, 0, 0), new WorldObject(1276, 3019, 3387, 0, 0), new WorldObject(1276, 3021, 3387, 0, 0), new WorldObject(1276, 3022, 3386, 0, 0), new WorldObject(24101, 3029, 3379, 0, 1), new WorldObject(11161, 3021, 3368, 0, 1), new WorldObject(11161, 3020, 3368, 0, 1), new WorldObject(11360, 3019, 3368, 0, 1), new WorldObject(11360, 3018, 3367, 0, 1), new WorldObject(11365, 3017, 3367, 0, 1), new WorldObject(11365, 3016, 3367, 0, 1), new WorldObject(11365, 3015, 3367, 0, 1), new WorldObject(11366, 3014, 3367, 0, 1), new WorldObject(11366, 3013, 3368, 0, 1), new WorldObject(11370, 3012, 3369, 0, 1), new WorldObject(11370, 3011, 3369, 0, 1), new WorldObject(9030, 3010, 3369, 0, 1), new WorldObject(11373, 3009, 3369, 0, 1), new WorldObject(11374, 3008, 3369, 0, 1), new WorldObject(114, 3030, 3383, 0, 1), new WorldObject(10820, 3007, 3376, 0, 0), new WorldObject(11730, 3003, 3384, 0, 2), new WorldObject(11731, 3003, 3381, 0, 0), new WorldObject(11732, 2999, 3384, 0, 2), new WorldObject(11734, 2999, 3381, 0, 0), new WorldObject(6150, 3027, 3375, 0, 2), new WorldObject(14888, 3030, 3381, 0, 3), new WorldObject(25824, 3029, 3381, 0, 1), new WorldObject(3840, 3001, 3372, 0, 2), new WorldObject(7674, 2999, 3373, 0, 0), new WorldObject(11731, 3003, 3384, 0, 0), 
    //new WorldObject(27115, 1818, 3484, 0, 0),
    // Barrows
    new WorldObject(3610, 3550, 9695, 0, 0), 
    // Flax
    new WorldObject(14896, 3014, 3372, 0, 0), new WorldObject(14896, 3014, 3373, 0, 0), new WorldObject(14896, 3014, 3374, 0, 0), new WorldObject(14896, 3013, 3371, 0, 0), new WorldObject(14896, 3013, 3372, 0, 0), new WorldObject(14896, 3013, 3373, 0, 0), new WorldObject(14896, 3013, 3374, 0, 0), new WorldObject(14896, 3013, 3375, 0, 0), new WorldObject(14896, 3012, 3371, 0, 0), new WorldObject(14896, 3012, 3372, 0, 0), new WorldObject(14896, 3012, 3373, 0, 0), new WorldObject(14896, 3012, 3374, 0, 0), new WorldObject(14896, 3012, 3375, 0, 0), new WorldObject(14896, 3011, 3371, 0, 0), new WorldObject(14896, 3011, 3372, 0, 0), new WorldObject(14896, 3011, 3373, 0, 0), new WorldObject(14896, 3011, 3374, 0, 0), new WorldObject(14896, 3011, 3375, 0, 0), new WorldObject(14896, 3010, 3372, 0, 0), new WorldObject(14896, 3010, 3373, 0, 0), new WorldObject(14896, 3010, 3374, 0, 0), 
    //Christmas
    new WorldObject(2147, 2957, 3704, 0, 0),  // Ladders
    new WorldObject(2147, 2952, 3821, 0, 0), new WorldObject(3309, 2953, 3821, 0, 0), 
    //Halloween
    new WorldObject(298, 3088, 3497, 0, 0),  //Bales
    new WorldObject(298, 3085, 3496, 0, 1), new WorldObject(298, 3085, 3493, 0, 1), 
    //Legendary donator zone
    //Skilling
    new WorldObject(24009, 3360, 9632, 1, 3),  //Furnace
    new WorldObject(170, 3369, 9640, 1, 3),  //Chest
    new WorldObject(10, 3373, 9645, 1, 1),  //Ladder
    //Adamant rocks
    new WorldObject(11374, 3373, 9626, 1, 1), new WorldObject(11374, 3374, 9627, 1, 1), new WorldObject(11374, 3375, 9628, 1, 1), new WorldObject(11374, 3376, 9629, 1, 1), new WorldObject(11374, 3377, 9630, 1, 1), 
    //Rune rocks
    new WorldObject(7494, 3377, 9649, 1, 1), new WorldObject(7494, 3376, 9650, 1, 1), new WorldObject(7494, 3375, 9651, 1, 1), new WorldObject(7494, 3374, 9652, 1, 1), new WorldObject(7494, 3373, 9653, 1, 1), 
    //Magic trees
    new WorldObject(10834, 3353, 9635, 1, 1), new WorldObject(10834, 3355, 9631, 1, 1), new WorldObject(10834, 3351, 9632, 1, 1), 
    //Yew trees
    new WorldObject(10822, 3359, 9650, 1, 1), new WorldObject(10822, 3354, 9651, 1, 1), new WorldObject(10822, 3355, 9645, 1, 1), 
    //Trapdoor
    new WorldObject(11803, 2126, 4919, 1, 0), 
    //Teleportation device
    new WorldObject(13641, 2121, 4914, 1, 0), new WorldObject(13641, 2032, 4536, 1, 0), 
    //Banks
    new WorldObject(24101, 3361, 9640, 1, 1), new WorldObject(24101, 3363, 9638, 1, 2), new WorldObject(24101, 3363, 9642, 1, 2), 
    //Crafting guild
    new WorldObject(9030, 2940, 3288, 0, 1), new WorldObject(9030, 2942, 3288, 0, 1), new WorldObject(9030, 2941, 3289, 0, 1), 
    //Bank room
    new WorldObject(24101, 2851, 2952, 0, 0),  //Banks
    new WorldObject(24101, 2852, 2952, 0, 0), new WorldObject(24101, 2853, 2952, 0, 0), new WorldObject(13641, 2856, 2954, 0, 0),  //Teleportation
    new WorldObject(14880, 2839, 2983, 0, 2),  //Trapdoor
    new WorldObject(170, 2848, 2954, 0, 1),  // Chest
    new WorldObject(14011, 2856, 2956, 0, 0),  // Stall
    new WorldObject(6150, 2859, 2967, 0, 0),  // Anvi
    new WorldObject(24101, 2854, 2967, 0, 0), 
    //2nd bank room, other side
    new WorldObject(24101, 2834, 2981, 0, 0),  //Banks
    new WorldObject(24101, 2835, 2981, 0, 0), new WorldObject(24101, 2836, 2981, 0, 0), new WorldObject(24101, 2837, 2981, 0, 0), new WorldObject(13641, 2837, 2991, 0, 0),  //Teleportation
    new WorldObject(14880, 2859, 2954, 0, 2),  //Trapdoor
    new WorldObject(170, 2833, 2991, 0, 1),  // Chest
    //Mining
    new WorldObject(7494, 2824, 3003, 0, 0),  //Rune
    new WorldObject(7494, 2823, 3002, 0, 0),  //Rune
    new WorldObject(7494, 2822, 3001, 0, 0),  //Rune
    new WorldObject(7494, 2821, 3000, 0, 0),  //Rune
    new WorldObject(7494, 2820, 2999, 0, 0),  //Rune
    new WorldObject(7494, 2819, 2998, 0, 0),  //Rune
    new WorldObject(11374, 2821, 2996, 0, 0),  //Adamant
    new WorldObject(11374, 2822, 2997, 0, 0),  //Adamant
    new WorldObject(11374, 2823, 2998, 0, 0),  //Adamant
    new WorldObject(11374, 2824, 2999, 0, 0),  //Adamant
    new WorldObject(11374, 2825, 3000, 0, 0),  //Adamant
    new WorldObject(11374, 2826, 3001, 0, 0),  //Adamant
    new WorldObject(9030, 2819, 2997, 0, 0),  //Gem
    new WorldObject(9030, 2825, 3003, 0, 0),  //Gem
    //Crafting Guild
    new WorldObject(9030, 2940, 3290, 0, 1),  //Gem
    new WorldObject(9030, 2941, 3288, 0, 1),  //Gem
    new WorldObject(9030, 2942, 3289, 0, 1),  //Gem
    new WorldObject(11373, 2943, 3285, 0, 1),  //Mith
    new WorldObject(11373, 2943, 3284, 0, 1),  //Mith
    new WorldObject(11373, 2943, 3283, 0, 1),  //Mith
    new WorldObject(11373, 2943, 3282, 0, 1),  //Mith
    //Woodcutting
    // Magic trees
    new WorldObject(10834, 2853, 2980, 0, 0), new WorldObject(10834, 2853, 2983, 0, 0), new WorldObject(10834, 2853, 2986, 0, 0), new WorldObject(10834, 2853, 2989, 0, 0), 
    // Yew trees
    new WorldObject(10822, 2842, 2980, 0, 0), new WorldObject(10822, 2842, 2983, 0, 0), new WorldObject(10822, 2842, 2986, 0, 0), new WorldObject(10822, 2842, 2989, 0, 0)};

    /**
     * Shilo
     */
    private void setProjectileClippingForVariableObject(int x, int y, int height, int type, int direction, boolean flag, boolean negative) {
        if (type == 0) {
            if (direction == 0) {
                addProjectileClipping(x, y, height, negative ? -128 : 128);
                addProjectileClipping(x - 1, y, height, negative ? -8 : 8);
            } else if (direction == 1) {
                addProjectileClipping(x, y, height, negative ? -2 : 2);
                addProjectileClipping(x, y + 1, height, negative ? -32 : 32);
            } else if (direction == 2) {
                addProjectileClipping(x, y, height, negative ? -8 : 8);
                addProjectileClipping(x + 1, y, height, negative ? -128 : 128);
            } else if (direction == 3) {
                addProjectileClipping(x, y, height, negative ? -32 : 32);
                addProjectileClipping(x, y - 1, height, negative ? -2 : 2);
            }
        } else if (type == 1 || type == 3) {
            if (direction == 0) {
                addProjectileClipping(x, y, height, negative ? -1 : 1);
                addProjectileClipping(x - 1, y + 1, height, negative ? -16 : 16);//wrong method217(16, x - 1, y + 1);
            } else if (direction == 1) {
                addProjectileClipping(x, y, height, negative ? -4 : 4);
                addProjectileClipping(x + 1, y + 1, height, negative ? -64 : 64);
            } else if (direction == 2) {
                addProjectileClipping(x, y, height, negative ? -16 : 16);
                addProjectileClipping(x + 1, y - 1, height, negative ? -1 : 1);
            } else if (direction == 3) {
                addProjectileClipping(x, y, height, negative ? -64 : 64);
                addProjectileClipping(x - 1, y - 1, height, negative ? -4 : 4);
            }
        } else if (type == 2) {
            if (direction == 0) {
                addProjectileClipping(x, y, height, 130);
                addProjectileClipping(x - 1, y, height, negative ? -8 : 8);
                addProjectileClipping(x, y + 1, height, negative ? -32 : 32);
            } else if (direction == 1) {
                addProjectileClipping(x, y, height, negative ? -10 : 10);
                addProjectileClipping(x, y + 1, height, negative ? -32 : 32);
                addProjectileClipping(x + 1, y, height, negative ? -128 : 128);
            } else if (direction == 2) {
                addProjectileClipping(x, y, height, negative ? -40 : 40);
                addProjectileClipping(x + 1, y, height, negative ? -128 : 128);
                addProjectileClipping(x, y - 1, height, negative ? -2 : 2);
            } else if (direction == 3) {
                addProjectileClipping(x, y, height, negative ? -160 : 160);
                addProjectileClipping(x, y - 1, height, negative ? -2 : 2);
                addProjectileClipping(x - 1, y, height, negative ? -8 : 8);
            }
        }
        if (flag) {
            if (type == 0) {
                if (direction == 0) {
                    addProjectileClipping(x, y, height, negative ? -65536 : 65536);
                    addProjectileClipping(x - 1, y, height, negative ? -4096 : 4096);
                } else if (direction == 1) {
                    addProjectileClipping(x, y, height, negative ? -1024 : 1024);
                    addProjectileClipping(x, y + 1, height, negative ? -16384 : 16384);
                } else if (direction == 2) {
                    addProjectileClipping(x, y, height, negative ? -4096 : 4096);
                    addProjectileClipping(x + 1, y, height, negative ? -65536 : 65536);
                } else if (direction == 3) {
                    addProjectileClipping(x, y, height, negative ? -16384 : 16384);
                    addProjectileClipping(x, y - 1, height, negative ? -1024 : 1024);
                }
            }
            if (type == 1 || type == 3) {
                if (direction == 0) {
                    addProjectileClipping(x, y, height, negative ? -512 : 512);
                    addProjectileClipping(x - 1, y + 1, height, negative ? -8192 : 8192);
                } else if (direction == 1) {
                    addProjectileClipping(x, y, height, negative ? -2048 : 2048);
                    addProjectileClipping(x + 1, y + 1, height, negative ? -32768 : 32768);
                } else if (direction == 2) {
                    addProjectileClipping(x, y, height, negative ? -8192 : 8192);
                    addProjectileClipping(x + 1, y + 1, height, negative ? -512 : 512);
                } else if (direction == 3) {
                    addProjectileClipping(x, y, height, negative ? -32768 : 32768);
                    addProjectileClipping(x - 1, y - 1, height, negative ? -2048 : 2048);
                }
            } else if (type == 2) {
                if (direction == 0) {
                    addProjectileClipping(x, y, height, negative ? -66560 : 66560);
                    addProjectileClipping(x - 1, y, height, negative ? -4096 : 4096);
                    addProjectileClipping(x, y + 1, height, negative ? -16384 : 16384);
                } else if (direction == 1) {
                    addProjectileClipping(x, y, height, negative ? -5120 : 5120);
                    addProjectileClipping(x, y + 1, height, negative ? -16384 : 16384);
                    addProjectileClipping(x + 1, y, height, negative ? -65536 : 65536);
                } else if (direction == 2) {
                    addProjectileClipping(x, y, height, negative ? -20480 : 20480);
                    addProjectileClipping(x + 1, y, height, negative ? -65536 : 65536);
                    addProjectileClipping(x, y - 1, height, negative ? -1024 : 1024);
                } else if (direction == 3) {
                    addProjectileClipping(x, y, height, negative ? -81920 : 81920);
                    addProjectileClipping(x, y - 1, height, negative ? -1024 : 1024);
                    addProjectileClipping(x - 1, y, height, negative ? -4096 : 4096);
                }
            }
        }
    }

    private void setClippingForVariableObject(int x, int y, int height, int type, int direction, boolean flag, boolean negative) {
        if (type == 0) {
            if (direction == 0) {
                addClipping(x, y, height, negative ? -128 : 128);
                addClipping(x - 1, y, height, negative ? -8 : 8);
            } else if (direction == 1) {
                addClipping(x, y, height, negative ? -2 : 2);
                addClipping(x, y + 1, height, negative ? -32 : 32);
            } else if (direction == 2) {
                addClipping(x, y, height, negative ? -8 : 8);
                addClipping(x + 1, y, height, negative ? -128 : 128);
            } else if (direction == 3) {
                addClipping(x, y, height, negative ? -32 : 32);
                addClipping(x, y - 1, height, negative ? -2 : 2);
            }
        } else if (type == 1 || type == 3) {
            if (direction == 0) {
                addClipping(x, y, height, negative ? -1 : 1);
                addClipping(x - 1, y + 1, height, negative ? -16 : 16);//wrong method217(16, x - 1, y + 1);
            } else if (direction == 1) {
                addClipping(x, y, height, negative ? -4 : 4);
                addClipping(x + 1, y + 1, height, negative ? -64 : 64);
            } else if (direction == 2) {
                addClipping(x, y, height, negative ? -16 : 16);
                addClipping(x + 1, y - 1, height, negative ? -1 : 1);
            } else if (direction == 3) {
                addClipping(x, y, height, negative ? -64 : 64);
                addClipping(x - 1, y - 1, height, negative ? -4 : 4);
            }
        } else if (type == 2) {
            if (direction == 0) {
                addClipping(x, y, height, 130);
                addClipping(x - 1, y, height, negative ? -8 : 8);
                addClipping(x, y + 1, height, negative ? -32 : 32);
            } else if (direction == 1) {
                addClipping(x, y, height, negative ? -10 : 10);
                addClipping(x, y + 1, height, negative ? -32 : 32);
                addClipping(x + 1, y, height, negative ? -128 : 128);
            } else if (direction == 2) {
                addClipping(x, y, height, negative ? -40 : 40);
                addClipping(x + 1, y, height, negative ? -128 : 128);
                addClipping(x, y - 1, height, negative ? -2 : 2);
            } else if (direction == 3) {
                addClipping(x, y, height, negative ? -160 : 160);
                addClipping(x, y - 1, height, negative ? -2 : 2);
                addClipping(x - 1, y, height, negative ? -8 : 8);
            }
        }
        // Old projectile clipping code
        /*if (flag) {
			if (type == 0) {
				if (direction == 0) {
					addClipping(x, y, height, negative ? -0x10000 : 0x10000);
					addClipping(x - 1, y, height, negative ? -4096 : 4096);
				} else if (direction == 1) {
					addClipping(x, y, height, negative ? -1024 : 1024);
					addClipping(x, y + 1, height, negative ? -16384 : 16384);
				} else if (direction == 2) {
					addClipping(x, y, height, negative ? -4096 : 4096);
					addClipping(x + 1, y, height, negative ? -0x10000 : 0x10000);
				} else if (direction == 3) {
					addClipping(x, y, height, negative ? -16384 : 16384);
					addClipping(x, y - 1, height, negative ? -1024 : 1024);
				}
			}
			if (type == 1 || type == 3) {
				if (direction == 0) {
					addClipping(x, y, height, negative ? -512 : 512);
					addClipping(x - 1, y + 1, height, negative ? -8192 : 8192);
				} else if (direction == 1) {
					addClipping(x, y, height, negative ? -2048 : 2048);
					addClipping(x + 1, y + 1, height, negative ? -32768 : 32768);
				} else if (direction == 2) {
					addClipping(x, y, height, negative ? -8192 : 8192);
					addClipping(x + 1, y + 1, height, negative ? -512 : 512);
				} else if (direction == 3) {
					addClipping(x, y, height, negative ? -32768 : 32768);
					addClipping(x - 1, y - 1, height, negative ? -2048 : 2048);
				}
			} else if (type == 2) {
				if (direction == 0) {
					addClipping(x, y, height, negative ? -0x10400 : 0x10400);
					addClipping(x - 1, y, height, negative ? -4096 : 4096);
					addClipping(x, y + 1, height, negative ? -16384 : 16384);
				} else if (direction == 1) {
					addClipping(x, y, height, negative ? -5120 : 5120);
					addClipping(x, y + 1, height, negative ? -16384 : 16384);
					addClipping(x + 1, y, height, negative ? -0x10000 : 0x10000);
				} else if (direction == 2) {
					addClipping(x, y, height, negative ? -20480 : 20480);
					addClipping(x + 1, y, height, negative ? -0x10000 : 0x10000);
					addClipping(x, y - 1, height, negative ? -1024 : 1024);
				} else if (direction == 3) {
					addClipping(x, y, height, negative ? -81920 : 81920);
					addClipping(x, y - 1, height, negative ? -1024 : 1024);
					addClipping(x - 1, y, height, negative ? -4096 : 4096);
				}
			}
		}*/
    }

    private boolean canShootOver(int x, int y, int z, int prevX, int prevY) {
        int dir = -1;
        int dir2 = -1;
        for (int i = 0; i < DIR.length; i++) {
            if (x + DIR[i][0] == prevX && y + DIR[i][1] == prevY) {
                dir = i;
            }
            if (prevX + DIR[i][0] == x && prevY + DIR[i][1] == y) {
                dir2 = i;
            }
        }
        if (dir == -1 || dir2 == -1) {
            return false;
        }
        Region region2 = provider.get(prevX, prevY);
        if (canMove(x, y, prevX, prevY, z, x - prevX, y - prevY)) {
            return true;
        }
        return (region2.getClip(x, y, z) & 131072) == 0;
    }

    public int id() {
        return id;
    }
}
