package sprintbot.battlecode2022.util;

import battlecode.common.*;
public class CommandCommunicator extends Communicator {

    private static final int BITS_FOR_ARCHON_IDS = 10;
    private static final int HEADER_PRIORITY_OFFSET = 2;
    private static int archon_id = -1;


    /* Header Schema - first 16 bits

    * Archon IDs
    * Goal: For spawned units to identify memory location
    * N number of icons. Stores ID of max 3 archons. Naturally sorted
    * 10 ^ 3 < 1024 : 10 bits allocated
    */

    /**
     * Runs on turn 0 to transmit archon ID to header
     * @throws GameActionException
     */
    public static void archonIDShare() throws GameActionException{
        System.out.println("Initializing header");
        if (controller.getRoundNum() > 1 || controller.getType() != RobotType.ARCHON) {
            System.out.println(controller.getRoundNum());
            System.out.println(controller.getType());
            System.out.println("Who wrote this bug? archonIDShare");
            return;
        }
        int shift = 16 - BITS_FOR_ARCHON_IDS;
        int me = controller.getID();
        System.out.println(controller.readSharedArray(0));
        int friends = controller.readSharedArray(0) >>> shift; // Number from 0 to 1023
        if (Constants.DEBUG) System.out.println(friends);
        // First archon ~
        if (friends == 0) {
            archon_id = 0;
            if (Constants.DEBUG) {
                System.out.printf("Header initialized to %d", me + 1);
            }
            controller.writeSharedArray(0,(me + 1) << shift);
            return;
        }
        // Other archons already written
        friends --;
        if (friends >= 100) {
            if (Constants.DEBUG) {
                System.out.printf("No initialization needed");
            }
            archon_id = 3; // Already full
        }
        else if (friends >= 10) {
            archon_id = 2;
            friends += 100 * me + 1;
            if (Constants.DEBUG) {
                System.out.printf("Header initialized to %d", friends);
            }
            controller.writeSharedArray(0,friends << shift);
        }
        else {
            archon_id = 1;
            friends += 10 * me + 1;
            controller.writeSharedArray(0,friends << shift);
            if (Constants.DEBUG) {
                System.out.printf("Header initialized to %d", friends);
            }
        }
    }

    /**
     * Get list of maximum 3 friendly archon ID
     * @throws GameActionException
     */
    public static int[] getArchonIDList() throws GameActionException{
        int friends = (controller.readSharedArray(0) >>> (16 - BITS_FOR_ARCHON_IDS)) - 1;
        if (friends / 100 > 0) {
            return new int[] {friends % 10, (friends / 10) % 10, (friends / 100) % 10};
        }
        if (friends / 10 > 0) {
            return new int[] {friends % 10, (friends / 10) % 10};
        }
        return new int[] {friends % 10};
    }

    /* Priority Schema
    *  16 - 31
    *  Archon update only communication channel
    * */

    // TODO: Implement Priority Schema

    /* Dead Man's Switch Schema
    32, 48, 64, 80
    * Alternating bit for other robots to check if archon is alive
    */

    public static void deadManSwitch() throws GameActionException {
        if (controller.getType() != RobotType.ARCHON) {
            System.out.println("Who wrote a bug?");
            return;
        }
        int bit_id = archon_id + HEADER_PRIORITY_OFFSET;
        if ((controller.getRoundNum() & 1) == 1) {
            controller.writeSharedArray(bit_id,1 << (BITS_PER_INTEGER-1));
        } else {
            controller.writeSharedArray(bit_id,0);
        }
    }

    /* Spawn Schema
    * 33 - 63
    * Each archon allocated 15 bits to transfer commands to newly spawned units
    * 3 bits role identifier (eg. farmer, defender, attacker, wall etc)
    * 12 bits uncompressed map location
    */

    // Rename as fit
    public enum RobotRole {
        A(0),
        B(1),
        C(2),
        D(3),
        E(4),
        F(5),
        G(6),
        DEFAULT(7);
        public final int id;
        RobotRole(int id) {
            this.id = id;
        }
    }

    private static final RobotRole[] int2role = new RobotRole[] {
            RobotRole.A,
            RobotRole.B,
            RobotRole.C,
            RobotRole.D,
            RobotRole.E,
            RobotRole.F,
            RobotRole.G
    };

    public static class SpawnOrder {
        public RobotRole role;
        public MapLocation loc;
        public SpawnOrder (RobotRole role, MapLocation loc) {
            this.role = role;
            this.loc = loc;
        }
    }

    /**
     *
     * @param role role
     * @param target_location location
     * @throws GameActionException
     */
    public static void spawnMessage(RobotRole role, MapLocation target_location) throws GameActionException {
        spawnMessage(new SpawnOrder(role,target_location));
    }

    /**
     *
     * @param order - spawn command
     * @throws GameActionException
     */
    public static void spawnMessage(SpawnOrder order) throws GameActionException {
        RobotRole role = order.role;
        MapLocation target_location = order.loc;
        if (controller.getType() != RobotType.ARCHON) {
            System.out.println("Who wrote a bug? spawnMessage A");
            return;
        }
        int value = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon_id);
        value = value + (role.id << 12) + (target_location.x << 6) + target_location.y;
        //System.out.println(HEADER_PRIORITY_OFFSET + archon_id);
        controller.writeSharedArray(HEADER_PRIORITY_OFFSET + archon_id,value);
    }


    /**
     *
     * @return spawn command
     * @throws GameActionException
     */
    public static SpawnOrder getSpawnRole() throws GameActionException {
        if (controller.getType() == RobotType.ARCHON || Cache.age > 0) {
            System.out.println("Who wrote a bug? getSpawnRole A");
            return new SpawnOrder(RobotRole.DEFAULT,controller.getLocation());
        }
        // Distance 1 first before 2 to account for edge case of two nearby archons
        for (Direction dir : new Direction[] {Direction.NORTH, Direction.EAST,
                Direction.SOUTH, Direction.WEST,
                Direction.NORTHEAST, Direction.SOUTHEAST,
                Direction.SOUTHWEST, Direction.NORTHWEST}) {
            RobotInfo robot = controller.senseRobotAtLocation(controller.getLocation().add(dir));
            if (null != robot && robot.getType() == RobotType.ARCHON && robot.getTeam() == Cache.OUR_TEAM) {
                int id = robot.getID();
                int[] archon_ids = getArchonIDList();
                for (int i = 0; i < archon_ids.length; i ++) {
                    if (archon_ids[i] == id) {
                        archon_id = i;
                        break;
                    }
                }
                if (archon_id == -1 && archon_ids.length == 3) {
                    archon_id = 3;
                }
                // Bug catching
                if (archon_id == -1) {
                    System.out.println("Who wrote a bug? getSpawnRole B");
                    return new SpawnOrder(RobotRole.DEFAULT,controller.getLocation());
                }
                // Decode message
                int value = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon_id);
                return new SpawnOrder(
                        int2role[(value & (0b11 << 12))>>>12],
                        new MapLocation(
                                (value >> 6)&0b111111,
                                value&0b111111
                        ));
            }
        }
        System.out.println("Who wrote a bug? getSpawnRole C");
        return new SpawnOrder(RobotRole.DEFAULT,controller.getLocation());
    }

}