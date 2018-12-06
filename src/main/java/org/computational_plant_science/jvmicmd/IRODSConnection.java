package org.computational_plant_science.jvmicmd;

import java.io.File;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy;
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy.SslNegotiationPolicy;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.transfer.TransferControlBlock;
import org.irods.jargon.core.packinstr.TransferOptions.ForceOption;
import org.irods.jargon.core.pub.DataTransferOperations;

/**
 * API onto of jargon to make it easy to connect to an iRods server.
 */
public class IRODSConnection {

  /**
   * Holds active irods account.
   */
  private IRODSAccount irodsAccount;

  /**
   * Link to irods filesystem.
   */
  private IRODSFileSystem irodsFileSystem;

  /**
   * Factory to access objects on the irods system.
   */
  private IRODSAccessObjectFactory irodsAccessObjectFactory;

  /****************************************************************************
   * API.
   ************************************************************************* **/

 /**
  * Open a connection to an irods host.
  *
  * @param host server hostname
  * @param port server port
  * @param username username to connect with
  * @param password user password
  * @param zone iRods zone
  * @param resource iRods connection resource (set to empty String ("")
  *                   for default)
  * @throws JargonException if connection fails
  */
  IRODSConnection(final String host, final int port, final String username,
                    final String password, final String zone,
                    final String resource) throws JargonException {
    this(host, port, username, password, zone, resource,
          SslNegotiationPolicy.NO_NEGOTIATION);
  }

  /**
   * Open a connection to an irods host.
   *
   * @param host server hostname
   * @param port server port
   * @param username username to connect with
   * @param password user password
   * @param zone iRods zone
   * @param resource iRods connection resource (set to empty String ("")
   *                          for default)
   * @param sslPolicy SSL policy to use for the conneciton
   * @throws JargonException if connection fails
   */
  IRODSConnection(final String host, final int port, final String username,
                    final String password, final String zone,
                    final String resource, final SslNegotiationPolicy sslPolicy)
                    throws JargonException {

    irodsAccount = IRODSAccount.instance(host,
                                          port,
                                          username,
                                          password,
                                          "",
                                          zone,
                                          resource);

    ClientServerNegotiationPolicy policy = new ClientServerNegotiationPolicy();
    policy.setSslNegotiationPolicy(sslPolicy);
    irodsAccount.setClientServerNegotiationPolicy(policy);

    irodsFileSystem = IRODSFileSystem.instance();
    irodsAccessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
    irodsAccessObjectFactory.authenticateIRODSAccount(irodsAccount);
  }

  /**
   * Get a link to a directory or file on the iRods server.
   *
   * @param absPath the path to the directory to list. Must be absolute.
   * @return file link
   * @throws IOException if operation fails
   */
  public IRODSFile ls(final Path absPath) throws IOException {

    if (!absPath.isAbsolute()) {
      throw new IOException("Must be absolute file path");
    }

    IRODSFile result = null;
    try {
      result = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount)
            .instanceIRODSFile(absPath.toString());
    } catch (JargonException e) {
      throw new IOException(e);
    }

