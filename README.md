# OpenAPI Generator for the ignition-codegen library

## Overview
This is a custom implementation of the OpenAPI Generator project for converting an OpenAPI specification into an API client runnable in Ignition.

## What's OpenAPI
The goal of OpenAPI is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection.
When properly described with OpenAPI, a consumer can understand and interact with the remote service with a minimal amount of implementation logic.
Similar to what interfaces have done for lower-level programming, OpenAPI removes the guesswork in calling the service.

Check out [OpenAPI-Spec](https://github.com/OAI/OpenAPI-Specification) for additional information about the OpenAPI project, including additional libraries with support for other languages and more.

## Generating a client
- [Build the project](#build-the-project) or download the ignition-codegen-openapi-generator-jar-with-dependencies from [releases](https://github.com/CorsoSource/IgnitionCodegen/releases)
- Download the OpenAPI Generator jar from [OpenAPITools](https://github.com/OpenAPITools/openapi-generator#13---download-jar)
- Invoke the OpenAPI Generator:

For mac/linux:
```
java -cp /path/to/openapi-generator-cli.jar:/path/to/ignition-codegen.jar org.openapitools.codegen.OpenAPIGenerator generate \
    --generator-name ignition-codegen \
    --input-spec /path/to/openapi.yaml \
    --output ./test
```

For Windows (PowerShell) users, you will need to use `;` instead of `:` in the classpath, e.g.
```
java -cp /path/to/openapi-generator-cli.jar;/path/to/ignition-codegen.jar org.openapitools.codegen.OpenAPIGenerator generate ` 
    --generator-name ignition-codegen `
    --input-spec /path/to/openapi.yaml `
    --output ./test
```
Note that the `--input-spec` parameter can be a YAML file as shown, a JSON file or a URL pointing to an OpenAPI specification. See the [usage documentation](https://openapi-generator.tech/docs/usage).

### Output
Generating a client with the above commands will produce two zip files in the output directory:
```
.
|- docs.zip     // Generated Markdown documentation
|-- README.md   // Overview of the client
|-- ModelDoc.md // One file per OpenAPI model
|-- ApiDoc.md   // One file per API
|- project.zip  // The script resources to be imported into Ignition
|-- ignition
|--- script-python
|---- package-name
|----- api           // One directory per API
|----- models        // One directory per OpenAPI model
|----- api_client    // Static supporting file
|----- configuration // ...
|----- exceptions    // ...
|----- rest          // ...
```

## Using the generated client
### Prerequisites
The following Python packages need to be added to Ignition's `{installDirectory}\user-lib\pylib` directory. They can be
downloaded from [jython-2.5-backports repository](https://github.com/CorsoSource/jython-2.5-backports)
- six
- urllib3
- dateutil

Refer to the documentation in the generated docs.zip file to see how instantiate the client and make a request. 

## Customization
The generated code can be customized without modifying this project by supplying custom templates at runtime.
1. Download the `ignition-codegen` templates folder
2. Modify the templates
3. Run `IgnitionCodegen` and supply the `--template-dir` param with the path to the modified templates

# Build the project
Clone the repo or download the source and run the Maven package command in the root of the project.
```
mvn package
```

Two jar files will be produced in `/target`.
- `ignition-codegen-openapi-generator-x.x.x.jar`
  * This contains only the classes defined in the project.
- `ignition-codegen-openapi-generator-x.x.x-jar-with-dependencies.jar`
  * This contains all dependencies. This is most likely the one you want.

You can now use `ignition-codegen` with [OpenAPI Generator](https://openapi-generator.tech). Be sure to [download the jar](https://github.com/OpenAPITools/openapi-generator#13---download-jar).

## How does it work?
### Project structure

```
.
|- README.md    // this file
|- pom.xml      // build script
|-- src
|--- main
|---- java
|----- com.corsosystems.codegen.IgnitionCodegen.java // generator implementation
|---- resources
|----- ignition-codegen // template files
|----- META-INF
|------ services
|------- org.openapitools.codegen.CodegenConfig
```
`IgnitionCodegen.java` extends the existing `PythonLegacyClientCodegen` generator, which handles most of the work by implementing `CodegenConfig`.
That class has the signature of all values that can be overridden.

The purview of `IgnitionCodegen` is primarily to post-process the output of `PythonLegacyClientCodegen`, reorganize the files to suit Ignition, adding any required supporting files (namely, `resource.json` and `project.json`).

You can also step through IgnitionCodegen.java in a debugger.  Just debug the JUnit
test in DebugCodegenLauncher.  That runs the command line tool and lets you inspect what the code is doing.

You can execute the `java` commands from above while passing different debug flags to show
the object you have available during client generation:

```
# The following additional debug options are available for all codegen targets:
# -DdebugOpenAPI prints the OpenAPI Specification as interpreted by the codegen
# -DdebugModels prints models passed to the template engine
# -DdebugOperations prints operations passed to the template engine
# -DdebugSupportingFiles prints additional data passed to the template engine

java -DdebugOperations -cp /path/to/openapi-generator-cli.jar:/path/to/your.jar org.openapitools.codegen.OpenAPIGenerator generate -g ignition-codegen -i /path/to/openapi.yaml -o ./test
```

Will, for example, output the debug info for operations.
You can use this info in the `api.mustache` file.