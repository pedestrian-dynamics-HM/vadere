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
    NO_INFORMATION,
    INFORMATION_STIMULUS,
    INFORMATION_RECEIVED,
    INFORMATION_CONVINCING_RECEIVED,
    INFORMATION_UNCONVINCING_RECEIVED,
    FOLLOW_INFORMED_GROUP_MEMBER;
}
