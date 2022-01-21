package sprintbot10.battlecode2022;

import battlecode.common.*;
import sprintbot10.RunnableBot;
import sprintbot10.battlecode2022.util.*;

public class Archon extends RunnableBot {

    // Command
    private CommandCommunicator.SpawnOrder last_order = null;
    private MapLocation last_location = null;

    // Build Strategy

    int income = 0;
    int farmer_number = 0;
    int soldier_number = 0;
    int idle_farmer_number = 0;
    double smoothed_income;

    private final DefaultBuild default_strategy = new DefaultBuild();
    private final FarmBuild farm_strategy = new FarmBuild();
    private final PeacefulBuild peaceful_strategy = new PeacefulBuild();

    // Relocate Strategy
    private final ShortRangeRelocate short_range_relocate = new ShortRangeRelocate();

    // Archon only
    public static int team_total_miners = 0;
    public static int team_total_soldiers = 0;

    MapLocation start_location = null;

    private int FARM_TRANSITION_DISTANCE = 8;

    // Repair Strategy
    private final DefaultRepair repair_strategy = new DefaultRepair();

    public Archon(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
        CommandCommunicator.archonIDShare();
        start_location = getRobotController().getLocation();
    }

    @Override
    public void turn() throws GameActionException {

        Cache.update();
        updateGlobal();

        MatrixCommunicator.read(Communicator.Event.SOLDIER);
        if (getRobotController().getRoundNum() % 5 == 0 || Cache.opponent_archon_compressed_locations == null) {
            MatrixCommunicator.read(Communicator.Event.ARCHON);
        }

        CommandCommunicator.deadManSwitch();

        // Last turn's spawn
        if (last_order != null) {
            CommandCommunicator.spawnMessage(last_order, last_location);
            last_location = null;
            last_order = null;
        }

        CommandCommunicator.updateTeamTotalSpawn();
        RobotController controller = getRobotController();

        BuildStrategy current_build_strategy = default_strategy;
        RepairStrategy current_repair_strategy = repair_strategy;

        controller.setIndicatorString(Integer.toString(team_total_miners));

        // Relocate
        if (short_range_relocate.shouldRelocate()) {
            short_range_relocate.relocate();
        }

        // Early game build
        if (Cache.opponent_archon_compressed_locations[0] == -1
                && Cache.opponent_soldier_compressed_locations[0] == -1
                && team_total_miners <= Math.max(3*controller.getArchonCount(), Math.min(8,controller.getMapHeight()*controller.getMapWidth() / 100))) {
            controller.setIndicatorString("Early miner");
            current_build_strategy = peaceful_strategy;
        }
        // Farmer build
        else {
            MapLocation closest_enemy = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                    Cache.controller.getLocation());
            if (Navigator.travelDistance(getRobotController().getLocation(),closest_enemy) > FARM_TRANSITION_DISTANCE
                    && Cache.opponent_soldiers.length == 0) {
                current_build_strategy = farm_strategy;
            }
        }


