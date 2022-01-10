package sprintbot.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprintbot.RobotPlayer;

public class Communicator {
    /* Connection Schema
     * 0 - 15 : Header
     * 16 - 16 + Archon Number * : Arcon communication
     * Other : Unit communication
     * */

    /* Header Schema
     * Map orientation - 2 bits
     * Archon number - 2 bits
     * Current strategy? - 3 bits
     *
     * */

    /* Archon Schema
     *
     * Shared overall macro strategy (eg. attack certain location) 16 bits
     * Each archon will then have a 16 bit channel for themselves to give out orders
     *
     *   Archon order
     *   1 bit alternating between rounds - still alive?
     *   3 bits - spawned unit position (archon will check to ensure no two archons spawn at same location)
     *   8 bits - location
     *   4 bits - flag
     *
     * */

    /* Unit Schema
     *
     *  Round Number % 5 = 0: Mining
     *  Scouting
     *  Micro
     *
     * */

    protected static RobotController controller;
    protected static int MAP_WIDTH;
    protected static int MAP_HEIGHT;

    protected static final int COMPRESSED_SIZE = 8; // compress map to 8*8
    protected static final int NUM_OF_COMPRESSED_LOCATIONS = 64;
    protected static int X_STEP; // the number of x units compressed to 1
    protected static int Y_STEP;

    protected static final int BITS_PER_INTEGER = 16;

    public enum Event {
        ARCHON, // Opponent Archon
        METAL, // Metal & no Miner around
        SOLDIER // Opponent Soldier or Sage
    }

    public static final Event[] events = {
            Event.ARCHON,
            Event.METAL,
            Event.SOLDIER
    };

    public static void init(RobotController controller) {
        Communicator.controller = controller;
        MAP_WIDTH = controller.getMapWidth();
        MAP_HEIGHT = controller.getMapHeight();
        X_STEP = (MAP_WIDTH + MAP_WIDTH % COMPRESSED_SIZE) / COMPRESSED_SIZE;
        Y_STEP = (MAP_HEIGHT + MAP_HEIGHT % COMPRESSED_SIZE) / COMPRESSED_SIZE;
    }

    protected static int eventNum(Event event) {
        int event_num;
        switch (event) {
            case ARCHON:
                event_num = 0;
                break;
            case METAL:
                event_num = 1;
                break;
            case SOLDIER:
                event_num = 2;
                break;
            default: // won't happen
                throw new IllegalStateException("Unexpected Event: " + event);
        }
        return event_num;
    }

    protected static int[] getCacheArray(Event event) {
        int[] cache_array;
        switch (event) {
            case ARCHON:
                cache_array = Cache.opponent_archon_compressed_locations;
                break;
            case METAL:
                cache_array = Cache.metal_compressed_locations;
                break;
            case SOLDIER:
                cache_array = Cache.opponent_soldier_compressed_locations;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event);
        }
        return cache_array;
    }


    // TODO: Please use OOP when transmitting different message types. No static switch statements.


    public static void update(Event event, MapLocation location) throws GameActionException {
        switch (Cache.age % 5) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
        }
    }

    // TODO: Compression ideas? Discuss.
    /**
     * Compresses a MapLocation to a number between 0~63 (x-major order).
     * @param loc a valid location on map
     * @return    compressed location
     */
    public static int compressLocation(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        return x / X_STEP + (y / Y_STEP) * COMPRESSED_SIZE;
    }

    // TODO: Test out different ways of message storing, bitset? boolean array? directly read? Discuss.

    // TODO: Should we attempt distributed pathfinding? Discuss.

    // TODO: Please store decoded information in cache!
}
