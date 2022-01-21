package sprintbot.battlecode2022.util;

import battlecode.common.*;
import sprintbot.battlecode2022.Archon;

public class CommandCommunicator extends Communicator {

    private static final int BITS_FOR_ARCHON_IDS = 10;
    private static final int HEADER_PRIORITY_OFFSET = 16;
    private static int archon_id = -1;
    private static int orignal_archon_number;
    private static MapLocation[] all_archon_spawn_locations = null;


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
        orignal_archon_number = controller.getArchonCount();
        all_archon_spawn_locations = new MapLocation[orignal_archon_number];
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

    public static final int BANK_INDEX = 3;
    public static final int LAB_INDEX = 4;
    public static final int SOLDIER_INDEX = 5;
    public static final int IDLE_FARMER_INDEX = 6;
    public static final int INCOME_INDEX = 7;
    public static final int TOTAL_FARMER_INDEX = 8;

    /* Global Data Schema
    *  2 - Build order
    *  3 - Desired bank
    *  4 - Lab number
    *  5 - Soldier number
    *  6 - Idle farmer number
    *  7 - Income
    *  8 - Total farmer number
    *  9 - Sage number
    *  10 - Watch tower number
    *
    * */

    // TODO: Implement Priority Schema

    /* Dead Man's Switch Schema
    * Alternating bit for other robots to check if archon is alive
    */

    public static void deadManSwitch() throws GameActionException {
        if (controller.getType() != RobotType.ARCHON) {
            System.out.println("Who wrote a bug?");
            return;
        }
        int bit_id = archon_id * 2 + HEADER_PRIORITY_OFFSET;
        if ((controller.getRoundNum() & 1) == 1) {
            controller.writeSharedArray(bit_id,1 << (BITS_PER_INTEGER-1));
        } else {
            controller.writeSharedArray(bit_id,0);
        }
        controller.writeSharedArray(bit_id + 1, 0);
    }

    /* Spawn Schema
    * 16th - 23rd
    * Each archon allocated 31 bits to transfer commands to newly spawned units
    * 3 bits role identifier (eg. farmer, defender, attacker, wall etc)
    * 12 bits uncompressed map location
    * 12 bits spawn location
    */

    // Rename as fit
    public enum RobotRole {
        MINER(0),
        SOLDIER(1),
        FIGHT_BUILDER(2),
        SAGE(3),
        FARM_BUILDER(4),
        LAB_BUILDER(5),
        G(6),
        H(7);
        public final int id;
        RobotRole(int id) {
            this.id = id;
        }
    }

    private static final RobotRole[] int2role = new RobotRole[] {
            RobotRole.MINER,
            RobotRole.SOLDIER,
            RobotRole.FIGHT_BUILDER,
            RobotRole.SAGE,
            RobotRole.FARM_BUILDER,
            RobotRole.LAB_BUILDER,
            RobotRole.G,
            RobotRole.H
    };

    public static RobotRole type2Role (RobotType type) {
        switch (type) {
            case SAGE:
                return RobotRole.SAGE;
            case MINER:
                return RobotRole.MINER;
            case BUILDER:
                return RobotRole.FIGHT_BUILDER;
            case SOLDIER:
                return RobotRole.SOLDIER;
            default:
                System.out.println("Who wrote a bug? type2Role");
                return RobotRole.H;
        }
    }

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
     */
    public static void spawnMessage(RobotRole role, MapLocation target_location, MapLocation spawn_location) throws GameActionException {
        spawnMessage(new SpawnOrder(role,target_location), spawn_location);
    }