        if (controller.isActionReady()) {
            if (current_build_strategy.build());
            else current_repair_strategy.repair();
        }
    }

    // Strategy

    interface RepairStrategy {
        boolean repair() throws GameActionException;
    }

    interface BuildStrategy {
        boolean build() throws GameActionException;
    }

    interface RelocateStrategy {
        boolean relocate() throws GameActionException;
        boolean shouldRelocate() throws GameActionException;
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

    class FarmBuild implements BuildStrategy {

        private int build_order = 1;
        private int[] farms = new int[8];

        int MAX_FARMER_PER_FARM = 10;
        int FARM_BUILT_BUFF = 10;

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
                        build_order = (build_order + 1) % 5;
                        return true;
                    }
                    return false;
                case 1:
                case 2:
                case 4:
                    if (tryBuild(RobotType.SOLDIER)) {
                        build_order = (build_order + 1) % 5;
                        return true;
                    }
                    return false;
                case 3:
                    double[] danger = new double[8];
                    double[] score = new double[8];
                    MapLocation my_location = getRobotController().getLocation();
                    for (int enemy : Cache.opponent_soldier_compressed_locations) {
                        if (enemy == -1) break;
                        MapLocation location = Communicator.unzipCompressedLocation(enemy);
                        int dir = Navigator.directionToInt(my_location.directionTo(location));
                        double dist = Navigator.travelDistance(my_location,location);
                        danger[dir] += 60 - dist;
                    }
                    double maximum_score = -9999;
                    int maximum_direction = -1;
                    for (int i = 0; i < 8; i ++) {
                        score[i] = -(danger[i] + danger[(i+1) % 8] * 0.5 + danger[(i+7)%8] * 0.5) + farms[i] * farm_strategy.FARM_BUILT_BUFF;
                        Direction dir = Navigator.intToDirection(i);
                        if (farms[i] > MAX_FARMER_PER_FARM || !getRobotController().onTheMap(my_location.add(dir).add(dir).add(dir))) {
                            score[i] = -9999;
                        }
                        if (score[i] > maximum_score) {
                            maximum_score = score[i];
                            maximum_direction = i;
                        }
                    }
                    if (maximum_score > -9999) {
                        Direction dir = Navigator.intToDirection(maximum_direction);
                        MapLocation farm_location = my_location.add(dir).add(dir).add(dir);
                        if (tryBuild(RobotType.BUILDER,dir, CommandCommunicator.RobotRole.FARM_BUILDER,farm_location)) {
                            build_order = (build_order + 1) % 5;
                            farms[maximum_direction] += 1;
                            //System.out.println("Building farmer");
                            return true;
                        }
                        else if (tryBuild(RobotType.BUILDER, CommandCommunicator.RobotRole.FARM_BUILDER,farm_location)) {
                            build_order = (build_order + 1) % 5;
                            farms[maximum_direction] += 1;
                            //System.out.println("Building farmer");
                            return true;
                        }
                    }
                    else {
                        build_order = (build_order + 1) % 5;
                    }
                    return false;
                default:
                    System.out.println("Default build order exception.");
                    build_order = 0;
                    return build();
            }
        }
    }

    class DefaultRepair implements RepairStrategy {

        final int LOW_THRESHOLD = 20;
        @Override
        public boolean repair() throws GameActionException {
            RobotController controller = getRobotController();

            int lowest_health = 9999;
            int highest_health = -9999;
            MapLocation lowest = null;
            MapLocation highest = null;
            for (RobotInfo robot : Cache.friendly_soldiers) {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation())) {
                    if (robot.getHealth() < lowest_health) {
                        lowest_health = robot.getHealth();
                        lowest = robot.getLocation();
                    }
                    if (robot.getHealth() > highest_health) {
                        highest_health = robot.getHealth();
                        highest = robot.getLocation();
                    }
                }
            }

            if (lowest_health < 9999) {
                if (lowest_health < 20) {
                    controller.repair(lowest);
                }
                else {
                    controller.repair(highest);
                }
                return true;
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

    class ShortRangeRelocate implements RelocateStrategy {

        MapLocation relocate_target = null;
        RobotController controller = getRobotController();

        @Override
        public boolean shouldRelocate() throws GameActionException {
            MapLocation my_location = controller.getLocation();
            if (controller.getMode() == RobotMode.PORTABLE) {
                return true;
            }
            double current_score = calculateScore(my_location);
            double max_score = -20000;
            MapLocation best = null;
            for (MapLocation potential : controller.getAllLocationsWithinRadiusSquared(my_location,5)) {
                double v = calculateScore(potential);
                if (v > max_score) {
                    max_score = v;
                    best = potential;
                }
            };
            if (current_score / 2 < max_score && controller.canTransform()) {
                relocate_target = best;
                return true;
            }

            relocate_target = null;
            return false;
        }

        private double calculateScore (MapLocation location) throws GameActionException {
            /*
            for (RobotInfo building : Cache.friendly_buildings) {
                if (building.getType() == RobotType.ARCHON
                        && building.getLocation().distanceSquaredTo(location) <= 8
                        && building.getMode() == RobotMode.TURRET) {
                    return -10000;
                }
            }
            */
            double score = 0;
            if (Cache.opponent_soldiers.length > 0 || controller.getRoundNum() < 20 && location != controller.getLocation()) {
                score -= 1000; // Probably shouldn't move
            }
            score = score - controller.senseRubble(location)-10;
            score = score - controller.getLocation().distanceSquaredTo(location) * 0.1;
            if (!location.equals(getRobotController().getLocation()) && controller.isLocationOccupied(location)) {
                score -= 100;
            }
            return score;
        }

        @Override
        public boolean relocate() throws GameActionException {

            controller.setIndicatorLine(controller.getLocation(),relocate_target,100,100,100);
            // Move
            if (! (controller.getMode() == RobotMode.PORTABLE)) {
                if (controller.canTransform()) {
                    controller.transform();
                    return true;
                }
                return false;
            }

            // Already there

            if (controller.getLocation().equals(relocate_target)) {
                if (controller.canTransform()) {
                    controller.transform();
                    return true;
                }
                return false;
            }
            // Move towards
            Navigator.MoveResult move_result = navigator.move(relocate_target);
            switch (move_result) {
                case SUCCESS:
                    return true;
                default:
                    return false;
            }
        }
    }

    // Util

    private void updateGlobal() throws GameActionException {
        income = getRobotController().readSharedArray(CommandCommunicator.INCOME_INDEX) + 2;
        smoothed_income = smoothed_income * 0.9 + income * 0.1;
        farmer_number = getRobotController().readSharedArray(CommandCommunicator.TOTAL_FARMER_INDEX);
        soldier_number = getRobotController().readSharedArray(CommandCommunicator.SOLDIER_INDEX);
        idle_farmer_number = getRobotController().readSharedArray(CommandCommunicator.IDLE_FARMER_INDEX);

        // Reset all
        if (CommandCommunicator.isLastArchon()) {
            // System.out.println("========");
            // System.out.println(income);
            // System.out.println(soldier_number);
            //System.out.println(idle_farmer_number);
            // System.out.println(farmer_number);
            for (int i = 5; i<=8; i ++) {
                getRobotController().writeSharedArray(i,0);
            }
        }
    }

    private boolean tryBuild(RobotType type, Direction dir, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException {

        if (getRobotController().canBuildRobot(type, dir)) {
            getRobotController().buildRobot(type, dir);
            last_order = new CommandCommunicator.SpawnOrder(
                    role,
                    loc);
            last_location = getRobotController().getLocation().add(dir);
            return true;
        }
        return false;
    }

    private boolean tryBuild(RobotType type, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException {

        for (Direction dir : Constants.DIRECTIONS) {
            if (getRobotController().canBuildRobot(type, dir)) {
                tryBuild(type,dir,role,loc);
                return true;
            }
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
                        int v = getRobotController().senseLead(lead);
                        if (v > Miner.LEAD_MINE_THRESHOLD && getRobotController().getLocation().directionTo(lead) == dir) {
                            score = score - 10 * Navigator.travelDistance(loc,lead) + (v - Miner.LEAD_MINE_THRESHOLD) / 5;
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


