package org.vadere.simulator.entrypoints;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesBuilder;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Teleporter;
import org.vadere.util.logging.Logger;

import java.lang.reflect.Field;
import java.util.List;


/**
 * {@link ScenarioBuilder} provide methods to manipulate the attributes/parameters of a scenario.
 * It's an interface for generate multiple Scenarios based on the same infrastructure but
 * with different attributes/parameters. The class does not provide methods to change the
 * infrastructure of the {@link org.vadere.state.scenario.Topography}. To use this effectively the user need a
 * good knowledge about the parameters of the given scenario!
 *
 * @author Benedikt Zoennchen
 *
 */
public class ScenarioBuilder {
    private static Logger logger = Logger.getLogger(ScenarioBuilder.class);

    private Scenario scenario;
    private ScenarioStore store;

    public ScenarioBuilder(final Scenario base) {
        scenario = base.clone();
        store = scenario.getScenarioStore();
    }

    /*public Scenario build() {
        store.topography = IOVadere
                JsonSerializerTopography.topographyToJson(scenario.getTopography());
        return scenario;
    }*/

    /**
     * Changes the name of the output-files that will be generated during and after the simulation.
     * To prevent yourself from overriding files it's necessary to set a new output name for every
     * generated scenario!
     *
     * @param name the name of the output file
     */
    public void setName(final String name) {
        scenario.setName(name);
    }

    public <T, E extends Attributes> void setAttributesField(final String fieldName, final T value, final Class<E> clazz) {
        AttributesBuilder<E> builder;

        // TODO: duplicated code
        if(AttributesSimulation.class == clazz){
            builder = new AttributesBuilder<>((E)store.getAttributesSimulation());
            builder.setField(fieldName, value);
            store.setAttributesSimulation((AttributesSimulation) builder.build());
        }
        else if(AttributesAgent.class == clazz){
			builder = new AttributesBuilder<>((E) store.getTopography().getAttributesPedestrian());
            builder.setField(fieldName, value);
            setAttributesAgent((AttributesAgent) builder.build());
        }
        else if(AttributesCar.class == clazz){
			builder = new AttributesBuilder<>((E) store.getTopography().getAttributesCar());
            builder.setField(fieldName, value);
            setAttributesCar((AttributesCar) builder.build());
        }
        else if(AttributesTopography.class == clazz){
			builder = new AttributesBuilder<>((E) store.getTopography().getAttributes());
            builder.setField(fieldName, value);
            ReflectionAttributeModifier reflectionAttributeModifier = new ReflectionAttributeModifier();
			reflectionAttributeModifier.setAttributes(store.getTopography(), builder.build());
        }
        else {
            builder = new AttributesBuilder<>(store.getAttributes(clazz));
            builder.setField(fieldName, value);
            store.removeAttributesIf(attributes -> attributes.getClass() == clazz);
            store.addAttributes(builder.build());
        }
    }

