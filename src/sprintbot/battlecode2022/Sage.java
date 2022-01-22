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
    private final DefaultMoveStrategy default_move_strategy = new DefaultMoveStrategy();
    private final SearchMoveStrategy search_move_strategy = new SearchMoveStrategy();
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
                    move_target = null;
                    return false;
                case IMPOSSIBLE:
                    //getRobotController().setIndicatorString("IMPOSSIBLE");
                    if (Navigator.travelDistance(my_location,move_target) <= 4) {
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

    class DefaultMoveStrategy implements MoveStrategy {
        private MapLocation move_target = null;

        @Override
        public boolean move() throws GameActionException {
            RobotController controller = getRobotController();
            MapLocation my_location = controller.getLocation();

            // find the closest opponent in vision
            int min_dist = 9999; // squared
            MapLocation closest_opponent_soldier = null;
            for (int i = 0; i < Cache.opponent_soldiers.length; i++) {
                int dist = my_location.distanceSquaredTo(Cache.opponent_soldiers[i].location);
                if (dist < min_dist) {
                    min_dist = dist;
                    closest_opponent_soldier = Cache.opponent_soldiers[i].location;
                }
            }

            // retreat when about to enter opponent soldier's vision range
            // Or action not ready
            final int buffer = 5;
            if (min_dist < RobotType.SOLDIER.visionRadiusSquared + buffer) {
                current_moving_strategy = retreat_move_strategy;
                current_moving_strategy.move();
            }

            // move towards the closest opponent soldier in vision range
            else if (controller.isActionReady() && closest_opponent_soldier != null && !closest_opponent_soldier.isWithinDistanceSquared(my_location,RobotType.SAGE.actionRadiusSquared)){
                move_target = closest_opponent_soldier;
            }

            if (move_target != null) {
                Navigator.MoveResult move_result = navigator.move(move_target);

                if (move_result == Navigator.MoveResult.SUCCESS) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    class RetreatMoveStrategy implements MoveStrategy
    {
        int HP_THRESHOLD = 13;
        @Override
        public boolean move() throws GameActionException
        {
            RobotController controller = getRobotController();
            Direction direction = null;
            Integer closest = Integer.MAX_VALUE;

            for (RobotInfo robot : Cache.opponent_soldiers) {
                int attack_radius = robot.getType().actionRadiusSquared;
                int distance = attack_radius-controller.getLocation().distanceSquaredTo(robot.getLocation());
                if (closest > distance) {
                    closest = distance;
                    direction = controller.getLocation().directionTo(robot.getLocation()).opposite();
                }
            }
            // Greedy move away
            if (direction != null && ((IntegratedNavigator)navigator).move(controller.getLocation().add(direction).add(direction),true) == Navigator.MoveResult.SUCCESS) {
                return true;
            }
            else {
                if (Cache.can_see_archon) {
                    for (RobotInfo robot : Cache.friendly_buildings) {
                        if (robot.getType() == RobotType.ARCHON && robot.getLocation().isWithinDistanceSquared(controller.getLocation(),8)) {
                            return false;
                        }
                    }
                }
                if (((IntegratedNavigator)navigator).move(Cache.MY_SPAWN_LOCATION,true) == Navigator.MoveResult.SUCCESS) return true;
            }
            return false;
        }

    }


    class DefaultAttackStrategy implements AttackStrategy {

        @Override
        public boolean attack() throws GameActionException {
            // TODO: make sure we are in a suitable position before envisioning
            RobotController controller = getRobotController();
            MapLocation my_location = controller.getLocation();
            RobotInfo[] robots = controller.senseNearbyRobots();

            MapLocation best_attack = null;
            int best_score = -9999;

            for (RobotInfo robot : robots) {

                if (robot.getTeam() == Cache.OUR_TEAM) {
                    continue;
                }

                if (!controller.canAttack(robot.getLocation())) {
                    continue;
                }

                int score = 0;
                switch (robot.getType()) {
                    case SOLDIER:
                    case SAGE:
                    case WATCHTOWER:
                        score += 5;
                        if (robot.getLocation().isWithinDistanceSquared(my_location, robot.getType().visionRadiusSquared)) {
                            score += 5;
                        }
                        if (robot.getLocation().isWithinDistanceSquared(my_location, robot.getType().actionRadiusSquared)) {
                            score += 5;
                        }
                        break;
                    case ARCHON:
                    case LABORATORY:
                        score += 4;
                        break;
                    case BUILDER:
                        score += 3;
                        break;
                    case MINER:
                        score += 2;
                        break;
                }
                if (robot.getHealth() <= 45) {
                    score += 3; // Kill bonus
                    score -= 0.3 * (45 - robot.getHealth()); // Overkill penalty
                }

                if (score > best_score) {
                    best_score = score;
                    best_attack = robot.getLocation();
                }
            }

            if (best_attack != null && controller.canAttack(best_attack)) {
                controller.attack(best_attack);
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
        current_moving_strategy = default_move_strategy;

        if (Cache.opponent_soldiers.length + Cache.opponent_villagers.length + Cache.opponent_buildings.length > 0) {
            current_moving_strategy = default_move_strategy;
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

        if (getRobotController().isActionReady()) {
            current_attacking_strategy.attack();
        }

        if (getRobotController().isMovementReady()) {
            current_moving_strategy.move();
        }

    }
}