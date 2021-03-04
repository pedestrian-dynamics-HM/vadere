package org.vadere.manager.traci.commandHandler;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolygonCommandHandlerTest extends CommandHandlerTest {

	private PolygonCommandHandler polyCmdHandler = PolygonCommandHandler.instance;

	// Get

	@Test
	public void process_getTopographyBounds() {
		PolygonVar var = PolygonVar.TOPOGRAPHY_BOUNDS;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		ArrayList<String> retVal1 = new ArrayList<>(List.of("0.0", "0.0", "3.0", "3.0"));
		Rectangle2D.Double retVal2 = new Rectangle2D.Double(0.0, 0.0, 3.0, 3.0);
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getBounds()).thenReturn(retVal2);
			}
		};
		TraCICommand ret = polyCmdHandler.process_getTopographyBounds(cmd, rm, var);
		testGetValue(ret, varID, varType, elementID, retVal1);
	}

	@Test
	public void process_getIDList() {
		PolygonVar var = PolygonVar.ID_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		ArrayList<String> retVal = new ArrayList<>(List.of("1", "2"));
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl1 = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ScenarioElement scenEl2 = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl1, scenEl2));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
				when(scenEl1.getId()).thenReturn(Integer.parseInt(retVal.get(0)));
				when(scenEl2.getId()).thenReturn(Integer.parseInt(retVal.get(1)));
			}
		};
		TraCICommand ret = polyCmdHandler.process_getIDList(cmd, rm, var);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getIDCount() {
		PolygonVar var = PolygonVar.COUNT;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		int retVal = 3;
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl1 = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ScenarioElement scenEl2 = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ScenarioElement scenEl3 = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl1, scenEl2, scenEl3));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
			}
		};
		TraCICommand ret = polyCmdHandler.process_getIDCount(cmd, rm, var);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getType() {
		PolygonVar var = PolygonVar.TYPE;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		String retVal = "OBSTACLE";
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
				when(scenEl.getId()).thenReturn(Integer.parseInt(elementID));
				when(scenEl.getType()).thenReturn(ScenarioElementType.OBSTACLE);
			}
		};
		TraCICommand ret = polyCmdHandler.process_getType(cmd, rm, var);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getShape() {
		PolygonVar var = PolygonVar.SHAPE;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		List<VPoint> retVal = List.of(
				new VPoint(0.0, 0.0),
				new VPoint(1.0, 0.0),
				new VPoint(1.0, 1.0),
				new VPoint(0.0, 1.0));
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
				when(scenEl.getId()).thenReturn(Integer.parseInt(elementID));
				when(scenEl.getShape().getPath()).thenReturn(retVal);
			}
		};
		TraCICommand ret = polyCmdHandler.process_getShape(cmd, rm, var);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getCentroid() {
		PolygonVar var = PolygonVar.CENTROID;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		VPoint retVal = new VPoint(3.0, 4.0);
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
				when(scenEl.getId()).thenReturn(Integer.parseInt(elementID));
				when(scenEl.getShape().getCentroid()).thenReturn(retVal);
			}
		};
		TraCICommand ret = polyCmdHandler.process_getCentroid(cmd, rm, var);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getDistance() {
		PolygonVar var = PolygonVar.DISTANCE;
		int varID = var.id;
		TraCIDataType varType = PolygonVar.DISTANCE.type;
		String elementID = "1";
		ArrayList<String> retVal = new ArrayList<>(List.of("0.0"));
		ArrayList<String> point = new ArrayList<>(List.of("0.0", "0.0"));
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_POLYGON, elementID, varID, varType, point));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ScenarioElement scenEl = mock(ScenarioElement.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<ScenarioElement> scenEls = new ArrayList<ScenarioElement>(List.of(scenEl));

				when(simState.getTopography().getAllScenarioElements()).thenReturn(scenEls);
				when(scenEl.getId()).thenReturn(Integer.parseInt(elementID));
			}
		};
		TraCICommand ret = polyCmdHandler.process_getDistance(cmd, rm);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}
}