package de.witcom.bpm.tasklistener;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.IdentityLink;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import de.witcom.bpm.tasklistener.notification.MailNotifier;

public class UsertaskListener implements TaskListener {

  private final Logger LOGGER = Logger.getLogger(UsertaskListener.class.getName());
  private static UsertaskListener instance = null;
  private Keycloak keycloak=null;
  private String groupPrefix = "camunda_group-";
  private String realmId=null;
  private MailNotifier notifier;
  
  protected UsertaskListener() {
	  
	  String tmpPrefix = System.getenv("ROLE_PREFIX_GROUP");
	  if (tmpPrefix!=null){
		    groupPrefix = System.getenv("CAMUNDA_GROUP_PREFIX");
	  }
	  notifier = new MailNotifier();
	  refreshKeycloakSession();
  }
  
  public static UsertaskListener getInstance() {
	  if(instance == null) {
	      instance = new UsertaskListener();
	  }
	  return instance;
  }
  
  private List<UserRepresentation> getCandidatesFromGroup(String candidateGroup){
	  
	  
	  String searchGroup = groupPrefix + candidateGroup;
	  refreshKeycloakSession();
	  if (keycloak==null) {
		  LOGGER.warning("Keycloak-Session could not be established - cannot get candidateGroup");
		  return null;
		  
	  }
	  RealmResource realm = keycloak.realms().realm(realmId);

	  try {
		  Optional<GroupRepresentation> group = realm.groups()
				  .groups(searchGroup, 0, 5)
				  .stream().filter(g-> g.getName().equals(searchGroup))
				  .findFirst();
		  
		  if (group.isPresent()) {
			  return realm.groups().group(group.get().getId()).members();
		  }
		  LOGGER.warning("no candidate-group "+searchGroup+" found");
		  return null;
		  
	  } catch (Exception e) {
		  LOGGER.severe("Error when trying to get Candidate-Group-Details " + e.getMessage());
		  return null;
	  
	  }
	  
	 
  }
  
  private UserRepresentation getAssigneeDetails(String assigneeName) {
	  
	  refreshKeycloakSession();
	  if (keycloak==null) {
		  LOGGER.warning("Keycloak-Session could not be established - cannot get assignee");
		  return null;
		  
	  }
	  
	  RealmResource realm = keycloak.realms().realm(realmId);
	  //realm.users().search(assigneeName);
	  Optional<UserRepresentation> user = null;
	  try {
		  user = realm
			  .users()
			  .search(assigneeName)
			  .stream()
			  .filter( u -> u.getUsername().equals(assigneeName))
			  .findFirst();
	  } catch (Exception e) {
		  LOGGER.severe("Error when tryoing to get Assignee-Details " + e.getMessage());
		  return null;
		  
	  }
	  if (!user.isPresent()) {
		  LOGGER.warning("Assignee "+assigneeName+" could not be found in Keycloak");
		  return null;
		  
	  }
	  return user.get();
  }
  
  
  private void refreshKeycloakSession() {
	  
	  
	  String serverUrl = System.getenv("KEYCLOAK_SERVER_URL");
	  
	  if (serverUrl==null || serverUrl.isEmpty()) {
		  LOGGER.warning("KEYCLOAK_SERVER_URL not set - keycloak setup not possible");
		  return;
	  }
	  realmId = System.getenv("KEYCLOAK_REALM_ID");
	  if (realmId==null || realmId.isEmpty()) {
		  LOGGER.warning("KEYCLOAK_REALM_ID not set - keycloak setup not possible");
		  return;
	  }
	  String clientId = System.getenv("KEYCLOAK_CLIENT_ID");
	  if (clientId==null || clientId.isEmpty()) {
		  LOGGER.warning("KEYCLOAK_CLIENT_ID not set - keycloak setup not possible");
		  return;
	  }
	  String clientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET");
	  if (clientSecret==null || clientSecret.isEmpty()) {
		  LOGGER.warning("KEYCLOAK_CLIENT_SECRET not set - keycloak setup not possible");
		  return;
	  }
	  
	  
	  if (keycloak == null) {
	  
		  keycloak = KeycloakBuilder.builder()
			       .serverUrl(serverUrl)
			       .realm(realmId)
			       .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
			       .clientId(clientId)
			       .clientSecret(clientSecret)
			       .build();
	  }
	  try {
		  keycloak.tokenManager().grantToken();
	  } catch (Exception e) {
		  LOGGER.severe("Autorization-Error with Keycloak " + e.getMessage());
		  keycloak = null;
	  }
	  
  }
  
  @Override
  public void notify(DelegateTask task) {
    LOGGER.info("Event '" + task.getEventName() + "' received by Task Listener for Task:"
        + " activityId=" + task.getTaskDefinitionKey()
        + ", name='" + task.getName() + "'"
        + ", taskId=" + task.getId()
        + ", assignee='" + task.getAssignee() + "'"
        + ", candidateGroups='" + task.getCandidates() + "'");
    
    
    
    if (task.getAssignee()!=null) {
    	UserRepresentation userDetails = getAssigneeDetails(task.getAssignee());
    	if (userDetails!=null) {
    		notifier.SendTaskNotification(task.getId(), "mytemplate.tmpl", "Dir wurde die Aufgabe " + task.getName()+" zugewiesen", userDetails);
    		LOGGER.info("Hey " + userDetails.getFirstName() + " there is some work for you to do");
    	}
    }
    
    if (!task.getCandidates().isEmpty()) {
    	for (IdentityLink candidate : task.getCandidates()){
    		if (candidate.getUserId() != null) {
    			UserRepresentation userDetails = getAssigneeDetails(candidate.getUserId());
    			if (userDetails!=null) {
    				notifier.SendTaskNotification(task.getId(), "mytemplate.tmpl", "Die Aufgabe " + task.getName()+" steht zur Bearbeitung bereit", userDetails);
    		    	
    				LOGGER.info("Hey " + userDetails.getFirstName()  + " there is some work avilable");
    			}
    		} else if (candidate.getGroupId() != null) {
    			List<UserRepresentation> groupMembers = this.getCandidatesFromGroup(candidate.getGroupId());
    			if (groupMembers != null) {
    				for (UserRepresentation member:groupMembers) {
    					notifier.SendTaskNotification(task.getId(), "mytemplate.tmpl", "Die Aufgabe " + task.getName()+" steht zur Bearbeitung bereit", member);
        		    	
    					LOGGER.info("Notify group member "+member.getFirstName()+" that there is some work avilable");
    				}
    			}
    		}
    	}
    }
  }

}
