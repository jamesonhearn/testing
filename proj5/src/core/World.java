package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.*;

public class World {
    public static final int WIDTH = 250;
    public static final int HEIGHT = 100;
    private static final int MIN_ROOM_SIZE = 10;
    private static final int MAX_ROOM_SIZE = 30;
    private static final int MAX_ROOM_ATTEMPTS = 1000;
    private static final double TARGET_FILL_RATIO = 0.85;

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
    // postcheck to validate connectivity via BFS
    public TETile[][] generate() {
        for (int attempt = 0; attempt < 5; attempt += 1) {
            resetWorld();
            carveRoomsWithHallways();
            addPerimeterWalls();
            //addSecondRingFrontWalls();    // add extra front walls around those walls
            correctBackWalls();
            placeElevator();
            if (allFloorsConnected()) {
                break;
            }
        }
        return world;
    }


    private void placeElevator() {
        List<Position> candidates = new ArrayList<>();

        // scan for valid back-wall positions
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {

                // must be a back wall
                if (!world[x][y].equals(Tileset.BACK_WALL))
                    continue;

                // elevator must sit above a floor
                if (!world[x][y - 1].equals(Tileset.FLOOR))
                    continue;

                // optional: avoid corners (looks cleaner)
                boolean leftWallOrFloor  = !world[x - 1][y].equals(Tileset.NOTHING);
                boolean rightWallOrFloor = !world[x + 1][y].equals(Tileset.NOTHING);
                if (!leftWallOrFloor || !rightWallOrFloor)
                    continue;

                candidates.add(new Position(x, y));
            }
        }

        if (candidates.isEmpty()) {
            // fallback: do nothing
            return;
        }

