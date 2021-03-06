/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import org.apache.commons.net.ftp.FTPClient;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.FileCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.internal.FtpCopyDelegate;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for {@link FileCommand} implementations that target a FTP/SFTP server
 *
 * @param <C> the generic type of the connection object
 * @since 4.0
 */
public abstract class FtpCommand<C extends FtpFileSystem> extends FileCommand<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpCommand.class);

  /**
   * Creates a new instance
   *
   * @param fileSystem a {@link FtpFileSystem} used as the connection object
   */
  public FtpCommand(C fileSystem) {
    super(fileSystem);
  }

  /**
   * Similar to {@link #getFile(String)} but throwing an {@link IllegalArgumentException} if the
   * {@code filePath} doesn't exists
   *
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes}
   * @throws IllegalArgumentException if the {@code filePath} doesn't exists
   */
  protected FtpFileAttributes getExistingFile(String filePath) {
    return getFile(filePath, true);
  }

  /**
   * Obtains a {@link FtpFileAttributes} for the given {@code filePath} by using the {@link FTPClient#mlistFile(String)} FTP
   * command
   *
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes} or {@code null} if it doesn't exists
   */
  public FtpFileAttributes getFile(String filePath) {
    return getFile(filePath, false);
  }

  protected abstract FtpFileAttributes getFile(String filePath, boolean requireExistence);

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean exists(Path path) {
    return getFile(path.toString()) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Path getBasePath(FileSystem fileSystem) {
    return Paths.get(getCurrentWorkingDirectory());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the {@link Path} to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(Path path) {
    changeWorkingDirectory(path.toString());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the path to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(String path) {
    if (!tryChangeWorkingDirectory(path)) {
      throw new IllegalArgumentException(format("Could not change working directory to '%s'. Path doesn't exists or is not a directory",
                                                path.toString()));
    }
    LOGGER.debug("working directory changed to {}", path);
  }

  /**
   * Attempts to change the current working directory. If it was not possible (for example, because it doesn't exists), it returns
   * {@code false}
   *
   * @param path the path to which you wish to move
   * @return {@code true} if the CWD was changed. {@code false} otherwise
   */
  protected abstract boolean tryChangeWorkingDirectory(String path);

  /**
   * Template method that renames the file at {@code filePath} to {@code newName}.
   * <p>
   * This method performs path resolution and validation and eventually delegates into {@link #doRename(String, String)}, in which
   * the actual renaming implementation is.
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @param overwrite whether to overwrite the target file if it already exists
   */
  protected void rename(String filePath, String newName, boolean overwrite) {
    Path source = resolveExistingPath(filePath);
    Path target = source.getParent().resolve(newName);

    if (exists(target)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(format("'%s' cannot be renamed because '%s' already exists", source, target));
      }

      try {
        fileSystem.delete(target.toString());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", target, source), e);
      }
    }

    try {
      doRename(source.toString(), target.toString());
      LOGGER.debug("{} renamed to {}", filePath, newName);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
    }
  }

  /**
   * Template method which works in tandem with {@link #rename(String, String, boolean)}.
   * <p>
   * Implementations are to perform the actual renaming logic here
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @throws Exception if anything goes wrong
   */
  protected abstract void doRename(String filePath, String newName) throws Exception;

  protected void createDirectory(String directoryPath) {
    final Path path = Paths.get(fileSystem.getBasePath()).resolve(directoryPath);
    FileAttributes targetFile = getFile(directoryPath);

    if (targetFile != null) {
      throw new FileAlreadyExistsException(format("Directory '%s' already exists", path.toAbsolutePath()));
    }

    mkdirs(path);
  }

  /**
   * Performs the base logic and delegates into
   * {@link FtpCopyDelegate#doCopy(FileConnectorConfig, FileAttributes, Path, boolean)} to perform the actual
   * copying logic
   *  @param config the config that is parameterizing this operation
   * @param source the path to be copied
   * @param target the path to the target destination
   * @param overwrite whether to overwrite existing target paths
   * @param createParentDirectory whether to create the target's parent directory if it doesn't exists
   */
  protected final void copy(FileConnectorConfig config, String source, String target, boolean overwrite,
                            boolean createParentDirectory, FtpCopyDelegate delegate) {
    FileAttributes sourceFile = getExistingFile(source);
    Path targetPath = resolvePath(target);
    FileAttributes targetFile = getFile(targetPath.toString());

    if (targetFile != null) {
      if (targetFile.isDirectory()) {
        if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite) {
          throw alreadyExistsException(targetPath);
        } else {
          Path sourcePath = resolvePath(sourceFile.getName());
          if (sourcePath.isAbsolute()) {
            targetPath = targetPath.resolve(sourcePath.getName(sourcePath.getNameCount() - 1));
          } else {
            targetPath = targetPath.resolve(sourceFile.getName());
          }
        }
      } else if (!overwrite) {
        throw alreadyExistsException(targetPath);
      }
    } else {
      if (createParentDirectory) {
        mkdirs(targetPath);
        targetPath = targetPath.resolve(sourceFile.getName());
      } else {
        throw new IllegalPathException(String
            .format("Can't copy '%s' to '%s' because the destination path " + "doesn't exists", sourceFile.getPath(),
                    targetPath.toAbsolutePath()));
      }
    }

    final String cwd = getCurrentWorkingDirectory();
    delegate.doCopy(config, sourceFile, targetPath, overwrite);
    LOGGER.debug("Copied '{}' to '{}'", sourceFile, targetPath);
    changeWorkingDirectory(cwd);
  }

  /**
   * @return the path of the current working directory
   */
  protected abstract String getCurrentWorkingDirectory();
}
