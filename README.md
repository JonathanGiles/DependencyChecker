# Maven Dependency Conflict Checker

This tool helps to determine when Maven projects bring in  dependencies that are in conflict with each other. In other 
words, if two projects each have a dependency on the same library, but with different versions, there is likely to be
a conflict that arises because of this, if users were to depend on both Maven projects and their transitive dependencies.

An example report for the [Azure Java SDKs can be viewed online](https://azurejavadocs.z5.web.core.windows.net/dependency-conflicts.html).

## Getting Started

At present there isn't a great deal of convenience available to users of this tool. So, in short, to use it you do the 
following:

1. Clone this repo onto your system
2. Read the instructions below on how to specify your configuration(s).
3. Run the app (by using your locally installed Maven) with the following command: `mvn clean package exec:java`
4. Wait for the application to finish running, and then check the `output` directory.

## Configuration

This project works by reading in a directory of json files, and writing out reports to another directory. The input
format is of one of the following three forms:

**Long form, downloading from Maven repositories:**

```json
[
  {
    "groupId": "com.microsoft.azure",
    "artifactId": "azure-batch",
    "version": "4.0.1"
  },
  {
    "groupId": "com.microsoft.azure",
    "artifactId": "azure-keyvault",
    "version": "1.1.1"
  }
]
```

**Short form, downloading from Maven repositories:**

```json
[
  "com.microsoft.azure:azure-batch:4.0.1",
  "com.microsoft.azure:azure-keyvault:1.1.1"
]
```

**Downloading POM files from other locations:**

```json
[
  {
    "projectName": "azure-sdk-for-java",
    "pomUrls": ["https://raw.githubusercontent.com/Azure/azure-sdk-for-java/master/pom.client.xml"]
  }
]
```

This input format in the first two formats should be recognisable to anyone familiar with Maven POM files - we are simply 
specifying the `groupId`, `artifactId`, and `version` values of a particular release. Including the `version` value is 
optional - if it is not specified the tool will attempt to resolve the latest version of the artifact and will use that. 
This is helpful if you just want to track the latest releases of particular SDKs, as you no longer need to ensure you are
checking the latest version.

The final format above allows us to avoid Maven for our initial configuration, and to instead download POM files from 
other locations, such as GitHub. This allows for a more real-time project 'pulse' to be measured.

There may be any number of projects in this json input. This file should be placed within an `input` directory beside 
the application, and it can be named anything, as long as the file ends with `.json`. After
running the application, a number of reports will be written out to the `output` folder, with the same file name
as the input file. This application can run with any number of input files, with a report being generated separately for 
each input.

### Custom Reporters

This project presently ships with three built-in reporters (`plain-text`, `json`, and `html`). Should there be a use case
where these are not sufficient, it is possible to include your own reporter using the standard Java SPI approach:

1. Create a class that implements the `net.jonathangiles.tool.maven.dependencies.report.Reporter` interface.
2. Create a `net.jonathangiles.tool.maven.dependencies.report.Reporter` text file in the `META-INF/services` directory.
   Inside this text file add the fully-qualified class name (i.e. package name and class name). If you have multiple 
   reporters, add one line for each of them.
3. Create a JAR file containing the implementation and the text file.
4. Run the dependency checker tool with your jar file on the classpath. If no reporters are specified, the dependency 
   checker will by default output all report types. If you only want a subset of the reporters, you may specify the names
   of the reporters you want to run as the arguments into the tool.

## Contributing

This project is open source and in relatively rough form. It is good enough, but there is always room for improvement :-)
If you want to submit improvements to this software, pull requests are always welcome. 