# Maven Dependency Conflict Checker

This tool helps to determine when Maven projects bring in  dependencies that are in conflict with each other. In other 
words, if two projects each have a dependency on the same library, but with different versions, there is likely to be
a conflict that arises because of this, if users were to depend on both Maven projects and their transitive dependencies.

This project works by reading in a directory of json files, and writing out reports to another directory. The input
format is of the following form:

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

This input format should be recognisable to anyone familiar with Maven POM files - we are simply specifying the
`groupId`, `artifactId`, and `version` values of a particular release. This application will then retrieve the pom.xml
file for this project from Maven Central (at present there is only support for Maven Central), and do the required
analysis.

There may be any number of projects in this json input, but shown above is just two. This file should be placed within
an `input` directory beside the application, and it can be named anything, as long as the file ends with `.json`. After
running the application, a JSON and a HTML report will be written out to the `output` folder, with the same file name
as the input file. This application can run with any number of input files, with a report being generated separately for 
each input.

An example report for the [Azure Java SDKs can be viewed online](https://azurejavadocs.z5.web.core.windows.net/dependency-conflicts.html).

## Contributing

This project is open source and in relatively rough form. It is good enough, but there is always room for improvement :-)
If you want to submit improvements to this software, pull requests are always welcome. 