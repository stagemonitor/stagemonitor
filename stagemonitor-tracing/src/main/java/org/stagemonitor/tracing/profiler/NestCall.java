package org.stagemonitor.tracing.profiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;

/**
 * Created by z81023253 on 2017/8/9.
 * Solve the bug of nested monitor,which caused the error of calculation of execution of time
 */
public class NestCall {

	private static final Logger logger = LoggerFactory.getLogger(NestCall.class);

	public static Stack<CallStackElement> printcallStackTree =new Stack<CallStackElement>();

	public static Stack<CallStackElement> nestCallStack = new Stack<CallStackElement>();

	/**
     * printf the callTree by breadth first search
	 * @param parent head node
	 */
	public static void PrintCallTreeStack(CallStackElement parent){

		if(parent == null){
			logger.debug("parent is null!!!");
			return;
		}

		List<CallStackElement> list = parent.getChildren();
		System.out.print("***"+parent.getSignature() + ":[");
		for(int i = 0;i<list.size();i++){
			CallStackElement callStackElement = list.get(i);
			System.out.print("(" + callStackElement.getSignature() + ":" + callStackElement.getExecutionTime()/1000000 + ") ");
			printcallStackTree.push(callStackElement);
		}
		System.out.println("]" +parent.getExecutionTime()/1000000 );

		if(printcallStackTree.size() > 0)
			PrintCallTreeStack(printcallStackTree.pop());
	}

	/**
	 * store the spot,must call befor the nested monitor method start
	 */
	public static void StoreSpot(){
		if(Profiler.getMethodCallParent() == null){
			logger.error("!!!store the spot:" + Profiler.getMethodCallParent());
			return;
		}

		nestCallStack.push( Profiler.getMethodCallParent());
	}

	/**
	 * recover the spot,call at the end of monitor method
	 * @param nestCallTree monitor tree
	 * @return the new calltree
	 */
	public static void  RecoverSpot(CallStackElement nestCallTree){//can use the calltree of SpanContextInformation
		if(nestCallStack.size() == 0){
			return;
		}

		CallStackElement spot = nestCallStack.pop();
		if(spot == null){
			logger.debug("spot is null");
			return;
		}

		if(nestCallTree.getSignature().equals("total")){//nestCallTree is not add to span
			Profiler.setMethodCallParent(spot);
			return;
		}

		List<CallStackElement> childs = nestCallTree.getChildren();
		List<CallStackElement> childsSpot = spot.getChildren();

		if(childs.size() == 0){//if noly one node in the nestCallTree
			nestCallTree.setParent(spot);
			childsSpot.add(nestCallTree);
		}
		else{
			for(int i = 0;i<childs.size();i++){
				CallStackElement child = childs.get(i);
				child.setParent(spot);//modify the parent of all childs，make them points to caller
				childsSpot.add(child);
			}
		}

		spot.setChildren(childsSpot);//update the childs of spot
		Profiler.setMethodCallParent(spot);//recover the spot
	}
}
