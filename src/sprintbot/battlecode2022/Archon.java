package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.awt.*;
import java.util.Random;

public class Archon extends RunnableBot {

    // Command
    private CommandCommunicator.SpawnOrder last_order = null;
    private MapLocation last_location = null;

    // Build Strategy

    int counter = 0;
    int income = 0;
    int miner_number = 0;
    int soldier_number = 0;
    int idle_miner_number = 0;
    double smoothed_income;
    Random rand_gen = new Random(213568721);
    public double rand = 0;

    private final PeacefulBuild peaceful_strategy = new PeacefulBuild();
    private final TeamBuild team_strategy = new TeamBuild();

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

        // Ensure consistent even when overflow bytecode
        while (counter < getRobotController().getRoundNum()) {
            counter ++;
            rand = rand_gen.nextDouble();
        }


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

        BuildStrategy current_build_strategy = team_strategy;
        RepairStrategy current_repair_strategy = repair_strategy;

        controller.setIndicatorString(Integer.toString(team_total_miners));

        // Relocate
        if (short_range_relocate.shouldRelocate()) {
            short_range_relocate.relocate();
        }

        // Early game build
        if (Cache.opponent_archon_compressed_locations[0] == -1
                && Cache.opponent_soldier_compressed_locations[0] == -1
                && team_total_miners <= Math.max(3*controller.getArchonCount(), Math.min(6,controller.getMapHeight()*controller.getMapWidth() / 100))) {
            controller.setIndicatorString("Early miner");
            current_build_strategy = peaceful_strategy;
        }
        // Farmer build
        else {
            /*
            MapLocation closest_enemy = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                    Cache.controller.getLocation());
            if (Navigator.travelDistance(getRobotController().getLocation(),closest_enemy) > FARM_TRANSITION_DISTANCE
                    && Cache.opponent_soldiers.length == 0) {
                current_build_strategy = farm_strategy;
            }
            */
            current_build_strategy = team_strategy;
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

    class TeamBuild implements  BuildStrategy {

        int MAX_FARMER_PER_FARM = 10;
        int FARM_BUILT_BUFF = 10;
        int LAB_DISTANCE;
        private int[] farms = new int[8];

        RobotController controller = getRobotController();
        int team_lead;
        int team_gold;
        int my_archon_id;
        int team_build_order;
        MapLocation[] team_spawn_locations; // Friendly Archon locations

        MapLocation[] closestSoldierLocation;
        int[] distanceToClosestSoldier;
        double[] distanceToClosestSoldierAdjusted;
        int totalDistance;
        double totalAdjustedDistance;
        int maxDistance;
        int my_ranking;

        TeamBuild() {
            LAB_DISTANCE = (Cache.MAP_WIDTH+Cache.MAP_HEIGHT)/20;
        }

        int TEAM_BUILD_ORDER_INDEX = 2;

        boolean wantMutate = false;
        boolean builtMutateSoldier = false;

        private void selfMutate() throws GameActionException {

            if (!builtMutateSoldier && wantMutate && Cache.injured <= 4) {
                wantMutate = false;
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)-340);
            }

            if (!builtMutateSoldier && !wantMutate && controller.getLevel() == 1 && Cache.injured >= 6 && smoothed_income >= 10) {
                wantMutate = true;
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)+340);
            }

            if (!builtMutateSoldier && team_lead >= controller.readSharedArray(CommandCommunicator.BANK_INDEX) && builtMutateSoldier) {
                if (tryBuild(RobotType.BUILDER)) {
                    controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)-40);
                    builtMutateSoldier = true;
                }
            }

            if (builtMutateSoldier && controller.getLevel() == 2) {
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)-300);
            }
        }

        private void buildLab() throws GameActionException {
            // Lab strategy
            // TODO: Maintain minimum distance
            int lab = controller.readSharedArray(CommandCommunicator.LAB_INDEX);

            if (smoothed_income - lab * 20 > 10
                    && (soldier_number >= 2 + lab * 15)
                    && (miner_number >= 4 + lab * 5)
                    && (team_lead >= 40)
                    && (closestSoldierLocation[my_archon_id] == null || distanceToClosestSoldier[my_archon_id] > 10)) {
                MapLocation my_location = controller.getLocation();
                MapLocation best = null;
                int best_score = -9999;
                for (Direction direction : Constants.DIRECTIONS) {
                    MapLocation potential = my_location.translate(direction.dx*LAB_DISTANCE,direction.dy*LAB_DISTANCE);
                    if (navigator.inMap(potential)) {
                        int score = 0;
                        if (closestSoldierLocation[my_archon_id] != null) {
                            score = Navigator.travelDistance(potential,closestSoldierLocation[my_archon_id]);
                        }
                        else {
                            score = Math.max(Cache.MAP_HEIGHT,Cache.MAP_WIDTH) - Math.min(
                                    Math.min(potential.x,Cache.MAP_WIDTH- potential.x),
                                    Math.min(potential.y,Cache.MAP_WIDTH- potential.y));
                        }
                        if (score > best_score) {
                            best_score = score;
                            best = potential;
                        }
                    }
                }

                if (best != null) {
                    if (tryBuild(RobotType.BUILDER, CommandCommunicator.RobotRole.LAB_BUILDER,best)) {
                        controller.writeSharedArray(CommandCommunicator.LAB_INDEX,lab+1);
                        controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)+180);
                    }
                }
            }
        }

        private boolean buildSage() throws GameActionException {
            double c = 0;
            if (totalDistance == 0) {
                // Equal distribution
                for (int i = 0; i < distanceToClosestSoldier.length; i ++) {
                    c += 1.0 / (double)distanceToClosestSoldier.length;
                    if (rand < c) {
                        if (i == my_archon_id && tryBuild(RobotType.SAGE)) {
                            return true;
                        }
                        return false;
                    }
                }
            }
            else {
                for (int i = 0; i < distanceToClosestSoldier.length; i ++) {
                    c += distanceToClosestSoldierAdjusted[i] / totalAdjustedDistance;
                    if (rand < c) {
                        if (i == my_archon_id && tryBuild(RobotType.SAGE)) {
                            return true;
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        private boolean buildSoldier() throws GameActionException {
            double c = 0;
            if (totalDistance == 0) {
                // Equal distribution
                for (int i = 0; i < distanceToClosestSoldier.length; i ++) {
                    c += 1.0 / (double)distanceToClosestSoldier.length;
                    if (rand < c) {
                        if (i == my_archon_id && tryBuild(RobotType.SOLDIER)) {
                            getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                            return true;
                        }
                        return false;
                    }
                }
            }
            else {
                for (int i = 0; i < distanceToClosestSoldier.length; i ++) {
                    c += distanceToClosestSoldierAdjusted[i] / totalAdjustedDistance;
                    if (rand < c) {
                        if (i == my_archon_id && tryBuild(RobotType.SOLDIER)) {
                            getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                            return true;
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        private boolean buildMiner() throws GameActionException {
            if (getRobotController().getRoundNum() % getRobotController().getArchonCount() == my_ranking ||
                    CommandCommunicator.isLastArchon()) {
                if (tryBuild(RobotType.MINER)) {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                    return true;
                }
            }
            return false;
        }

        private boolean buildFarmer() throws GameActionException {
            if (distanceToClosestSoldier[my_archon_id] < 8) {
                if (maxDistance < 8) {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                    return false;
                }
                return false;
            }
            if (getRobotController().getRoundNum() % getRobotController().getArchonCount() == my_ranking ||
                    CommandCommunicator.isLastArchon()) {
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
                    score[i] = -(danger[i] + danger[(i+1) % 8] * 0.5 + danger[(i+7)%8] * 0.5) + farms[i] * FARM_BUILT_BUFF;
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
                        getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                        farms[maximum_direction] += 1;
                        //System.out.println("Building farmer");
                        return true;
                    }
                    else if (tryBuild(RobotType.BUILDER, CommandCommunicator.RobotRole.FARM_BUILDER,farm_location)) {
                        getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                        farms[maximum_direction] += 1;
                        //System.out.println("Building farmer");
                        return true;
                    }
                }
                if (distanceToClosestSoldier[my_archon_id] == maxDistance) {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX,(team_build_order + 1) % 5);
                }
                return false;
            }
            return false;
        }

        @Override
        public boolean build() throws GameActionException {

            //Update state information
            team_lead = controller.getTeamLeadAmount(Cache.OUR_TEAM);
            team_gold = controller.getTeamGoldAmount(Cache.OUR_TEAM);

            if (team_lead < 40 && team_gold < 20) { //Can't build anything, don't waste bytecode
                return false;
            }
            my_archon_id = CommandCommunicator.getMyID();
            team_build_order = controller.readSharedArray(TEAM_BUILD_ORDER_INDEX) % 5;
            team_spawn_locations = CommandCommunicator.getSpawnLocations();
            distanceToClosestSoldier = new int[team_spawn_locations.length];
            distanceToClosestSoldierAdjusted = new double[team_spawn_locations.length];
            totalDistance = 0;
            totalAdjustedDistance = 0;
            maxDistance = 0;
            closestSoldierLocation = new MapLocation[team_spawn_locations.length];

            for (int i = 0; i < team_spawn_locations.length; i ++) {
                if (team_spawn_locations[i] != null && Cache.opponent_soldier_compressed_locations[0] != -1) {

                    MapLocation soldier = MatrixCommunicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,team_spawn_locations[i]);
                    distanceToClosestSoldier[i] = Navigator.travelDistance(team_spawn_locations[i],
                            soldier);

                    closestSoldierLocation[i] = soldier;
                    //distanceToClosestSoldierAdjusted[i] = Math.sqrt(60 - distanceToClosestSoldier[i]);
                    distanceToClosestSoldierAdjusted[i] = Math.pow((Math.max (Cache.MAP_WIDTH,Cache.MAP_HEIGHT)- distanceToClosestSoldier[i]),2);
                    totalAdjustedDistance += distanceToClosestSoldierAdjusted[i];

                    totalDistance += distanceToClosestSoldier[i];

                    if (distanceToClosestSoldier[i] > maxDistance) {
                        maxDistance = distanceToClosestSoldier[i];
                    }
                }
                else {
                    distanceToClosestSoldier[i] = 9999;
                    distanceToClosestSoldierAdjusted[i] = 0;
                }
            }

            buildLab();
            selfMutate();

            my_ranking = 0;
            for (int i = 0; i < team_spawn_locations.length; i++) {
                if (i == my_archon_id) {
                    break;
                }
                if (team_spawn_locations[i] != null) {
                    my_ranking ++;
                }
            }

            if (buildSage()) return true;

            switch (team_build_order) {
                case 0: // Miner, anyone can build tbh
                    return buildMiner();
                case 1:
                case 2:
                case 4: // Soldier
                    return buildSoldier();
                case 3: // Farmer or miner
                    if (miner_number < 6 * controller.getArchonCount() || distanceToClosestSoldier[my_archon_id] <= 8) {
                        return buildMiner();
                    }
                    return buildFarmer();
                default:
                    System.out.println("BUGGGGG Spawn");
            }
            return false;
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
                        if (lowest_health < 15) {
                            break;
                        }
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

            if (controller.getRoundNum() % 5 == 0) {
                return false;
            }

            double current_score = calculateScore(my_location);
            if (current_score > -18) {
                return false;
            }
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
            if (Cache.opponent_soldiers.length > 0 || controller.getRoundNum() < 20 && !location.equals(controller.getLocation())) {
                return -1000; // Probably shouldn't move
            }
            score = score - controller.senseRubble(location)-10 - controller.getLocation().distanceSquaredTo(location) * 0.1;
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
        smoothed_income = smoothed_income * (14.0/15.0) + income * (1.0/15.0);
        miner_number = getRobotController().readSharedArray(CommandCommunicator.TOTAL_FARMER_INDEX);
        soldier_number = getRobotController().readSharedArray(CommandCommunicator.SOLDIER_INDEX);
        idle_miner_number = getRobotController().readSharedArray(CommandCommunicator.IDLE_FARMER_INDEX);

        // Reset all
        if (CommandCommunicator.isLastArchon()) {
            //System.out.println("hi");
            // System.out.println("========");
            // System.out.println(income);
            // System.out.println(soldier_number);
            // System.out.println(idle_miner_number);
            // System.out.println(miner_number);
            for (int i = 5; i<=10; i ++) {
                getRobotController().writeSharedArray(i,0);
            }

            getRobotController().writeSharedArray(CommandCommunicator.SMOOTH_INCOME,(int)(smoothed_income * 100));
        }
    }

    private boolean tryBuild(RobotType type, Direction dir, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException {

        int save = getRobotController().readSharedArray(CommandCommunicator.BANK_INDEX);
        int money = getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) - save;

        if (money < type.buildCostLead) {
            return false;
        }

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