    private void setAttributesCar(@NotNull final AttributesCar attributesCar) {
        Field field;
        try {
			field = store.getTopography().getClass().getDeclaredField("attributesCar");
            field.setAccessible(true);
			field.set(store.getTopography(), attributesCar);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }

    private void setAttributesAgent(@NotNull final AttributesAgent attributesAgent) {
        Field field;
        try {
			field = store.getTopography().getClass().getDeclaredField("attributesPedestrian");
            field.setAccessible(true);
			field.set(store.getTopography(), attributesAgent);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }


    // change topography
    /**
     * Assign the value to the field identified by fieldName of the first source, identified by sourceId.
     * If no source was found, nothing will happen.
     *
     * @param fieldName name of the field (e.g. spawnNumber)
     * @param sourceId id of the source
     * @param value value that will be assigned
     */
    public <T> void setSourceField(final String fieldName, final int sourceId, final T value) {
        setField(scenario.getTopography().getSources(), sourceId, fieldName, value);
    }

    /**
     * Assign the value to the field identified by fieldName of the first target, identified by sourceId.
     * If no source was found, nothing will happen.
     *
     * @param fieldName name of the field (e.g. spawnNumber)
     * @param targetId id of the target
     * @param value value that will be assigned
     */
    public <T> void setTargetField(final String fieldName, final int targetId, final T value) {
        setField(scenario.getTopography().getTargets(), targetId, fieldName, value);
    }

    /**
     * Assign the value to the field identified by fieldName.
     * @param fieldName field identifier
     * @param value the assigned value
     */
    public <T> void setTeleporterField(final String fieldName, final T value) {
        Teleporter teleporter = scenario.getTopography().getTeleporter();
        teleporter = setField(fieldName, teleporter, value);
        scenario.getTopography().setTeleporter(teleporter);
    }

    /**
     * Assign the value to all the fields, identified by fieldName, of all sources.
     *
     * @param fieldName field identifier
     * @param value the assigned value
     */
    public <T> void setAllSourcesField(final String fieldName, final T value) {
        setAllField(scenario.getTopography().getSources(), fieldName, value);
    }

    /**
     * Assign the value to all the fields, identified by fieldName, of all targets.
     *
     * @param fieldName field identifier
     * @param value the assigned value
     */
    public <T> void setAllTargetsField(final String fieldName, final T value) {
        setAllField(scenario.getTopography().getTargets(), fieldName, value);
    }

    /**
     * Assign the i-th element of value to the field, identified by the fieldName, of the i-th source.
     * sources.get(i)[field] = values(i).
     *
     * @param fieldName name of the attribute
     * @param values array of values
     */
    @SuppressWarnings("unchecked")
    public <T> void setMultipleSourcesField(final String fieldName, final T ...values) {
        setFieldOfElements(fieldName, scenario.getTopography().getSources(), values);
    }

    /**
     * Assign the i-th element of value to the field, identified by the fieldName, of the i-th target.
     * targets.get(i)[field] = values(i).
     *
     * @param fieldName name of the attribute
     * @param values array of values
     */
    @SuppressWarnings("unchecked")
    public <T> void setMulitipleTargetsField(final String fieldName, final T ...values) {
        setFieldOfElements(fieldName, scenario.getTopography().getTargets(), values);
    }


    // private helpers
    /**
     * Sets the value of the field, identified by fieldName, of the i-th element in the list to the i-th value.
     * list.get(i)[field] = values(i).
     *
     * @param fieldName the name of the field that will be affected
     * @param list list of elements that will be affected
     * @param values values that correspond to the element.
     */
    @SuppressWarnings("unchecked")
    private static <T, E extends ScenarioElement> void setFieldOfElements(final String fieldName, final List<E> list, final T ...values){
        int i = 0;
        while(i < values.length && i < list.size()){
            E element = setField(fieldName, list.get(i), values[i]);
            list.remove(i);
            list.add(i, element);
            i++;
        }
    }

    private static <T, E extends ScenarioElement> void setAllField(final List<E> scenarioElements, final String name, final T value) {
        E tmp = null;
        for (int i = 0; i < scenarioElements.size(); i++) {
            tmp = scenarioElements.get(i);
            E element = setField(name, tmp, value);
            scenarioElements.remove(i);
            scenarioElements.add(i, element);
        }
    }

    /**
     * Sets the field with the name=fieldName of element in scenarioElements, identified by his id, to value.
     * If no element was found, nothing will happen. If there are more than one element with the id in the list,
     * only the first one will be affected.
     */
    private static <T, E extends ScenarioElement> void setField(final List<E> scenarioElements, final int id, final String name, final T value) {
        E tmp = null;
        for (int i = 0; i < scenarioElements.size(); i++) {
            tmp = scenarioElements.get(i);
            if(tmp.getId() == id) {
                E element = setField(name, tmp, value);
                scenarioElements.remove(i);
                scenarioElements.add(i, element);
                return;
            }
        }
        logger.warn("couldn't find scenarioElement ("+(tmp!=null?tmp.getType():null)+") with the id " + id);
    }

    /**
     * Set's the field with the name=fieldName of element to value.
     */
    private static <T, E extends ScenarioElement> E setField(final String fieldName, final E element, final T value){
        ReflectionAttributeModifier reflectionAttributeModifier = new ReflectionAttributeModifier();
        AttributesBuilder<Attributes> attBuilder = new AttributesBuilder<>(element.getAttributes());
        attBuilder.setField(fieldName, value);
        E clone = (E) element.clone();

		//NOTE see issue #91:
		//This class is not tested - revert change by uncommenting reflextion and comment in
		//clone.setAttributes
		//reflectionAttributeModifier.setAttributes(clone, attBuilder.build());
		clone.setAttributes(attBuilder.build());
        return clone;
    }

    public Scenario build() {
        return scenario.clone();
    }
}

