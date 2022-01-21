package sprintbot.battlecode2022.util.navigation;

import sprintbot.battlecode2022.util.Cache;
import sprintbot.battlecode2022.util.Constants;
import sprintbot.battlecode2022.util.Navigator;
import battlecode.common.*;

public class BugNavigatorLimit extends Navigator {


    private boolean is_bugging = false;
    private boolean start_clockwise;
    private boolean clockwise;
    private int count = 0;
    private MapLocation prev_target;
    private MapLocation start_bug_location;
    private MapLocation obstacle;
    private double gradient;
    private int stuck_turns;

    private final int rubble_limit;

    private final GreedyNavigatorLimit greedy;
    private final NaiveLimit naive;

    public BugNavigatorLimit(RobotController controller, int rubble) {
        super(controller);
        if (controller.getRoundNum() % 2 == 0) {
            clockwise = true;
            start_clockwise = true;
        } else {
            clockwise = false;
            start_clockwise = false;
        }
        rubble_limit = rubble;
        greedy = new GreedyNavigatorLimit(controller,rubble);
        naive = new NaiveLimit(controller,rubble);
    }

    public int getStuckTurns() {
        return stuck_turns;
    }

    // Bug Pathing
    public MoveResult move(MapLocation target_location)
            throws GameActionException {


        MapLocation current_location = controller.getLocation();
        //controller.setIndicatorLine(current_location,target_location,0,0,0);

        /*
        // Is target out of the map
        if (target_location == null ||
                !controller.onTheMap(current_location.add(
                        current_location.directionTo(
                                target_location)))) return MoveResult.IMPOSSIBLE;

        // At destination
        if (target_location.equals(current_location)) {
            return MoveResult.REACHED;
        }

        // Our miners have priority, don't disturb them
        // Buildings are walls
        if (current_location.distanceSquaredTo(target_location) <= 2
                && controller.canSenseLocation(target_location)) {
            RobotInfo robot = controller.senseRobotAtLocation(target_location);
            if (robot != null
                    && robot.getTeam() == Cache.OUR_TEAM
                    && robot.getType() == RobotType.MINER) {
                return MoveResult.IMPOSSIBLE;
            }
            if (robot != null
                    && robot.getType().isBuilding()
                    && robot.getMode().canMove == false) {
                return MoveResult.IMPOSSIBLE;
            }
        }

         */

        //controller.setIndicatorString(Boolean.toString(clockwise));

        if (controller.canSenseLocation(target_location) && controller.senseRubble(target_location) >= rubble_limit) {
            return MoveResult.IMPOSSIBLE;
        }

        // Target change, stop bugging
        if (!target_location.equals(prev_target)) {
            controller.setIndicatorString("A");
            is_bugging = false;
        }
        prev_target = target_location;

        if (!is_bugging) {
            MoveResult res = greedy.move(target_location);
            // Can move
            if (res == MoveResult.SUCCESS || res == MoveResult.IMPOSSIBLE ) {
                return res;
            }
            // Stuck, start bugging
            is_bugging = true;
            //clockwise = start_clockwise;
            count = 0;
            start_bug_location = current_location;
            gradient =
                    calculateGradient(current_location,
                            target_location);
            obstacle =
                    controller.adjacentLocation(
                            current_location.directionTo(
                                    target_location));
            stuck_turns = 0;
            return move(target_location);
        } else {
            //controller.setIndicatorString("Bugging");
            // Robot trapped

            if (start_bug_location.equals(current_location)) {
                count += 1;
                if (count >= 3) {
                    return MoveResult.FAIL;
                }
            }

            Direction
                    obstacleDirection =
                    current_location.directionTo(
                            obstacle);
            Direction target_direction = obstacleDirection;
            // Edge Case: Obstacle gone
            if (naive.move(obstacleDirection) == MoveResult.SUCCESS) {
                controller.setIndicatorString("B");
                is_bugging = false;
                clockwise = start_clockwise;
                return MoveResult.SUCCESS;
            }
            if (clockwise) {
                target_direction =
                        target_direction.rotateRight();
            } else {
                target_direction =
                        target_direction.rotateLeft();
            }
            while (naive.move(target_direction) != MoveResult.SUCCESS) {
                if (clockwise) {
                    target_direction =
                            target_direction.rotateRight();
                } else {
                    target_direction =
                            target_direction.rotateLeft();
                }
                //If on the edge of the map, switch bug directions
                //Or, there is no way past
                if (!controller.onTheMap(controller.adjacentLocation(
                        target_direction))) {
                    if (clockwise == start_clockwise) {
                        controller.setIndicatorString("RESET?");
                        clockwise = !clockwise;
                        return move(target_location);
                    } else {
                        stuck_turns += 1;
                        return MoveResult.FAIL;
                    }
                }
                if (target_direction == obstacleDirection) {
                    stuck_turns += 1;
                    return MoveResult.FAIL;
                }
            }
            if (clockwise) {
                obstacle =
                        controller.adjacentLocation(
                                target_direction.rotateLeft());
            } else {
                obstacle =
                        controller.adjacentLocation(
                                target_direction.rotateRight());
            }
            MapLocation
                    moveLoc =
                    controller.adjacentLocation(
                            target_direction);
            //Check if it's passing the original line closer to the target
            if (current_location.distanceSquaredTo(
                    target_location) <
                    start_bug_location.distanceSquaredTo(
                            target_location)) {
                if (calculateGradient(current_location,
                        target_location) > gradient &&
                        calculateGradient(moveLoc,
                                target_location) <= gradient) {
                    controller.setIndicatorString("C");
                    is_bugging = false;
                    clockwise = start_clockwise;
                } else if (calculateGradient(moveLoc,
                        target_location) >= gradient) {
                    controller.setIndicatorString("D");
                    is_bugging = false;
                    clockwise = start_clockwise;
                }
            }

            return naive.move(target_direction);
        }
    }

    // Util

    private static double calculateGradient(
            MapLocation start, MapLocation end) {
        if (start == null || end == null) return -2;
        if (end.x - start.x == 0) {
            return -1;
        }
        //Rise over run
        return 1.0 * (end.y - start.y) / (end.x - start.x);
    }

}
