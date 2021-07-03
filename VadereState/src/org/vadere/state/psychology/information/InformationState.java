package org.vadere.state.psychology.information;

/**
 *
 * A person can receive multiple forms of information.
 * In the default setting, we assume that each agents have global knowledge about the location to target.
 * If the psychology layer is used, the information is set up as a stimulus.
 * If the crowd is managed e.g. over TraCI, information can be passed as directly to an agent or as stimuli.
 * If the reaction behavior is modelled, it is also possible that agents refuse to react to information.
 * author: Christina Mayr

 */

public enum InformationState {
    INFORMATION_STIMULUS,
    NO_INFORMATION,
    INFORMATION_RECEIVED,
    INFORMATION_CONVINCING_RECEIVED,
    INFOREMD_UNCONVINCING_RECEIVED,
}
