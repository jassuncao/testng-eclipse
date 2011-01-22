package org.testng.eclipse.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.launch.components.Filters.ITypeFilter;
import org.testng.eclipse.refactoring.FindTestsRunnableContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Utils {
  public static class JavaElement {
    public IJavaProject m_project;
    public IPackageFragmentRoot packageFragmentRoot;
    public IPackageFragment packageFragment;
    public ICompilationUnit compilationUnit;
    public String sourceFolder;

    public String getPath() {
      String result = null;
      if (compilationUnit != null) {
        result = resourceToPath(compilationUnit);
      } else if (packageFragmentRoot != null) {
        result = resourceToPath(packageFragmentRoot);
      } else if (packageFragment != null) {
        result = resourceToPath(packageFragment);
      } else {
        result = resourceToPath(getProject());
      }
      return result;
    }

    public IJavaProject getProject() {
      if (m_project != null) return m_project;
      else if (packageFragmentRoot != null) return packageFragmentRoot.getJavaProject();
      else if (packageFragment != null) return packageFragment.getJavaProject();
      else if (compilationUnit != null) return compilationUnit.getJavaProject();
      else throw new AssertionError("Couldn't find a project");
    }

    private String resourceToPath(IJavaElement element) {
      return ((IResource) element.getAdapter(IResource.class)).getFullPath().toOSString();
    }

    public String getPackageName() {
      String result = null;
      if (packageFragment != null) {
        result = packageFragment.getElementName();
      } else if (compilationUnit != null) {
        try {
          result = compilationUnit.getPackageDeclarations()[0].getElementName();
        } catch (JavaModelException e) {
          // ignore
        }
      }

      return result;
    }

    public String getClassName() {
      String result = null;
      if (compilationUnit != null) {
        result = compilationUnit.getElementName();
        if (result.endsWith(".java")) {
          result = result.substring(0, result.length() - ".java".length());
        }
      }
      return result;
    }


  }

  /**
   * @return all the ITypes included in the current selection.
   */
  public static List<IType> findSelectedTypes(IWorkbenchPage page) {
    return findTypes(Utils.getSelectedJavaElements(page));
  }
  
  public static List<IType> findTypes(List<JavaElement> elements) {
    List<IType> result = Lists.newArrayList();

    for (JavaElement pp : elements) {
      if (pp.compilationUnit != null) {
        try {
          result.addAll(Arrays.asList(pp.compilationUnit.getAllTypes()));
        } catch (JavaModelException e) {
          e.printStackTrace();
        }
      } else {
        IPackageFragmentRoot pfr = pp.packageFragmentRoot;
        IPackageFragment pf = pp.packageFragment;
        try {
          ITypeFilter filter = new ITypeFilter() {
            public boolean accept(IType type) {
              return true;
            }
          };

          IRunnableContext context = new FindTestsRunnableContext();
          if (pf != null) {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pf }, filter)));
          } else if (pfr != null) {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pfr }, filter)));
          } else {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pp.getProject() }, filter)));
          }
        }
        catch(InvocationTargetException ex) {
          ex.printStackTrace();
        }
        catch(InterruptedException ex) {
          // ignore
        }
      }
    }

    return result;
  }

  /**
   * Known limitation of this method: if the selection is happening in the Navigator,
   * the selected tree item will contain a path that I'm not bothering turning into
   * Java elements: instead, I just return the entire project. Therefore, right clicking
   * on a file in the Navigator and selecting "Convert to TestNG" will cause the refactoring
   * to apply to the entire project.
   *
   * TODO: handle the Navigator as well as the Package Explorer.
   *
   * @param page
   * @return
   */
  public static List<JavaElement> getSelectedJavaElements() {
    return getSelectedJavaElements(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
  }

  public static List<JavaElement> getSelectedJavaElements(IWorkbenchPage page) {
    List<JavaElement> result = Lists.newArrayList();
    ISelection selection = page.getSelection();

    if (selection instanceof TreeSelection) {
      //
      // If we have a selection, extract the Java information from it
      //
      TreeSelection sel = (TreeSelection) selection;
      for (Iterator it = sel.iterator(); it.hasNext();) {
        result.add(convertToJavaElement(it.next()));
      }
    } else {
      //
      // No selection, extract the Java information from the current editor, if applicable
      //
      
      IEditorPart editor = page.getActiveEditor();
      if (editor != null) {
        ITypeRoot root = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
        if (root != null && root.getElementType() == IJavaElement.COMPILATION_UNIT) {
          result.add(convertToJavaElement(root));
        }
      }
    }

    return result;
  }

  private static JavaElement convertToJavaElement(Object element) {
    JavaElement result = new JavaElement();
    if (element instanceof IFile) {
      IJavaElement je = JavaCore.create((IFile) element);
      if (je instanceof ICompilationUnit) {
        result.compilationUnit = (ICompilationUnit) je;
      }
    }
    else if (element instanceof ICompilationUnit) {
      result.compilationUnit = (ICompilationUnit) element;
    } else if (element instanceof IPackageFragment) {
      result.packageFragment = (IPackageFragment) element;
    } else if (element instanceof IPackageFragmentRoot) {
      result.packageFragmentRoot = (IPackageFragmentRoot) element;
    } else if (element instanceof IJavaProject) {
      result.m_project = (IJavaProject) element;
    } else if (element instanceof IProject) {
      result.m_project = JavaCore.create((IProject) element);
    }

    // If we have a project, initialize the source folder too
    if (result.compilationUnit != null) {
      IResource resource = (IResource) result.compilationUnit.getAdapter(IResource.class);
      for (IClasspathEntry entry : Utils.getSourceFolders(result.getProject())) {
        String source = entry.getPath().toOSString();
        if (resource.getFullPath().toString().startsWith(source)) {
          result.sourceFolder = source;
          break;
        }
      }
    }

    return result;
  }

  /**
   * @return the source folders for this Java project.
   */
  public static List<IClasspathEntry> getSourceFolders(IJavaProject jp) {
    List<IClasspathEntry> result = Lists.newArrayList();
    try {
      for (IClasspathEntry entry : jp.getRawClasspath()) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          result.add(entry);
        }
      }
    } catch (JavaModelException e) {
      e.printStackTrace();
    }
    return result;
  }
  
  public static boolean isVisible(IMember member, IPackageFragment pack) throws JavaModelException {

    int type= member.getElementType();
    if  (type == IJavaElement.INITIALIZER ||  (type == IJavaElement.METHOD && member.getElementName().startsWith("<"))) { //$NON-NLS-1$
      return false;
    }

    int otherflags= member.getFlags();
    IType declaringType= member.getDeclaringType();
    if (Flags.isPublic(otherflags) || (declaringType != null && declaringType.isInterface())) {
      return true;
    } else if (Flags.isPrivate(otherflags)) {
      return false;
    }

    IPackageFragment otherpack= (IPackageFragment) member.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
    return (pack != null && otherpack != null && pack.getElementName().equals(otherpack.getElementName()));
  }
}
