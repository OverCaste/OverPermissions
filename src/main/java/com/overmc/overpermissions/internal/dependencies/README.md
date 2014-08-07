#Dependencies
Dependencies are loaded on a requirement basis, to reduce the required filesize.

The initial check for dependencies is based on the plugin configuration, and occurs in the main `OverPermissions.java` class.

The dependencies are downloaded into the /plugins/lib folder.

##Adding a new dependency
You need to add an entry to the `initDependencies` method with the required dependency.

You then need to add jar file to the manifest entry in the maven pom.xml.