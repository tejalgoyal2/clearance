package me.tgoyal.clearance.domain;

/** A reviewer's call on a flagged component. NONE until someone decides. */
public enum Decision {
    NONE,
    ACCEPTED,
    REJECTED
}
