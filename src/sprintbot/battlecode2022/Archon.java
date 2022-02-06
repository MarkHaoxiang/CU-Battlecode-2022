package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class Archon extends RunnableBot
{

    // Command
    private CommandCommunicator.SpawnOrder last_order = null;
    private MapLocation last_location = null;

    // Build Strategy

    boolean is_peaceful = true;
    int counter = 0;
    int income = 0;
    int miner_number = 0;
    int soldier_number = 0;
    int idle_miner_number = 0;
    double smoothed_income;
    Random rand_gen = new Random(213568721);
    public double rand = 0;
    int num_relocating = 0;

    private final TeamBuild team_strategy = new TeamBuild();

    // Relocate Strategy
    private RelocateStrategy current_relocate_strategy;
    private final ShortRangeRelocate short_range_relocate = new ShortRangeRelocate();
    private final LongRangeRelocate long_range_relocate = new LongRangeRelocate();

    // Archon only
    public static int team_total_miners = 0;
    public static int team_total_soldiers = 0;

    boolean is_long_range_relocate = false;
    boolean is_short_range_relocate = false;

    MapLocation start_location = null;

    private int FARM_TRANSITION_DISTANCE = 8;

    // Repair Strategy
    private final DefaultRepair repair_strategy = new DefaultRepair();

    public Archon(RobotController rc) throws GameActionException
    {
        super(rc);
    }

    @Override
    public void init() throws GameActionException
    {
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
            counter++;
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

        //controller.setIndicatorString(Integer.toString(team_total_miners));


        boolean sr = is_long_range_relocate || is_short_range_relocate;
        // Relocate
        /*
        if (controller.getRoundNum() % 2 == 1 || is_long_range_relocate) {
            if (!is_short_range_relocate && long_range_relocate.shouldRelocate()) {
                long_range_relocate.relocate();
            }
            else if (is_long_range_relocate && controller.getMode() == RobotMode.PORTABLE && controller.isTransformReady()) {
                controller.transform();
                controller.writeSharedArray(CommandCommunicator.MIGRATION_INDEX,num_relocating-1);
                is_long_range_relocate = false;
            }
        }
        else if (controller.getRoundNum() % 2 == 0 || is_short_range_relocate) {
            if (!is_long_range_relocate && short_range_relocate.shouldRelocate()) {
                short_range_relocate.relocate();
            }
        }

         */
        if (!is_short_range_relocate && long_range_relocate.shouldRelocate()) {
            long_range_relocate.relocate();
        }
        else if (is_long_range_relocate && controller.getMode() == RobotMode.PORTABLE && controller.isTransformReady()) {
            controller.transform();
            //controller.writeSharedArray(CommandCommunicator.MIGRATION_INDEX,num_relocating-1);
            is_long_range_relocate = false;
        }

        //System.out.println(is_short_range_relocate);
        //System.out.println(is_long_range_relocate);
        /*
        if (!sr && (is_long_range_relocate || is_short_range_relocate)) {
            controller.writeSharedArray(CommandCommunicator.MIGRATION_INDEX,num_relocating+1);
            System.out.println("Adding");
        }
        */
        /*
        if (long_range_relocate.relocate_target == null)
            controller.setIndicatorString("no relocation target");
        else
            controller.setIndicatorString(long_range_relocate.relocate_target.toString());

         */


        if (controller.getHealth() < controller.getType().getMaxHealth(controller.getLevel()) && controller.getMode() == RobotMode.TURRET) {
            MatrixCommunicator.update(Communicator.Event.BUILDER_REQUEST,getRobotController().getLocation(),true); }
        else { MatrixCommunicator.update(Communicator.Event.BUILDER_REQUEST,getRobotController().getLocation(),false); }

        // Early game build
        if (Cache.opponent_archon_compressed_locations[0] == -1
                &&
                Cache.opponent_soldier_compressed_locations[0] == -1
                &&
                team_total_miners <=
                        Math.max(3 * controller.getArchonCount(), Math.min(6, controller.getMapHeight() * controller.getMapWidth() / 100)))
        {
            //controller.setIndicatorString("Early miner");
            is_peaceful = true;
        }
        // Farmer build
        else
        {
            /*
            MapLocation closest_enemy = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                    Cache.controller.getLocation());
            if (Navigator.travelDistance(getRobotController().getLocation(),closest_enemy) > FARM_TRANSITION_DISTANCE
                    && Cache.opponent_soldiers.length == 0) {
                current_build_strategy = farm_strategy;
            }
            */
            is_peaceful = false;
        }


        if (controller.isActionReady())
        {
            if (current_build_strategy.build()) ;
            else current_repair_strategy.repair();
        }
    }

    // Strategy

    interface RepairStrategy
    {
        boolean repair() throws GameActionException;
    }

    interface BuildStrategy
    {
        boolean build() throws GameActionException;
    }

    interface RelocateStrategy
    {
        boolean relocate() throws GameActionException;

        boolean shouldRelocate() throws GameActionException;
    }

    class TeamBuild implements BuildStrategy
    {

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
        boolean is_best_lab;
        int[] distEdge;

        TeamBuild()
        {
            LAB_DISTANCE = (Cache.MAP_WIDTH + Cache.MAP_HEIGHT) / 20;
        }

        int TEAM_BUILD_ORDER_INDEX = 2;

        boolean wantMutate = false;
        boolean builtMutateSoldier = false;

        private void selfMutate() throws GameActionException
        {

            if (!builtMutateSoldier && wantMutate && Cache.injured <= 4)
            {
                wantMutate = false;
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX, controller.readSharedArray(CommandCommunicator.BANK_INDEX) - 340);
            }

            if (!builtMutateSoldier && !wantMutate && controller.getLevel() == 1 && Cache.injured >= 6 && smoothed_income >= 10)
            {
                wantMutate = true;
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX, controller.readSharedArray(CommandCommunicator.BANK_INDEX) + 340);
            }

            if (!builtMutateSoldier && team_lead >= controller.readSharedArray(CommandCommunicator.BANK_INDEX) && builtMutateSoldier)
            {
                if (tryBuild(RobotType.BUILDER))
                {
                    controller.writeSharedArray(CommandCommunicator.BANK_INDEX, controller.readSharedArray(CommandCommunicator.BANK_INDEX) - 40);
                    builtMutateSoldier = true;
                }
            }

            if (builtMutateSoldier && controller.getLevel() == 2)
            {
                controller.writeSharedArray(CommandCommunicator.BANK_INDEX, controller.readSharedArray(CommandCommunicator.BANK_INDEX) - 300);
            }
        }

        int[] labs_direction = new int[8];
        private void buildLab() throws GameActionException
        {
            // Lab strategy
            // TODO: Maintain minimum distance
            int lab = controller.readSharedArray(CommandCommunicator.LAB_INDEX);
            if (smoothed_income - lab * 8 > 0
                    // && (soldier_number >= 1)
                    && (miner_number >= 2 + lab * 6)
                    && (team_lead >= 40)
                    && (Cache.opponent_soldiers.length + Cache.opponent_villagers.length + Cache.opponent_buildings.length == 0)
                    && (closestSoldierLocation[my_archon_id] == null || distanceToClosestSoldier[my_archon_id] > 6))
            {
                MapLocation my_location = controller.getLocation();
                MapLocation best = null;
                Direction best_direction = null;
                int best_score = -9999;
                for (Direction direction : Constants.DIRECTIONS) {
                    MapLocation potential = my_location.translate(direction.dx*LAB_DISTANCE,direction.dy*LAB_DISTANCE);
                    potential = new MapLocation(Math.max(0, potential.x),Math.max(0, potential.y));
                    potential = new MapLocation(Math.min(Cache.MAP_WIDTH-1, potential.x),Math.min(Cache.MAP_HEIGHT-1, potential.y));
                    if (navigator.inMap(potential)) {
                        int score = 0;
                        if (closestSoldierLocation[my_archon_id] != null) {
                            score = Navigator.travelDistance(potential,closestSoldierLocation[my_archon_id]);
                        }
                        else {
                            score = -navigator.distanceToEdge(potential);
                        }
                        score -= labs_direction[Navigator.directionToInt(direction)] * 1;
                        if (score > best_score) {
                            best_score = score;
                            best = potential;
                            best_direction = direction;
                        }
                    }
                }

                if (best != null) {
                    System.out.println(best);
                    if (tryBuild(RobotType.BUILDER, CommandCommunicator.RobotRole.LAB_BUILDER,best)) {
                        labs_direction[Navigator.directionToInt(best_direction)] += 1;
                        controller.writeSharedArray(CommandCommunicator.LAB_INDEX,lab+1);
                        controller.writeSharedArray(CommandCommunicator.BANK_INDEX,  controller.readSharedArray(CommandCommunicator.BANK_INDEX)+180);
                    }
                }
            }
        }

        private boolean buildSage() throws GameActionException
        {
            double c = 0;
            if (totalDistance == 0)
            {
                // Equal distribution
                for (int i = 0; i < distanceToClosestSoldier.length; i++)
                {
                    c += 1.0 / (double) distanceToClosestSoldier.length;
                    if (rand < c)
                    {
                        if (i == my_archon_id && tryBuild(RobotType.SAGE))
                        {
                            return true;
                        }
                        return false;
                    }
                }
            }
            else
            {
                for (int i = 0; i < distanceToClosestSoldier.length; i++)
                {
                    c += distanceToClosestSoldierAdjusted[i] / totalAdjustedDistance;
                    if (rand < c)
                    {
                        if (i == my_archon_id && tryBuild(RobotType.SAGE))
                        {
                            return true;
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        private boolean buildSoldier() throws GameActionException
        {
            double c = 0;
            if (totalDistance == 0)
            {
                // Equal distribution
                for (int i = 0; i < distanceToClosestSoldier.length; i++)
                {
                    c += 1.0 / (double) distanceToClosestSoldier.length;
                    if (rand < c)
                    {
                        if (i == my_archon_id && tryBuild(RobotType.SOLDIER))
                        {
                            getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                            return true;
                        }
                        return false;
                    }
                }
            }
            else
            {
                for (int i = 0; i < distanceToClosestSoldier.length; i++)
                {
                    c += distanceToClosestSoldierAdjusted[i] / totalAdjustedDistance;
                    if (rand < c)
                    {
                        if (i == my_archon_id && tryBuild(RobotType.SOLDIER))
                        {
                            getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                            return true;
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        private boolean buildMiner() throws GameActionException
        {
            if (getRobotController().getRoundNum() % getRobotController().getArchonCount() == my_ranking ||
                    CommandCommunicator.isLastArchon())
            {
                if (tryBuild(RobotType.MINER))
                {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                    return true;
                }
            }
            return false;
        }

        private boolean buildFarmer() throws GameActionException
        {
            if (distanceToClosestSoldier[my_archon_id] < 8)
            {
                if (maxDistance < 8)
                {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                    return false;
                }
                return false;
            }
            if (getRobotController().getRoundNum() % getRobotController().getArchonCount() == my_ranking ||
                    CommandCommunicator.isLastArchon())
            {
                double[] danger = new double[8];
                double[] score = new double[8];
                MapLocation my_location = getRobotController().getLocation();
                for (int enemy : Cache.opponent_soldier_compressed_locations)
                {
                    if (enemy == -1) break;
                    MapLocation location = Communicator.unzipCompressedLocation(enemy);
                    int dir = Navigator.directionToInt(my_location.directionTo(location));
                    double dist = Navigator.travelDistance(my_location, location);
                    danger[dir] += 60 - dist;
                }
                double maximum_score = -9999;
                int maximum_direction = -1;
                for (int i = 0; i < 8; i++)
                {
                    score[i] = -(danger[i] + danger[(i + 1) % 8] * 0.5 + danger[(i + 7) % 8] * 0.5) + farms[i] * FARM_BUILT_BUFF;
                    Direction dir = Navigator.intToDirection(i);
                    if (farms[i] > MAX_FARMER_PER_FARM || !getRobotController().onTheMap(my_location.add(dir).add(dir).add(dir)))
                    {
                        score[i] = -9999;
                    }
                    if (score[i] > maximum_score)
                    {
                        maximum_score = score[i];
                        maximum_direction = i;
                    }
                }
                if (maximum_score > -9999)
                {
                    Direction dir = Navigator.intToDirection(maximum_direction);
                    MapLocation farm_location = my_location.add(dir).add(dir).add(dir);
                    if (tryBuild(RobotType.BUILDER, dir, CommandCommunicator.RobotRole.FARM_BUILDER, farm_location))
                    {
                        getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                        farms[maximum_direction] += 1;
                        //System.out.println("Building farmer");
                        return true;
                    }
                    else if (tryBuild(RobotType.BUILDER, CommandCommunicator.RobotRole.FARM_BUILDER, farm_location))
                    {
                        getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                        farms[maximum_direction] += 1;
                        //System.out.println("Building farmer");
                        return true;
                    }
                }
                if (distanceToClosestSoldier[my_archon_id] == maxDistance)
                {
                    getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                }
                return false;
            }
            return false;
        }

        @Override
        public boolean build() throws GameActionException
        {

            //Update state information
            team_lead = controller.getTeamLeadAmount(Cache.OUR_TEAM);
            team_gold = controller.getTeamGoldAmount(Cache.OUR_TEAM);

            if (team_lead < 40 && team_gold < 20)
            { //Can't build anything, don't waste bytecode
                return false;
            }

            if (controller.getHealth() < 300 && !Cache.friendly_builder && Cache.friendly_soldiers.length >= Cache.opponent_soldiers.length) {
                tryBuild(RobotType.BUILDER);
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
            is_best_lab = false;
            int minEdge = 9999;
            int[] distEdge = new int[team_spawn_locations.length];

            for (int i = 0; i < team_spawn_locations.length; i++)
            {

                if (team_spawn_locations[i] != null) {
                    distEdge[i] = navigator.distanceToEdge(team_spawn_locations[i]);
                    minEdge = Math.min(minEdge,distEdge[i]);
                }

                if (team_spawn_locations[i] != null && Cache.opponent_soldier_compressed_locations[0] != -1)
                {

                    MapLocation
                            soldier =
                            MatrixCommunicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                                    team_spawn_locations[i]);
                    distanceToClosestSoldier[i] = Navigator.travelDistance(team_spawn_locations[i],
                            soldier);

                    closestSoldierLocation[i] = soldier;
                    //distanceToClosestSoldierAdjusted[i] = Math.sqrt(60 - distanceToClosestSoldier[i]);
                    distanceToClosestSoldierAdjusted[i] = Math.pow((Math.max(Cache.MAP_WIDTH, Cache.MAP_HEIGHT) - distanceToClosestSoldier[i]), 2);
                    totalAdjustedDistance += distanceToClosestSoldierAdjusted[i];

                    totalDistance += distanceToClosestSoldier[i];

                    if (distanceToClosestSoldier[i] > maxDistance)
                    {
                        maxDistance = distanceToClosestSoldier[i];
                    }
                }
                else
                {
                    distanceToClosestSoldier[i] = 9999;
                    distanceToClosestSoldierAdjusted[i] = 0;
                }
            }


            if (controller.getRoundNum() > 1) {
                if (CommandCommunicator.isLastArchon()) {
                    buildLab();
                }
                else if (Cache.opponent_soldier_compressed_locations[0] != -1 && maxDistance <= distanceToClosestSoldier[my_archon_id]) {
                    buildLab();
                }
                else if (minEdge < 9999 && minEdge >= distEdge[my_archon_id]) {
                    buildLab();
                }
            }

            /*
            System.out.println(my_archon_id);
            if (CommandCommunicator.isLastArchon()) {
                for (double i : distanceToClosestSoldierAdjusted) {
                    System.out.println(i);
                }
            }

             */

            selfMutate();

            my_ranking = 0;
            for (int i = 0; i < team_spawn_locations.length; i++)
            {
                if (i == my_archon_id)
                {
                    break;
                }
                if (team_spawn_locations[i] != null)
                {
                    my_ranking++;
                }
            }

            if (is_peaceful) {
                if (miner_number >= 4 && soldier_number == 0) {
                    return tryBuild(RobotType.SOLDIER);
                }
                return tryBuild(RobotType.MINER);
            }
            switch (team_build_order)
            {
                case 0: // Miner, anyone can build tbh
                    if (buildSage()) return true;
                    if (Cache.opponent_soldiers.length > 0 && miner_number >= soldier_number) {
                        return buildSoldier();
                    }
                    return buildMiner();
                case 1:
                    if (Cache.opponent_soldiers.length > 0) {
                        if (buildSage()) return true;
                        return buildSoldier();
                    }
                    return buildMiner();
                case 2:
                case 4: // Soldier
                    if (buildSage()) {
                        getRobotController().writeSharedArray(TEAM_BUILD_ORDER_INDEX, (team_build_order + 1) % 5);
                        return true;
                    }
                    return buildSoldier();
                case 3: // Farmer or miner
                    if (buildSage()) {
                        return true;
                    }
                    if (Cache.opponent_soldiers.length > 0) {
                        return buildSoldier();
                    }
                    if (distanceToClosestSoldier[my_archon_id] <= 8)
                    {
                        return buildMiner();
                    }
                    return buildFarmer();
                default:
                    System.out.println("BUGGGGG Spawn");
            }
            return false;
        }
    }

    class DefaultRepair implements RepairStrategy
    {

        final int LOW_THRESHOLD = 20;

        @Override
        public boolean repair() throws GameActionException
        {
            RobotController controller = getRobotController();

            int lowest_health = 9999;
            int highest_health = -9999;
            MapLocation lowest = null;
            MapLocation highest = null;
            for (RobotInfo robot : Cache.friendly_soldiers)
            {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation()))
                {
                    if (robot.getHealth() < lowest_health)
                    {
                        lowest_health = robot.getHealth();
                        lowest = robot.getLocation();
                        if (lowest_health < 4)
                        {
                            break;
                        }
                    }
                    if (robot.getHealth() > highest_health)
                    {
                        highest_health = robot.getHealth();
                        highest = robot.getLocation();
                    }
                }
            }

            if (lowest_health < 9999)
            {
                if (lowest_health < 20)
                {
                    controller.repair(lowest);
                }
                else
                {
                    controller.repair(highest);
                }
                return true;
            }

            for (RobotInfo robot : Cache.friendly_villagers)
            {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation()))
                {
                    controller.setIndicatorLine(controller.getLocation(), robot.getLocation(), 0, 255, 0);
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

            if (num_relocating >= controller.getArchonCount() - 1) {
                return false;
            }

            if (Cache.opponent_soldiers.length >= 0) {
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

            is_short_range_relocate = true;

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
                    //controller.writeSharedArray(CommandCommunicator.MIGRATION_INDEX,num_relocating-1);
                    is_long_range_relocate = false;
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

    class LongRangeRelocate implements RelocateStrategy
    {
        MapLocation relocate_target = null;
        RobotController controller = getRobotController();

        private boolean needRepair()
        {
            for (RobotInfo robot : Cache.friendly_villagers)
                if (robot.getHealth() < robot.getType().health - controller.getType().getHealing(controller.getLevel()))
                    return true;
            return false;
        }

        @Override
        public boolean shouldRelocate() throws GameActionException
        {
            boolean rechoose = false;
            MapLocation my_location = controller.getLocation();
            if (controller.getMode() == RobotMode.PORTABLE)
            {
                if (relocate_target != null && controller.canSenseLocation(relocate_target) && controller.isLocationOccupied(relocate_target))
                    rechoose = true;
                else
                    return true;
            }

            //if (controller.getRoundNum() % 5 == 0)
            //	return false;

            if (controller.getMode() == RobotMode.TURRET && !controller.canTransform()) {
                return false;
            }

            if (!rechoose && num_relocating >= controller.getArchonCount() - 1) {
                return false;
            }

            MapLocation closest_enemy = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                    Cache.controller.getLocation());
            double current_score = calculateScore(my_location,closest_enemy);
            if (current_score > -25 || needRepair() && !rechoose)
                return false;
            double max_score = -2000000;
            MapLocation best = null;

            for (MapLocation potential : controller.getAllLocationsWithinRadiusSquared(my_location, controller.getType().visionRadiusSquared))
            {
                double v = calculateScore(potential,closest_enemy);
                if (v > max_score)
                {
                    max_score = v;
                    best = potential;
                }
            }
            if (current_score < max_score && controller.canTransform() && controller.getTeamLeadAmount(Cache.OUR_TEAM) <= 200)
            {
                relocate_target = best;
                //System.out.printf("relocate_target:%s, score:%f, scoreofsouth:%f",relocate_target,max_score,
                //        calculateScore(controller.getLocation().add(Direction.SOUTH)));
                return true;
            }
            if (rechoose)
            {
                relocate_target = controller.getLocation();
                return true;
            }
            relocate_target = null;
            return false;
        }

        private double calculateScore(MapLocation location, MapLocation enemy_loc) throws GameActionException
        {
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
            if (Cache.opponent_soldiers.length > 0 || controller.getRoundNum() < 20 && !location.equals(controller.getLocation()))
            {
                return -10000; // Probably shouldn't move
            }
            score = score - controller.senseRubble(location) - controller.getLocation().distanceSquaredTo(location) * 0.1;
            if (enemy_loc == null)
                enemy_loc = new MapLocation(Cache.MAP_WIDTH / 2, Cache.MAP_HEIGHT / 2);
            //System.out.printf("%s, %f - %d\n",location, score, Navigator.travelDistance(controller.getLocation(), enemy_loc));
            int dist_to_enemy = Math.abs(location.x-enemy_loc.x) + Math.abs(location.y-enemy_loc.y);
            score -= Math.abs(dist_to_enemy - 8); // Keep at suitable distance
            if (dist_to_enemy <= 8)
                score -= 3 * (8 - dist_to_enemy);
            if (!location.equals(getRobotController().getLocation()) && controller.isLocationOccupied(location))
            {
                score -= 10000;
            }
            return score;
        }

        @Override
        public boolean relocate() throws GameActionException
        {
            is_long_range_relocate = true;
            controller.setIndicatorLine(controller.getLocation(), relocate_target, 100, 100, 100);
            // Move
            if (!(controller.getMode() == RobotMode.PORTABLE))
            {
                if (controller.canTransform())
                {
                    controller.transform();
                    return true;
                }
                return false;
            }

            // Already there

            if (controller.getLocation().equals(relocate_target))
            {
                if (controller.canTransform())
                {
                    controller.transform();
                    //controller.writeSharedArray(CommandCommunicator.MIGRATION_INDEX,num_relocating-1);
                    is_long_range_relocate = false;
                    return true;
                }
                return false;
            }
            // Move towards
            Navigator.MoveResult move_result = navigator.move(relocate_target);
            return move_result.equals(Navigator.MoveResult.SUCCESS);
        }
    }

    // Util

    private void updateGlobal() throws GameActionException
    {
        income = getRobotController().readSharedArray(CommandCommunicator.INCOME_INDEX) + 2;
        smoothed_income = smoothed_income * (14.0 / 15.0) + income * (1.0 / 15.0);
        miner_number = getRobotController().readSharedArray(CommandCommunicator.TOTAL_FARMER_INDEX);
        soldier_number = getRobotController().readSharedArray(CommandCommunicator.SOLDIER_INDEX);
        idle_miner_number = getRobotController().readSharedArray(CommandCommunicator.IDLE_FARMER_INDEX);
        num_relocating = getRobotController().readSharedArray(CommandCommunicator.MIGRATION_INDEX);

        // Reset all
        if (CommandCommunicator.isLastArchon())
        {
            //System.out.println("hi");
            // System.out.println("========");
            // System.out.println(income);
            // System.out.println(soldier_number);
            // System.out.println(idle_miner_number);
            // System.out.println(miner_number);
            for (int i = 5; i <= 10; i++)
            {
                getRobotController().writeSharedArray(i, 0);
            }

            getRobotController().writeSharedArray(CommandCommunicator.SMOOTH_INCOME, (int) (smoothed_income * 100));
        }
    }

    private boolean tryBuild(RobotType type, Direction dir, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException
    {

        int save = Math.min(500,getRobotController().readSharedArray(CommandCommunicator.BANK_INDEX));
        int money = getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) - save;

        if (money < type.buildCostLead && !is_peaceful && Cache.opponent_soldiers.length == 0 && (team_total_soldiers > 0 || type != RobotType.SOLDIER))
        {
            return false;
        }

        if (getRobotController().canBuildRobot(type, dir))
        {
            getRobotController().buildRobot(type, dir);
            last_order = new CommandCommunicator.SpawnOrder(
                    role,
                    loc);
            last_location = getRobotController().getLocation().add(dir);
            return true;
        }
        return false;
    }

    private boolean tryBuild(RobotType type, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException
    {

        for (Direction dir : Constants.DIRECTIONS)
        {
            if (getRobotController().canBuildRobot(type, dir))
            {
                tryBuild(type, dir, role, loc);
                return true;
            }
        }
        return false;
    }

    private boolean tryBuild(RobotType type, Direction dir) throws GameActionException
    {
        return tryBuild(type, dir, CommandCommunicator.type2Role(type), getRobotController().getLocation().add(dir));
    }

    private boolean tryBuild(RobotType type) throws GameActionException
    {
        int highest = -9999;
        Direction highest_dir = null;
        for (Direction dir : Constants.DIRECTIONS)
        {
            if (!getRobotController().canBuildRobot(type, dir)) continue;
            MapLocation loc = getRobotController().getLocation().add(dir);
            if (getRobotController().onTheMap(loc))
            {
                int score = -getRobotController().senseRubble(loc);
                if (type == RobotType.MINER && Cache.lead_spots.length > 0 && getRobotController().getRoundNum() < 500)
                {
                    for (MapLocation lead : Cache.lead_spots)
                    {
                        int v = getRobotController().senseLead(lead);
                        if (v > Miner.LEAD_MINE_THRESHOLD && getRobotController().getLocation().directionTo(lead) == dir)
                        {
                            score = score + 10 * (6-Navigator.travelDistance(loc, lead));
                            break;
                        }

                    }
                }
                if (score > highest)
                {
                    highest = score;
                    highest_dir = dir;
                }
            }
        }
        if (highest_dir != null && tryBuild(type, highest_dir))
        {
            return true;
        }
        return false;
    }
}
