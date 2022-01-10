package sprintbot.battlecode2022.util;

import battlecode.common.*;

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
        return location.x < MAP_WIDTH && location.y < MAP_HEIGHT;
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

    public MapLocation randomLocation() {
        return new MapLocation(random.nextInt() % MAP_WIDTH, random.nextInt() % MAP_HEIGHT);
    }

    /**
     *
     * @param location center
     * @return list of map locations around location on the map
     * @throws GameActionException
     */
    public MapLocation[] adjacentLocationWithCenter(MapLocation location) throws GameActionException {
        int n = 0;
        for (Direction dir : Direction.allDirections()) {
            if (inMap(location.add(dir))) {
                n ++;
            }
        }
        MapLocation[] res = new MapLocation[n];
        n = 0;
        for (Direction dir : Direction.allDirections()) {
            if (inMap(location.add(dir))) {
                res[n] = location.add(dir);
                n ++;
            }
        }
        return res;
    }

}


