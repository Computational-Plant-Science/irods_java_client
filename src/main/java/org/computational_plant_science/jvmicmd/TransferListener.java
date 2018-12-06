package org.computational_plant_science.jvmicmd;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

class TransferListener implements TransferStatusCallbackListener{
  boolean overwrite;
  boolean verbose;

  TransferListener(boolean overwrite, boolean verbose){
    this.overwrite = overwrite;
    this.verbose = verbose;
  }

  public FileStatusCallbackResponse statusCallback(final TransferStatus transferStatus) throws JargonException{
    if(verbose){
      System.out.println("File Transfer Status: " + transferStatus);
    }
    return FileStatusCallbackResponse.CONTINUE;
  }

  public void overallStatusCallback(final TransferStatus transferStatus) throws JargonException{
    if(verbose){
      System.out.println("Overall Transfer Status: " + transferStatus);
    }
  }

  public CallbackResponse transferAsksWhetherToForceOperation(final String irodsAbsolutePath, final boolean isCollection){
    if(verbose){
      System.out.println("Force operation ask for file: " + irodsAbsolutePath);
    }

    if(overwrite){
      return CallbackResponse.YES_FOR_ALL;
    } else {
      return CallbackResponse.NO_FOR_ALL;
    }
  }
}
