/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.sample;

import java.util.function.Function;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.event.FlowableEventSupport;
import org.flowable.engine.ProcessEngine;
import org.flowable.serverless.NoDbProcessEngineConfiguration;
import org.flowable.serverless.ServerlessProcessDefinitionUtil;
import org.flowable.serverless.Util;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;

import experiment.MyJavaDelegate;

/**
 * Build AWS specific jar (with aws suffix) using 'mvn clean package'
 */
@SpringBootApplication
public class DemoApplication implements ApplicationContextInitializer<GenericApplicationContext> {

  public static ProcessEngine processEngine;

  static {

    long start = System.currentTimeMillis();

    NoDbProcessEngineConfiguration engineConfiguration = new NoDbProcessEngineConfiguration();
    processEngine = engineConfiguration.buildProcessEngine();
    BpmnModel bpmnModel = SimpleServiceTask.createSimpleServiceTaskBpmnModel();

    // TODO: move to processor?
    bpmnModel.setEventSupport(new FlowableEventSupport());

    // This is trickier to move
    Util.processFlowElements(bpmnModel.getMainProcess().getFlowElements(), bpmnModel.getMainProcess());

    // END TODO

    ServerlessProcessDefinitionUtil.deployServerlessProcessDefinition(bpmnModel, engineConfiguration);

    long end = System.currentTimeMillis();
    System.out.println("Flowable engine booted up in " + (end - start) + " ms");
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  public Function<FunctionInput, String> startProcess() {
    return value -> {
      String processInstanceId = processEngine.getRuntimeService().startProcessInstanceById(ServerlessProcessDefinitionUtil.PROCESS_DEFINITION_ID).getId();
      return "[Spring Cloud] - new process instance " + processInstanceId + " started. Number of delegation executions = " + MyJavaDelegate.COUNTER.get();
    };
  }

  @Override
  public void initialize(GenericApplicationContext genericApplicationContext) {
    genericApplicationContext.registerBean("startProcess", FunctionRegistration.class,
        () -> new FunctionRegistration<Function<FunctionInput, String>>(startProcess())
            .type(FunctionType.from(FunctionInput.class).to(String.class).getType()));
  }
}