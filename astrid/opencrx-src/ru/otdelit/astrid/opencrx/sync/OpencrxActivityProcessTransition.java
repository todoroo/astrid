package ru.otdelit.astrid.opencrx.sync;

public class OpencrxActivityProcessTransition {
	private String id;
	private String name;

	private OpencrxActivityProcessState prevState;
	private OpencrxActivityProcessState nextState;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public OpencrxActivityProcessState getPrevState() {
		return prevState;
	}
	public void setPrevState(OpencrxActivityProcessState prevStateId) {
		this.prevState = prevStateId;
	}
	public OpencrxActivityProcessState getNextState() {
		return nextState;
	}
	public void setNextState(OpencrxActivityProcessState nextStateId) {
		this.nextState = nextStateId;
	}

}
