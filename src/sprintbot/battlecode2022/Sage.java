package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;
import sprintbot.battlecode2022.util.navigation.IntegratedNavigator;

public class Sage extends RunnableBot {

    private MoveStrategy current_moving_strategy;
    private AttackStrategy current_attacking_strategy;
    private DefaultAttackStrategy default_attack_strategy = new DefaultAttackStrategy();

    private final RetreatMoveStrategy retreat_move_strategy = new RetreatMoveStrategy();
    private final FightMoveStrategy fight_move_strategy = new FightMoveStrategy();
    private final SearchMoveStrategy search_move_strategy = new SearchMoveStrategy();

    boolean has_moved = false;

    public Sage(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
    }

    // Strategy

    interface MoveStrategy
    {
        boolean move() throws GameActionException;
    }

    interface AttackStrategy
    {
        boolean attack() throws GameActionException;
    }

    // Macro
    class SearchMoveStrategy implements MoveStrategy
    {
        private MapLocation move_target;
        private boolean is_random = false;
        private final int GIVE_UP_THRESHOLD_TURN = 1;
        private final int IGNORE_SOLDIER_THRESHOLD = 60;

        @Override
        public boolean move() throws GameActionException
        {

            // Might have just left range
            if (!getRobotController().isActionReady()) {
                return false;
            }

            // Go for nearby soldiers first
            MapLocation my_location = getRobotController().getLocation();
            MapLocation potential_target = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_soldier_compressed_locations,
                    Cache.controller.getLocation());

            if (potential_target != null
                    && potential_target != my_location) {
                move_target = potential_target;
                is_random = false;
            }

            // And then opponent archons
            if (is_random || move_target == null) {
                potential_target = Communicator.getClosestFromCompressedLocationArray(Cache.opponent_archon_compressed_locations,
                        Cache.controller.getLocation());
                if (potential_target != null) {
                    is_random = false;
                    move_target = potential_target;
                }
            }


            // If nothing is available then choose a random location
            if (move_target == null)
            {
                is_random = true;
                move_target = navigator.randomLocation();
            }


            Navigator.MoveResult move_result = navigator.move(move_target);

