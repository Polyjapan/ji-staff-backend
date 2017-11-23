# JI Staff Backend

The backend of the Japan Impact Staff extranet.

Technologies used:
 - Scala
 - Play2 framework (for Web development in Scala)
 - ReactiveMongo (to communicate with the MongoDB database)
 - Libs to parse JWT identification tokens and insure they are valid

## Dev server

To run a server I personnally use the Play2 extension in IntelliJ IDEA. There must be a SBT command to run it outside any IDE but I don't know it.

## Deployment

To deploy, type `dist` in a SBT shell. It will build the artifact and save it in `target/universal` as a zip file. This zip contains a `bin/` directory. To run the server you just have to run the script contained in this directory.

## Configuration

A sample configuration is included in the `conf/` directory. It contains some information on the application. Please be careful to not push the mongo password on this git repository, though.