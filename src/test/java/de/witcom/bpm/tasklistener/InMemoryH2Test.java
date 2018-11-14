package de.witcom.bpm.tasklistener;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Test case starting an in-memory database-backed Process Engine.
 */
public class InMemoryH2Test {

  @ClassRule
  @Rule
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  private static final String PROCESS_DEFINITION_KEY = "task-listener";

  private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
  
  static {
    LogFactory.useSlf4jLogging(); // MyBatis
  }

  @Before
  public void setup() {
    init(rule.getProcessEngine());
  }

  @Test
  @Deployment(resources = "process.bpmn")

  public void testHappyPath() {
	  ProcessInstance processInstance = processEngine().getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

	  assertThat(processInstance).task("Task_DoSomething");
	  
	  complete(task());
	  
	  assertThat(processInstance).isEnded();
  }
  
  @Test
  @Ignore
  public void testKeycloak() {
	  Keycloak keycloak = KeycloakBuilder.builder()
			       .serverUrl("https://auth.witcom-dev.services/auth")
			       .realm("demo-realm")
			       .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
			       .clientId("camunda-process-engine")
			       .clientSecret("29ac13ea-f644-415b-9bf2-c00baa8324cd")
			       .build();
	  
	  //keycloak = new Keycloak(serverUrl,  realm, null,null, clientId,  authToken,OAuth2Constants.CLIENT_CREDENTIALS,null,null);
	  LOGGER.info(keycloak.toString());
	  keycloak.realms().realm("demo-realm").users().list();
	  RealmResource realm = keycloak.realms().realm("demo-realm");
	  
	  String candiateGroup = "camunda_group-dummy-group";
	  Optional<GroupRepresentation> group = realm.groups().groups(candiateGroup, 0, 2).stream().filter(g-> g.getName().equals(candiateGroup)).findFirst();
	  if (group.isPresent()) {
		  GroupResource g = realm.groups().group(group.get().getId());
		  LOGGER.severe(g.members().toString());
	  }
	  
	  
	  List<UserRepresentation> userlist = realm.users().search("webuser");
	  Optional<UserRepresentation> user = userlist.stream().filter( u -> u.getUsername().equals("webuser")).findFirst();
	  if (user.isPresent()) {
		  LOGGER.info(user.get().getId());
		  //realm.
	  }
	  
	  
	  //keycloak.tokenManager().getAccessToken();
	  
	  
	  
	  //keycloak.realm("demo-realm").users().list();
	  //keycloak.tokenManager().getAccessToken();
	  
  }

}