    return result;

  }

  /**
   * Copy a file or directory from the iRods server to the client
   *  (this computer).
   *
   * @param listener transfer listener
   * @param irodsAbsPath path to the file to get. Must be absolute.
   * @param localPath path to where the copied file should be placed on
   *           the client. Can be relitve or absolute.
   * @param overwrite overwrite the file on the client if it already exists.
   * @param recursive copy files recursively.
   * @throws IOException if operation fails
   */
  public void get(final TransferStatusCallbackListener listener,
                  final Path irodsAbsPath,
                  final Path localPath,
                  final boolean overwrite,
                  final boolean recursive) throws IOException {

    if (!irodsAbsPath.isAbsolute()) {
      throw new IOException("Must be absolute file path");
    }

    try {
      if (recursive) {
        Path localAbsPath = localPath.toAbsolutePath().normalize();

        IRODSFile irodsDirPath = irodsAccessObjectFactory
              .getIRODSFileFactory(irodsAccount)
              .instanceIRODSFile(irodsAbsPath.toString());

        if (!irodsDirPath.isDirectory()) {
          throw new IOException("Directory \"" + irodsDirPath
                                  + "\" does not exist.");
        }

        //Base Dir
        Path localDirPath = localAbsPath.resolve(irodsDirPath.getName());
        localDirPath.toFile().mkdir();

        for (File child : irodsDirPath.listFiles()) {

          Path childAbsPath = Paths.get(child.getAbsolutePath());

          if (child.isDirectory()) {
            Path newDirPath = localDirPath.resolve(child.getName());
            newDirPath.toFile().mkdir();
            get(listener, childAbsPath, newDirPath, overwrite, true);
          } else if (child.isFile()) {

            getFile(listener, localDirPath, childAbsPath, overwrite);

          }
        }
      } else {
          getFile(listener, localPath, irodsAbsPath, overwrite);
      }
    } catch (JargonException e) {
      throw new IOException(e);
    } catch (FileNotFoundException e) {
      throw new IOException(e);
    }

  }

  /**
   * Copy a file or directory from the client (this computer) to the
   *  iRods server.
   *
   * @param listener transfer listener
   * @param localFile path to the file or folder to copy. Can be
   *          relative or absolute.
   * @param irodsAbsPath path to place the file or folder on the iRods server.
              Must be absolute.
   * @param overwrite overwrite the file on the server if it already exists.
   * @param recursive copy files recursively
   * @throws IOException if operation fails
   */
  public void put(final TransferStatusCallbackListener listener,
                  final File localFile,
                  final Path irodsAbsPath,
                  final boolean overwrite,
                  final boolean recursive) throws IOException {

    if (recursive) {

      Files.walkFileTree(Paths.get(localFile.getPath()),
        new SimpleFileVisitor<Path>() {
           @Override
           public FileVisitResult preVisitDirectory(final Path dir,
                                          final BasicFileAttributes attrs)
                                          throws IOException {

             Path irodsPath = rebasePath(dir, irodsAbsPath);

             try {
               IRODSFile irodsDir = irodsAccessObjectFactory
                    .getIRODSFileFactory(irodsAccount)
                    .instanceIRODSFile(irodsPath.toString());

               irodsDir.mkdir();
             } catch (JargonException e) {
               throw new IOException(e);
             }

             return FileVisitResult.CONTINUE;
           }

           @Override
           public FileVisitResult visitFile(final Path file,
                                      final BasicFileAttributes attrs)
                                      throws IOException {
                Path irodsPath = rebasePath(file, irodsAbsPath).getParent();

                try {
                  if (attrs.isRegularFile()) {
                       putFile(listener, file.toFile(), irodsPath, overwrite);
                  }
                } catch (JargonException e) {
                  throw new IOException(e);
                } catch (FileNotFoundException e) {
                  throw new IOException(e);
                }

                return FileVisitResult.CONTINUE;
            }
      });
    } else {

      try {
        putFile(listener, localFile, irodsAbsPath, overwrite);
      } catch (JargonException e) {
        throw new IOException(e);
      } catch (FileNotFoundException e) {
        throw new IOException(e);
      }

    }
  }

  /**
   * Make directory on server.
   *
   * @param irodsAbsPath path to make the directory on the iRods server.
   *          Must be absolute.
   * @return the created directory
   * @throws IOException if operation fails
   */
  public IRODSFile mkDir(final Path irodsAbsPath) throws IOException {

    IRODSFile irodsDir = null;
    try {
      irodsDir = irodsAccessObjectFactory
           .getIRODSFileFactory(irodsAccount)
           .instanceIRODSFile(irodsAbsPath.toString());

      if (irodsDir.exists()) {
        throw new IOException("\"" + irodsAbsPath.toString()
                                  + "\" already exists.");
      }

      irodsDir.mkdir();
    } catch (JargonException e) {
      throw new IOException(e);
    }

    return irodsDir;

  }

  /**
   * Delete folder or file.
   *
   * @param irodsAbsPath path to to delete
   * @param force force delete
   * @throws IOException if operation fails
   */
  public void rm(final Path irodsAbsPath,
                      final boolean force) throws IOException {

    try {
      IRODSFile irodsFile = irodsAccessObjectFactory
           .getIRODSFileFactory(irodsAccount)
           .instanceIRODSFile(irodsAbsPath.toString());

      if (force) {
        irodsFile.deleteWithForceOption();
      } else {
        if (irodsFile.isDirectory()) {
          throw new IOException("\"" + irodsAbsPath.toString()
                                    + "\" is a directry, use the force option"
                                    +  " to delete.");
        } else {
          irodsFile.delete();
        }
      }
    } catch (JargonException e) {
          throw new IOException(e);
    }
  }

  /**
   * Close the connection to the iRods Server.
   **/
  public void close() {
    irodsAccessObjectFactory.closeSessionAndEatExceptions();
  }

  /****************************************************************************
  * Private functions.
  ************************************************************************** **/

  /**
   * @exclude
   */
  private void putFile(final TransferStatusCallbackListener listener,
                       final File localFile,
                       final Path irodsAbsPath,
                       final boolean overwrite)
                       throws JargonException, FileNotFoundException {

      TransferControlBlock tcb = irodsAccessObjectFactory
					.buildDefaultTransferControlBlockBasedOnJargonProperties();

      if (overwrite) {
        tcb.getTransferOptions().setForceOption(ForceOption.USE_FORCE);
      }

      DataTransferOperations dto = irodsAccessObjectFactory
          .getDataTransferOperations(irodsAccount);

			if (!localFile.exists()) {
				throw new FileNotFoundException("\"" + localFile + "\" does not exist.");
			}

      if (! irodsAbsPath.isAbsolute()) {
        throw new FileNotFoundException("Must be absolute file path");
      }

      IRODSFile targetFile = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount)
          .instanceIRODSFile(irodsAbsPath.toString());

			if (targetFile.exists() && !overwrite) {
				throw new FileNotFoundException("irods file \"" + targetFile
           + "\" already exists. use overwrite to force.");
			}

			if (targetFile.isDirectory()) {
				throw new FileNotFoundException("irods file \"" + targetFile + "\" is a directory. Directories are not supported.");
			}

			dto.putOperation(localFile, targetFile, listener, tcb);
  }

  /**
   * @exclude
   */
  private void getFile(final TransferStatusCallbackListener listener,
                       final Path localPath,
                       final Path irodsAbsPath,
                       final boolean overwrite)
                       throws JargonException, FileNotFoundException{

    TransferControlBlock tcb = irodsAccessObjectFactory
      .buildDefaultTransferControlBlockBasedOnJargonProperties();

    if (overwrite) {
      tcb.getTransferOptions().setForceOption(ForceOption.USE_FORCE);
    }

    DataTransferOperations dto = irodsAccessObjectFactory
        .getDataTransferOperations(irodsAccount);

    if (! irodsAbsPath.isAbsolute() ) {
      throw new FileNotFoundException("Must be absolute file path");
    }

    IRODSFile irodsFile = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount)
      .instanceIRODSFile(irodsAbsPath.toString());

    if (!irodsFile.exists()) {
      throw new FileNotFoundException("Error: irods file does not exist");
    }

    if (!irodsFile.isFile()) {
      throw new FileNotFoundException("Error: irods file is a directory, they are not yet supported");
    }

    dto.getOperation(irodsFile, localPath.toFile(), listener, tcb);
  }

  /**
   * Converts a local (absolute or relative) path to a path relative to
   * basePath. Local is cut off at the current directory.
   *
   * @exclude
   **/
  private Path rebasePath(final Path path, final Path basePath){
    if(path.isAbsolute()){
      Path absLocalBasePath = Paths.get(".").toAbsolutePath().normalize();
      return basePath.resolve(absLocalBasePath.relativize(path));
    } else {
      return basePath.resolve(path).normalize();
    }
  }
}
