package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

public class Archon extends RunnableBot {

    // Command
    private CommandCommunicator.SpawnOrder last_order = null;

    // Build Strategy
    private final DefaultBuild default_strategy = new DefaultBuild();
    private final PeacefulBuild peaceful_strategy = new PeacefulBuild();
    private final RatioBuild ratio_strategy = new RatioBuild();

    // Archon only
    public static int team_total_miners = 0;
    public static int team_total_soldiers = 0;

    // Repair Strategy
    private final DefaultRepair repair_strategy = new DefaultRepair();

    // Miner to Soldier ratio
    private int miners_per_six_droids = 3;

    public Archon(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
        CommandCommunicator.archonIDShare();
        MatrixCommunicator.update(Communicator.Event.FRIENDLY_ARCHON, getRobotController().getLocation());
    }

    @Override
    public void turn() throws GameActionException {

        Cache.update();

        MatrixCommunicator.read(Communicator.Event.SOLDIER);
        MatrixCommunicator.read(Communicator.Event.ARCHON);

        CommandCommunicator.deadManSwitch();

        // Last turn's spawn
        if (last_order != null) {
            CommandCommunicator.spawnMessage(last_order);
            last_order = null;
        }

        CommandCommunicator.updateTeamTotalSpawn();
        RobotController controller = getRobotController();

        int round_num = controller.getRoundNum();
        // recalculate spawning ratio between archons every 50 rounds
        if (round_num % 100 == 0) {
            calculateRatioBetweenArchons();
        }

        BuildStrategy current_build_strategy = default_strategy;
//        BuildStrategy current_build_strategy = ratio_strategy;
        RepairStrategy current_repair_strategy = repair_strategy;

        controller.setIndicatorString(Integer.toString(team_total_miners));

        // Early game build
        if (Cache.opponent_archon_compressed_locations[0] == -1
                && Cache.opponent_soldier_compressed_locations[0] == -1
                && team_total_miners <= Math.max(3*controller.getArchonCount(), Math.min(8,controller.getMapHeight()*controller.getMapWidth() / 100))) {
            controller.setIndicatorString("Early miner");
            current_build_strategy = peaceful_strategy;
        }

        if (controller.isActionReady()) {
            if (current_build_strategy.build());
            else current_repair_strategy.repair();
        }
    }

    private void calculateRatioBetweenArchons() throws GameActionException {
        // METAL and SOLDIER are already read
        MatrixCommunicator.read(Communicator.Event.FRIENDLY_ARCHON);

        // TODO: not to recalculate in every archon
        // TODO: only include archons that are still alive
        MapLocation[] friendly_archon_locations = new MapLocation[4];
        int archon_num = 0;
        for (int i = 0; i < Cache.friendly_archon_compressed_locations.length; i++) {
            if (Cache.friendly_archon_compressed_locations[i] == -1) {
                break;
            }
            friendly_archon_locations[archon_num++] = MatrixCommunicator.unzipCompressedLocation(Cache.friendly_archon_compressed_locations[i]);
        }

        int[] soldier_count = new int[4];
        for (int i = 0; i < Cache.opponent_soldier_compressed_locations.length; i++) {
            if (Cache.opponent_soldier_compressed_locations[i] == -1) {
                break;
            }
            int min_dist = 9999;
            Integer min_archon = null;
            for (int j = 0; j < archon_num; j++) {
                int dist = MatrixCommunicator.unzipCompressedLocation(Cache.opponent_soldier_compressed_locations[i]).distanceSquaredTo(friendly_archon_locations[j]);
                if (dist < min_dist) {
                    min_dist = dist;
                    min_archon = j;
                }
            }
            if (min_archon != null) {
                soldier_count[min_archon]++;
            }
        }

        int[] metal_count = new int[4];
        for (int i = 0; i < Cache.metal_compressed_locations.length; i++) {
            if (Cache.metal_compressed_locations[i] == -1) {
                break;
            }
            int min_dist = 9999;
            Integer min_archon = null;
            for (int j = 0; j < archon_num; j++) {
                int dist = MatrixCommunicator.unzipCompressedLocation(Cache.opponent_soldier_compressed_locations[i]).distanceSquaredTo(friendly_archon_locations[j]);
                if (dist < min_dist) {
                    min_dist = dist;
                    min_archon = j;
                }
            }
            if (min_archon != null) {
                metal_count[min_archon]++;
            }
        }

        RobotController controller = getRobotController();
        int my_compressed_location = MatrixCommunicator.compressLocation(controller.getLocation());
        miners_per_six_droids = -1;
        for (int i = 0; i < archon_num; i++) {
            if (my_compressed_location == Cache.friendly_archon_compressed_locations[i]) {
                // TODO: Metal not communicated
                System.out.println(String.format("Metal count: %d; Soldier count: %d", metal_count[i], soldier_count[i]));
                if (metal_count[i] != 0) {
                    miners_per_six_droids = (int) ((double) metal_count[i] / (double) (metal_count[i] + soldier_count[i]) * 6.0);
                }
                else {
                    miners_per_six_droids = 3;
                }
                break;
            }
        }
    }

    // Strategy

    interface RepairStrategy {
        boolean repair() throws GameActionException;
    }

    interface BuildStrategy {
        boolean build() throws GameActionException;
    }

    class PeacefulBuild implements BuildStrategy {

        int i = 0;

