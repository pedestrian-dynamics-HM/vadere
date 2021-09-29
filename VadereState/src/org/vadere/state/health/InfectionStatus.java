package org.vadere.state.health;

/**
 * Infection status defined similarly to compartmental S(E)IR models (Kermack–McKendrick theory, "kermack-1927"):
 *
 * Simplifications and assumptions:
 * <ul>
 *     <li>{@link #SUSCEPTIBLE}: absorbs and accumulates pathogens, does not emit pathogens, becomes {@link #EXPOSED} if
 *     susceptibility exceeded</li>
 *     <li>{@link #EXPOSED}: absorbs and accumulates pathogens, does not emit pathogens, becomes {@link #INFECTIOUS}
 *     after exposed period</li>
 *     <li>{@link #INFECTIOUS}: does not absorb and accumulate pathogens, emits pathogens, becomes {@link #RECOVERED}
 *     after infectious period</li>
 *     <li>{@link #RECOVERED}: does not absorb and accumulate pathogens, does not emit pathogens, becomes
 *     {@link #SUSCEPTIBLE} after recovered period; {@link #RECOVERED} can be interpreted as immune. An alternative
 *     definition of R is removed (e.g. deceased, isolated, ...)</li>
 * </ul>
 *
 * Extending the infection status:
 * Infection statuses SYMPTOMATIC_INFECTIOUS and ASYMPTOMATIC_INFECTIOUS could be considered to account for adaptation
 * to the pedestrians' behavior (e.g. keeping greater distance from symptomatic pedestrians). Note the difference
 * between latent period and incubation period.
 *
 */

public enum InfectionStatus {
    SUSCEPTIBLE, EXPOSED, INFECTIOUS, RECOVERED
}
