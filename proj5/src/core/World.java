package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class World {
    public static final int WIDTH = 50;
    public static final int HEIGHT = 50;
    private static final int MIN_ROOM_SIZE = 6;
    private static final int MAX_ROOM_SIZE = 12;
    private static final int MAX_ROOM_ATTEMPTS = 1000;
    private static final double TARGET_FILL_RATIO = 0.80;

    private final Random random;
    private final TETile[][] world;
    private final List<Room> rooms;

    public World(long seed) {
        this.random = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];
        this.rooms = new ArrayList<>();
        initializeVoid();
    }


    // Build world via rooms and turning hallways 1-2 width, retuns generated tile grid
    public TETile[][] generate() {
        carveRoomsWithHallways();
        addPerimeterWalls();
        return world;
    }


    public void initializeVoid() {
        for (int x=0; x<WIDTH; x+=1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }
    private void addExtraConnectors() {
        int maxConnectorDist = 30; // squared distance; tune to taste

        List<Room> shuffled = new ArrayList<>(rooms);
        Collections.shuffle(shuffled, random);

        for (int i = 0; i < shuffled.size() - 1; i++) {
            Room a = shuffled.get(i);
            Room b = shuffled.get(i + 1);

            int dx = a.center().x - b.center().x;
            int dy = a.center().y - b.center().y;
            int dist = dx*dx + dy*dy;

            // Only connect if they are reasonably close
            if (dist <= maxConnectorDist) {
                carveHallway(a.center(), b.center());
            }
        }
    }
    // attempt to carve rooms with hallways out of existing void
    public void carveRoomsWithHallways() {
        int targetFloorTiles = (int) (WIDTH * HEIGHT * TARGET_FILL_RATIO);
        int carvedTiles = 0;
        int attempts = 0;

        while (carvedTiles < targetFloorTiles && attempts < MAX_ROOM_ATTEMPTS) {
            attempts += 1;
            Room candidate = randomRoom();
            if (overlaps(candidate)) {
                continue;
            }
            rooms.add(candidate);
            carvedTiles += carveRoom(candidate);
            connectToPrevious(candidate);
            carvedTiles = countFloorTiles();
        }


        if (rooms.size() < 2) {
            return;
        }
        addExtraConnectors();
    }
    // Removed carvehallway in place of extra connectors in attempt to reduce excessively long hallways
//        List<Room> shuffled = new ArrayList<>(rooms);
//        //Add some random connectors to encourage turns/loops in hallways
//        Collections.shuffle(shuffled, random);
//        int pairCount = shuffled.size() / 2;
//        for (int pair = 0; pair < pairCount; pair += 1) {
//            int aIndex = pair * 2;
//            int bIndex = aIndex + 1;
//            carveHallway(shuffled.get(aIndex).center(), shuffled.get(bIndex).center());
//        }
//    }

    //Room constructor
    private Room randomRoom() {
        int width = randomRange(MIN_ROOM_SIZE, MAX_ROOM_SIZE);
        int height = randomRange(MIN_ROOM_SIZE, MAX_ROOM_SIZE);

        int x = randomRange(1, WIDTH - width -1);
        int y = randomRange(1, HEIGHT - height -1);
        return new Room(x,y,width,height);
    }

    private boolean overlaps(Room candidate) {
        for (Room room : rooms) {
            if (candidate.overlaps(room)) {
                return true;
            }
        }
        return false;
    }

    private int carveRoom(Room room) {
        int carved = 0;
        for (int x = room.left; x < room.left + room.width; x+=1) {
            for (int y = room.bottom; y < room.bottom + room.height; y+=1) {
                if (world[x][y].equals(Tileset.NOTHING)) {
                    world[x][y] = Tileset.FLOOR;
                    carved +=1;
                }
            }
        }
        return carved;
    }
    private Room findNearestRoom(Room source) {
        Room best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Room r : rooms) {
            if (r == source) continue;
            int dx = r.center().x - source.center().x;
            int dy = r.center().y - source.center().y;
            int dist = dx*dx + dy*dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = r;
            }
        }
        return best;
    }
    //Introduce hallway between created room and previous room
    //Updated to now seek nearest room instead - need to rename
    private void connectToPrevious(Room room) {
        if (rooms.size() <= 1) {
            return;
        }
        Room nearest = findNearestRoom(room);
        carveHallway(nearest.center(), room.center());
    }
    // construct hallway using random turns
