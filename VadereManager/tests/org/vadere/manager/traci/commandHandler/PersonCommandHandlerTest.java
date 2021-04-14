package org.vadere.manager.traci.commandHandler;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.CmdType;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.types.KnowledgeItem;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersonCommandHandlerTest extends CommandHandlerTest {

	private PersonCommandHandler persCmdHandler = PersonCommandHandler.instance;

	// Get

	@Test
	public void process_getHasNextTarget() {

		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));

		// Return 1
		int retVal = 1;
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).hasNextTarget())
						.thenReturn(true);
			}
		};
		TraCICommand ret = persCmdHandler.process_getHasNextTarget(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);

		// Return 0
		int retVal2 = 0;
		RemoteManager rm2 = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).hasNextTarget())
						.thenReturn(false);
			}
		};
		TraCICommand ret2 = persCmdHandler.process_getHasNextTarget(cmd, rm2);
		testTraCICommand(ret2, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret2);
		testGetValue(ret2, varID, varType, elementID, retVal2);
	}

	@Test
	public void process_getHasNextTarget2() {
		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getHasNextTarget(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getNextTargetListIndex() {
		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		int retVal = 0;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(1).getNextTargetListIndex())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getNextTargetListIndex(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getNextTargetListIndex2() {
		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getNextTargetListIndex(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getIDList() {
		PersonVar var = PersonVar.ID_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		String[] ids = {"1", "2", "3"};
		ArrayList<String> retVal = new ArrayList<String>(List.of("1", "2", "3"));
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID
		));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian pedEl1 = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				Pedestrian pedEl2 = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				Pedestrian pedEl3 = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				ArrayList<Pedestrian> pedEls = new ArrayList<>(List.of(pedEl1, pedEl2, pedEl3));

				when(pedEl1.getId()).thenReturn(1);
				when(pedEl2.getId()).thenReturn(2);
				when(pedEl3.getId()).thenReturn(3);
				when(simState.getTopography().getPedestrianDynamicElements().getElements()).thenReturn(pedEls);
			}
		};
		TraCICommand ret = persCmdHandler.process_getIDList(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getNextFreeID() {
		PersonVar var = PersonVar.NEXT_ID;
		int varID = var.id;
		TraCIDataType varType = var.type;
		int retVal = 2;
		String elementID = "-1";
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getNextDynamicElementId()).thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getNextFreeId(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getIDCount() {
		PersonVar var = PersonVar.COUNT;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		int retVal = 5;
		TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElements().size())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getIDCount(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getFreeFlowSpeed() {
		PersonVar var = PersonVar.SPEED;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		double retVal = 0.;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).getFreeFlowSpeed())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getFreeFlowSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getFreeFlowSpeed2() {
		PersonVar var = PersonVar.SPEED;
		int varID = var.id;
		String elementID = "1";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getFreeFlowSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getPosition3D() {
		PersonVar var = PersonVar.POSITION3D;
		int varID = var.id;
		TraCIDataType varType = var.type;;
		String elementID = "1";
		Vector3D retVal = new Vector3D(1.0, 1.0, 0.0);
		VPoint retVal2D = new VPoint(retVal.x, retVal.y);
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getPosition()).thenReturn(retVal2D);
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(ped);
			}
		};
		TraCICommand ret = persCmdHandler.process_getPosition3D(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getPosition2D() {
		PersonVar var = PersonVar.POSITION;
		int varID = var.id;
		TraCIDataType varType = var.type;;
		String elementID = "1";
		VPoint retVal = new VPoint(2.5, 2.5);
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).getPosition())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getPosition(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getPosition2D2() {
		PersonVar var = PersonVar.POSITION;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getPosition(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getPositionList() {
		PersonVar var = PersonVar.POSITION_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;;
		String elementID = "-1";
		HashMap<String, VPoint> retVal = new HashMap<>();
		retVal.put("1", new VPoint(0.0, 0.0));
		retVal.put("2", new VPoint(1.0, 0.0));
		retVal.put("3", new VPoint(0.0, 1.0));
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

				ArrayList<Pedestrian> pedEls = new ArrayList<>();
				for(String key:retVal.keySet()){
					Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
					when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(key)))
							.thenReturn(ped);
					when(ped.getId()).thenReturn(Integer.parseInt(key));
					when(ped.getPosition()).thenReturn(retVal.get(key));
					pedEls.add(ped);
				}

				when(simState.getTopography().getPedestrianDynamicElements().getElements()).thenReturn(pedEls);
			}
		};
		TraCICommand ret = persCmdHandler.process_getPosition2DList(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getVelocity() {
		PersonVar var = PersonVar.VELOCITY;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		Vector2D retVal = new Vector2D(2.5, 2.5);
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).getVelocity())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getVelocity(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getVelocity2() {
		PersonVar var = PersonVar.VELOCITY;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getVelocity(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getMaximumSpeed() {
		PersonVar var = PersonVar.MAXSPEED;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		double retVal = 2.2;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)).getAttributes().getMaximumSpeed())
						.thenReturn(retVal);
			}
		};
		TraCICommand ret = persCmdHandler.process_getMaximumSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getMaximumSpeed2() {
		PersonVar var = PersonVar.MAXSPEED;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getMaximumSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getType() {
		PersonVar var = PersonVar.TYPE;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		String retVal = "pedestrian";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				return;
			}
		};
		TraCICommand ret = persCmdHandler.process_getType(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getTargetList() {
		PersonVar var = PersonVar.TARGET_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		ArrayList<String> retVal = new ArrayList<>(List.of("2", "5"));
		LinkedList<Integer> internalRetVal = new LinkedList<>(List.of(2, 5));
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getTargets()).thenReturn(internalRetVal);
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(ped);
			}
		};
		TraCICommand ret = persCmdHandler.process_getTargetList(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getTargetList2() {
		PersonVar var = PersonVar.TARGET_LIST;
		int varID = var.id;
		String elementID = "1";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getTargetList(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getLength() {
		PersonVar var = PersonVar.LENGTH;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		double retVal = 0.4;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getRadius()).thenReturn(retVal / 2);
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(ped);
			}
		};
		TraCICommand ret = persCmdHandler.process_getLength(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getLength2() {
		PersonVar var = PersonVar.LENGTH;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getLength(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getWidth() {
		PersonVar var = PersonVar.WIDTH;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		double retVal = 0.4;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getRadius()).thenReturn(retVal / 2);
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(ped);
			}
		};
		TraCICommand ret = persCmdHandler.process_getWidth(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getWidth2() {
		PersonVar var = PersonVar.WIDTH;
		int varID = var.id;
		String elementID = "10";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_getWidth(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_Err(ret);
		testGetValue(ret, varID, null, elementID, null);
	}

	@Test
	public void process_getRoadId() {
		PersonVar var = PersonVar.ROAD_ID;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String retVal = "road000";
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
			}
		};
		TraCICommand ret = persCmdHandler.process_getRoadId(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}

	@Test
	public void process_getAngle() {
		PersonVar var = PersonVar.ANGLE;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		double retVal = 0.0;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_PERSON_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
			}
		};
		TraCICommand ret = persCmdHandler.process_getAngle(cmd, rm);
		testTraCICommand(ret, TraCICmd.GET_PERSON_VALUE, CmdType.VALUE_GET);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}



	// Set

	@Test
	public void process_setNextTargetListIndex() {
		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		int data = 0;
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

			}
		};
		TraCICommand ret = persCmdHandler.process_setNextTargetListIndex(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}


	@Test
	public void process_setInformationItem() {
		PersonVar var = PersonVar.INFORMATION_ITEM;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "10";
		Pedestrian p = new Pedestrian(new AttributesAgent(10), new Random(1));
		CompoundObject data = CompoundObjectBuilder.builder()
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.STRING)
				.build(12.2, 13.2, "reason001");

		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(p);
			}
		};
		KnowledgeItem s = new KnowledgeItem(12.2, 13.2, "reason001");

		TraCICommand ret = persCmdHandler.process_setStimulus(cmd, rm);
		assertThat(p.getKnowledgeBase().getKnowledge().get(0), equalTo(s));
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setNextTargetListIndex2() {
		PersonVar var = PersonVar.NEXT_TARGET_LIST_INDEX;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "10";
		int data = 0;
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_setNextTargetListIndex(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setFreeFlowSpeed() {
		PersonVar var = PersonVar.SPEED;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		double data = 1.5;
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

			}
		};
		TraCICommand ret = persCmdHandler.process_setFreeFlowSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setFreeFlowSpeed2() {
		PersonVar var = PersonVar.SPEED;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "10";
		double data = 1.5;
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_setFreeFlowSpeed(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setPosition2D() {
		PersonVar var = PersonVar.POSITION;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		VPoint data = new VPoint(1.5, 1.5);
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

			}
		};
		TraCICommand ret = persCmdHandler.process_setPosition(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setPosition2D2() {
		PersonVar var = PersonVar.POSITION;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "10";
		VPoint data = new VPoint(1.5, 1.5);
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_setPosition(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setTargetList() {
		PersonVar var = PersonVar.TARGET_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "1";
		ArrayList<String> data = new ArrayList<>(List.of("1", "2", "3"));
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

			}
		};
		TraCICommand ret = persCmdHandler.process_setTargetList(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_setTargetList2() {
		PersonVar var = PersonVar.TARGET_LIST;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "10";
		ArrayList<String> data = new ArrayList<>(List.of("1", "2", "3"));
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(elementID)))
						.thenReturn(null);
			}
		};
		TraCICommand ret = persCmdHandler.process_setTargetList(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_addPerson() {
		PersonVar var = PersonVar.ADD;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String dataPath = "testResources/personCreateData.json";
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch(IOException e) {
			e.printStackTrace();
		}
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				ArrayList<Integer> al = new ArrayList<>(List.of(1));
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getId()).thenReturn(2);
				when(simState.getTopography().getPedestrianDynamicElements().getElements())
						.thenReturn(new ArrayList<Pedestrian>(List.of(ped)));
				when(simState.getTopography().getPedestrianDynamicElements().getElement(2))
						.thenReturn(ped);
				MainModel mm = mock(OptimalStepsModel.class, Mockito.RETURNS_DEEP_STUBS);
				when(simState.getMainModel()).thenReturn(Optional.ofNullable(mm));
			}
		};
		TraCICommand ret = persCmdHandler.process_addPerson(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_addPerson2() {
		PersonVar var = PersonVar.ADD;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String dataPath = "testResources/personCreateData.json";
		String data = "";
		Pedestrian desPed;
		int elementID = -1;
		try {
			data = IOUtils.readTextFile(dataPath);
			desPed = StateJsonConverter.deserializePedestrian(data);
			elementID = desPed.getId();
		} catch(IOException e) {
			e.printStackTrace();
		}
		final int finalElementID = elementID;
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, Integer.toString(finalElementID), varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getId()).thenReturn(finalElementID);
				when(simState.getTopography().getPedestrianDynamicElements().getElements())
						.thenReturn(new ArrayList<Pedestrian>(List.of(ped)));
				when(simState.getTopography().getPedestrianDynamicElements().getElement(finalElementID))
						.thenReturn(ped);
				MainModel mm = mock(OptimalStepsModel.class, Mockito.RETURNS_DEEP_STUBS);
				when(simState.getMainModel()).thenReturn(Optional.ofNullable(mm));
			}
		};
		TraCICommand ret = persCmdHandler.process_addPerson(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret); // id already in use todo: systematise error types.
		testSetValue(ret, varID, varType, Integer.toString(finalElementID), data);
	}

	@Test
	public void process_addPerson3() {
		PersonVar var = PersonVar.ADD;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String dataPath = "testResources/personCreateData.json";
		String data = "";
		Pedestrian desPed;
		String elementID = "-1";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch(IOException e) {
			e.printStackTrace();
		}
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_PERSON_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				Pedestrian ped = mock(Pedestrian.class, Mockito.RETURNS_DEEP_STUBS);
				when(ped.getId()).thenReturn(3);
				when(simState.getTopography().getPedestrianDynamicElements().getElements())
						.thenReturn(new ArrayList<Pedestrian>(List.of(ped)));
				when(simState.getTopography().getPedestrianDynamicElements().getElement(3))
						.thenReturn(ped);
				when(simState.getMainModel()).thenReturn(Optional.ofNullable(null));

			}
		};
		TraCICommand ret = persCmdHandler.process_addPerson(cmd, rm);
		testTraCICommand(ret, TraCICmd.SET_PERSON_STATE, CmdType.VALUE_SET);
		checkSET_Err(ret); // main model not present todo: systematise error types.
		testSetValue(ret, varID, varType, elementID, data);
	}

}
