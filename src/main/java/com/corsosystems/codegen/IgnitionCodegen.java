package com.corsosystems.codegen;

import org.apache.commons.io.FilenameUtils;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractPythonCodegen;
import org.openapitools.codegen.languages.PythonLegacyClientCodegen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IgnitionCodegen extends PythonLegacyClientCodegen {
  private final Logger LOGGER = LoggerFactory.getLogger(AbstractPythonCodegen.class);
  private static final String NAME = "ignition-codegen";

  public IgnitionCodegen() {
    super();

    embeddedTemplateDir = templateDir = NAME;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String packagePath() {
    return "ignition" + File.separatorChar + "script-python" + File.separatorChar + super.packagePath();
  }

  @Override
  public String modelFileFolder() {
    return outputFolder() + File.separatorChar + packagePath() + File.separatorChar + "models";
  }

  @Override
  public String apiFileFolder() {
    return outputFolder() + File.separatorChar +  packagePath() + File.separatorChar + "api";
  }

  @Override
  public void processOpts() {
    if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
      setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
    }

    if (additionalProperties.containsKey(CodegenConstants.PROJECT_NAME)) {
      setProjectName((String) additionalProperties.get(CodegenConstants.PROJECT_NAME));
    } else {
      // default: set project based on package name
      // e.g. petstore_api (package name) => petstore-api (project name)
      setProjectName(packageName.replaceAll("_", "-"));
    }

    if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
      setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
    }

    additionalProperties.put(CodegenConstants.PROJECT_NAME, projectName);
    additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
    additionalProperties.put(CodegenConstants.PACKAGE_VERSION, packageVersion);

    // make api and model doc path available in mustache template
    additionalProperties.put("apiDocPath", apiDocPath);
    additionalProperties.put("modelDocPath", modelDocPath);

    additionalProperties.put("actor", NAME);
    additionalProperties.put("timestamp", "2023-01-07T02:03:27Z");
    additionalProperties.put("hintScope", 2);

    if (additionalProperties.containsKey(PACKAGE_URL)) {
      setPackageUrl((String) additionalProperties.get(PACKAGE_URL));
    }

    // check to see if setRecursionLimit is set and whether it's an integer
    if (additionalProperties.containsKey(RECURSION_LIMIT)) {
      try {
        Integer.parseInt((String) additionalProperties.get(RECURSION_LIMIT));
      } catch (NumberFormatException | NullPointerException e) {
        throw new IllegalArgumentException("recursionLimit must be an integer, e.g. 2000.");
      }
    }

    String readmePath = "docs" + File.separatorChar + "README.md";
    String readmeTemplate = "README.mustache";

    supportingFiles.add(new SupportingFile(readmeTemplate, "", readmePath));
    supportingFiles.add(new SupportingFile("api_client.mustache", packagePath(), "api_client.py"));
    supportingFiles.add(new SupportingFile("api_client_resource.mustache", packagePath(), "api_client.json"));
    supportingFiles.add(new SupportingFile("configuration.mustache", packagePath(), "configuration.py"));
    supportingFiles.add(new SupportingFile("configuration_resource.mustache", packagePath(), "configuration.json"));
    supportingFiles.add(new SupportingFile("exceptions.mustache", packagePath(), "exceptions.py"));
    supportingFiles.add(new SupportingFile("exceptions_resource.mustache", packagePath(), "exceptions.json"));
    supportingFiles.add(new SupportingFile("rest.mustache", packagePath(), "rest.py"));
    supportingFiles.add(new SupportingFile("rest_resource.mustache", packagePath(), "rest.json"));

    String modelPath = packagePath() + File.separatorChar + modelPackage.replace('.', File.separatorChar);
    supportingFiles.add(new SupportingFile("__init__model.mustache", modelPath, "__init__.py"));
    supportingFiles.add(new SupportingFile("__init__model_resource.mustache", modelPath, "__init__.json"));

    supportingFiles.add(new SupportingFile("project_manifest.mustache", "", "project.json"));

    modelTemplateFiles.put("model_resource.mustache", ".json");
    apiTemplateFiles.put("api_resource.mustache", ".json");

    // If the package name consists of dots(openapi.client), then we need to create the directory structure like openapi/client with __init__ files.
    String[] packageNameSplits = packageName.split("\\.");
    String currentPackagePath = "";
    for (int i = 0; i < packageNameSplits.length - 1; i++) {
      if (i > 0) {
        currentPackagePath = currentPackagePath + File.separatorChar;
      }
      currentPackagePath = currentPackagePath + packageNameSplits[i];
    }

    modelPackage = this.packageName + "." + modelPackage;
    apiPackage = this.packageName + "." + apiPackage;
  }

  @Override
  public void postProcess() {
    ResourceSigner resourceSigner = new ResourceSigner();

    String clientFolder = outputFolder() + File.separatorChar + packagePath();

    Set<String> resourcePaths = convertToResources(clientFolder, new HashSet<>());
    resourcePaths.forEach(res -> {
      Path pathToSign = Paths.get(res);
      String signedResource = resourceSigner.signResource(pathToSign, NAME, Instant.now().truncatedTo( ChronoUnit.SECONDS ));
      Path resourceJsonPath = Paths.get(res + File.separatorChar + "resource.json");
      try {
        Files.write(resourceJsonPath, signedResource.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    File ignoreFile = new File(outputFolder() + File.separatorChar + ".openapi-generator-ignore");
    ignoreFile.delete();

    deleteDirectory(outputFolder() + File.separatorChar + ".openapi-generator");
    deleteDirectory(outputFolder() + File.separatorChar + "test");

    String docsPath = outputFolder() + File.separatorChar + "docs";
    try {
      zipDirectory(docsPath, outputFolder() + File.separatorChar + "docs.zip");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    deleteDirectory(docsPath);

    String zipPath = outputFolder() + File.separatorChar + "project.zip";
    try {
      zipDirectory(outputFolder() + File.separatorChar + "ignition", zipPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      addFileToZip(outputFolder() + File.separatorChar + "project.json", zipPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    new File(outputFolder() + File.separatorChar + "project.json").delete();

    deleteDirectory(outputFolder() + File.separatorChar + "ignition");

    super.postProcess();
  }

  private static Set<String> convertToResources(String path, Set<String> resourcePaths) {
    File directory = new File(path);
    File[] allFiles = directory.listFiles();

    for (File file : allFiles) {
      if (file.isDirectory() == false) {
        String fullName = file.getName();
        String extension = FilenameUtils.getExtension(fullName);
        String oldName = FilenameUtils.removeExtension(fullName);

        String newName = "";
        if (extension.equals("py")) {
          newName = "code.py";
        } else if (extension.equals("json")) {
          newName = "resource.json";
        }

        String destinationPath = directory.getAbsolutePath() + File.separatorChar + oldName;
        try {
          Files.createDirectories(Paths.get(destinationPath));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        resourcePaths.add(destinationPath);

        String destination = destinationPath + File.separatorChar + newName;
        file.renameTo(new File(destination));
      } else {
        convertToResources(file.getAbsolutePath(), resourcePaths);
      }
    }
    return resourcePaths;
  }
  private static void zipDirectory(String path, String destination) throws IOException {
    FileOutputStream fos = null;
    fos = new FileOutputStream(destination);

    ZipOutputStream zipOut = new ZipOutputStream(fos);

    File fileToZip = new File(path);
    zipFile(fileToZip, fileToZip.getName(), zipOut);

    zipOut.close();
    fos.close();
  }
  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    if (fileToZip.isDirectory()) {
      if (fileName.endsWith("/")) {
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.closeEntry();
      } else {
        zipOut.putNextEntry(new ZipEntry(fileName + "/"));
        zipOut.closeEntry();
      }
      File[] children = fileToZip.listFiles();
      for (File childFile : children) {
        zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
      }
      return;
    }
    FileInputStream fis = new FileInputStream(fileToZip);
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zipOut.write(bytes, 0, length);
    }
    fis.close();
  }
  private static void addFileToZip(String filePath, String destinationPath) throws IOException {
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");

    Path path = Paths.get(destinationPath);
    URI uri = URI.create("jar:" + path.toUri());

    FileSystem fs = FileSystems.newFileSystem(uri, env);
    Path nf = fs.getPath("project.json");
    Files.write(nf, Files.readAllBytes(Paths.get(filePath)), StandardOpenOption.CREATE);
    fs.close();
  }
  private static void deleteDirectory(String path) {

    File directory = new File(path);

    if(directory.isDirectory()) {
      File[] files = directory.listFiles();

      // if the directory contains any file
      if(files != null) {
        for(File file : files) {

          // recursive call if the subdirectory is non-empty
          deleteDirectory(file.getAbsolutePath());
        }
      }
    }
    directory.delete();
  }
}
