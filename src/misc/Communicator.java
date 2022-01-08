package sprintbot.battlecode2022.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;


public class Communicator {

    /**
     *  Queue-based communication.
     *  Uses index=14~63 in shared memory.
     *  Each message consists of 2 bits for event type and 6 bits for compressed location.
     *  Can hold a maximum of 100 messages simultaneously.
     */

    private static RobotController controller;
    private static int MAP_WIDTH;
    private static int MAP_HEIGHT;

    private static final int COMPRESSED_SIZE = 8; // compress map to 8*8
    private static final int BITS_PER_LOCATION = 6; // 8*8 = 2^6
    private static final int BITS_PER_MESSAGE = 8;

    private static int X_STEP; // the number of x units compressed to 1
    private static int Y_STEP;

    private static final int p = 100;
    private static final int offset = 14;
    private static int tail = 0; // end of queue content (open end, tail not included)
    private static int length = 0; // length of queue content

    // can add one more event type
    public enum Event {
        ARCHON, // 0
        METAL, // 1  Lead or Gold
        SOLDIER // 2  Soldier or Sage
    }

    public Communicator(RobotController controller) {
        this.controller = controller;
        MAP_WIDTH = controller.getMapWidth();
        MAP_HEIGHT = controller.getMapHeight();
        X_STEP = (MAP_WIDTH + MAP_WIDTH % COMPRESSED_SIZE) / COMPRESSED_SIZE;
        Y_STEP = (MAP_HEIGHT + MAP_HEIGHT % COMPRESSED_SIZE) / COMPRESSED_SIZE;
    }

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

    /**
     * Encodes a message into 8 bits.
     * @param event            type of event
     * @param compressed_loc   location of the event
     * @return                 integer representation of the message
     */
    private static int encode(Event event, int compressed_loc) {
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
        return (event_num << BITS_PER_LOCATION) + compressed_loc;
    }

    /**
     * Writes a message to shared memory.
     * @param event          event type
     * @param compressed_loc compressed location of the event
     * @throws GameActionException
     */
    public static void push(Event event, int compressed_loc) throws GameActionException {
        int log = encode(event, compressed_loc);
        if (length == p) { // no more space
            return;
        }
        int id = offset + tail / 2;
        int num = controller.readSharedArray(id);
        if (length % 2 == 1) {
            log = (num - num % (1 << BITS_PER_MESSAGE)) + log;
        }
        else {
            log  = (log << BITS_PER_MESSAGE) + (num % (1 << BITS_PER_MESSAGE));
        }
        controller.writeSharedArray(id, log);
        tail = (tail + 1) % p; // wrap around the end
        length++;
    }

    // TODO: store decoded information in cache instead of returning
    /**
     * Pops and returns the first message in shared memory.
     * @return the first message as an integer
     * @throws GameActionException
     */
    public static int pop() throws GameActionException {
        if (length == 0) { // pop empty queue
            return -1;
        }
        int head = (tail - length + p) % p;
        int id = offset + head / 2;
        int log = controller.readSharedArray(id);
        if (head % 2 == 1) {
            log %= (1 << BITS_PER_MESSAGE);
        }
        else {
            log = (log >> BITS_PER_MESSAGE);
        }
        length--;
        return log;
    }
}
