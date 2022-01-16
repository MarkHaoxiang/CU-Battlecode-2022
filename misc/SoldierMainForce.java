package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.util.Arrays;
import java.util.Comparator;

public class Soldier extends RunnableBot
{
    // Move towards metal strategy (or it could be scouting strategy, or etc., but basically it's a strategy to move)
    private MoveStrategy current_moving_strategy;
    private final DefaultMoveStrategy default_moving_strategy = new DefaultMoveStrategy();
    private final MainForceMoveStrategy main_force_moving_strategy = new MainForceMoveStrategy();
    // Same here
    private AttackStrategy current_attacking_strategy;
    private final DefaultAttackStrategy default_attacking_strategy = new DefaultAttackStrategy();
    private final MainForceAttackStrategy main_force_attacking_strategy = new MainForceAttackStrategy();
    private RobotInfo[] enemies;
    private int opponent_soldier_cnt;
    private int our_soldier_cnt;
    private static final int start_main_force = 100; // the round when we switch to main force strategies
    Direction main_force_direction = null; // invasion direction, not moving direction (doesn't record if retreating)
    private final boolean is_main_force = Math.random() < 0.7; // 70% soldiers are main force members
    // TODO: limit max number of soldiers in one gathering? more than one gathering? or just one gathering but line up as a circle?

    public Soldier(RobotController rc) throws GameActionException
    {
        super(rc);
    }

    @Override
    public void init() throws GameActionException
    {
        super.init();
        Communicator.updateMainForceTarget(navigator.randomLocation());
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

    class DefaultMoveStrategy implements MoveStrategy
    {
        private MapLocation move_target = null;
        private final int GIVE_UP_THRESHOLD_TURN = 1;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */

        @Override
        public boolean move() throws GameActionException
        {
            // Move towards the closest enemy if found
            // Reuse the enemies[] info fetched in observeNearbyEnemies()

            if (enemies.length > 0)
            {
                Arrays.sort(enemies, new Comparator<RobotInfo>()
                {
                    @Override
                    public int compare(RobotInfo a, RobotInfo b)
                    {
                        return Navigator.travelDistance(Cache.controller.getLocation(), a.getLocation()).
                                compareTo(Navigator.travelDistance(Cache.controller.getLocation(), b.getLocation()));
                    }
                });
                move_target = enemies[0].getLocation();
                Navigator.MoveResult move_result = navigator.move(move_target);
                if (move_result == Navigator.MoveResult.IMPOSSIBLE ||
                        move_result == Navigator.MoveResult.REACHED)
                    return false;
                // TODO: Fix bug here where location might not be uploaded
                // Report enemy location [COMMUNICATE]
                MatrixCommunicator.update(Communicator.Event.SOLDIER, move_target);
            }
            else // If no enemies nearby, either moves towards the closest reported location or chooses a random location
            {
                Navigator.MoveResult move_result = navigator.move(move_target);
                // Only changes the target if moving fails
                int tot_attempts = 0;
                while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
                        move_result == Navigator.MoveResult.REACHED)
                {
                    // TODO: Fix bug here where location might not be fetched
                    // Update reported location to get the desired target [COMMUNICATE]
                    MatrixCommunicator.read(Communicator.Event.SOLDIER);
                    move_target = Communicator.getClosestFromCompressedLocationArray(Cache.metal_compressed_locations,
                            Cache.controller.getLocation());
                    // If nothing is available then choose a random location
                    if (move_target == null)
                        move_target = navigator.randomLocation();
                    move_result = navigator.move(move_target);
                    tot_attempts++;
                    if (tot_attempts >= GIVE_UP_THRESHOLD_TURN) // Give up on further attempts
                        return false;
                }
            }
            return true;
        }
    }

    class MainForceMoveStrategy implements MoveStrategy
    {
        private MapLocation move_target = null;
        private final int GIVE_UP_THRESHOLD_TURN = 1;
		/* Number of turns to give up moving if repeatedly stuck
		   Note that it is necessary because it might get surrounded by robots
		   It is also probably related to bytecode limit, but we don't account for that now */


        @Override
        public boolean move() throws GameActionException
        {
            move_target = Cache.main_force_target;

            Navigator.MoveResult move_result = navigator.move(move_target);

            // can easily get stuck in troop
            int tot_attempts = 0;
            while (move_result == Navigator.MoveResult.IMPOSSIBLE ||
                    move_result == Navigator.MoveResult.REACHED)
            {
                tot_attempts++;
                if (tot_attempts > GIVE_UP_THRESHOLD_TURN) // Give up on further attempts
                    return false;

                // choose a random location
                move_target = navigator.randomLocation();
                move_result = navigator.move(move_target);
            }

            return true;
        }
    }

    class DefaultAttackStrategy implements AttackStrategy
    {
        @Override
        public boolean attack() throws GameActionException
        {
            // TODO: Better implementation like prioritize
            // Reuse the enemies[] info fetched in observeNearbyEnemies()
            if (enemies.length > 0)
            {
                MapLocation toAttack = enemies[0].location;
                if (Cache.controller.canAttack(toAttack))
                    Cache.controller.attack(toAttack);
                // TODO: Upload all enemy locations here if bytecodes permit
                MatrixCommunicator.update(Communicator.Event.SOLDIER, enemies[0].location);
            }
            return true;
        }
    }