    /**
     *
     * @param order - spawn command
     */
    public static void spawnMessage(SpawnOrder order, MapLocation spawn_location) throws GameActionException {
        RobotRole role = order.role;
        MapLocation target_location = order.loc;
        int value = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon_id * 2);
        value = value + (role.id << 12) + (target_location.x << 6) + target_location.y + 1;
        //System.out.println(HEADER_PRIORITY_OFFSET + archon_id);
        controller.writeSharedArray(HEADER_PRIORITY_OFFSET + archon_id * 2,value);
        controller.writeSharedArray(HEADER_PRIORITY_OFFSET + archon_id * 2 + 1, (spawn_location.x << 6) + spawn_location.y + 1);
    }

    public static int getMyID() {
        return archon_id;
    }

    public static MapLocation[] getSpawnLocations() {
        return all_archon_spawn_locations;
    }

    /**
     *
     * @return spawn command
     */
    public static SpawnOrder getSpawnRole() throws GameActionException {

        MapLocation my_location = controller.getLocation();
        for (int archon = 0; archon < 4; archon ++) {
            int spawn_location = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2 + 1);
            if (spawn_location - 1 == (my_location.x << 6) + my_location.y) {
                // Found the archon
                int value = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2);
                if ((value&0b111111) == 0) {
                    System.out.println("No spawn message. Theres a bug.");
                }
                value --;
                return new SpawnOrder(
                        int2role[(value & (0b111 << 12))>>>12],
                        new MapLocation(
                                (value >> 6)&0b111111,
                                value&0b111111
                        ));
            }
        }
        System.out.println("No spawn message. Theres a bug. B");

        return new SpawnOrder(type2Role(controller.getType()),controller.getLocation());
    }

    public static void updateTeamTotalSpawn() throws GameActionException {
        if (controller.getType() != RobotType.ARCHON) {
            System.out.println("Who wrote a bug? updateSpawnA");
            return;
        }

        for (int archon = orignal_archon_number; --archon >= 0; ) {
            if (controller.getRoundNum() <= 1) {
                // Bad idea
                return;
            }

            int v1 = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2);
            int v2 = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2 + 1);

            // Check for dead archons
            if ((v2 >> (BITS_PER_INTEGER - 1) == 1)) {
                // Dead friendly archon
                all_archon_spawn_locations[archon] = null;
                continue;
            } else if (
                    (archon>archon_id && (v1 >> (BITS_PER_INTEGER - 1)) == (controller.getRoundNum() & 1)) ||
                    (archon<archon_id && (v1 >> (BITS_PER_INTEGER - 1)) != (controller.getRoundNum() & 1))
            ) {
                // Dead friendly archon but unlabelled
                //System.out.println("NEW DEAD FRIENDLY?");
                all_archon_spawn_locations[archon] = null;
                controller.writeSharedArray(HEADER_PRIORITY_OFFSET + archon * 2 + 1, 1 << 15);
                continue;
            }

            if ((v1 & 0b111111) == 0) {
                continue;
            }
            // Early game
            RobotRole role = int2role[(v1 & (0b111 << 12)) >>> 12];
            switch (role) {
                case MINER:
                    Archon.team_total_miners += 1;
                    break;
                case SOLDIER:
                    Archon.team_total_soldiers += 1;
                    break;
            }

            if (v2 > 0) {
                v2 -= -1;
                int x = (v2 >> 6) & 0b111111;
                int y = v2 & 0b111111;
                all_archon_spawn_locations[archon] = new MapLocation(x, y);
            }
        }
    }

    public static boolean isLastArchon () throws GameActionException {
        if (controller.getRoundNum() <= 1) {
            // Bad idea
            return false;
        }
        for (int archon = orignal_archon_number; --archon >=0;) {
            if (archon == archon_id) {
                return true;
            }
            int v1 = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2);
            int v2 = controller.readSharedArray(HEADER_PRIORITY_OFFSET + archon * 2+1);
            if ((v2 >> (BITS_PER_INTEGER - 1) == 1)) {
                // Dead friendly archon
                continue;
            }
            else if ((v1 >> (BITS_PER_INTEGER - 1)) == (controller.getRoundNum() & 1)) {
                // Dead friendly archon but unlabelled
                //System.out.println("NEW DEAD FRIENDLY?");
                controller.writeSharedArray(HEADER_PRIORITY_OFFSET + archon * 2+1,1 << 15);
            }
            else {
                // Alive friendly archon with higher id
                return false;
            }
        }
        System.out.println("BUGGGGGGGG. isLastArchon");
        return false;
    }

}
