package org.computational_plant_science.jvmicmd;

import java.io.IOException;
import java.nio.file.Paths;
import java.io.File;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.io.IRODSFile;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(description = "Java wrapper for iRODS iCommands.",
         name = "jvmicmd", mixinStandardHelpOptions = true, version = "1.0 alpha",
         subcommands = {
           Get.class,
           Put.class,
           Ls.class,
           Test.class,
           Mkdir.class,
           Rm.class
         })
class Main implements Runnable {
    @Parameters(index = "0", description = "hostname")
    private String host;

    @Parameters(index = "1", description = "username")
    private String username;

    @Option(names = "-p", description = "port (default: ${DEFAULT-VALUE})")
    private int port = 1247;

    @Option(names = "-s", description = "password (default: \"${DEFAULT-VALUE}\")")
    private String password = "";

    @Option(names = "-z", description = "zone (default: \"${DEFAULT-VALUE}\")")
    private String zone = "tempZone";

    @Option(names = "-r", description = "resource (default: \"${DEFAULT-VALUE}\")")
    private String resource = "";

    @Option(names = "-v", description = "verbose")
    public boolean verbose = false;

    public static void main(String[] args) throws Exception {
      CommandLine.run(new Main(), args);
    }

    public void run() {
      CommandLine.usage(new Main(), System.out);
    }

    public IRODSConnection connect() {
      IRODSConnection irodsConnection = null;

      try {
        irodsConnection = new IRODSConnection(host,
                                              port,
                                              username,
                                              password,
                                              zone,
                                              resource);
      } catch (JargonException e) {

        System.out.println("Connection Error " + e);
        System.exit(1);

      }

      return irodsConnection;
    }
}

@Command(name="test", description = "Test connection to server.")
class Test implements Runnable {
  @ParentCommand
  private Main main;

  public void run() {
      System.out.println("Connection: " + main.connect().toString());
      System.out.println("Connnection: SUCESS");
  }
}

@Command(name="get",
 description="Copy a file or directory from the iRods server to this comptuer.")
class Get implements Runnable {
  @ParentCommand
  private Main main;

  @Parameters(index = "0",
    description = "Path to the file to get. Must be absolute.")
  private String irodsPath;

  @Option(names = "-o",
   description = "Path to where the copied file should be placed on the" +
    "client. Can be relitve or absolute.")
  private String localPath = ".";

  @Option(names = "-f",
   description = "Overwrite the file on the client if it already exists.")
  private boolean force = false;

  @Option(names = "-r", description = "Copy files recursively.")
  private boolean recursive = false;

  public void run() {
    try {
      main.connect().get(new TransferListener(force,main.verbose),
                         Paths.get(irodsPath),
                         Paths.get(localPath),
                         force,
                         recursive);
    } catch ( IOException e){
      System.out.println("Error:" + e.getMessage());
      System.exit(1);
    }
  }
}

@Command(name="put",
 description="Copy a file or directory form this computer to the iRods server")
class Put implements Runnable {
  @ParentCommand
  private Main main;

  @Parameters(index = "0",
   description = "Path to the file or folder to copy to the server."
    + " Can be relative or absolute.")
  private String localPath;

  @Parameters(index = "1",
   description = "Path to place the file or folder on the iRods server."
    + " Must be absolute.")
  private String irodsPath;

  @Option(names = "-f",
   description = "Overwrite the file on the server if it already exists.")
  private boolean force = false;

  @Option(names = "-r", description = "Copy files recursively.")
  private boolean recursive = false;

  public void run() {
    try {
      main.connect().put(new TransferListener(force,main.verbose),
                         new File(localPath),
                         Paths.get(irodsPath),
                         force,recursive);
    } catch ( IOException e){
      System.out.println("Error:" + e.getMessage());
      System.exit(1);
    }
  }
}

@Command(name="ls",description="List the files in a directory.")
class Ls implements Runnable {
  @ParentCommand
  private Main main;

  @Parameters(index = "0",
   description = "Path to list. Must be absolute.")
  private String path;

  public void run() {
    try {
      IRODSFile parent = main.connect().ls(Paths.get(path));

      StringBuilder sb = new StringBuilder();
      sb.append(parent);
      for (String child : parent.list()) {
        sb.append("\n");
        sb.append("\t");
        sb.append(child);
      }
      System.out.println(sb.toString());
    } catch ( IOException e){
      System.out.println("Error:" + e.getMessage());
      System.exit(1);
    }
  }
}

@Command(name="mkdir",description="Create a directory on the iRods server.")
class Mkdir implements Runnable {
  @ParentCommand
  private Main main;

  @Parameters(index = "0",
   description = "Path to directory to create. Must be absolute.")
  private String path;

  public void run() {
    try {
      main.connect().mkDir(Paths.get(path));
    } catch ( IOException e){
      System.out.println("Error:" + e.getMessage());
      System.exit(1);
    }
  }
}

@Command(name="rm",description="Delete directory or file on the iRods server.")
class Rm implements Runnable {
  @ParentCommand
  private Main main;

  @Parameters(index = "0",
   description = "Path to directory to create. Must be absolute.")
  private String path;

  @Option(names = "-f",
   description = "Force deletion.")
  private boolean force = false;

  public void run() {
    try {
      main.connect().rm(Paths.get(path),force);
    } catch ( IOException e){
      System.out.println("Error:" + e.getMessage());
      System.exit(1);
    }
  }
}