    class MainForceAttackStrategy implements AttackStrategy
    {
        private MapLocation getNearest(int[] compressed_locations) {
            int min_dist = Integer.MAX_VALUE;
            MapLocation nearest_location = null;
            MapLocation my_location = Cache.controller.getLocation();
            for (int i = 0; i < compressed_locations.length; i++) {
                if (compressed_locations[i] == -1) {
                    break;
                }
                MapLocation location = Communicator.unzipCompressedLocation(compressed_locations[i]);
                int dist = Navigator.travelDistance(my_location, location);
                if (dist < min_dist) {
                    min_dist = dist;
                    nearest_location = location;
                }
            }
            return nearest_location;
        }

        private MapLocation getNearest(RobotInfo[] robots) {
            int min_dist = Integer.MAX_VALUE;
            MapLocation nearest_location = null;
            MapLocation my_location = Cache.controller.getLocation();
            for (int i = 0; i < robots.length; i++) {
                MapLocation location = robots[i].location;
                int dist = Navigator.travelDistance(my_location, location);
                if (dist < min_dist) {
                    min_dist = dist;
                    nearest_location = location;
                }
            }
            return nearest_location;
        }

        public MapLocation pickNewMainForceTarget() throws GameActionException {
            // currently, opponent soldier is prioritised over opponent archon. can change the order

            // choose a new target from matrix instead of from nearby enemies
            MatrixCommunicator.read(Communicator.Event.SOLDIER);

            MapLocation new_target = getNearest(Cache.opponent_soldier_compressed_locations);
            if (new_target != null) {
                Communicator.updateMainForceTarget(new_target);
                return new_target;
            }
            else { // no enemies on the map
                MapLocation nearest_opponent_archon = getNearest(Cache.opponent_archon_compressed_locations);
                if (nearest_opponent_archon != null) { // nearest opponent archon
                    Communicator.updateMainForceTarget(nearest_opponent_archon);
                    return nearest_opponent_archon;
                }
                else { // opponent archon locations unknown -> random location
                    // TODO: can guess opponent archon location?
                    MapLocation random_location = navigator.randomLocation();
                    Communicator.updateMainForceTarget(random_location);
                    return random_location;
                }
            }
        }


        @Override
        public boolean attack() throws GameActionException
        {
            if (enemies.length > 0)
            {
                // attack no matter how strong/weak we are
                MapLocation toAttack = getNearest(enemies);
                if (Cache.controller.canAttack(toAttack))
                    Cache.controller.attack(toAttack);

                // advantage -> move forward
                if (our_soldier_cnt > opponent_soldier_cnt) {
                    // assume we win, so remove soldier message and add metal message to matrix
                    MatrixCommunicator.update(Communicator.Event.SOLDIER, toAttack, false);
                    MatrixCommunicator.update(Communicator.Event.METAL, toAttack);
                    // move forward
                    Communicator.updateMainForceTarget(toAttack);
                    main_force_direction = Cache.main_force_target.directionTo(toAttack); // not read yet, so the value in Cache is still the original target
                }

                // disadvantage -> retreat
                else {
                    MapLocation new_target = Cache.main_force_target.subtract(Cache.main_force_target.directionTo(enemies[0].location));
                    if (new_target.x < 0 || new_target.x >= Cache.MAP_WIDTH || new_target.y < 0 || new_target.y >= Cache.MAP_HEIGHT) {
                        new_target = navigator.randomLocation();
                    }
                    Communicator.updateMainForceTarget(new_target);
                }
            }


            // no enemies -> pick a new target
            else {
                // might be on the way to enemies
                if (main_force_direction != null) {
                    MapLocation new_target = Cache.main_force_target.add(main_force_direction);
                    if (new_target.x < 0 || new_target.x >= Cache.MAP_WIDTH || new_target.y < 0 || new_target.y >= Cache.MAP_HEIGHT) {
                        pickNewMainForceTarget();
                    }
                    else {
                        Cache.main_force_target = new_target;
                    }
                }
                // or just choose a random location
                else {
                    MapLocation new_target = pickNewMainForceTarget();
                    main_force_direction = Cache.main_force_target.directionTo(new_target);
                }

            }
            return true;
        }
    }

    private int countSoldiers(RobotInfo[] robots) {
        int cnt = 0;
        for (int i = 0; i < robots.length; i++) {
            RobotType type = robots[i].getType();
            if (type == RobotType.SOLDIER || type == RobotType.SAGE) {
                cnt++;
            }
        }
        return cnt;
    }

    private void observeNearbyEnemies()
    {
        // Fetch enemies[] info. Also used later
        int radius = Cache.controller.getType().actionRadiusSquared;
        Team opponent = Cache.controller.getTeam().opponent();
        enemies = Cache.controller.senseNearbyRobots(radius, opponent);
        opponent_soldier_cnt = countSoldiers(enemies);
    }

    // TODO: differentiate vision range and attack range?
    private void observeNearbyAllies() {
        int radius = Cache.controller.getType().actionRadiusSquared;
        our_soldier_cnt = countSoldiers(Cache.controller.senseNearbyRobots(radius, Cache.OUR_TEAM));
    }


    @Override
    public void turn() throws GameActionException
    {
        Cache.update();

        observeNearbyEnemies();

        if (is_main_force && Cache.age > start_main_force) {
            current_attacking_strategy = main_force_attacking_strategy;
            current_moving_strategy = main_force_moving_strategy;
            observeNearbyAllies();
        }
        else {
            current_attacking_strategy = default_attacking_strategy;
            current_moving_strategy = default_moving_strategy;
        }

        Communicator.readMainForceTarget();
        current_attacking_strategy.attack();
        current_moving_strategy.move();
    }
}