package dev.strataindustria.power;

/** A machine on a network (spec 8): it costs {@code impact} SU per RPM it turns at. */
public interface KineticConsumer extends Kinetic {
    /** SU per RPM. */
    int impact();

    /** Below this RPM the machine waits with "Too slow". */
    int minSpeed();
}
