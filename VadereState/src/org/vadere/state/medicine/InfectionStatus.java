package org.vadere.state.medicine;

/**
 * Infection status defined similarly to compartmental S(E)IR models (Kermack–McKendrick theory, "kermack-1927"):
 *
 * Simplifications and assumptions:
 *  SUSCEPTIBLE:    absorbs and accumulates pathogens, does not emit pathogens, becomes EXPOSED if susceptibility
 *                  exceeded
 *  EXPOSED:        absorbs and accumulates pathogens, does not emit pathogens, becomes INFECTIOUS after latent period
 *  INFECTIOUS:     does not absorb and accumulate pathogens, emits pathogens, becomes RECOVERED after infectious period
 *  RECOVERED:      does not absorb and accumulate pathogens, does not emit pathogens, becomes SUSCEPTIBLE after
 *                  recovered period; RECOVERED can be interpreted as immune. An alternative definition of R is REMOVED
 *                  (e.g. deceased, isolated, ...)
 *
 * Extending the infection status:
 *  Infection statuses SYMPTOMATIC_INFECTIOUS and ASYMPTOMATIC_INFECTIOUS could be considered to account for adaptation
 *  to the pedestrians' behavior (e.g. keeping greater distance from symptomatic pedestrians). Note the difference
 *  between latent period and incubation period.
 *
 */

public enum InfectionStatus {
    SUSCEPTIBLE, EXPOSED, INFECTIOUS, RECOVERED
}
