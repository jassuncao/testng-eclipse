package org.testng.eclipse.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.testng.eclipse.TestNGPlugin;

public class TestNGStatus extends Status implements IStatus {

  protected TestNGStatus(int severity, String message) {
    super(severity, TestNGPlugin.getPluginId(), message);    
  }
   
  public static IStatus createOK() {
    return new TestNGStatus(IStatus.OK,null);
  }
  
  public static IStatus createError(String message) {
    return new TestNGStatus(IStatus.ERROR, message);
  }

  public static IStatus createWarning(String message) {
    return new TestNGStatus(IStatus.WARNING, message);
  }

  public static IStatus createInfo(String message) {
    return new TestNGStatus(IStatus.INFO, message);
  }

}
