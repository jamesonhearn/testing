
package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Experimental world generator that builds 2-tile-high wall stacks with clearance-aware
 * room and hallway placement. This class intentionally avoids cross-calls to the legacy
 * World generator so it can be swapped wholesale later.
 */
public class World {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 40;

    private static final int MIN_ROOM_SIZE = 8;
    private static final int MAX_ROOM_SIZE = 16;
    private static final int MAX_ROOM_ATTEMPTS = 1000;
    private static final double TARGET_FILL_RATIO = 0.60;

    private static final int CLEARANCE_VERTICAL = 3;
    private static final int CLEARANCE_HORIZONTAL = 1;

    private static final TETile SIDE_WALL = Tileset.BACK_WALL;
    private static final TETile TOP_WALL = Tileset.FRONT_WALL;

    private final Random random;
    private final TETile[][] world;
    private final List<Room> rooms;

    public World(long seed) {
        this.random = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];
        this.rooms = new ArrayList<>();
        initializeVoid();
    }

    /**
     * Build a world using the two-tier wall approach. Multiple attempts are made to keep
     * connectivity guarantees similar to the legacy generator.
     */
    public TETile[][] generate() {
        for (int attempt = 0; attempt < 5; attempt += 1) {
            resetWorld();
            carveRoomsWithHallways();
            addSideWalls();
            addTopWalls();
            if (allFloorsConnected()) {
                break;
            }
        }
        return world;
    }

    private void initializeVoid() {
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    private void resetWorld() {
        rooms.clear();
        initializeVoid();
    }

    /**
     * Main generation loop: attempt to place rooms under clearance rules, connect them
     * with hallways that respect the same buffers (except when entering a room), and add
     * a few extra connectors for variety.
     */
    private void carveRoomsWithHallways() {
        int targetFloorTiles = (int) (WIDTH * HEIGHT * TARGET_FILL_RATIO);
        int carvedTiles = 0;
        int attempts = 0;

        while (carvedTiles < targetFloorTiles && attempts < MAX_ROOM_ATTEMPTS) {
            attempts += 1;
            Room candidate = randomRoom();
            if (overlapsWithClearance(candidate)) {
                continue;
            }
            rooms.add(candidate);
            carvedTiles += carveRoom(candidate);
            connectToNearest(candidate);
        }

        if (rooms.size() > 1) {
            addExtraConnectors();
        }
    }

    private Room randomRoom() {
        int width = randomRange(MIN_ROOM_SIZE, MAX_ROOM_SIZE);
        int height = randomRange(MIN_ROOM_SIZE, MAX_ROOM_SIZE);

        int x = randomRange(1, WIDTH - width - 1);
        int y = randomRange(1, HEIGHT - height - 1);
        return new Room(x, y, width, height);
    }

    /**
     * Enforce a clearance padded overlap: 3 tiles vertically and 1 tile horizontally.
     */
    private boolean overlapsWithClearance(Room candidate) {
        for (Room room : rooms) {
            if (candidate.overlapsWithClearance(room, CLEARANCE_HORIZONTAL, CLEARANCE_VERTICAL)) {
                return true;
            }
        }
        return false;
    }

    private int carveRoom(Room room) {
        int carved = 0;
        for (int x = room.left; x < room.left + room.width; x += 1) {
            for (int y = room.bottom; y < room.bottom + room.height; y += 1) {
                if (world[x][y].equals(Tileset.NOTHING)) {
                    world[x][y] = Tileset.FLOOR;
                    carved += 1;
                }
            }
        }
        return carved;
    }

    private void connectToNearest(Room source) {
        if (rooms.size() <= 1) {
            return;
        }
        Room nearest = findNearestRoom(source);
        carveHallway(source, nearest);
    }

    private Room findNearestRoom(Room source) {
        Room best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Room room : rooms) {
            if (room == source) {
                continue;
            }
            int dx = room.center().x - source.center().x;
            int dy = room.center().y - source.center().y;
            int dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = room;
            }
        }
        return best;
    }

    private void addExtraConnectors() {
        int maxConnectorDist = 30; // squared distance
        List<Room> shuffled = new ArrayList<>(rooms);
        Collections.shuffle(shuffled, random);

        for (int i = 0; i < shuffled.size() - 1; i++) {
            Room a = shuffled.get(i);
            Room b = shuffled.get(i + 1);

            int dx = a.center().x - b.center().x;
            int dy = a.center().y - b.center().y;
            int dist = dx * dx + dy * dy;
            if (dist <= maxConnectorDist) {
                carveHallway(a, b);
            }
        }
    }

    /**
     * Attempt to carve a clearance-aware L-shaped hallway. If one orientation fails the
     * clearance checks, try the other. Failing both leaves the rooms disconnected for this pass.
     */
    private void carveHallway(Room a, Room b) {
        Position start = a.center();
        Position end = b.center();

        List<Position[]> candidates = new ArrayList<>();
        candidates.add(new Position[] { start, new Position(end.x, start.y), end });
        candidates.add(new Position[] { start, new Position(start.x, end.y), end });

        for (Position[] path : candidates) {
            if (carvePathWithClearance(path, a, b)) {
                return;
            }
        }
    }

    private boolean carvePathWithClearance(Position[] waypoints, Room startRoom, Room endRoom) {
        Set<Position> currentPath = new HashSet<>();
        for (int i = 0; i < waypoints.length - 1; i += 1) {
            Position segmentStart = waypoints[i];
            Position segmentEnd = waypoints[i + 1];
            if (!carveSegmentWithClearance(segmentStart, segmentEnd, startRoom, endRoom, currentPath)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Carve a straight segment while respecting clearance against existing floors, except
     * when stepping inside the start/end rooms. Intermediate hallway tiles are allowed to
     * be adjacent to the path already being carved via {@code currentPath}.
     */
    private boolean carveSegmentWithClearance(Position start, Position end, Room startRoom,
                                              Room endRoom, Set<Position> currentPath) {
        int xStep = Integer.compare(end.x, start.x);
        int yStep = Integer.compare(end.y, start.y);
        Position current = start;

        while (true) {
            boolean atEndpointRoom = (startRoom != null && startRoom.contains(current))
                    || (endRoom != null && endRoom.contains(current));
            if (!atEndpointRoom && !isClearForHallway(current, currentPath, startRoom, endRoom)) {
                return false;
            }

            world[current.x][current.y] = Tileset.FLOOR;
            currentPath.add(current);

            if (current.equals(end)) {
                break;
            }
            int nextX = current.x;
            int nextY = current.y;
            if (current.x != end.x) {
                nextX += xStep;
            } else if (current.y != end.y) {
                nextY += yStep;
            }
            current = new Position(nextX, nextY);
        }
        return true;
    }

    private boolean isClearForHallway(Position pos, Set<Position> currentPath,
                                      Room startRoom, Room endRoom) {
        for (int dx = -CLEARANCE_HORIZONTAL; dx <= CLEARANCE_HORIZONTAL; dx += 1) {
            for (int dy = -CLEARANCE_VERTICAL; dy <= CLEARANCE_VERTICAL; dy += 1) {
                int nx = pos.x + dx;
                int ny = pos.y + dy;
                if (!inBounds(nx, ny)) {
                    continue;
                }
                Position neighbor = new Position(nx, ny);
                if (currentPath.contains(neighbor)) {
                    continue;
                }
                if (world[nx][ny].equals(Tileset.FLOOR)) {
                    if ((startRoom != null && startRoom.contains(neighbor))
                            || (endRoom != null && endRoom.contains(neighbor))) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Place lower (side) walls according to the provided corner/edge rules.
     */
    private void addSideWalls() {
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                if (!world[x][y].equals(Tileset.FLOOR)) {
                    continue;
                }

                boolean nothingBelow = isNothing(x, y - 1);
                boolean nothingAbove = isNothing(x, y + 1);
                boolean nothingLeft = isNothing(x - 1, y);
                boolean nothingRight = isNothing(x + 1, y);

                // Bottom corner handling
                if (nothingBelow && nothingLeft) {
                    placeSideWallsBelow(x, y);
                    placeSideWallsBelow(x - 1,y);
                } else if (nothingBelow && nothingRight) {
                    placeSideWallsBelow(x, y);
                    placeSideWallsBelow(x+1, y);
                } else if (nothingBelow) {
                    placeSideWallsBelow(x, y);
                }

                if (nothingAbove) {
                    placeSideWallsAbove(x, y);
                }
            }
        }
    }

    private void placeSideWallsBelow(int x, int y) {
        // Skip the immediate tile below, then place two side walls
        for (int offset = 2; offset <= 3; offset += 1) {
            setIfNothing(x, y - offset, SIDE_WALL);
        }
    }

    private void placeSideWallsHorizontal(int startX, int y, int direction) {
        setIfNothing(startX, y, SIDE_WALL);
        setIfNothing(startX + direction, y, SIDE_WALL);
    }

    private void placeSideWallsAbove(int x, int y) {
        setIfNothing(x, y + 1, SIDE_WALL);
        setIfNothing(x, y + 2, SIDE_WALL);
    }

    /**
     * Place the upper wall course (top walls) based on a snapshot of the side wall layer.
     */
    private void addTopWalls() {
        boolean[][] sideSnapshot = new boolean[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                sideSnapshot[x][y] = world[x][y].equals(SIDE_WALL);
            }
        }

        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                if (!world[x][y].equals(Tileset.FLOOR)) {
                    continue;
                }

                boolean nothingBelow = isNothing(x, y - 1);
                boolean nothingLeft = isNothing(x - 1, y);
                boolean nothingRight = isNothing(x + 1, y);
                boolean sideAbove = inBounds(x, y + 1) && sideSnapshot[x][y + 1];

                // Upper Right
                if (sideAbove && nothingRight) {
                    placeTopWallWithColumn(x + 1, y, 1);
                }
                // Upper Left
                if (sideAbove && nothingLeft) {
                    placeTopWallWithColumn(x - 1, y, -1);
                }
                // Lower corner: gaps on both sides and below

                //Lower left:
                if (nothingBelow && nothingLeft) {
                    placeTopWallDiagonal(x - 1, y, -1);
                }

                //Lower right:
                if (nothingBelow && nothingRight) {
                    placeTopWallDiagonal(x + 1, y, 1);
                }

                // If side wall directly above, place top wall three above
                if (sideAbove) {
                    setIfNothing(x, y + 3, TOP_WALL);
                }

                // If void below, cap with a top wall just below
                if (nothingBelow) {
                    setIfNothing(x, y - 1, TOP_WALL);
                }

                // If lateral void and not backed by floor beneath that void, add side cap
                if (nothingLeft && !isFloor(x - 1, y - 1)) {
                    setIfNothing(x - 1, y, TOP_WALL);
                }
                if (nothingRight && !isFloor(x + 1, y - 1)) {
                    setIfNothing(x + 1, y, TOP_WALL);
                }
            }
        }
    }

    private void placeTopWallWithColumn(int sideX, int y, int direction) {
        setIfNothing(sideX, y, TOP_WALL);
        for (int offset = 1; offset <= 3; offset += 1) {
            setIfNothing(sideX, y + offset, TOP_WALL);
        }
        //setIfNothing(sideX + direction, y, TOP_WALL);
    }

    private void placeTopWallDiagonal(int sideX, int y, int direction) {
        setIfNothing(sideX, y, TOP_WALL);
        setIfNothing(sideX, y - 1, TOP_WALL);
        //setIfNothing(sideX + direction, y, TOP_WALL);
    }

    private void setIfNothing(int x, int y, TETile tile) {
        if (inBounds(x, y) && world[x][y].equals(Tileset.NOTHING)) {
            world[x][y] = tile;
        }
    }

    private boolean isNothing(int x, int y) {
        return inBounds(x, y) && world[x][y].equals(Tileset.NOTHING);
    }

    private boolean isFloor(int x, int y) {
        return inBounds(x, y) && world[x][y].equals(Tileset.FLOOR);
    }

    private boolean allFloorsConnected() {
        Position start = firstFloor();
        int total = countFloorTiles();
        if (start == null || total == 0) {
            return false;
        }
        boolean[][] visited = new boolean[WIDTH][HEIGHT];
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(start);
        visited[start.x][start.y] = true;
        int seen = 0;

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            seen += 1;
            int[][] deltas = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] delta : deltas) {
                int nx = current.x + delta[0];
                int ny = current.y + delta[1];
                if (!inBounds(nx, ny) || visited[nx][ny]) {
                    continue;
                }
                if (world[nx][ny].equals(Tileset.FLOOR)) {
                    visited[nx][ny] = true;
                    queue.add(new Position(nx, ny));
                }
            }
        }
        return seen == total;
    }

    private Position firstFloor() {
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    private int countFloorTiles() {
        int count = 0;
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private int randomRange(int min, int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }

    /**
     * Represents an axis-aligned rectangular room.
     */
    private static class Room {
        private final int left;
        private final int bottom;
        private final int width;
        private final int height;

        Room(int left, int bottom, int width, int height) {
            this.left = left;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
        }

        Position center() {
            int cx = left + width / 2;
            int cy = bottom + height / 2;
            return new Position(cx, cy);
        }

        boolean contains(Position p) {
            return p.x >= left && p.x < left + width && p.y >= bottom && p.y < bottom + height;
        }

        boolean overlapsWithClearance(Room other, int horizontal, int vertical) {
            return this.left - horizontal < other.left + other.width + horizontal
                    && this.left + this.width + horizontal > other.left - horizontal
                    && this.bottom - vertical < other.bottom + other.height + vertical
                    && this.bottom + this.height + vertical > other.bottom - vertical;
        }
    }

    private record Position(int x, int y) { }
}
