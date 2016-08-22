package org.vadere.simulator.projects.migration.incidents;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;

import java.io.IOException;

public class ExceptionIncident extends Incident {

	private JsonNode node;

	public ExceptionIncident(JsonNode node) {
		this.node = node;
	}

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {
		while (true) {
			try {
				JsonConverter.deserializeScenarioRunManagerFromNode(node);
				break;
			} catch (IOException e) {
				String errMsg = e.getMessage();
				if (errMsg.startsWith("Unrecognized field")) { // UnrecognizedFieldExceptionIncident
					Pair<String, String> referenceChainNodes = getReferenceChainNodes(errMsg);
					graph.deleteUnrecognizedField(referenceChainNodes.getLeft(), referenceChainNodes.getRight(), log,
							this);
				} else if (errMsg.startsWith("Can not coerce a floating-point")) { // EnforceIntegerExceptionIncident
					Pair<String, String> referenceChainNodes = getReferenceChainNodes(errMsg);
					graph.enforceIntegerValue(referenceChainNodes.getLeft(), referenceChainNodes.getRight(), log, this);

					// add new auto-corrections here

				} else {
					String logMsg = "Can't automatically deal with this JsonProcessException: \n" + e.getMessage();

					if (e.getMessage().startsWith("Can't parse \"false\" as boolean")
							|| e.getMessage().startsWith("Can't parse \"true\" as boolean")) {
						logMsg = "One or more (the migration was stopped at the first occurrence) boolean-value " +
								"is wrapped in quotation marks and is thereby erroneously parsed as String " +
								"instead of boolean. This is the full error message:\n" + e.getMessage();
					}

					throw new MigrationException(this, logMsg);
				}
			}
		}
	}

	private Pair<String, String> getReferenceChainNodes(String errMsg) {
		String referenceChain =
				errMsg.substring(errMsg.indexOf("through reference chain: ") + "through reference chain: ".length());
		String parentKey = referenceChain.split("\"")[0].substring(0, referenceChain.split("\"")[0].length() - 1);
		String childKey = referenceChain.split("\"")[1];
		return Pair.of(parentKey, childKey);
	}

}