//    private void carveHallway(Position start, Position end) {
//        int corridorWidth = random.nextBoolean() ? 1:2;
//        Position corner = turningCorner(start,end);
//        carveSegment(start,corner,corridorWidth);
//        carveSegment(corner,end,corridorWidth);
//    }
    // Removed turningCorner, just doing L shaped
    private void carveHallway(Position a, Position b) {
        int corridorWidth = random.nextBoolean() ? 1:2;
        if (random.nextBoolean()) {
            // horizontal first
            carveSegment(a, new Position(b.x, a.y), corridorWidth);
            carveSegment(new Position(b.x, a.y), b, corridorWidth);
        } else {
            // vertical first
            carveSegment(a, new Position(a.x, b.y), corridorWidth);
            carveSegment(new Position(a.x, b.y), b, corridorWidth);
        }
    }
//    private Position turningCorner(Position start, Position end) {
//        boolean horizontalFirst = random.nextBoolean();
//        if (start.x == end.x && start.y == end.y) {
//            return start;
//        }
//
//        if (start.x == end.x) {
//            int deltaY = random.nextBoolean() ? 1 : -1;
//            int cornerY = clamp(end.y + deltaY, 1, HEIGHT - 2);
//            return new Position(start.x + (random.nextBoolean() ? 1 : -1), cornerY);
//        }
//        if (start.y == end.y) {
//            int deltaX = random.nextBoolean() ? 1 : -1;
//            int cornerX = clamp(end.x + deltaX, 1, WIDTH - 2);
//            return new Position(cornerX, start.y + (random.nextBoolean() ? 1 : -1));
//        }
//
//        if (horizontalFirst) {
//            return new Position(end.x, start.y);
//        } else {
//            return new Position(start.x, end.y);
//        }
//    }
    // Removed as unnecessary
//    private Position turningCorner(Position a, Position b) {
//        if (random.nextBoolean()) {
//            return new Position(b.x, a.y);
//        } else {
//            return new Position(a.x, b.y);
//        }
//    }

    private void carveSegment(Position start, Position end, int corridorWidth) {
        int xStep = Integer.compare(end.x, start.x);
        int yStep = Integer.compare(end.y, start.y);
        Position current = start;
        while (!current.equals(end)) {
            carveCellWithThickness(current.x, current.y, corridorWidth);
            if (current.x != end.x) {
                current = new Position(current.x + xStep, current.y);
            }
            else if (current.y != end.y) {
                current = new Position(current.x, current.y + yStep);
            }
        }
        carveCellWithThickness(end.x, end.y, corridorWidth);
    }

    private void carveCellWithThickness(int x, int y, int width) {
        for (int dx = 0; dx < width; dx +=1) {
            for (int dy = 0; dy < width; dy+=1) {
                int nx = clamp(x + dx, 1, WIDTH-2);
                int ny = clamp(y + dy, 1, HEIGHT-2);
                world[nx][ny] = Tileset.FLOOR;
            }
        }
    }
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private void addPerimeterWalls() {
        int count = 0;
        for (int x = 0; x < WIDTH; x+=1) {
            for (int y =0; y < HEIGHT; y+=1) {
                if (!world[x][y].equals(Tileset.FLOOR)) {
                    continue;
                }
                for (int dx  = -1; dx <=1; dx+=1) {
                    for (int dy = -1; dy <= 1; dy+=1) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (inBounds(nx,ny) && world[nx][ny].equals(Tileset.NOTHING)) {
                            world[nx][ny] = Tileset.WALL;
                        }
                    }
                }
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private int randomRange(int min, int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }

    // Room Class
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
//        boolean overlaps(Room other) {
//            return this.left < other.left + other.width + 1
//                    && this.left + this.width + 1 > other.left
//                    && this.bottom < other.bottom + other.height + 1
//                    && this.bottom + this.height + 1 > other.bottom;
//        }
        // Trying to introduce spacing between rooms to avoid excessive direct neighbors
        boolean overlaps(Room other) {
            return this.left - 1 < other.left + other.width + 1 &&
                    this.left + this.width + 1 > other.left - 1 &&
                    this.bottom - 1 < other.bottom + other.height + 1 &&
                    this.bottom + this.height + 1 > other.bottom - 1;
        }

        Position center() {
            int cx = left + width / 2;
            int cy = bottom + height / 2;
            return new Position(cx, cy);
            }
        }

    // internal positioning class
    private record Position(int x, int y) {

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Position)) {
                return false;
            }
            Position that = (Position) other;
            return this.x == that.x && this.y == that.y;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            return result;
        }
    }

}
