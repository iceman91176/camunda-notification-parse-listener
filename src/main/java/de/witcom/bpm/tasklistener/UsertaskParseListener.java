package de.witcom.bpm.tasklistener;

import java.util.List;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

public class UsertaskParseListener extends AbstractBpmnParseListener implements BpmnParseListener {

  private final Logger LOGGER = Logger.getLogger(UsertaskParseListener.class.getName());

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEvent) {
    LOGGER.info("Parsing Start Event "
        + ", activityId=" + startEvent.getId()
        + ", activityName='" + startEvent.getName() + "'"
        + ", scopeId=" + scope.getId()
        + ", scopeName=" + scope.getName());
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    LOGGER.info("Adding Task Listener to User Task:"
        + " activityId=" + activity.getId()
        + ", activityName='" + activity.getName() + "'"
        + ", scopeId=" + scope.getId()
        + ", scopeName=" + scope.getName());
    
    
    if (!createTaskListener(userTaskElement)) {
    	return;
    	
    }
    ActivityBehavior behavior = activity.getActivityBehavior();
    if (behavior instanceof UserTaskActivityBehavior) {
      ((UserTaskActivityBehavior) behavior)
      .getTaskDefinition()
      .addTaskListener(TaskListener.EVENTNAME_CREATE,   UsertaskListener.getInstance());
    }
  }
  
  private boolean createTaskListener(Element userTaskElement) {
	 
	  Element extensionElement = userTaskElement.element("extensionElements");
	  
	  if (extensionElement != null) {
		    
	      // get the <camunda:properties ...> element from the service task
	      Element propertiesElement = extensionElement.element("properties");
	      if (propertiesElement != null) {
	    	
	        //  get list of <camunda:property ...> elements from the service task
	        List<Element> propertyList = propertiesElement.elements("property");
	        for (Element property : propertyList) {
	        
	          // get the name and the value of the extension property element
	          String name = property.attribute("name");
	          String value = property.attribute("value");
	          
	          // check if name attribute has the expected value
	          if("disableNotification".equals(name)) {
	        	  if ("true".equals(value)) {
	        		  return false;
	        	  }
	          }
	        }
	      }
	  }
	  return true;
	  
	  
	  
  }

}