            switch (move_result) {
                case SUCCESS:
                    //getRobotController().setIndicatorString("SUCCESS");
                    return true;
                case REACHED:
                    // Nothing here, go somewhere else
                    MatrixCommunicator.update(Communicator.Event.SOLDIER,my_location,false);
                    MatrixCommunicator.update(Communicator.Event.ARCHON,my_location,false);
                    MatrixCommunicator.update(Communicator.Event.OPPONENT_MINER,my_location,false);
                    move_target = null;
                    return false;
                case IMPOSSIBLE:
                    //getRobotController().setIndicatorString("IMPOSSIBLE");
                    if (Navigator.travelDistance(my_location,move_target) <= 8) {
                        MatrixCommunicator.update(Communicator.Event.SOLDIER,move_target,false);
                    }
                    move_target = navigator.randomLocation();
                    is_random = true;
                    if (navigator.move(move_target) == Navigator.MoveResult.SUCCESS) {
                        return true;
                    };
                    return false;
                case FAIL:
                    //getRobotController().setIndicatorString("FAIL");
                default:
                    return false;
            }
        }
    }

    class FightMoveStrategy implements MoveStrategy {
        private MapLocation move_target = null;

        public double calculateScore (MapLocation location) throws GameActionException {
            RobotController controller = getRobotController();
            MapLocation my_location = controller.getLocation();

            boolean in_opponent_vision = false;
            boolean opponent_in_my_vision = false;
            boolean in_range = false;

            double expected_damage_from_opponents = 0;
            double my_expected_damage = 0;

            int max_health_in_range = 0;
            double damage_from_envision = 0;
            int envision_potential_kills = 0;
            int envision_bonus = 0;

            int distance_to_closest = 9999;

            if (location.equals(my_location)
                    || controller.canMove(my_location.directionTo(location))
                    || (!controller.isMovementReady() && !controller.isLocationOccupied(location))) { // !ismovementready for attackStrategy to use. Careful
                for (RobotInfo robot : Cache.opponent_soldiers) {
                    MapLocation robot_location = robot.getLocation();
                    int distance_to_robot = robot_location.distanceSquaredTo(location);
                    distance_to_closest = Math.min(distance_to_robot, distance_to_closest);

                    if (!opponent_in_my_vision && distance_to_robot <= RobotType.SAGE.visionRadiusSquared) {
                        opponent_in_my_vision = true;
                    }
                    if (!in_opponent_vision && distance_to_robot < robot.getType().visionRadiusSquared) {
                        in_opponent_vision = true;
                    }

                    if (distance_to_robot <= robot.getType().actionRadiusSquared) {
                        double rubble = controller.senseRubble(robot_location);
                        expected_damage_from_opponents += robot.getType().damage / ((1.0 + rubble / 10.0) * robot.getType().actionCooldown / 10.0);
                    }

                    if (distance_to_robot <= RobotType.SAGE.actionRadiusSquared) {
                        if (max_health_in_range < 45) {
                            max_health_in_range = Math.max(max_health_in_range, Math.min(45, robot.getHealth()));
                        }
                        in_range = true;
                        int potential_damage = (int) (robot.getType().getMaxHealth(1) * 0.22);
                        damage_from_envision += potential_damage;
                        if (robot.getHealth() % RobotType.SAGE.damage < robot.getType().getMaxHealth(robot.getLevel()) * 0.22) {
                            envision_bonus += 1;
                            if (potential_damage > robot.getHealth()) {
                                envision_potential_kills += 1;
                            }
                        }
                    }
                }

                double rubble = controller.senseRubble(location);
                double damage = controller.getType().damage;
                double base_cooldown = controller.getType().actionCooldown;
                double cooldown_rubble = ((1.0 + rubble / 10.0) * base_cooldown / 10.0);
                my_expected_damage = damage / cooldown_rubble;
                damage_from_envision = damage_from_envision / cooldown_rubble;

                // Calculate score
                double score = 0;
                if (!opponent_in_my_vision && controller.isActionReady()) {
                    score = -10;
                }

                if (!controller.isActionReady()) {
                    if (!in_opponent_vision) {
                        score += 5;
                    }
                    score -= ((1.0 + rubble / 10.0) * controller.getType().movementCooldown / 10.0);
                    ;
                    score -= expected_damage_from_opponents;
                } else {
                    //score = Math.max(my_expected_damage, damage_from_abyss / base_cooldown);
                    double score_attack = my_expected_damage;
                    double score_envision = damage_from_envision + ((envision_bonus - 1) * 2 * base_cooldown) / cooldown_rubble + base_cooldown * envision_potential_kills * 0.5 / cooldown_rubble;
                    score = Math.max(score_attack,score_envision);
                }
                return score;
            }
            return -100;
        }

        @Override
        public boolean move() throws GameActionException {
            RobotController controller = getRobotController();
            MapLocation my_location = controller.getLocation();


            double best_score = -9999.0;
            MapLocation best_location = null;

            // Not attacking soldier
            if (Cache.opponent_soldiers.length == 0) {
                // Prioritize buildings
                if (Cache.opponent_buildings.length > 0) {
                    if (!my_location.isWithinDistanceSquared(Cache.opponent_buildings[0].getLocation(),
                            RobotType.SOLDIER.actionRadiusSquared)) {
                        move_target = Cache.opponent_buildings[0].getLocation();
                        Navigator.MoveResult move_result = navigator.move(move_target);
                        if (move_result == Navigator.MoveResult.SUCCESS) {
                            return true;
                        }
                        return false;
                    }
                }

                if (Cache.opponent_villagers.length > 0) {
                    if (!my_location.isWithinDistanceSquared(Cache.opponent_villagers[0].getLocation(),
                            RobotType.SOLDIER.actionRadiusSquared)) {
                        move_target = Cache.opponent_villagers[0].getLocation();
                        if (getRobotController().getLocation().distanceSquaredTo(move_target) > RobotType.SAGE.actionRadiusSquared) {
                            Navigator.MoveResult move_result = navigator.move(move_target);
                            if (move_result == Navigator.MoveResult.SUCCESS) {
                                return true;
                            }
                            return false;
                        }
                    }
                }

            }

            // Heuristics

            for (MapLocation location : navigator.adjacentLocationWithCenter(my_location)) {

                if (location.equals(my_location) || controller.canMove(my_location.directionTo(location))) {

                    double score = calculateScore(location);

                    if (score > best_score && score > -50) {
                        best_score = score;
                        best_location = location;
                    }
                }

            }

            if (best_location != null) {
                Navigator.MoveResult move_result = ((IntegratedNavigator)navigator).move(best_location,true);
                //controller.setIndicatorString(best_location.toString());
                if (move_result == Navigator.MoveResult.SUCCESS || move_result == Navigator.MoveResult.REACHED) {
                    return true;
                }
            }
            return false;
        }

    }

    class RetreatMoveStrategy implements MoveStrategy
    {

        int HP_THRESHOLD = 23;
        @Override
        public boolean move() throws GameActionException
        {

            CommandCommunicator.updateArchonLocations();
            RobotController controller = getRobotController();
            // Greedy move away
            if (Cache.can_see_archon)
            {
                for (RobotInfo robot : Cache.friendly_buildings)
                {
                    if (robot.getType() == RobotType.ARCHON && robot.getLocation().isWithinDistanceSquared(controller.getLocation(), 8))
                    {
                        return false;
                    }
                }
            }

            Navigator.MoveResult move_result = ((IntegratedNavigator)navigator).move(Cache.MY_SPAWN_LOCATION,true);
            switch (move_result) {
                case SUCCESS:
                    return true;
                case FAIL:
                    return false;
                case REACHED:
                case IMPOSSIBLE:
                    controller.setIndicatorDot(getRobotController().getLocation(),0,0,0);
                    MapLocation close = null;
                    for (MapLocation archon : CommandCommunicator.getSpawnLocations()) {
                        if (archon != null ) {
                            if (close == null || close.distanceSquaredTo(getRobotController().getLocation()) > archon.distanceSquaredTo(getRobotController().getLocation())) {
                                close = archon;
                            }
                        }
                    }
                    if (close != null) {
                        Cache.MY_SPAWN_LOCATION = close;
                    }
                    return false;
            }
            return false;
        }

        public boolean shouldRun () throws GameActionException
        {

            int health = getRobotController().getHealth();

            if (getRobotController().isActionReady() && Cache.opponent_soldiers.length > 0) {
                return false;
            }

            if (health >= 91) {
                return false;
            }
            if (!Cache.can_see_archon) {
                RobotController controller = getRobotController();
                //if (controller.getHealth() < HP_THRESHOLD || (controller.getHealth() <= Cache.lowest_health_soldier && Cache.can_see_archon)) {
                if (health < HP_THRESHOLD) {
                    return true;
                }
            }
            else {
                if (!getRobotController().isActionReady()) {
                    return true;
                }
                if (Cache.opponent_soldiers.length > 0 && (getRobotController().getHealth() > HP_THRESHOLD)) {
                    return false;
                }
                if (health > 20 && Cache.injured > 5) {
                    return false;
                }
                return true;
            }
            return false;
        }

    }


    class DefaultAttackStrategy implements AttackStrategy {


        int WAIT_SCORE_THRESHOLD = 1;
        @Override
        public boolean attack() throws GameActionException {
            // TODO: make sure we are in a suitable position before envisioning
            RobotController controller = getRobotController();
            MapLocation my_location = controller.getLocation();
            RobotInfo[] robots = controller.senseNearbyRobots(RobotType.SAGE.actionRadiusSquared,Cache.OPPONENT_TEAM);

            int droids = 0;

            // Save attack for better spot?
            if (!has_moved && controller.getHealth() > 50 && controller.getActionCooldownTurns() / 10 < 2) {
                double current_score = fight_move_strategy.calculateScore(my_location);
                for (MapLocation potential : navigator.adjacentLocationWithCenter(my_location)) {
                    if (fight_move_strategy.calculateScore(potential) > current_score + WAIT_SCORE_THRESHOLD) {
                        return false;
                    }
                }
            }

            int area_bonus = 0;

            RobotInfo best = null;
            MapLocation best_attack = null;
            int best_score = -9999;
            for (RobotInfo robot : robots) {
                int score = 0;
                switch (robot.getType()) {
                    case SOLDIER:
                    case SAGE:
                        droids += 2;
                        if (robot.getHealth() % RobotType.SAGE.damage < robot.getType().getMaxHealth(robot.getLevel()) * 0.22) {
                            area_bonus += 1;
                        }
                    case WATCHTOWER:
                        score += 2;
                        if (robot.getLocation().isWithinDistanceSquared(my_location, robot.getType().visionRadiusSquared)) {
                            score += 5;
                        }
                        if (robot.getLocation().isWithinDistanceSquared(my_location, robot.getType().actionRadiusSquared)) {
                            score += 5;
                        }
                        if (robot.getHealth() <= 45) {
                            score += 4; // Kill bonus
                            if (robot.getType() == RobotType.SAGE) {
                                score += 4;
                                score -= 0.1 * (45 - robot.getHealth());
                            }
                        }
                        break;
                    case ARCHON:
                    case LABORATORY:
                        score += 13;
                        break;
                    case BUILDER:
                        droids += 1;
                        score += 5;
                        break;
                    case MINER:
                        droids += 1;
                        score += 13;
                        break;
                }

                if (score > best_score) {
                    best_score = score;
                    best_attack = robot.getLocation();
                    best = robot;
                }
            }


            if (droids >= 10 || area_bonus > 1) {
                controller.envision(AnomalyType.CHARGE);
                return true;
            }

            if (best_attack != null && controller.canAttack(best_attack)) {
                if (!best.getType().isBuilding() && best.getHealth() < best.getType().getMaxHealth(best.getLevel()) * 0.22) {
                    controller.envision(AnomalyType.CHARGE);
                }
                else if (best.getType() == RobotType.ARCHON && !Cache.can_see_archon) {
                    controller.envision(AnomalyType.FURY);
                }
                else {
                    controller.attack(best_attack);
                }
            }
            return false;
        }
    }

    @Override
    public void turn() throws GameActionException {

        Cache.update();

        getRobotController().writeSharedArray(CommandCommunicator.TOTAL_SAGE_INDEX,
                getRobotController().readSharedArray(CommandCommunicator.TOTAL_SAGE_INDEX)+1);

        current_attacking_strategy = default_attack_strategy;
        current_moving_strategy = search_move_strategy;
        has_moved = false;

        if (retreat_move_strategy.shouldRun()) {
            current_moving_strategy = retreat_move_strategy;
            getRobotController().setIndicatorString("Run");
        }
        else if (Cache.opponent_soldiers.length + Cache.opponent_villagers.length + Cache.opponent_buildings.length > 0) {
            current_moving_strategy = fight_move_strategy;
            getRobotController().setIndicatorString("Fight");
        }
        else {
            if ((Cache.age & 1) == 1) {
                MatrixCommunicator.read(Communicator.Event.ARCHON);
            }
            else {
                MatrixCommunicator.read(Communicator.Event.SOLDIER);
            }
            getRobotController().setIndicatorString("Search");
            current_moving_strategy = search_move_strategy;
        }

        if (getRobotController().isMovementReady()) {
            if (current_moving_strategy.move()) {
                has_moved = true;
            }
        }

        if (getRobotController().isActionReady()) {
            getRobotController().setIndicatorDot(getRobotController().getLocation(),255,255,255);
            current_attacking_strategy.attack();
        }

        //getRobotController().setIndicatorString(String.valueOf(getRobotController().isActionReady()));
    }
}