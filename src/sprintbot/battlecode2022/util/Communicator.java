package sprintbot.battlecode2022.util;

public class Communicator {

    /* Connection Schema
     * 0 - 15 : Header
     * 16 - 16 + Archon Number * : Arcon communication
     * Other : Unit communication
     * */

    /* Header Schema

     * Map orientation - 2 bits
     * Archon number - 2 bits
     * Current strategy? - 3 bits
     *
     * */

    /* Archon Schema
     *
     * Shared overall macro strategy (eg. attack certain location) 16 bits
     * Each archon will then have a 16 bit channel for themselves to give out orders
     *
     *   Archon order
     *   1 bit alternating between rounds - still alive?
     *   3 bits - spawned unit position (archon will check to ensure no two archons spawn at same location)
     *   8 bits - location
     *   4 bits - flag
     *
     * */

    /* Unit Schema
     *
     *  Round Number % 5 = 0: Mining
     *  Scouting
     *  Micro
     *
     * */

    // TODO: Please use OOP when transmitting different message types. No static switch statements.

    // TODO: Compression ideas? Discuss.

    // TODO: Test out different ways of message storing, bitset? boolean array? directly read? Discuss.

    // TODO: Should we attempt distributed pathfinding? Discuss.

    // TODO: Please store decoded information in cache!

}