        // pick one random location
        Position p = candidates.get(new Random().nextInt(candidates.size()));
        world[p.x][p.y] = Tileset.ELEVATOR;
    }



    public void initializeVoid() {
        for (int x=0; x<WIDTH; x+=1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    private void resetWorld() {
        rooms.clear();
        initializeVoid();
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
        int corridorWidth = 2; //random.nextBoolean() ? 1:2;
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

    // While I have not had any generated world that were not fully connected give the hallway implementation, this check should help
    // protect against future implementation changes causing disconnect
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

    private void addPerimeterWalls() {
        TETile[][] updated = world;

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {

                if (!world[x][y].equals(Tileset.FLOOR)) continue;

                boolean upEmpty    = isEmpty(x, y + 1);
                boolean downEmpty  = isEmpty(x, y - 1);
                boolean leftEmpty  = isEmpty(x - 1, y);
                boolean rightEmpty = isEmpty(x + 1, y);

                //
                // ─────────────── TOP EDGE ───────────────
                //
                if (upEmpty) {
                    // 1 side wall above
                    if (inBounds(x, y + 1))
                        updated[x][y + 1] = Tileset.WALL_SIDE;

                    // 1 cap above that
                    if (inBounds(x, y + 2))
                        updated[x][y + 2] = Tileset.WALL_TOP;
                }

                //
                // ─────────────── BOTTOM EDGE ───────────────
                //
                if (downEmpty) {
                    // 1 side wall below
                    if (inBounds(x, y - 1))
                        updated[x][y - 1] = Tileset.WALL_TOP;

                    // 1 cap
                    if (inBounds(x, y - 2) && !world[x][y-2].equals(Tileset.WALL_TOP))
                        updated[x][y - 2] = Tileset.WALL_SIDE;
                }

                //
                // ─────────────── LEFT EDGE + CORNERS ───────────────
                //
                if (leftEmpty || world[x-1][y].equals(Tileset.WALL_SIDE) ) {

                    // ░░ TOP-LEFT CORNER: up & left are empty
                    if (upEmpty) {
                        // side-left-up
                        if (inBounds(x - 1, y + 1)) {
                            updated[x - 1][y + 1] = Tileset.WALL_TOP;
                            updated[x - 1][y] = Tileset.WALL_TOP;
                        }
                        // cap above that
                        if (inBounds(x - 1, y + 2))
                            updated[x - 1][y + 2] = Tileset.WALL_TOP;
                    }

                    // ░░ BOTTOM-LEFT CORNER
                    else if (downEmpty) {
                        if (inBounds(x - 1, y - 1)){
                            updated[x - 1][y - 1] = Tileset.WALL_TOP;
                            updated[x-1][y] = Tileset.WALL_TOP;
                        }

                        if (inBounds(x - 1, y - 2) && !world[x-1][y-2].equals(Tileset.WALL_TOP))
                            updated[x - 1][y - 2] = Tileset.WALL_SIDE;
                    }

                    // ░░ MID-LEFT general case
                    else {
                        if (inBounds(x - 1, y))
                            updated[x - 1][y] = Tileset.WALL_TOP;
                    }
                }

                //
                // ─────────────── RIGHT EDGE + CORNERS (MIRRORED) ───────────────
                //
                if (rightEmpty) {

                    // ░░ TOP-RIGHT CORNER
                    if (upEmpty) {
                        if (inBounds(x + 1, y + 1)) {
                            updated[x + 1][y + 1] = Tileset.WALL_TOP;
                            updated[x + 1][y] = Tileset.WALL_TOP;
                        }
                        if (inBounds(x + 1, y + 2))
                            updated[x + 1][y + 2] = Tileset.WALL_TOP;
                    }

                    // ░░ BOTTOM-RIGHT CORNER
                    else if (downEmpty) {
                        if (inBounds(x + 1, y - 1)) {
                            updated[x + 1][y] = Tileset.WALL_TOP;
                            updated[x + 1][y - 1] = Tileset.WALL_TOP;
                        }
                        if (inBounds(x + 1, y - 2) && !world[x+1][y-2].equals(Tileset.WALL_TOP))
                            updated[x + 1][y - 2] = Tileset.WALL_SIDE;
                    }

                    // ░░ MID-RIGHT
                    else {
                        if (inBounds(x + 1, y))
                            updated[x + 1][y] = Tileset.WALL_TOP;
                    }
                }
            }
        }

        // commit
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                world[x][y] = updated[x][y];
    }
    private boolean isEmpty(int x, int y) {
        return inBounds(x, y) && world[x][y].equals(Tileset.NOTHING);
    }

    private void addSecondRingFrontWalls() {
        int[][] dirs = {
                { 0,  1},
                { 0, -1},
                { 1,  0},
                {-1,  0}
        };

        // Step A: Snapshot original perimeter walls
        boolean[][] isPerimeterWall = new boolean[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (isPrimaryWall(world[x][y])) {
                    isPerimeterWall[x][y] = true;
                }
            }
        }

        // Step B: Use ONLY the snapshot to place the extra front wall layer
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {

                if (!isPerimeterWall[x][y]) continue;

                for (int[] d : dirs) {
                    int nx = x + d[0];
                    int ny = y + d[1];

                    if (inBounds(nx, ny) && world[nx][ny].equals(Tileset.NOTHING)) {
                        world[nx][ny] = Tileset.FRONT_WALL;
                    }
                }
            }
        }
    }

    private boolean isPrimaryWall(TETile tile) {
        return tile.equals(Tileset.BACK_WALL) ||
                tile.equals(Tileset.FRONT_WALL) ||
                tile.equals(Tileset.LEFT_WALL) ||
                tile.equals(Tileset.RIGHT_WALL);
    }
    private void correctBackWalls() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {

                // Only fix wall tiles
                if (!world[x][y].equals(Tileset.LEFT_WALL) &&
                        !world[x][y].equals(Tileset.BACK_WALL)) continue;

                boolean floorBelow =
                        inBounds(x, y - 1) && world[x][y - 1].equals(Tileset.FLOOR);

                if (floorBelow) {
                    world[x][y] = Tileset.BACK_WALL;
                }
            }
        }
    }


    //    private TETile selectWallTile(int x, int y) {
//        boolean floorNorth = inBounds(x, y+1) && world[x][y+1].equals(Tileset.FLOOR);
//        //boolean wallSouth = inBounds(x, y-1) && (world[x][y-1].equals(Tileset.LEFT_WALL) || world[x][y-1].equals(Tileset.RIGHT_WALL));
//        boolean floorSouth = inBounds(x, y-1) && world[x][y-1].equals(Tileset.FLOOR);
//        boolean floorEast  = inBounds(x+1, y) && world[x+1][y].equals(Tileset.FLOOR);
//        boolean floorWest  = inBounds(x-1, y) && world[x-1][y].equals(Tileset.FLOOR);
//
//
//        // Floor BELOW → front wall
//        if (floorNorth) return Tileset.FRONT_WALL;
//
//        // Floor to the right → left-facing wall
//        if (floorEast) return Tileset.LEFT_WALL;
//
//        // Floor to the left → right-facing wall
//        if (floorWest) return Tileset.RIGHT_WALL;
//
//        // Floor ABOVE → back wall (wall above floor)
//        //if (wallSouth) return Tileset.LEFT_WALL;
//
//        // Floor ABOVE → back wall (wall above floor)
//        if (floorSouth) return Tileset.BACK_WALL;
//
//
//        // Default (isolated)
//        return Tileset.WALL;
//    }
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