        @Override
        public boolean build() throws GameActionException {

            // Evenly distribute spawning
            int[] other_archons = CommandCommunicator.getArchonIDList();
            int archon_num = other_archons.length + 1;
            Integer ranking = null;
            for (int i = 0; i < other_archons.length; i ++) {
                if (other_archons[i] == getRobotController().getID()) {
                    archon_num --;
                    ranking = i;
                }
            }
            if (ranking == null) {
                ranking = 3;
            }

            if (getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) < (getRobotController().getArchonCount()-ranking-1) * 50
                    && getRobotController().getRoundNum() % archon_num != ranking) {
                return false;
            }
            RobotType to_build = RobotType.MINER;
            if (i % 5 == 4) {
                to_build = RobotType.SOLDIER;
            }
            if (tryBuild(to_build)) {
                i ++;
                return true;
            }
            return false;
        }
    }

    class DefaultBuild implements BuildStrategy {

        private int build_order = 1;

        @Override
        public boolean build() throws GameActionException {


            // Evenly distribute spawning
            int[] other_archons = CommandCommunicator.getArchonIDList();
            int archon_num = other_archons.length + 1;
            Integer ranking = null;
            for (int i = 0; i < other_archons.length; i ++) {
                if (other_archons[i] == getRobotController().getID()) {
                    archon_num --;
                    ranking = i;
                }
            }
            if (ranking == null) {
                ranking = 3;
            }
            if (getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) < 150
                    && getRobotController().getRoundNum() % archon_num != ranking) {
                return false;
            }

                switch (build_order) {
                    case 0:
                        if (tryBuild(RobotType.MINER)) {
                            build_order = (build_order + 1) % 2;
                            return true;
                        }
                        return false;
                    case 1:
                        if (tryBuild(RobotType.SOLDIER)) {
                            build_order = (build_order + 1) % 2;
                            return true;
                        }
                        return false;
                    default:
                        System.out.println("Default build order exception.");
                        build_order = 0;
                        return build();
            }
        }
    }

    class RatioBuild implements BuildStrategy {

        private int build_order = 1;

        @Override
        public boolean build() throws GameActionException {
            
            // Spawn according to ratio
            int[] other_archons = CommandCommunicator.getArchonIDList();
            int archon_num = other_archons.length + 1;
            Integer ranking = null;
            for (int i = 0; i < other_archons.length; i ++) {
                if (other_archons[i] == getRobotController().getID()) {
                    archon_num --;
                    ranking = i;
                }
            }
            if (ranking == null) {
                ranking = 3;
            }
            if (getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) < 150
                    && getRobotController().getRoundNum() % archon_num != ranking) {
                return false;
            }

            if (miners_per_six_droids == 0) {
                if (tryBuild(RobotType.SOLDIER)) {
                    build_order = (build_order + 1) % 6;
                    return true;
                }
                return false;
            }
            else if (miners_per_six_droids <= 3) {
                if (build_order % (6 / miners_per_six_droids) == 0) {
                    if (tryBuild(RobotType.MINER)) {
                        build_order = (build_order + 1) % 6;
                        return true;
                    }
                    return false;
                }
                else {
                    if (tryBuild(RobotType.SOLDIER)) {
                        build_order = (build_order + 1) % 6;
                        return true;
                    }
                    return false;
                }
            }
            else if (miners_per_six_droids < 6) {
                if (build_order % (6 / (6 - miners_per_six_droids)) == 0) {
                    if (tryBuild(RobotType.SOLDIER)) {
                        build_order = (build_order + 1) % 6;
                        return true;
                    }
                    return false;
                }
                else {
                    if (tryBuild(RobotType.MINER)) {
                        build_order = (build_order + 1) % 6;
                        return true;
                    }
                    return false;
                }
            }
            else {
                if (tryBuild(RobotType.MINER)) {
                    build_order = (build_order + 1) % 6;
                    return true;
                }
                return false;
            }
        }
    }

    class DefaultRepair implements RepairStrategy {
        @Override
        public boolean repair() throws GameActionException {
            RobotController controller = getRobotController();

            for (RobotInfo robot : Cache.friendly_soldiers) {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation())) {
                    controller.setIndicatorLine(controller.getLocation(),robot.getLocation(),0,255,0);
                    controller.repair(robot.getLocation());
                    return true;
                }
            }

            for (RobotInfo robot : Cache.friendly_villagers) {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation())) {
                    controller.setIndicatorLine(controller.getLocation(),robot.getLocation(),0,255,0);
                    controller.repair(robot.getLocation());
                    return true;
                }
            }

            return false;
        }
    }

    // Util

    private boolean tryBuild(RobotType type, Direction dir, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException {

        // TODO: Deal with edge case of adjacent archons

        if (getRobotController().canBuildRobot(type, dir)) {
            getRobotController().buildRobot(type, dir);
            last_order = new CommandCommunicator.SpawnOrder(
                    role,
                    loc);
            return true;
        }
        return false;
    }

    private boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        return tryBuild(type,dir,CommandCommunicator.type2Role(type),getRobotController().getLocation().add(dir));
    }

    private boolean tryBuild(RobotType type) throws GameActionException {
        int highest = -9999;
        Direction highest_dir = null;
        for (Direction dir : Constants.DIRECTIONS) {
            if (!getRobotController().canBuildRobot(type,dir)) continue;
            MapLocation loc = getRobotController().getLocation().add(dir);
            if (getRobotController().onTheMap(loc)) {
                int score = -getRobotController().senseRubble(loc);
                if (type == RobotType.MINER && Cache.lead_spots.length > 0) {
                    for (MapLocation lead : Cache.lead_spots) {
                        if (getRobotController().senseLead(lead) > Miner.LEAD_MINE_THRESHOLD) {
                            score = score -10 * Navigator.travelDistance(loc,lead);
                            break;
                        }
                    }
                }
                if (score > highest) {
                    highest = score;
                    highest_dir = dir;
                }
            }
        }
        if (highest_dir!=null && tryBuild(type, highest_dir)) {
            return true;
        }
        return false;
    }
}


