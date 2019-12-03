package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 * Evaluates the flow based on FundamentalDiagramAProcessor (zhang-2011)
 */

public class AttributesMeanFlowProcessor extends AttributesProcessor {
	private int fundamentalDiagramAProcessorId;
	private int evacuationTimeProcessorId;

    public int getPedestrianFundamentalDiagramAProcessorId() {
        return this.fundamentalDiagramAProcessorId;
    }
    public int getEvacuationTimeProcessorId() {
        return this.evacuationTimeProcessorId;
    }


    public void setFundamentalDiagramAProcessorId(int fundamentalDiagramAProcessorId) {
        checkSealed();
        this.fundamentalDiagramAProcessorId = fundamentalDiagramAProcessorId;
    }
    public void setPedestrianEvacuationTimeProcessorId(int evacuationTimeProcessorId) {
        checkSealed();
        this.evacuationTimeProcessorId = evacuationTimeProcessorId;
    }
}
