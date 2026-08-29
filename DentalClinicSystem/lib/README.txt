Put mysql-connector-j-<version>.jar in this folder.

Rename it to exactly:   mysql-connector.jar

That is the name nbproject/project.properties already points at, so the
project compiles with no extra clicking. If you would rather keep the
original filename, edit this line in nbproject/project.properties:

    file.reference.mysql-connector.jar=lib/mysql-connector.jar

...or just remove and re-add the jar through
Projects panel -> right-click Libraries -> Add JAR/Folder.
