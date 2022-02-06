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
    private double angle;
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

    public void bugReset() {
        is_bugging = false;
    }

    // Bug Pathing
    public MoveResult move(MapLocation target_location)
            throws GameActionException {

        MapLocation current_location = controller.getLocation();

        if (controller.canSenseLocation(target_location) && controller.senseRubble(target_location) >= rubble_limit && controller.getLocation().distanceSquaredTo(target_location) <= 13) {
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
            clockwise = start_clockwise;
            count = 0;
            start_bug_location = current_location;
            angle = calculateAngle(current_location,target_location);
            /*
            gradient =
                    calculateGradient(current_location,
                            target_location);
             */
            //System.out.println(gradient);
            obstacle =
                    controller.adjacentLocation(
                            current_location.directionTo(
                                    target_location));
            stuck_turns = 0;
            return move(target_location);
        } else {
            //controller.setIndicatorString("Bugging");
            // Robot trapped

            controller.setIndicatorString(start_bug_location.toString());

            if (start_bug_location.equals(current_location)) {
                count += 1;
                if (count >= 3) {
                    stuck_turns += 1;
                    return MoveResult.FAIL;
                }
            }


            Direction
                    obstacleDirection =
                    current_location.directionTo(
                            obstacle);
            Direction target_direction = obstacleDirection;
            // Edge Case: Obstacle gone

            controller.setIndicatorString(obstacle.toString());
            //System.out.println(obstacle);
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
            //MapLocation start_loc = controller.getLocation();
            while ((controller.canSenseLocation(controller.getLocation().add(target_direction)) && controller.senseRubble(controller.getLocation().add(target_direction)) >= rubble_limit)
                    || !controller.canMove(target_direction)) {

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
                double angle_a = calculateAngle(current_location,target_location);
                double angle_b = calculateAngle(moveLoc,target_location);
                double a = (angle - angle_a + 2*Math.PI) % Math.PI;
                double b =  (angle - angle_b + 2*Math.PI) % Math.PI;
                if (a<Math.PI && b > Math.PI || a > Math.PI && b < Math.PI) {
                    //controller.setIndicatorString(String.valueOf(a));
                    //controller.setIndicatorString(String.valueOf(b));
                    is_bugging = false;
                    clockwise = start_clockwise;
                }
                /*
                if (start_clockwise) {
                    if (calculateGradient(current_location, target_location) > gradient
                            && calculateGradient(moveLoc, target_location) <= gradient
                            && sameSign(calculateGradient(current_location, target_location),calculateGradient(moveLoc, target_location))) {
                        controller.setIndicatorString("C");
                        is_bugging = false;
                        clockwise = start_clockwise;
                    } else if (!clockwise && calculateGradient(moveLoc,
                            target_location) >= gradient) {
                        controller.setIndicatorString("D");
                        is_bugging = false;
                        clockwise = start_clockwise;
                    }
                }
                else {
                    if (calculateGradient(current_location,
                            target_location) < gradient
                            &&  calculateGradient(moveLoc, target_location) >= gradient
                            && sameSign(calculateGradient(moveLoc, target_location),calculateGradient(current_location,
                            target_location))) {
                        controller.setIndicatorString("E");
                        System.out.println(gradient);
                        System.out.println(calculateGradient(current_location,
                                target_location));
                        System.out.println(calculateGradient(moveLoc,
                                target_location));
                        is_bugging = false;
                        clockwise = start_clockwise;
                    } else if (clockwise && calculateGradient(moveLoc,
                            target_location) <= gradient) {
                        controller.setIndicatorString("F");
                        is_bugging = false;
                        clockwise = start_clockwise;
                    }
                }

                 */

            }
            return naive.move(target_direction);
        }
    }

    // Util

    private static boolean sameSign(double x, double y) {
        return ((x<0) == (y<0));
    }

    private static double calculateAngle (MapLocation start, MapLocation end) {
        double x = end.x-start.x;
        double y = end.y-start.y;

        // Edge cases
        if (x > 0 && y == 0) {
            return 0.0;
        }
        if (x < 0 && y == 0) {
            return  Math.PI;
        }
        double angle = Math.atan(x/y);
        if (x>0) {
            return angle;
        }
        return angle + Math.PI;
    }

    private static double calculateGradient(
            MapLocation start, MapLocation end) {
        if (start == null || end == null) {
            System.out.println("Who did this. BugNav bug");
        }
        if (end.x - start.x == 0) {
            return -10000;
        }
        //Rise over run
        return 1.0 * (end.y - start.y) / (end.x - start.x);
    }

}
