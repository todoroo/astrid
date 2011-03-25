package ru.otdelit.astrid.opencrx.sync;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class OpencrxActivityProcessGraph {

	private final List<OpencrxActivityProcessTransition> transitions;
	private final List<OpencrxActivityProcessState> states;

	private HashMap<OpencrxActivityProcessState, Boolean> visited;
	private HashMap<OpencrxActivityProcessState, OpencrxActivityProcessTransition> lastEdge;
	private HashMap<OpencrxActivityProcessState, List<OpencrxActivityProcessTransition>> edges;

	public OpencrxActivityProcessGraph(
			List<OpencrxActivityProcessTransition> transitions,
			List<OpencrxActivityProcessState> states) {

		this.transitions = transitions;
		this.states = states;

		normalize();

		preInit();
	}

	private void normalize(){
		for (OpencrxActivityProcessTransition trans : transitions){
			if (trans.getPrevState() != null)
				trans.setPrevState(this.getStateById(trans.getPrevState().getId()));
			if (trans.getNextState() != null)
				trans.setNextState(this.getStateById(trans.getNextState().getId()));
		}
	}

	private void init(){
		for (OpencrxActivityProcessState s : visited.keySet())
			visited.put(s, Boolean.FALSE);
	}

	private void preInit(){
		visited = new HashMap<OpencrxActivityProcessState, Boolean>();
		lastEdge = new HashMap<OpencrxActivityProcessState, OpencrxActivityProcessTransition>();
		edges = new HashMap<OpencrxActivityProcessState, List<OpencrxActivityProcessTransition>>();

		for (OpencrxActivityProcessTransition trans : transitions){

			if ( isTransitionCorrupted(trans))
				continue;

			OpencrxActivityProcessState prev = trans.getPrevState();
			OpencrxActivityProcessState next = trans.getNextState();

			visited.put(prev, Boolean.FALSE);
			visited.put(next, Boolean.FALSE);

			if (edges.get(prev) == null)
				edges.put(prev, new LinkedList<OpencrxActivityProcessTransition>());

			edges.get(prev).add(trans);

		}
	}

	@SuppressWarnings("nls")
    public Stack<OpencrxActivityProcessTransition> getPath(OpencrxActivityProcessState from, OpencrxActivityProcessState to){
		if (from == null || to == null)
			return null;

		init();

		visited.put(from, Boolean.TRUE);

		Queue<OpencrxActivityProcessState> q = new LinkedList<OpencrxActivityProcessState>();
		q.add(from);

		while(! q.isEmpty()){
			OpencrxActivityProcessState cur = q.remove();

			if (edges.get(cur) == null)
				continue;

			for (OpencrxActivityProcessTransition e : edges.get(cur)){
				if (visited.get(e.getNextState()) == null || visited.get(e.getNextState()).equals(Boolean.FALSE)
						|| "Assign".equals(e.getName()) || "Complete".equals(e.getName()) || "Close".equals(e.getName()) ){

					visited.put(e.getNextState(), Boolean.TRUE);
					lastEdge.put(e.getNextState(), e);
					q.add(e.getNextState());

				}

			}

		}

		if (visited.get(to) == null || visited.get(to).equals(Boolean.FALSE)){
			return null;
		}

		Stack<OpencrxActivityProcessTransition> ret = new Stack<OpencrxActivityProcessTransition>();
		OpencrxActivityProcessState cur = to;

		while (!cur.equals(from)){
			OpencrxActivityProcessTransition e = lastEdge.get(cur);
			ret.push(e);

			cur = e.getPrevState();
		}

		return ret;

	}

	public OpencrxActivityProcessState getStateById(String id){
		if (id == null)
			return null;

		for (OpencrxActivityProcessState state : states)
			if (id.equals(state.getId()))
				return state;

		return null;
	}

	public OpencrxActivityProcessState getStateByName(String name){
		if (name == null)
			return null;

		for (OpencrxActivityProcessState state : states)
			if (name.equals(state.getName()))
				return state;

		return null;
	}

   public OpencrxActivityProcessTransition getTransitionByName(String name){
        if (name == null)
            return null;

        for (OpencrxActivityProcessTransition trans : transitions)
            if (name.equals(trans.getName()))
                return trans;

        return null;
    }

   public OpencrxActivityProcessTransition getTransitionByStates(String prevStateId, String nextStateId){
       if (prevStateId == null || nextStateId == null)
           return null;

       for (OpencrxActivityProcessTransition trans : transitions){
           if (isTransitionCorrupted(trans))
               continue;
           if (prevStateId.equals(trans.getPrevState().getId()) && nextStateId.equals(trans.getNextState().getId()))
               return trans;
       }

       return null;
   }

	private boolean isTransitionCorrupted(OpencrxActivityProcessTransition trans){
		return trans == null || trans.getPrevState() == null || trans.getNextState() == null || trans.getId() == null;
	}

}
