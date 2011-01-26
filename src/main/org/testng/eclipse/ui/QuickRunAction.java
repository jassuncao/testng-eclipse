package org.testng.eclipse.ui;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.ui.tree.TreeItemType;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.LaunchUtil;
import org.testng.eclipse.util.ResourceUtil;


/**
 * A quick launcher from the TestNG viewer.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class QuickRunAction extends Action {
  private final IJavaProject m_javaProject;
  private final ILaunch m_previousRun;
  private final RunInfo m_runInfo;
  private final String m_runMode;
  private final TreeItemType m_treeItemType;
  
  public QuickRunAction(IJavaProject javaProject, ILaunch prevLaunch, RunInfo runInfo, TreeItemType treeItemType, String mode) {
    m_javaProject= javaProject;
    m_previousRun= prevLaunch;
    m_runInfo= runInfo;
    m_runMode= mode;
    m_treeItemType = treeItemType;
    initUI();
  }

  private void initUI() {
    if(ILaunchManager.RUN_MODE.equals(m_runMode)) {
      setText(ResourceUtil.getString("QuickRunAction.run.action.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("QuickRunAction.run.action.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
    }
    else {
      setText(ResourceUtil.getString("QuickRunAction.debug.action.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("QuickRunAction.debug.action.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/debug.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/debug.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/debug.gif")); //$NON-NLS-1$
    }
  }
  
  public void run() {
    switch (m_treeItemType) {
    case SUITE:
      launchSuite();
      break;
    case TEST:
      launchTest();
      break;
    case CLASS:
      launchClass();
      break;      
    default:
      launchMethod();
      break;
    }    
  }

  private void launchMethod() {
    IMethod imethod= null;  
    try {
      imethod= (IMethod) JDTUtil.findElement(m_javaProject, m_runInfo); 
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, 3333, 
          "Cannot find method " + m_runInfo.getMethodDisplay() + " in class " + m_runInfo.getClassName(), //$NON-NLS-1$ $NON-NLS-2$
          jmex));
    }

    if(null == imethod) return;
    /*
     * The runInfo is passed along in order to preserve any 
     * jvm args used in the original launcher when
     * QuickRunAction is activated from the FailureTab to re-run failed 
     * methods. 
     */
    ILaunchConfiguration config = m_previousRun.getLaunchConfiguration();

    m_runInfo.setJvmArgs(ConfigurationHelper.getJvmArgs(config));
    LaunchUtil.launchMethodConfiguration(m_javaProject, 
        imethod, 
        m_runMode, m_runInfo);
  }

  private void launchClass() {    
    IType type = null;  
    try {
      type = (IType) JDTUtil.findElement(m_javaProject,m_runInfo.getClassName());           
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, 3333, 
          "Cannot find class " + m_runInfo.getClassName(), jmex));
    }

    if(null == type) return;   
    ILaunchConfiguration config = m_previousRun.getLaunchConfiguration();

    m_runInfo.setJvmArgs(ConfigurationHelper.getJvmArgs(config));         
    LaunchUtil.launchTypeConfiguration(m_javaProject, type, m_runMode); 
  }

  private void launchTest() {    
    // Not supported     
  }

  private void launchSuite() {
    DebugUITools.launch(m_previousRun.getLaunchConfiguration(), m_previousRun.getLaunchMode());    
  }
}
