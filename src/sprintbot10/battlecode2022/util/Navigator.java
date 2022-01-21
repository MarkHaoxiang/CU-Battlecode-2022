package sprintbot10.battlecode2022.util;

import battlecode.common.*;

import java.util.Map;
import java.util.Random;


/**
 * Navigator is the class that describes main navigation strategy.
 * Various implementations in 'navigation' package
 */

public abstract class Navigator {

    // Init

    public Random random = new Random();

    public enum MoveResult {
        FAIL,
        IMPOSSIBLE,
        REACHED,
        SUCCESS
    }

    protected static final Direction[] DIRECTIONS =
            {Direction.NORTH, Direction.NORTHEAST,
                    Direction.EAST, Direction.SOUTHEAST,
                    Direction.SOUTH, Direction.SOUTHWEST,
                    Direction.WEST, Direction.NORTHWEST};

    protected final RobotController controller;
    protected final int MAP_HEIGHT;
    protected final int MAP_WIDTH;


    protected Navigator(RobotController controller) {
        this.controller = controller;
        this.MAP_HEIGHT = controller.getMapHeight();
        this.MAP_WIDTH = controller.getMapWidth();
    }

    // Definition
    // Core pathing strategy
    public abstract MoveResult move(MapLocation target_location) throws GameActionException;

    public MoveResult move(Direction direction) throws GameActionException {
        if (direction != null && controller.canMove(direction)) {
            controller.move(direction);
            return MoveResult.SUCCESS;
        }
        return MoveResult.FAIL;
    }

    // Util

    public boolean inMap(MapLocation location) {
        if (location == null) return false;
        return location.x >= 0 && location.y >= 0 && location.x < MAP_WIDTH && location.y < MAP_HEIGHT;
    }

    public static Integer travelDistance(MapLocation from,
                                         MapLocation to) {
        if (from == null || to == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(from.x - to.x),
                Math.abs(from.y - to.y));
    }

    public static int[] relative(MapLocation from,
                                 MapLocation to) {
        return new int[]{to.x - from.x, to.y - from.y};
    }

    public static int directionToInt (Direction direction) {
        switch (direction) {
            case NORTH:
                return 0;
            case NORTHEAST:
                return 1;
            case EAST:
                return 2;
            case SOUTHEAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTHWEST:
                return 5;
            case WEST:
                return 6;
            case NORTHWEST:
                return 7;
            default:
                return 8;
        }
    }

    public static Direction intToDirection (int i) {
        switch (i) {
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.NORTHEAST;
            case 2:
                return Direction.EAST;
            case 3:
                return Direction.SOUTHEAST;
            case 4:
                return Direction.SOUTH;
            case 5:
                return Direction.SOUTHWEST;
            case 6:
                return Direction.WEST;
            case 7:
                return Direction.NORTHWEST;
            default:
                return Direction.CENTER;
        }
    }

    public  MapLocation randomLocation() {
        return new MapLocation((random.nextInt() % MAP_WIDTH + MAP_WIDTH) % MAP_WIDTH, (random.nextInt() % MAP_HEIGHT + MAP_HEIGHT) % MAP_HEIGHT);
//        return new MapLocation(random.nextInt() % MAP_WIDTH, random.nextInt() % MAP_HEIGHT);
    }

    /**
     *
     * @param location center
     * @return list of map locations around location on the map
     * @throws GameActionException
     */
    public MapLocation[] adjacentLocationWithCenter(MapLocation location) throws GameActionException {
        return controller.getAllLocationsWithinRadiusSquared(location,2);
    }

}


