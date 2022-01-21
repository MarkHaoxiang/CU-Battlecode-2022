package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.util.*;


public class Builder extends RunnableBot
{
    // Archon assigned role
    CommandCommunicator.RobotRole assigned_role;
    MapLocation assigned_location;

    private FarmMoveStrategy farm_move_strategy = new FarmMoveStrategy();
    private FightMoveStrategy fight_move_strategy = new FightMoveStrategy();
    private RepairStrategy repair_strategy = new DefaultRepairStrategy();

    enum BuilderState {
        FIGHTING,
        FARMING,
        BUILDING
    };

    BuilderState state;

    public Builder(RobotController rc) throws GameActionException
    {
        super(rc);
    }

    @Override
    public void init() throws GameActionException
    {
        super.init();
        CommandCommunicator.SpawnOrder order = CommandCommunicator.getSpawnRole();
        assigned_role = order.role;
        assigned_location = order.loc;

        if (assigned_location == null) {
            assigned_role = CommandCommunicator.RobotRole.FIGHT_BUILDER;
        }

        Cache.MY_SPAWN_LOCATION = getRobotController().getLocation();
        switch (assigned_role) {
            case FARM_BUILDER:
                state = BuilderState.FARMING;
                break;
            case LAB_BUILDER:
                state = BuilderState.BUILDING;
                break;
            default:
                state = BuilderState.FIGHTING;
                break;
        }
    }

    @Override
    public void turn() throws GameActionException
    {
        Cache.update();

        MoveStrategy move_strategy;

        if (state == BuilderState.FARMING) {
            move_strategy = farm_move_strategy;
        }
        else {
            move_strategy = fight_move_strategy;
        }

        if (getRobotController().isMovementReady()) {
            move_strategy.move();
        }
        if (getRobotController().isActionReady()) {
            repair_strategy.repair();
        }

    }

    // Strategy

    interface MoveStrategy
    {
        boolean move() throws GameActionException;
    }

    interface RepairStrategy
    {
        boolean repair() throws GameActionException;
    }

    class LabMoveStrategy implements MoveStrategy {

        RobotController controller = getRobotController();
        private boolean has_reached_assigned = false;
        private MapLocation lowest_rubble = null;

        public LabMoveStrategy() {

            if (assigned_location == null) {
                // Nearest edge you go
                MapLocation my_location = controller.getLocation();
                MapLocation closest = null;
                for (MapLocation loc : new MapLocation[] {
                        new MapLocation(0, my_location.y),
                        new MapLocation(Cache.MAP_WIDTH-1, my_location.y),
                        new MapLocation(my_location.x, Cache.MAP_WIDTH-1),
                        new MapLocation(my_location.x, 0),
                }) {
                    if (closest == null || closest.distanceSquaredTo(my_location) > loc.distanceSquaredTo(my_location)) {
                        closest = loc;
                    }
                }
                assigned_location = closest;
            }

        }

        @Override
        public boolean move() throws GameActionException {


            if (controller.getLocation().distanceSquaredTo(assigned_location) <= 4) {
                has_reached_assigned = true;
            }

            if (!has_reached_assigned) {
                if (navigator.move(assigned_location) == Navigator.MoveResult.SUCCESS) {
                    return true;
                }
                return false;
            }

            int lowest_rubble = 9999;
            for (MapLocation location : controller.getAllLocationsWithinRadiusSquared(controller.getLocation(),8)) {
                if (controller.onTheMap(location) && controller.senseRubble(location) < lowest_rubble) {
                    lowest_rubble = controller.senseRubble(location);
                }
            }
            return false;

        }
    }

    class FightMoveStrategy implements MoveStrategy {

        MapLocation move_target = navigator.randomLocation();
        RobotController controller = getRobotController();

        @Override
        public boolean move() throws GameActionException {


            // Add watchtower support
            MapLocation my_location = controller.getLocation();
            for (RobotInfo robot : Cache.friendly_buildings) {
                if (robot.getHealth() < robot.getType().health && my_location.distanceSquaredTo(robot.getLocation()) > 5) {
                    move_target = robot.getLocation();
                    break;
                }
                else if (robot.getHealth() < robot.getType().health && my_location.distanceSquaredTo(robot.getLocation()) <= 2) {
                    Direction dir = robot.getLocation().directionTo(my_location);
                    move_target = (robot.getLocation().add(dir).add(dir));
                    break;
                }
                else if (robot.getHealth() < robot.getType().health) {
                    move_target = my_location;
                    break;
                }
            }

            Navigator.MoveResult move_result =  navigator.move(move_target);

            switch (move_result) {
                case IMPOSSIBLE:
                case REACHED:
                    move_target = navigator.randomLocation();
                    return false;
                case SUCCESS:
                    return true;
                case FAIL:
                default:
                    return false;
            }
        }
    }

    class FarmMoveStrategy implements MoveStrategy {

        RobotController controller = getRobotController();

        @Override
        public boolean move() throws GameActionException {
            MapLocation[] potential_spots = controller.senseNearbyLocationsWithLead(assigned_location,RobotType.BUILDER.visionRadiusSquared,-1);
            MapLocation best_location = null;
            int best_score = -9999;
            for (int i = potential_spots.length; --i>=0;) {
                MapLocation location = potential_spots[i];
                if (controller.senseLead(location) == 0) {
                    boolean is_valid = true;
                    for (RobotInfo robot : Cache.friendly_buildings) {
                        if (robot.getType() == RobotType.ARCHON && robot.getLocation().distanceSquaredTo(location) <= 4) {
                            is_valid = false;
                            break;
                        }
                    }
                    if (is_valid) {
                        int score = - controller.senseRubble(location) / 2 - controller.getLocation().distanceSquaredTo(location);
                        if (MatrixCommunicator.read(Communicator.Event.FRIENDLY_MINER,location)) {
                            score += 5;
                        }
                        if (score > best_score) {
                            best_score = score;
                            best_location = location;
                        }
                    }
                }
            }

            if (best_location != null) {
                Navigator.MoveResult move_result = navigator.move(best_location);
                switch (move_result) {
                    case SUCCESS:
                        return true;
                    case REACHED:
                        controller.disintegrate();
                        return true;
                    default:
                        return false;
                }
            }
            else {
                if (controller.getLocation().distanceSquaredTo(assigned_location) <= 1) {
                    System.out.println("Transition");
                    state = BuilderState.FIGHTING;
                    return false;
                }
                Navigator.MoveResult move_result = navigator.move(assigned_location);
                switch (move_result) {
                    case SUCCESS:
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    class DefaultRepairStrategy implements RepairStrategy {
        RobotController controller = getRobotController();

        @Override
        public boolean repair() throws GameActionException {

            for (RobotInfo robot : Cache.friendly_buildings) {
                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation())) {
                    controller.setIndicatorLine(controller.getLocation(),robot.getLocation(),0,255,0);
                    controller.repair(robot.getLocation());
                    return true;
                }
            }
            return false;
        }
    }



}
