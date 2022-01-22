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
    private LabMoveStrategy lab_move_strategy = new LabMoveStrategy();
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
                lab_move_strategy = new LabMoveStrategy();
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
            getRobotController().setIndicatorString("Farm");
        }
        else if (state == BuilderState.BUILDING) {
            getRobotController().setIndicatorString("Lab");
            move_strategy = lab_move_strategy;
        }
        else {
            move_strategy = fight_move_strategy;
            getRobotController().setIndicatorString("Fight");
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
        private MapLocation best_location = null;
        private boolean has_built = false;

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

            //System.out.println(assigned_location);

            int lowest_rubble = 9999;

            if (best_location == null) {
                for (MapLocation location : controller.getAllLocationsWithinRadiusSquared(controller.getLocation(),8)) {
                    int penalty = controller.senseRubble(location)
                            + (controller.isLocationOccupied(location) ? 1 : 0) * 10 + Math.max(controller.getLocation().x,controller.getLocation().y)
                            - (Cache.MAP_WIDTH+Cache.MAP_HEIGHT)/20;
                    if (controller.onTheMap(location) && penalty < lowest_rubble) {
                        lowest_rubble = penalty;
                        best_location = location;
                    }
                }
            }

            if (controller.getLocation().distanceSquaredTo(best_location) > 2) {
                if (navigator.move(best_location) == Navigator.MoveResult.SUCCESS) {
                    return true;
                }
                return false;
            }

            controller.setIndicatorString(String.valueOf(has_built));

            if (controller.getLocation().equals(best_location)) {
                for (Direction dir : Constants.DIRECTIONS) {
                    if (navigator.move(getRobotController().getLocation().add(dir)) == Navigator.MoveResult.SUCCESS) {
                        return true;
                    }
                }
                return false;
            }

            if (!has_built && controller.canSenseLocation(best_location) && controller.isLocationOccupied(best_location)) {
                lab_move_strategy = new LabMoveStrategy();
            }

            if (!has_built) {
                if (controller.canBuildRobot(RobotType.LABORATORY,controller.getLocation().directionTo(best_location))) {
                    controller.buildRobot(RobotType.LABORATORY,controller.getLocation().directionTo(best_location));
                    int v = controller.readSharedArray(CommandCommunicator.BANK_INDEX);
                    controller.writeSharedArray(CommandCommunicator.BANK_INDEX,v-180);
                    has_built = true;
                }
            }
            else {
                for (RobotInfo robot : Cache.friendly_buildings) {
                    if (robot.getType() == RobotType.LABORATORY
                            && robot.getLocation().isWithinDistanceSquared(getRobotController().getLocation(),2)
                            && robot.getHealth() < RobotType.LABORATORY.health) {
                        return false;
                    }
                }
                state = BuilderState.FIGHTING;
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

            // Mutate priority

            for (RobotInfo robot : Cache.friendly_buildings) {

                if (robot.getType() == RobotType.ARCHON) {
                    if (Cache.injured >= 4 && controller.canMutate(robot.getLocation())) {
                        controller.mutate(robot.getLocation());
                    }
                }

            }

            // Then repair

            for (RobotInfo robot : Cache.friendly_buildings) {

                if (robot.getHealth() < robot.getType().health && controller.canRepair(robot.getLocation())) {
                    //controller.setIndicatorLine(controller.getLocation(),robot.getLocation(),0,255,0);
                    controller.repair(robot.getLocation());
                    return true;
                }

            }

            return false;
        }
    }



}
