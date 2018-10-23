package org.vadere.state.attributes;

import org.vadere.util.geometry.shapes.VShape;


/**
 * Abstract Base Class for {@link Attributes} that also consist of a {@link VShape}. The shape can
 * be changed and 'undo' and 'redo' operations can be carried out in EditUpdateElementShape in
 * package org.vadere.gui.topographycreator.control.
 */

public abstract class AttributesEmbedShape extends Attributes {

	public abstract void setShape(VShape shape);

	public abstract VShape getShape();
}
