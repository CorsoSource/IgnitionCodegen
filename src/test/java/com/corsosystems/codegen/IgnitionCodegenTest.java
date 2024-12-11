package com.corsosystems.codegen;

import org.junit.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import java.util.HashMap;
import java.util.Map;

/***
 * This test allows you to easily launch your code generation software under a debugger.
 * Then run this test under debug mode.  You will be able to step through your java code
 * and then see the results in the out directory.
 *
 * To experiment with debugging your code generator:
 * 1) Set a break point in IgnitionCodegenGenerator.java in the postProcessOperationsWithModels() method.
 * 2) To launch this test in Eclipse: right-click | Debug As | JUnit Test
 *
 */
public class IgnitionCodegenTest {

  // use this test to launch you code generator in the debugger.
  // this allows you to easily set break points in MyclientcodegenGenerator.
  @Test
  public void launchCodeGenerator() {
    // to understand how the 'openapi-generator-cli' module is using 'CodegenConfigurator', have a look at the 'Generate' class:
    // https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-cli/src/main/java/org/openapitools/codegen/cmd/Generate.java

    Map<String, String> globalProperties = new HashMap<>();
    //globalProperties.put("debugModels", "true");
    //globalProperties.put("debugOperations", "true");

    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put(IgnitionCodegenConstants.RESOURCE_LAST_MODIFICATION_ACTOR, "ignition-codegen-test");
    additionalProperties.put(IgnitionCodegenConstants.PROJECT_PARENT, "ParentProject");
    additionalProperties.put(IgnitionCodegenConstants.PROJECT_HAS_PARENT, true); // not a CLI Option but is set by code if PROJECT_PARENT is present

    final CodegenConfigurator configurator = new CodegenConfigurator()
            .setGeneratorName("ignition-codegen") // use this codegen library
            .setInputSpec("https://dev.corsosystems.com/openapi/v1.json")
            //.setInputSpec("https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/2_0/petstore.yaml") // or from the server
            //.setInputSpec("https://developer.opto22.com/static/generated/manage-rest-api/manage-api-public.yaml")
            .setOutputDir("out/ignition-codegen") // output directory
            .setGlobalProperties(globalProperties)
            .setAdditionalProperties(additionalProperties);

    final ClientOptInput clientOptInput = configurator.toClientOptInput();
    DefaultGenerator generator = new DefaultGenerator();
    generator.opts(clientOptInput).generate();
  }
}