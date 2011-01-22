package org.testng.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SuiteGenerator;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "java". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */
public class NewTestNGClassWizard extends Wizard implements INewWizard {
	private NewTestNGClassWizardPage m_page;
  private TestNGMethodWizardPage m_methodPage;
  private IStructuredSelection m_selection;

	/**
	 * Constructor for NewTestNGClassWizard.
	 */
	public NewTestNGClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public IStructuredSelection getSelection() {
	  return m_selection;
	}
	
	/**
	 * Adding the pages to the wizard.
	 */
	@Override
  public void addPages() {	  
	  m_methodPage = new TestNGMethodWizardPage();
	  m_page = new NewTestNGClassWizardPage(m_methodPage);
    m_page.init(getSelection());
    addPage(m_page);
    addPage(m_methodPage);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
  public boolean performFinish() {
		String containerName = m_page.getSourceFolder();
		String className = m_page.getClassName();
		String packageName = m_page.getPackageName();
		List<String> methods = m_methodPage != null
		    ? m_methodPage.getSelectedMethods() : Collections.<String>emptyList();
    try {
      return doFinish(containerName, packageName, className, m_page.getXmlFile(), methods,
          new NullProgressMonitor());
    } catch (CoreException e) {
      TestNGPlugin.log(e);
    }
//		IRunnableWithProgress op = new IRunnableWithProgress() {
//			public void run(IProgressMonitor monitor) throws InvocationTargetException {
//				try {
//					doFinish(containerName, fileName, monitor);
//				} catch (CoreException e) {
//					throw new InvocationTargetException(e);
//				} finally {
//					monitor.done();
//				}
//			}
//		};
//		try {
//			getContainer().run(true /* fork */, false /* cancelable */, op);
//		} catch (InterruptedException e) {
//			return false;
//		} catch (InvocationTargetException e) {
//			Throwable realException = e.getTargetException();
//			MessageDialog.openError(getShell(), "Error", realException.getMessage());
//			return false;
//		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file(s) if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 *
	 * @return true if the operation succeeded, false otherwise.
	 */
	private boolean doFinish(String containerName, String packageName, String className,
	    String xmlPath, List<String> methods, IProgressMonitor monitor) throws CoreException {
	  boolean result = true;

	  //
	  // Create XML file at the root directory, if applicable
	  //
	  if (!Utils.isEmpty(xmlPath)) {
	    IFile file = createFile(containerName, "", xmlPath, createXmlContentStream(), monitor);
	    if (file != null) openFile(file, monitor);
	    else result = false;
	  }

	  //
	  // Create Java file
	  //
	  if (result) {
  	  IFile file = createFile(containerName, packageName, className + ".java",
          createJavaContentStream(className, methods), monitor);
  	  if (file != null) openFile(file, monitor);
  	  else result = false;
	  }

	  return result;
	}

  private void openFile(final IFile javaFile, IProgressMonitor monitor) {
    monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
      public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, javaFile, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
  }
	
	private IFile createFile(String containerName, String packageName, String fileName,
	    InputStream contentStream, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating " + fileName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IResource resource = root.findMember(new Path(containerName));
    if (!resource.exists() || !(resource instanceof IContainer)) {
      throwCoreException("Container \"" + containerName + "\" does not exist.");
    }
    IContainer container = (IContainer) resource;
    String fullPath = fileName;
    if (packageName != null && ! "".equals(packageName)) {
      fullPath = packageName.replace(".", File.separator) + File.separatorChar + fileName;
    }
    final IFile result = container.getFile(new Path(fullPath));
    try {
      if (result.exists()) {
        boolean overwrite = MessageDialog.openConfirm(getShell(),
            ResourceUtil.getString("NewTestNGClassWizard.alreadyExists.title"), //$NON-NLS-1$
            ResourceUtil.getFormattedString("NewTestNGClassWizard.alreadyExists.message",
                fullPath)); //$NON-NLS-1$
        if (overwrite) {
          result.setContents(contentStream, true, true, monitor);
        } else {
          return null;
        }
      } else {
        createResourceRecursively(result, monitor);
        result.setContents(contentStream, IFile.FORCE | IFile.KEEP_HISTORY, monitor);
//        result.create(contentStream, true, monitor);
      }
      contentStream.close();
    } catch (IOException e) {
    }
    monitor.worked(1);

    return result;
	}

  protected void createResourceRecursively(IResource resource, IProgressMonitor monitor)
      throws CoreException {
    if (resource == null || resource.exists()) return;
    if (!resource.getParent().exists()) createResourceRecursively(resource.getParent(), monitor);
    switch (resource.getType()) {
    case IResource.FILE:
      ((IFile) resource).create(new ByteArrayInputStream(new byte[0]), true, monitor);
      break;
    case IResource.FOLDER:
      ((IFolder) resource).create(IResource.NONE, true, monitor);
      break;
    case IResource.PROJECT:
      ((IProject) resource).create(monitor);
      ((IProject) resource).open(monitor);
      break;
    }
  }

	/**
	 * Create the content for the Java file.
	 * @param testMethods 
	 */
	private InputStream createJavaContentStream(String className, List<String> testMethods) {
	  StringBuilder imports = new StringBuilder("import org.testng.annotations.Test;\n");
	  StringBuilder methods = new StringBuilder();
	  String dataProvider = "";
	  String signature = "()";

	  //
	  // Configuration methods
	  //
	  for (String a : NewTestNGClassWizardPage.ANNOTATIONS) {
	    if (!"".equals(a) && m_page.containsAnnotation(a)) {
	      imports.append("import org.testng.annotations." + a + ";\n");
	      if ("DataProvider".equals(a)) {
	        dataProvider = "(dataProvider = \"dp\")";
	        methods.append("\n  @DataProvider\n"
	            + "  public Object[][] dp() {\n"
	            + "    return new Object[][] {\n"
	            + "      new Object[] { 1, \"a\" },\n"
              + "      new Object[] { 2, \"b\" },\n"
              + "    };\n"
              + "  }\n"
              );
              ;
            signature = "(Integer n, String s)";
	      } else {
  	      methods.append("  @" + a  + "\n"
  	          + "  public void " + toMethod(a) + "() {\n"
  	          + "  }\n\n"
  	          );
	      }
	    }
	  }

	  //
	  // Test methods
	  //
    for (String m : testMethods) {
      methods.append("\n"
          + "  @Test\n"
          + "  public void " + m + "() {\n"
          + "    throw new RuntimeException(\"Test not implemented\");\n"
          + "  }\n");
    }

    String contents =
	      "package " + m_page.getPackageName() + ";\n\n"
	      + imports
	      + "\n"
	      + "public class " + className + " {\n"
	      ;

    if (testMethods.size() == 0 || !Utils.isEmpty(dataProvider)) {
      contents +=
          "  @Test" + dataProvider + "\n"
  	      + "  public void f" + signature + " {\n"
  	      + "  }\n";
    }

    contents += methods + "}\n";

	  return new ByteArrayInputStream(contents.getBytes());
	}

  /**
   * Create the content for the XML file.
   */
	private InputStream createXmlContentStream() {
	  String cls = m_page.getClassName();
	  String pkg = m_page.getPackageName();
	  String className = Utils.isEmpty(pkg) ? cls : pkg + "." + cls;
	  return new ByteArrayInputStream(
	      SuiteGenerator.createSingleClassSuite(className).getBytes());
	}

	private String toMethod(String a) {
    return Character.toLowerCase(a.charAt(0)) + a.substring(1);
  }

  private void throwCoreException(String message) throws CoreException {
    IStatus status = TestNGStatus.createError(message);		
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    m_selection= currentSelection;
	}

}